package br.com.obrix.solicitacao;

import br.com.obrix.cobranca.CobrancaRepository;
import br.com.obrix.seguranca.SessaoUsuario;
import br.com.obrix.seguranca.UsuarioLogado;
import br.com.obrix.solicitacao.dto.SolicitacaoDtos.*;
import br.com.obrix.usuario.Endereco;
import br.com.obrix.usuario.dto.Dtos.EnderecoDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Acoes de estado como sub-recursos explicitos, e nao um PUT generico:
 * o nome do endpoint diz qual transicao esta sendo pedida.
 */
@RestController
@RequestMapping("/api/v1/solicitacoes")
public class SolicitacaoController {

    private static final int TAMANHO_PAGINA = 20;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SolicitacaoService servico;
    private final SolicitacaoMapper mapper;
    private final CobrancaRepository cobrancas;
    private final UsuarioLogado logado;

    public SolicitacaoController(SolicitacaoService servico, SolicitacaoMapper mapper,
                                 CobrancaRepository cobrancas, UsuarioLogado logado) {
        this.servico = servico;
        this.mapper = mapper;
        this.cobrancas = cobrancas;
        this.logado = logado;
    }

    // ------------------------------------------------------------------
    // Criacao e listagem
    // ------------------------------------------------------------------

    @PostMapping
    public ResponseEntity<SolicitacaoResponse> criar(@Valid @RequestBody CriarRequest req,
                                                     HttpServletRequest http) {
        Long clienteId = logado.idCliente(http);
        Solicitacao s = servico.criar(clienteId, req.prestadorId(), req.categoriaId(),
                req.regiaoId(), req.descricao(), req.dataDesejada(),
                converter(req.enderecoExecucao()));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.paraResposta(s, clienteId));
    }

    /** Lista do painel: a origem depende do perfil de quem pediu. */
    @GetMapping
    public List<SolicitacaoResponse> minhas(@RequestParam(defaultValue = "0") int pagina,
                                            HttpServletRequest http) {
        SessaoUsuario u = logado.atual(http);
        var pageable = PageRequest.of(pagina, TAMANHO_PAGINA);
        var pagina1 = "CLIENTE".equals(u.perfil())
                ? servico.doCliente(u.id(), pageable)
                : servico.doPrestador(u.id(), pageable);
        return pagina1.getContent().stream().map(s -> mapper.paraResposta(s, u.id())).toList();
    }

    @GetMapping("/{id}")
    public SolicitacaoResponse detalhe(@PathVariable Long id, HttpServletRequest http) {
        Long usuarioId = logado.id(http);
        return mapper.paraResposta(servico.buscar(id, usuarioId), usuarioId);
    }

    // ------------------------------------------------------------------
    // Transicoes
    // ------------------------------------------------------------------

    @PostMapping("/{id}/aceitar")
    public SolicitacaoResponse aceitar(@PathVariable Long id, HttpServletRequest http) {
        Long prestadorId = logado.idPrestador(http);
        return mapper.paraResposta(servico.aceitar(id, prestadorId), prestadorId);
    }

    @PostMapping("/{id}/recusar")
    public SolicitacaoResponse recusar(@PathVariable Long id,
                                       @Valid @RequestBody MotivoRequest req,
                                       HttpServletRequest http) {
        Long prestadorId = logado.idPrestador(http);
        return mapper.paraResposta(servico.recusar(id, prestadorId, req.motivo()), prestadorId);
    }

    @PostMapping("/{id}/iniciar")
    public SolicitacaoResponse iniciar(@PathVariable Long id, HttpServletRequest http) {
        Long prestadorId = logado.idPrestador(http);
        return mapper.paraResposta(servico.iniciarExecucao(id, prestadorId), prestadorId);
    }

    @PostMapping("/{id}/concluir")
    public SolicitacaoResponse concluir(@PathVariable Long id,
                                        @Valid @RequestBody ConclusaoRequest req,
                                        HttpServletRequest http) {
        Long prestadorId = logado.idPrestador(http);
        return mapper.paraResposta(
                servico.registrarConclusao(id, prestadorId, req.valorFinal()), prestadorId);
    }

    /** O unico clique do sistema que gera cobranca. */
    @PostMapping("/{id}/confirmar")
    public SolicitacaoResponse confirmar(@PathVariable Long id, HttpServletRequest http) {
        Long clienteId = logado.idCliente(http);
        return mapper.paraResposta(servico.confirmar(id, clienteId), clienteId);
    }

    @PostMapping("/{id}/contestar")
    public SolicitacaoResponse contestar(@PathVariable Long id,
                                         @Valid @RequestBody MotivoRequest req,
                                         HttpServletRequest http) {
        Long clienteId = logado.idCliente(http);
        return mapper.paraResposta(servico.contestar(id, clienteId, req.motivo()), clienteId);
    }

    @PostMapping("/{id}/cancelar")
    public SolicitacaoResponse cancelar(@PathVariable Long id,
                                        @Valid @RequestBody MotivoRequest req,
                                        HttpServletRequest http) {
        Long usuarioId = logado.id(http);
        return mapper.paraResposta(servico.cancelar(id, usuarioId, req.motivo()), usuarioId);
    }

    @PostMapping("/{id}/avaliar")
    public SolicitacaoResponse avaliar(@PathVariable Long id,
                                       @Valid @RequestBody AvaliarRequest req,
                                       HttpServletRequest http) {
        Long clienteId = logado.idCliente(http);
        servico.avaliar(id, clienteId, req.nota(), req.comentario());
        return mapper.paraResposta(servico.buscar(id, clienteId), clienteId);
    }

    // ------------------------------------------------------------------
    // Extrato do prestador (RF22)
    // ------------------------------------------------------------------

    @GetMapping("/extrato")
    public ExtratoResponse extrato(@RequestParam(defaultValue = "0") int pagina,
                                   HttpServletRequest http) {
        Long prestadorId = logado.idPrestador(http);
        var page = cobrancas.doPrestador(prestadorId, PageRequest.of(pagina, TAMANHO_PAGINA));

        List<ItemExtrato> itens = page.getContent().stream()
                .map(c -> new ItemExtrato(
                        c.getSolicitacao().getId(),
                        c.getSolicitacao().getCategoria().getNome(),
                        c.getSolicitacao().getCliente().getNome(),
                        c.getDataGeracao().toLocalDate().format(DATA),
                        c.getValorServico(),
                        c.getValorComissao()))
                .toList();

        // Totais de todas as cobrancas do prestador, nao so da pagina atual.
        BigDecimal totalComissao = cobrancas.totalComissaoDoPrestador(prestadorId);
        BigDecimal totalServicos = cobrancas.totalServicosDoPrestador(prestadorId);

        return new ExtratoResponse(itens, totalComissao, totalServicos);
    }

    private Endereco converter(EnderecoDto dto) {
        return new Endereco(dto.cep(), dto.logradouro(), dto.numero(),
                dto.bairro(), dto.cidade(), dto.estado());
    }
}
