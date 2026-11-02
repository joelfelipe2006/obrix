package br.com.obrix.usuario;

import br.com.obrix.catalogo.CategoriaServico;
import br.com.obrix.catalogo.RegiaoAtuacao;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Quem executa. Unico perfil com CPF, porque e o unico sujeito a cobranca
 * de comissao.
 *
 * <p>Categorias e regioes sao agregacao: existem no catalogo independentemente
 * dele. Mas um prestador precisa de ao menos uma de cada — validado no cadastro,
 * nao apenas no diagrama.</p>
 */
@Entity
@Table(name = "prestador")
@PrimaryKeyJoinColumn(name = "id")
public class Prestador extends Usuario {

    @Column(nullable = false, length = 14, unique = true)
    private String cpf;

    @Column(name = "descricao_profissional", length = 500)
    private String descricaoProfissional;

    /** Campo calculado, mantido por desempenho na busca; recalculado a cada avaliacao. */
    @Column(name = "nota_media", nullable = false, precision = 3, scale = 2)
    private BigDecimal notaMedia = BigDecimal.ZERO;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "prestador_categoria",
            joinColumns = @JoinColumn(name = "prestador_id"),
            inverseJoinColumns = @JoinColumn(name = "categoria_id"))
    private Set<CategoriaServico> categorias = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "prestador_regiao",
            joinColumns = @JoinColumn(name = "prestador_id"),
            inverseJoinColumns = @JoinColumn(name = "regiao_id"))
    private Set<RegiaoAtuacao> regioes = new LinkedHashSet<>();

    protected Prestador() { }

    public Prestador(String nome, String email, String telefone, String senhaHash,
                     String cpf, String descricaoProfissional) {
        super(nome, email, telefone, senhaHash);
        this.cpf = cpf;
        this.descricaoProfissional = descricaoProfissional;
    }

    @Override
    public String getPerfil() { return "PRESTADOR"; }

    public void adicionarCategoria(CategoriaServico c) { categorias.add(c); }
    public void removerCategoria(CategoriaServico c)   { categorias.remove(c); }
    public void adicionarRegiao(RegiaoAtuacao r)       { regioes.add(r); }
    public void removerRegiao(RegiaoAtuacao r)         { regioes.remove(r); }

    /**
     * Regra que sustenta o RF12: o sistema recusa uma solicitacao cuja categoria
     * ou regiao nao estejam entre as que este profissional declarou atender.
     */
    public boolean atende(CategoriaServico categoria, RegiaoAtuacao regiao) {
        return categorias.contains(categoria) && regioes.contains(regiao);
    }

    public void recalcularNotaMedia(double media, long quantidade) {
        this.notaMedia = quantidade == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(media).setScale(2, RoundingMode.HALF_UP);
    }

    public String getCpf() { return cpf; }
    public String getCpfMascarado() {
        if (cpf == null || cpf.length() < 4) return "***";
        return "***.***.***-" + cpf.substring(cpf.length() - 2);
    }
    public String getDescricaoProfissional() { return descricaoProfissional; }
    public void setDescricaoProfissional(String d) { this.descricaoProfissional = d; }
    public BigDecimal getNotaMedia() { return notaMedia; }
    public Set<CategoriaServico> getCategorias() { return categorias; }
    public Set<RegiaoAtuacao> getRegioes() { return regioes; }
}
