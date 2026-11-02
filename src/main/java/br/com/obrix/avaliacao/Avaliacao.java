package br.com.obrix.avaliacao;

import br.com.obrix.solicitacao.Solicitacao;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Uma nota por servico, e somente depois da confirmacao do cliente (RF23). */
@Entity
@Table(name = "avaliacao")
public class Avaliacao {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_id", nullable = false, unique = true)
    private Solicitacao solicitacao;

    @Column(nullable = false)
    private int nota;

    @Column(length = 500)
    private String comentario;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora = LocalDateTime.now();

    protected Avaliacao() { }

    public Avaliacao(Solicitacao solicitacao, int nota, String comentario) {
        if (!validarNota(nota)) {
            throw new IllegalArgumentException("A nota deve estar entre 1 e 5.");
        }
        this.solicitacao = solicitacao;
        this.nota = nota;
        this.comentario = comentario;
    }

    public static boolean validarNota(int nota) { return nota >= 1 && nota <= 5; }

    public Long getId() { return id; }
    public Solicitacao getSolicitacao() { return solicitacao; }
    public int getNota() { return nota; }
    public String getComentario() { return comentario; }
    public LocalDateTime getDataHora() { return dataHora; }
}
