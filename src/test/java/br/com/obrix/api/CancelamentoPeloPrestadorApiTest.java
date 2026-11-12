package br.com.obrix.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("API - prestador nao cancela antes de aceitar (usa recusar)")
class CancelamentoPeloPrestadorApiTest extends ApiTestBase {

    @Test
    @DisplayName("prestador cancelando em SOLICITADA: 422 USE_RECUSAR e nada muda")
    void prestadorNaoCancelaSolicitada() throws Exception {
        Partes p = new Partes();
        Long id = criarSolicitacao(p);

        acao(p.prestador, id, "cancelar", motivo("Nao posso."))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(erroPadrao("USE_RECUSAR"));

        mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.prestador))
                .andExpect(jsonPath("$.status").value("SOLICITADA"))
                .andExpect(jsonPath("$.historico.length()").value(1));
    }

    @Test
    @DisplayName("em SOLICITADA o prestador continua podendo recusar")
    void recusarContinuaValendo() throws Exception {
        Partes p = new Partes();
        Long id = criarSolicitacao(p);
        acao(p.prestador, id, "recusar", motivo("Sem agenda."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECUSADA"));
    }

    @Test
    @DisplayName("em SOLICITADA o cliente continua podendo cancelar")
    void clienteContinuaCancelando() throws Exception {
        Partes p = new Partes();
        Long id = criarSolicitacao(p);
        acao(p.cliente, id, "cancelar", motivo("Resolvi de outra forma."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
    }
}
