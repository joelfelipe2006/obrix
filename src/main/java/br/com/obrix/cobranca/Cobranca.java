package br.com.obrix.cobranca;

import br.com.obrix.solicitacao.Solicitacao;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Registro da comissao devida pelo prestador (RF21).
 *
 * <p>Simulada: nao ha integracao bancaria, por exclusao expressa do escopo na TAP.
 * O percentual fica gravado aqui dentro, e nao numa configuracao global — assim
 * mudar a politica amanha nao altera nenhuma cobranca historica.</p>
 */
@Entity
@Table(name = "cobranca")
public class Cobranca {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_id", nullable = false, unique = true)
    private Solicitacao solicitacao;

    @Column(name = "valor_servico", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorServico;

    @Column(name = "percentual_comissao", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualComissao;

    @Column(name = "valor_comissao", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorComissao;

    @Column(name = "data_geracao", nullable = false)
    private LocalDateTime dataGeracao = LocalDateTime.now();

    protected Cobranca() { }

    public Cobranca(Solicitacao solicitacao, BigDecimal valorServico, BigDecimal percentualComissao) {
        this.solicitacao = solicitacao;
        this.valorServico = valorServico.setScale(2, RoundingMode.HALF_UP);
        this.percentualComissao = percentualComissao;
        this.valorComissao = calcularComissao(this.valorServico, percentualComissao);
    }

    /** Dinheiro sempre em BigDecimal, escala 2, HALF_UP. Nunca double. */
    public static BigDecimal calcularComissao(BigDecimal valorServico, BigDecimal percentual) {
        return valorServico
                .multiply(percentual)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public Solicitacao getSolicitacao() { return solicitacao; }
    public BigDecimal getValorServico() { return valorServico; }
    public BigDecimal getPercentualComissao() { return percentualComissao; }
    public BigDecimal getValorComissao() { return valorComissao; }
    public LocalDateTime getDataGeracao() { return dataGeracao; }
}
