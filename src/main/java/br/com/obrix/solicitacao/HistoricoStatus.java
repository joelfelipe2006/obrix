package br.com.obrix.solicitacao;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Trilha de auditoria (RNF17). O status da solicitacao guarda o estado atual;
 * o historico guarda o caminho — o que mudou, quando, por que e quem fez.
 *
 * <p>O motivo mora aqui, e nao so na solicitacao: recusa, cancelamento e
 * contestacao compartilhariam o mesmo campo, e a segunda transicao apagaria
 * o texto da primeira.</p>
 */
@Entity
@Table(name = "historico_status")
public class HistoricoStatus {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private Solicitacao solicitacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 30)
    private StatusSolicitacao statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 30)
    private StatusSolicitacao statusNovo;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora = LocalDateTime.now();

    @Column(length = 500)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AutorTransicao autor;

    protected HistoricoStatus() { }

    HistoricoStatus(Solicitacao solicitacao, StatusSolicitacao anterior,
                    StatusSolicitacao novo, String motivo, AutorTransicao autor) {
        this.solicitacao = solicitacao;
        this.statusAnterior = anterior;
        this.statusNovo = novo;
        this.motivo = motivo;
        this.autor = autor;
    }

    public Long getId() { return id; }
    public StatusSolicitacao getStatusAnterior() { return statusAnterior; }
    public StatusSolicitacao getStatusNovo() { return statusNovo; }
    public LocalDateTime getDataHora() { return dataHora; }
    public String getMotivo() { return motivo; }
    public AutorTransicao getAutor() { return autor; }
}
