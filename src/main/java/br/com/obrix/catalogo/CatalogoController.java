package br.com.obrix.catalogo;

import br.com.obrix.catalogo.dto.CatalogoDtos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/catalogo")
public class CatalogoController {

    private final CategoriaServicoRepository categorias;
    private final RegiaoAtuacaoRepository regioes;
    private final BigDecimal percentualComissao;
    private final int prazoConfirmacaoDias;

    /** As regras vem das mesmas propriedades que o SolicitacaoService usa. */
    public CatalogoController(CategoriaServicoRepository categorias, RegiaoAtuacaoRepository regioes,
                              @Value("${obrix.comissao.percentual}") BigDecimal percentualComissao,
                              @Value("${obrix.confirmacao.prazo-dias}") int prazoConfirmacaoDias) {
        this.categorias = categorias;
        this.regioes = regioes;
        this.percentualComissao = percentualComissao;
        this.prazoConfirmacaoDias = prazoConfirmacaoDias;
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
                        .toList(),
                percentualComissao,
                prazoConfirmacaoDias);
    }
}
