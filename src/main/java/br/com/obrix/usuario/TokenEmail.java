package br.com.obrix.usuario;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * E classe, e nao campo, porque o token tem vida propria: tipo, criacao,
 * validade e data de uso. Guardado com hash e valido uma vez so (RF04, RF28).
 */
@Entity
@Table(name = "token_email")
public class TokenEmail {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, length = 100)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TipoToken tipo;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "usado_em")
    private LocalDateTime usadoEm;

    protected TokenEmail() { }

    public TokenEmail(Usuario usuario, String tokenHash, TipoToken tipo, int validadeHoras) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.tipo = tipo;
        this.expiraEm = this.criadoEm.plusHours(validadeHoras);
    }

    public boolean valido() {
        return usadoEm == null && expiraEm.isAfter(LocalDateTime.now());
    }

    public void marcarComoUsado() { this.usadoEm = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getTokenHash() { return tokenHash; }
    public TipoToken getTipo() { return tipo; }
}
