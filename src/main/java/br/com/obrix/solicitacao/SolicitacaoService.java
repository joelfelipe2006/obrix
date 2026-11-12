package br.com.obrix.solicitacao;

import br.com.obrix.avaliacao.Avaliacao;
import br.com.obrix.avaliacao.AvaliacaoRepository;
import br.com.obrix.catalogo.CategoriaServico;
import br.com.obrix.catalogo.CategoriaServicoRepository;
import br.com.obrix.catalogo.RegiaoAtuacao;
import br.com.obrix.catalogo.RegiaoAtuacaoRepository;
import br.com.obrix.cobranca.Cobranca;
import br.com.obrix.cobranca.CobrancaRepository;
import br.com.obrix.notificacao.NotificacaoService;
import br.com.obrix.shared.NaoAutorizadoException;
import br.com.obrix.shared.NaoEncontradoException;
import br.com.obrix.shared.RegraDeNegocioException;
import br.com.obrix.usuario.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Onde vivem as regras de negocio do ciclo da solicitacao.
 *
 * <p>A regra que define o produto esta no metodo {@link #confirmar}: e o unico
 * ponto do sistema que cria uma {@link Cobranca}. Nenhum outro evento gera
 * cobranca — quebrar isso descaracteriza o Obrix.</p>
 */
@Service
public class SolicitacaoService {

    private static final Logger log = LoggerFactory.getLogger(SolicitacaoService.class);

    private final SolicitacaoRepository solicitacoes;
    private final ClienteRepository clientes;
    private final PrestadorRepository prestadores;
    private final CategoriaServicoRepository categorias;
    private final RegiaoAtuacaoRepository regioes;
    private final CobrancaRepository cobrancas;
    private final AvaliacaoRepository avaliacoes;
    private final NotificacaoService notificacoes;

    private final BigDecimal percentualComissao;
    private final int prazoConfirmacaoDias;

    public SolicitacaoService(SolicitacaoRepository solicitacoes,
                              ClienteRepository clientes,
                              PrestadorRepository prestadores,
                              CategoriaServicoRepository categorias,
                              RegiaoAtuacaoRepository regioes,
                              CobrancaRepository cobrancas,
                              AvaliacaoRepository avaliacoes,
                              NotificacaoService notificacoes,
                              @Value("${obrix.comissao.percentual}") BigDecimal percentualComissao,
                              @Value("${obrix.confirmacao.prazo-dias}") int prazoConfirmacaoDias) {
        this.solicitacoes = solicitacoes;
        this.clientes = clientes;
        this.prestadores = prestadores;
        this.categorias = categorias;
        this.regioes = regioes;
        this.cobrancas = cobrancas;
        this.avaliacoes = avaliacoes;
        this.notificacoes = notificacoes;
        this.percentualComissao = percentualComissao;
        this.prazoConfirmacaoDias = prazoConfirmacaoDias;
    }

    public int getPrazoConfirmacaoDias() { return prazoConfirmacaoDias; }
    public BigDecimal getPercentualComissao() { return percentualComissao; }

    // ------------------------------------------------------------------
    // Criacao e consulta
    // ------------------------------------------------------------------

    @Transactional
    public Solicitacao criar(Long clienteId, Long prestadorId, Long categoriaId, Long regiaoId,
                             String descricao, LocalDate dataDesejada, Endereco enderecoExecucao) {

        Cliente cliente = clientes.findById(clienteId)
                .orElseThrow(() -> new NaoEncontradoException("Cliente", clienteId));
        Prestador prestador = prestadores.buscarComCatalogo(prestadorId)
                .orElseThrow(() -> new NaoEncontradoException("Prestador", prestadorId));
        CategoriaServico categoria = categorias.findById(categoriaId)
                .orElseThrow(() -> new NaoEncontradoException("Categoria", categoriaId));
        RegiaoAtuacao regiao = regioes.findById(regiaoId)
                .orElseThrow(() -> new NaoEncontradoException("Regiao", regiaoId));

        // RF12: o sistema impede o envio quando a categoria ou a regiao nao
        // correspondem as cadastradas pelo prestador.
        if (!prestador.atende(categoria, regiao)) {
            throw new RegraDeNegocioException("FORA_DE_ATUACAO",
                    "Este profissional nao atende essa categoria nessa regiao.");
        }
        if (!prestador.podeOperar()) {
            throw new RegraDeNegocioException("PRESTADOR_INDISPONIVEL",
                    "Este profissional nao esta disponivel no momento.");
        }

        Solicitacao solicitacao = new Solicitacao(cliente, prestador, categoria, regiao,
                descricao, dataDesejada, enderecoExecucao);
        solicitacoes.save(solicitacao);

        notificacoes.criar(prestador, "Voce recebeu uma nova solicitacao de " + cliente.getNome() + ".");
        log.info("Solicitacao {} criada: cliente={} prestador={}",
                solicitacao.getId(), clienteId, prestadorId);
        return solicitacao;
    }

    @Transactional(readOnly = true)
    public Solicitacao buscar(Long id, Long usuarioId) {
        Solicitacao s = solicitacoes.buscarComHistorico(id)
                .orElseThrow(() -> new NaoEncontradoException("Solicitacao", id));
        exigirParteInteressada(s, usuarioId);
        return s;
    }

    @Transactional(readOnly = true)
    public Page<Solicitacao> doCliente(Long clienteId, Pageable pageable) {
        return solicitacoes.findByClienteIdOrderByDataCriacaoDesc(clienteId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Solicitacao> doPrestador(Long prestadorId, Pageable pageable) {
        return solicitacoes.findByPrestadorIdOrderByDataCriacaoDesc(prestadorId, pageable);
    }

    // ------------------------------------------------------------------
    // Transicoes — cada uma verifica quem pode fazer o que
    // ------------------------------------------------------------------

    @Transactional
    public Solicitacao aceitar(Long id, Long prestadorId) {
        Solicitacao s = carregarDoPrestador(id, prestadorId);
        s.aceitar();
        notificacoes.criar(s.getCliente(),
                s.getPrestador().getNome() + " aceitou a sua solicitacao. Os contatos foram liberados.");
        return s;
    }

    @Transactional
    public Solicitacao recusar(Long id, Long prestadorId, String motivo) {
        Solicitacao s = carregarDoPrestador(id, prestadorId);
        s.recusar(motivo);
        notificacoes.criar(s.getCliente(), "Sua solicitacao foi recusada. Motivo: " + motivo);
        return s;
    }

    @Transactional
    public Solicitacao iniciarExecucao(Long id, Long prestadorId) {
        Solicitacao s = carregarDoPrestador(id, prestadorId);
        s.iniciarExecucao();
        notificacoes.criar(s.getCliente(), "O servico foi iniciado.");
        return s;
    }

    @Transactional
    public Solicitacao registrarConclusao(Long id, Long prestadorId, BigDecimal valorFinal) {
        Solicitacao s = carregarDoPrestador(id, prestadorId);
        s.registrarConclusao(valorFinal);
        notificacoes.criar(s.getCliente(),
                "O profissional registrou a conclusao. Confirme ou conteste em ate "
                        + prazoConfirmacaoDias + " dias.");
        return s;
    }

    /**
     * RF18 + RF21. Unico ponto do sistema que cria cobranca.
     *
     * <p>A unicidade tambem e garantida por restricao no banco: um duplo clique
     * ou um retry de rede nao pode gerar duas cobrancas.</p>
     */
    @Transactional
    public Solicitacao confirmar(Long id, Long clienteId) {
        Solicitacao s = carregarDoCliente(id, clienteId);
        return concluir(s, false);
    }

    @Transactional
    public Solicitacao contestar(Long id, Long clienteId, String motivo) {
        Solicitacao s = carregarDoCliente(id, clienteId);
        s.contestar(motivo);
        notificacoes.criar(s.getPrestador(),
                "O cliente contestou a conclusao. Motivo: " + motivo
                        + ". Nenhuma cobranca foi gerada.");
        return s;
    }

    @Transactional
    public Solicitacao cancelar(Long id, Long usuarioId, String motivo) {
        Solicitacao s = solicitacoes.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Solicitacao", id));
        exigirParteInteressada(s, usuarioId);

        boolean ehCliente = s.getCliente().getId().equals(usuarioId);
        s.cancelar(motivo, ehCliente ? AutorTransicao.CLIENTE : AutorTransicao.PRESTADOR);

        notificacoes.criar(ehCliente ? s.getPrestador() : s.getCliente(),
                "A solicitacao foi cancelada. Motivo: " + motivo);
        return s;
    }

    /** Confirmacao por decurso de prazo (RF19), disparada pela tarefa agendada. */
    @Transactional
    public Solicitacao confirmarAutomaticamente(Long id) {
        Solicitacao s = solicitacoes.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Solicitacao", id));
        if (!s.prazoConfirmacaoExpirado(prazoConfirmacaoDias)) {
            throw new RegraDeNegocioException("PRAZO_NAO_EXPIRADO",
                    "O prazo de confirmacao ainda nao venceu.");
        }
        return concluir(s, true);
    }

    private Solicitacao concluir(Solicitacao s, boolean automatica) {
        s.confirmar(automatica);

        if (cobrancas.existsBySolicitacaoId(s.getId())) {
            throw new RegraDeNegocioException("COBRANCA_DUPLICADA",
                    "Esta solicitacao ja possui cobranca.");
        }
        Cobranca cobranca = new Cobranca(s, s.getValorFinal(), percentualComissao);
        cobrancas.save(cobranca);
        // Lado inverso do 1:1: sem isto a resposta do proprio /confirmar sai sem a cobranca.
        s.vincularCobranca(cobranca);

        notificacoes.criar(s.getPrestador(),
                "Servico confirmado. Comissao de " + cobranca.getValorComissao() + " registrada.");
        if (!automatica) {
            notificacoes.criar(s.getCliente(), "Servico concluido. Voce ja pode avaliar o profissional.");
        }

        log.info("Solicitacao {} concluida (automatica={}), comissao={}",
                s.getId(), automatica, cobranca.getValorComissao());
        return s;
    }

    // ------------------------------------------------------------------
    // Avaliacao
    // ------------------------------------------------------------------

    @Transactional
    public Avaliacao avaliar(Long solicitacaoId, Long clienteId, int nota, String comentario) {
        Solicitacao s = carregarDoCliente(solicitacaoId, clienteId);

        if (s.getStatus() != StatusSolicitacao.CONCLUIDA) {
            throw new RegraDeNegocioException("AVALIACAO_NAO_LIBERADA",
                    "A avaliacao so e liberada apos a confirmacao da conclusao.");
        }
        if (avaliacoes.existsBySolicitacaoId(solicitacaoId)) {
            throw new RegraDeNegocioException("AVALIACAO_DUPLICADA",
                    "Este servico ja foi avaliado.");
        }

        Avaliacao avaliacao = avaliacoes.save(new Avaliacao(s, nota, comentario));
        s.vincularAvaliacao(avaliacao);

        // A nota media e recalculada na mesma transacao que grava a avaliacao,
        // nunca em job separado: o perfil publico nao pode ficar desatualizado.
        Prestador p = s.getPrestador();
        p.recalcularNotaMedia(
                avaliacoes.mediaDoPrestador(p.getId()),
                avaliacoes.quantidadeDoPrestador(p.getId()));

        notificacoes.criar(p, "Voce recebeu uma nova avaliacao: " + nota + " estrelas.");
        return avaliacao;
    }

    // ------------------------------------------------------------------
    // Autorizacao por objeto
    // ------------------------------------------------------------------

    private Solicitacao carregarDoCliente(Long id, Long clienteId) {
        Solicitacao s = solicitacoes.buscarComHistorico(id)
                .orElseThrow(() -> new NaoEncontradoException("Solicitacao", id));
        if (!s.getCliente().getId().equals(clienteId)) {
            throw new NaoAutorizadoException("Esta acao e exclusiva do cliente dono da solicitacao.");
        }
        return s;
    }

    private Solicitacao carregarDoPrestador(Long id, Long prestadorId) {
        Solicitacao s = solicitacoes.buscarComHistorico(id)
                .orElseThrow(() -> new NaoEncontradoException("Solicitacao", id));
        if (!s.getPrestador().getId().equals(prestadorId)) {
            throw new NaoAutorizadoException("Esta acao e exclusiva do profissional destinatario.");
        }
        return s;
    }

    private void exigirParteInteressada(Solicitacao s, Long usuarioId) {
        boolean parte = s.getCliente().getId().equals(usuarioId)
                || s.getPrestador().getId().equals(usuarioId);
        if (!parte) {
            throw new NaoAutorizadoException("Voce nao participa desta solicitacao.");
        }
    }
}
