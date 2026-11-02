package br.com.obrix.catalogo;

import br.com.obrix.catalogo.dto.CatalogoDtos.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalogo")
public class CatalogoController {

    private final CategoriaServicoRepository categorias;
    private final RegiaoAtuacaoRepository regioes;

    public CatalogoController(CategoriaServicoRepository categorias, RegiaoAtuacaoRepository regioes) {
        this.categorias = categorias;
        this.regioes = regioes;
    }

    /** Publico: a busca e livre, a conta so e exigida na hora de solicitar. */
    @GetMapping
    public CatalogoResponse catalogo() {
        return new CatalogoResponse(
                categorias.findAllByOrderByNomeAsc().stream()
                        .map(c -> new CategoriaResponse(c.getId(), c.getNome(), c.getDescricao()))
                        .toList(),
                regioes.findAllByOrderByCidadeAscBairroAsc().stream()
                        .map(r -> new RegiaoResponse(r.getId(), r.getCidade(), r.getBairro(), r.descrever()))
                        .toList());
    }
}
