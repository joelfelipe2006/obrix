package br.com.obrix.catalogo.dto;

import java.math.BigDecimal;
import java.util.List;

public final class CatalogoDtos {

    private CatalogoDtos() { }

    public record CategoriaResponse(Long id, String nome, String descricao) { }

    public record RegiaoResponse(Long id, String cidade, String bairro, String descricao) { }

    /**
     * Alem das listas, as regras que o front-end mostra nos textos e usa na
     * previa da comissao: assim a tela nunca diverge da configuracao do servidor.
     */
    public record CatalogoResponse(List<CategoriaResponse> categorias,
                                   List<RegiaoResponse> regioes,
                                   BigDecimal percentualComissao,
                                   int prazoConfirmacaoDias) { }
}
