package br.com.obrix.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Percentual de comissao e prazo de confirmacao publicados no /catalogo (sem login). */
@DisplayName("API - regras de comissao e prazo no catalogo")
class RegrasNoCatalogoApiTest extends ApiTestBase {

    @Test
    @DisplayName("o catalogo publica o percentual (10%) e o prazo (7 dias) configurados")
    void valoresPadrao() throws Exception {
        mvc.perform(get("/api/v1/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentualComissao").value(10.0))
                .andExpect(jsonPath("$.prazoConfirmacaoDias").value(7));
    }

    /** Contexto proprio, com outra configuracao: prova que o valor vem da propriedade. */
    @Nested
    @SpringBootTest(properties = {
            "obrix.comissao.percentual=12.50",
            "obrix.confirmacao.prazo-dias=5"})
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    @DisplayName("com outra configuracao")
    class OutraConfiguracao {

        @Autowired MockMvc outro;

        @Test
        @DisplayName("o catalogo acompanha a configuracao do servidor")
        void acompanhaConfiguracao() throws Exception {
            outro.perform(get("/api/v1/catalogo"))
                    .andExpect(jsonPath("$.percentualComissao").value(12.5))
                    .andExpect(jsonPath("$.prazoConfirmacaoDias").value(5));
        }
    }
}
