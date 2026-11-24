package br.com.obrix.solicitacao;

import br.com.obrix.avaliacao.Avaliacao;
import br.com.obrix.catalogo.CategoriaServico;
import br.com.obrix.catalogo.RegiaoAtuacao;
import br.com.obrix.cobranca.Cobranca;
import br.com.obrix.usuario.Cliente;
import br.com.obrix.usuario.Endereco;
import br.com.obrix.usuario.Prestador;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Classe central do dominio. E ela que percorre o ciclo de vida do RF16, e e
 * das suas transicoes que nascem o historico, a cobranca e a avaliacao.
 *
 * <p>Cada metodo publico abaixo e uma transicao permitida. O que nao esta aqui
 * nao pode acontecer — e o que esta, so acontece a partir dos estados previstos
 * no mapa {@link #TRANSICOES}.</p>
 */
@Entity
@Table(name = "solicitacao")
public class Solicitacao {

    /**
     * O mapa de transicoes permitidas. E a regra do sistema em forma de dado:
     * qualquer caminho fora daqui falha com erro de negocio.
     */
    private static final Map<StatusSolicitacao, EnumSet<StatusSolicitacao>> TRANSICOES =
            new EnumMap<>(StatusSolicitacao.class);

    static {
        TRANSICOES.put(StatusSolicitacao.SOLICITADA, EnumSet.of(
                StatusSolicitacao.ACEITA, StatusSolicitacao.RECUSADA, StatusSolicitacao.CANCELADA));
        TRANSICOES.put(StatusSolicitacao.ACEITA, EnumSet.of(
                StatusSolicitacao.EM_EXECUCAO, StatusSolicitacao.CANCELADA));
        TRANSICOES.put(StatusSolicitacao.EM_EXECUCAO, EnumSet.of(
                StatusSolicitacao.AGUARDANDO_CONFIRMACAO));
        TRANSICOES.put(StatusSolicitacao.AGUARDANDO_CONFIRMACAO, EnumSet.of(
                StatusSolicitacao.CONCLUIDA, StatusSolicitacao.CONTESTADA));
        // A unica saida de uma contestacao e um novo registro de conclusao,
        // que devolve o servico para AGUARDANDO_CONFIRMACAO (RF32).
        TRANSICOES.put(StatusSolicitacao.CONTESTADA, EnumSet.of(
                StatusSolicitacao.AGUARDANDO_CONFIRMACAO));
        TRANSICOES.put(StatusSolicitacao.RECUSADA, EnumSet.noneOf(StatusSolicitacao.class));
        TRANSICOES.put(StatusSolicitacao.CANCELADA, EnumSet.noneOf(StatusSolicitacao.class));
        TRANSICOES.put(StatusSolicitacao.CONCLUIDA, EnumSet.noneOf(StatusSolicitacao.class));
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Prestador prestador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaServico categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "regiao_id", nullable = false)
    private RegiaoAtuacao regiao;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(name = "data_desejada", nullable = false)
    private LocalDate dataDesejada;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusSolicitacao status;

    @Column(name = "valor_final", precision = 12, scale = 2)
    private BigDecimal valorFinal;

    /**
     * Quanto o cliente diz ter pago. Quem paga a comissao e o prestador, entao o
     * valor dele sozinho nao basta: a cobranca so nasce quando os dois batem.
     * Fica nulo na confirmacao automatica, que usa o valorFinal.
     */
    @Column(name = "valor_confirmado_cliente", precision = 12, scale = 2)
    private BigDecimal valorConfirmadoCliente;

    /** Motivo da transicao corrente. O historico guarda o de cada uma. */
    @Column(length = 500)
    private String motivo;

    /** Marco de onde o prazo do RF19 conta. Sem ele, o requisito nao teria origem. */
    @Column(name = "data_registro_conclusao")
    private LocalDateTime dataRegistroConclusao;

    @Column(name = "confirmacao_automatica", nullable = false)
    private boolean confirmacaoAutomatica = false;

    /** Evita corrida entre acoes concorrentes (cliente cancela enquanto prestador aceita). */
    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    /** Copia do endereco, nao referencia ao cadastro do cliente. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "cep",        column = @Column(name = "exec_cep")),
            @AttributeOverride(name = "logradouro", column = @Column(name = "exec_logradouro")),
            @AttributeOverride(name = "numero",     column = @Column(name = "exec_numero")),
            @AttributeOverride(name = "bairro",     column = @Column(name = "exec_bairro")),
            @AttributeOverride(name = "cidade",     column = @Column(name = "exec_cidade")),
            @AttributeOverride(name = "estado",     column = @Column(name = "exec_estado"))
    })
    private Endereco enderecoExecucao;

    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dataHora ASC, id ASC")
    private List<HistoricoStatus> historico = new ArrayList<>();

    @OneToOne(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Cobranca cobranca;

    @OneToOne(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Avaliacao avaliacao;

    protected Solicitacao() { }

    public Solicitacao(Cliente cliente, Prestador prestador, CategoriaServico categoria,
                       RegiaoAtuacao regiao, String descricao, LocalDate dataDesejada,
                       Endereco enderecoExecucao) {
        this.cliente = cliente;
        this.prestador = prestador;
        this.categoria = categoria;
        this.regiao = regiao;
        this.descricao = descricao;
        this.dataDesejada = dataDesejada;
        this.enderecoExecucao = enderecoExecucao.copia();
        this.status = StatusSolicitacao.SOLICITADA;
        // Nasce ja com um registro: criar a solicitacao e a primeira transicao.
        // E por isso que a multiplicidade do historico e 1..*, nunca zero.
        this.historico.add(new HistoricoStatus(this, null, StatusSolicitacao.SOLICITADA,
                null, AutorTransicao.CLIENTE));
    }

    // ------------------------------------------------------------------
    // Transicoes
    // ------------------------------------------------------------------

    public void aceitar() {
        mover(StatusSolicitacao.ACEITA, null, AutorTransicao.PRESTADOR);
    }

    public void recusar(String motivo) {
        exigirMotivo(motivo);
        mover(StatusSolicitacao.RECUSADA, motivo, AutorTransicao.PRESTADOR);
    }

    public void iniciarExecucao() {
        mover(StatusSolicitacao.EM_EXECUCAO, null, AutorTransicao.PRESTADOR);
    }

    /**
     * O prestador informa que terminou e qual foi o valor. Nao fecha o ciclo
     * financeiro: nenhuma cobranca nasce aqui.
     */
    public void registrarConclusao(BigDecimal valorFinal) {
        if (valorFinal == null || valorFinal.signum() <= 0) {
            throw new br.com.obrix.shared.RegraDeNegocioException(
                    "VALOR_INVALIDO", "O valor final deve ser maior que zero.");
        }
        this.valorFinal = valorFinal;
        this.dataRegistroConclusao = LocalDateTime.now();
        mover(StatusSolicitacao.AGUARDANDO_CONFIRMACAO, null, AutorTransicao.PRESTADOR);
    }

    /**
     * O unico evento que fecha o servico. Quem chama decide se partiu do cliente
     * (RF18) ou do decurso de prazo (RF19) — e isso fica gravado.
     */
    public void confirmar(boolean automatica) {
        this.confirmacaoAutomatica = automatica;
        mover(StatusSolicitacao.CONCLUIDA, null,
                automatica ? AutorTransicao.SISTEMA : AutorTransicao.CLIENTE);
    }

    /**
     * O cliente informa quanto pagou. Se bate com o valor registrado pelo
     * prestador, o valor fica gravado e devolve {@code true}: quem chama segue
     * para a confirmacao. Se diverge, a solicitacao e contestada com motivo
     * automatico e devolve {@code false} — sem confirmacao, sem cobranca.
     *
     * <p>A comparacao usa compareTo: 280 e 280.00 sao o mesmo valor.</p>
     */
    public boolean informarValorPago(BigDecimal valorPago) {
        if (valorPago == null || valorPago.signum() <= 0) {
            throw new br.com.obrix.shared.RegraDeNegocioException(
                    "VALOR_INVALIDO", "O valor pago deve ser maior que zero.");
        }
        // Fora de AGUARDANDO_CONFIRMACAO nao ha o que confirmar: o erro fala da
        // confirmacao pedida, nao da contestacao que uma divergencia geraria.
        if (status != StatusSolicitacao.AGUARDANDO_CONFIRMACAO) {
            throw new TransicaoInvalidaException(status, StatusSolicitacao.CONCLUIDA);
        }
        if (valorFinal.compareTo(valorPago) == 0) {
            this.valorConfirmadoCliente = valorPago;
            return true;
        }
        contestar("Divergencia de valor: o cliente informou " + reais(valorPago)
                + " e o profissional registrou " + reais(valorFinal));
        return false;
    }

    private static String reais(BigDecimal valor) {
        var simbolos = java.text.DecimalFormatSymbols.getInstance(java.util.Locale.of("pt", "BR"));
        var formato = new java.text.DecimalFormat("#,##0.00", simbolos);
        return "R$ " + formato.format(valor.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    public void contestar(String motivo) {
        exigirMotivo(motivo);
        mover(StatusSolicitacao.CONTESTADA, motivo, AutorTransicao.CLIENTE);
    }

    public void cancelar(String motivo, AutorTransicao autor) {
        // Antes do aceite, o caminho do prestador e recusar: cancelar e para
        // quem ja assumiu o servico. Assim a recusa fica registrada como recusa.
        if (autor == AutorTransicao.PRESTADOR && status == StatusSolicitacao.SOLICITADA) {
            throw new br.com.obrix.shared.RegraDeNegocioException("USE_RECUSAR",
                    "Antes de aceitar, o profissional nao cancela: use Recusar.");
        }
        exigirMotivo(motivo);
        mover(StatusSolicitacao.CANCELADA, motivo, autor);
    }

    private void mover(StatusSolicitacao novo, String motivo, AutorTransicao autor) {
        if (!TRANSICOES.getOrDefault(status, EnumSet.noneOf(StatusSolicitacao.class)).contains(novo)) {
            throw new TransicaoInvalidaException(status, novo);
        }
        StatusSolicitacao anterior = this.status;
        this.status = novo;
        this.motivo = motivo;
        this.historico.add(new HistoricoStatus(this, anterior, novo, motivo, autor));
    }

    private void exigirMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new br.com.obrix.shared.RegraDeNegocioException(
                    "MOTIVO_OBRIGATORIO", "Informe o motivo.");
        }
    }

    // ------------------------------------------------------------------
    // Consultas de regra
    // ------------------------------------------------------------------

    /**
     * Atributo derivado: nao fica guardado, e calculado a partir do historico.
     * Serve para desligar a confirmacao automatica depois de uma contestacao —
     * contestar e protecao permanente, nao adiamento de sete dias.
     */
    @Transient
    public boolean isFoiContestada() {
        return historico.stream()
                .anyMatch(h -> h.getStatusNovo() == StatusSolicitacao.CONTESTADA);
    }

    /** O relogio do RF19 so corre enquanto ninguem contestou. */
    public boolean prazoConfirmacaoExpirado(int prazoDias) {
        if (status != StatusSolicitacao.AGUARDANDO_CONFIRMACAO) return false;
        if (isFoiContestada()) return false;
        if (dataRegistroConclusao == null) return false;
        return dataRegistroConclusao.plusDays(prazoDias).isBefore(LocalDateTime.now());
    }

    @Transient
    public LocalDateTime getPrazoConfirmacao(int prazoDias) {
        if (dataRegistroConclusao == null || isFoiContestada()) return null;
        return dataRegistroConclusao.plusDays(prazoDias);
    }

    public boolean podeSerCancelada() {
        return status == StatusSolicitacao.SOLICITADA || status == StatusSolicitacao.ACEITA;
    }

    /** Contato so e liberado depois do aceite (RF14, RNF04). */
    public boolean contatoLiberado() {
        return status != StatusSolicitacao.SOLICITADA
                && status != StatusSolicitacao.RECUSADA
                && status != StatusSolicitacao.CANCELADA;
    }

    void vincularCobranca(Cobranca cobranca) { this.cobranca = cobranca; }
    public void vincularAvaliacao(Avaliacao avaliacao) { this.avaliacao = avaliacao; }

    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public Prestador getPrestador() { return prestador; }
    public CategoriaServico getCategoria() { return categoria; }
    public RegiaoAtuacao getRegiao() { return regiao; }
    public String getDescricao() { return descricao; }
    public LocalDate getDataDesejada() { return dataDesejada; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public StatusSolicitacao getStatus() { return status; }
    public BigDecimal getValorFinal() { return valorFinal; }
    public BigDecimal getValorConfirmadoCliente() { return valorConfirmadoCliente; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getDataRegistroConclusao() { return dataRegistroConclusao; }
    public boolean isConfirmacaoAutomatica() { return confirmacaoAutomatica; }
    public Endereco getEnderecoExecucao() { return enderecoExecucao; }
    public List<HistoricoStatus> getHistorico() { return historico; }
    public Cobranca getCobranca() { return cobranca; }
    public Avaliacao getAvaliacao() { return avaliacao; }
}
