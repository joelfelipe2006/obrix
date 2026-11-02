package br.com.obrix.catalogo;

import jakarta.persistence.*;

/** Catalogo carregado por seed (RF07) — nao ha perfil administrativo no escopo. */
@Entity
@Table(name = "categoria_servico")
public class CategoriaServico {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60, unique = true)
    private String nome;

    @Column(length = 200)
    private String descricao;

    protected CategoriaServico() { }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoriaServico outra)) return false;
        return id != null && id.equals(outra.id);
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}
