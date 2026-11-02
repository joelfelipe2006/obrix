package br.com.obrix.catalogo.dto;

import java.util.List;

public final class CatalogoDtos {

    private CatalogoDtos() { }

    public record CategoriaResponse(Long id, String nome, String descricao) { }

    public record RegiaoResponse(Long id, String cidade, String bairro, String descricao) { }

    public record CatalogoResponse(List<CategoriaResponse> categorias, List<RegiaoResponse> regioes) { }
}
