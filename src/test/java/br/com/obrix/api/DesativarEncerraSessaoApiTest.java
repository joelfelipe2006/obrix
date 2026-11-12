package br.com.obrix.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("API - desativar a conta encerra a sessao")
class DesativarEncerraSessaoApiTest extends ApiTestBase {

    @Test
    @DisplayName("depois de desativar, a mesma sessao recebe 401 e /auth/sessao responde 204")
    void desativarEncerraSessao() throws Exception {
        String email = emailUnico("desativa.sessao");
        cadastrarCliente(email);
        MockHttpSession s = login(email);

        mvc.perform(post("/api/v1/conta/desativar").session(s)).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/conta").session(s))
                .andExpect(status().isUnauthorized())
                .andExpect(erroPadrao("NAO_AUTENTICADO"));
        mvc.perform(get("/api/v1/auth/sessao").session(s)).andExpect(status().isNoContent());
    }
}
