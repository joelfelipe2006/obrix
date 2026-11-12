package br.com.obrix.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Formato de pagina proprio: { itens, pagina, tamanho, total, temMais },
 * igual na busca, na lista de solicitacoes e no extrato.
 */
@DisplayName("API - formato de pagina")
class PaginacaoApiTest extends ApiTestBase {

    @Test
    @DisplayName("lista de solicitacoes e extrato: 21 itens viram uma pagina de 20 com temMais e uma de 1 sem")
    void solicitacoesEExtrato() throws Exception {
        Partes p = new Partes();
        for (int i = 0; i < 21; i++) {
            Long id = ateAguardandoConfirmacao(p, "100.00");
            acao(p.cliente, id, "confirmar");
        }

        mvc.perform(get("/api/v1/solicitacoes").session(p.cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(20)))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.total").value(21))
                .andExpect(jsonPath("$.temMais").value(true));
        mvc.perform(get("/api/v1/solicitacoes").param("pagina", "1").session(p.cliente))
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.temMais").value(false));

        mvc.perform(get("/api/v1/solicitacoes/extrato").session(p.prestador))
                .andExpect(jsonPath("$.pagina.itens", hasSize(20)))
                .andExpect(jsonPath("$.pagina.total").value(21))
                .andExpect(jsonPath("$.pagina.temMais").value(true))
                .andExpect(jsonPath("$.totalComissao").value(210.0));
        mvc.perform(get("/api/v1/solicitacoes/extrato").param("pagina", "1").session(p.prestador))
                .andExpect(jsonPath("$.pagina.itens", hasSize(1)))
                .andExpect(jsonPath("$.pagina.temMais").value(false))
                .andExpect(jsonPath("$.totalComissao").value(210.0));
    }

    @Test
    @DisplayName("busca: mesmo formato, sem os campos do Page do Spring")
    void busca() throws Exception {
        cadastrarPrestador(emailUnico("pag.busca"),
                List.of(categoria("Alvenaria")), List.of(regiao("Setor Oeste")));

        mvc.perform(get("/api/v1/prestadores/busca").param("categoriaId", categoria("Alvenaria").toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isArray())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.temMais").isBoolean())
                .andExpect(jsonPath("$.content").doesNotExist())
                .andExpect(jsonPath("$.pageable").doesNotExist());
    }

    @Test
    @DisplayName("pagina negativa e tratada como a primeira, nao como erro")
    void paginaNegativa() throws Exception {
        Partes p = new Partes();
        mvc.perform(get("/api/v1/solicitacoes").param("pagina", "-1").session(p.cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0));
    }
}
