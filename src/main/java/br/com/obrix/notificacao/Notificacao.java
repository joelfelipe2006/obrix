package br.com.obrix.notificacao;

import br.com.obrix.usuario.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Aviso interno de mudanca de status (RF27). */
@Entity
@Table(name = "notificacao")
public class Notificacao {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 300)
    private String mensagem;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora = LocalDateTime.now();

    @Column(nullable = false)
    private boolean lida = false;

    protected Notificacao() { }

    public Notificacao(Usuario usuario, String mensagem) {
        this.usuario = usuario;
        this.mensagem = mensagem;
    }

    public void marcarComoLida() { this.lida = true; }

    public Long getId() { return id; }
    public String getMensagem() { return mensagem; }
    public LocalDateTime getDataHora() { return dataHora; }
    public boolean isLida() { return lida; }
}
