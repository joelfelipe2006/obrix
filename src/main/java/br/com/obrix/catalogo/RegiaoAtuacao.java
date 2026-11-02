package br.com.obrix.catalogo;

import jakarta.persistence.*;

/**
 * Regiao escolhida numa lista, nunca digitada: a comparacao e por identidade,
 * entao a busca nao falha por acento, caixa ou grafia.
 */
@Entity
@Table(name = "regiao_atuacao")
public class RegiaoAtuacao {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80) private String cidade;
    @Column(nullable = false, length = 80) private String bairro;

    protected RegiaoAtuacao() { }

    public String descrever() { return bairro + " - " + cidade; }

    public Long getId() { return id; }
    public String getCidade() { return cidade; }
    public String getBairro() { return bairro; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegiaoAtuacao outra)) return false;
        return id != null && id.equals(outra.id);
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}
