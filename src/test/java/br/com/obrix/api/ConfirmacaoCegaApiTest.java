package br.com.obrix.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Confirmacao as cegas: se o cliente visse o valor do prestador antes de
 * informar quanto pagou, bastaria copiar o numero e a protecao contra a
 * subdeclaracao nao serviria para nada.
 */
@DisplayName("API - confirmacao as cegas")
class ConfirmacaoCegaApiTest extends ApiTestBase {

    private static Map<String, Object> pago(String valor) {
        return Map.of("valorPago", new BigDecimal(valor));
    }

    private String itemDaLista(Long id) {
        return "$.itens[?(@.id == " + id + ")]";
    }

    @Nested
    @DisplayName("Antes da conclusao")
    class AntesDaConclusao {

        @Test
        @DisplayName("o cliente nao ve o valorFinal em AGUARDANDO_CONFIRMACAO, no detalhe")
        void clienteNaoVeNoDetalhe() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "280.00");

            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.cliente))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("AGUARDANDO_CONFIRMACAO"))
                    .andExpect(jsonPath("$.valorFinal").value(nullValue()))
                    .andExpect(jsonPath("$.valorConfirmadoCliente").value(nullValue()))
                    .andExpect(jsonPath("$.cobranca").value(nullValue()));
        }

        @Test
        @DisplayName("o cliente nao ve o valorFinal em AGUARDANDO_CONFIRMACAO, na lista")
        void clienteNaoVeNaLista() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "280.00");

            mvc.perform(get("/api/v1/solicitacoes").session(p.cliente))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(itemDaLista(id) + ".status", contains("AGUARDANDO_CONFIRMACAO")))
                    .andExpect(jsonPath(itemDaLista(id) + ".valorFinal", contains(nullValue())));
        }

        @Test
        @DisplayName("o prestador continua vendo o proprio valorFinal, no detalhe e na lista")
        void prestadorVe() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "280.00");

            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.prestador))
                    .andExpect(jsonPath("$.valorFinal").value(280.0));
            mvc.perform(get("/api/v1/solicitacoes").session(p.prestador))
                    .andExpect(jsonPath(itemDaLista(id) + ".valorFinal", contains(280.0)));
        }

        @Test
        @DisplayName("a resposta da acao do cliente tambem nao traz o valor (divergencia vira CONTESTADA)")
        void respostaDaAcaoNaoTraz() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "280.00");

            acao(p.cliente, id, "confirmar", pago("300.00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONTESTADA"))
                    .andExpect(jsonPath("$.valorFinal").value(nullValue()));
            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.prestador))
                    .andExpect(jsonPath("$.valorFinal").value(280.0));
        }

        @Test
        @DisplayName("o motivo da divergencia nao contem o valor do prestador")
        void motivoNaoRevelaValor() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "280.00");

            acao(p.cliente, id, "confirmar", pago("300.00")).andExpect(status().isOk());

            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.cliente))
                    .andExpect(jsonPath("$.motivo", containsString("R$ 300,00")))
                    .andExpect(jsonPath("$.motivo", not(containsString("280"))))
                    .andExpect(jsonPath("$.historico[-1].motivo", not(containsString("280"))));
        }
    }

    @Nested
    @DisplayName("Depois da conclusao")
    class DepoisDaConclusao {

        @Test
        @DisplayName("o cliente ve o valorFinal e o valorConfirmadoCliente")
        void clienteVeOsDois() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "280.00");

            acao(p.cliente, id, "confirmar", pago("280.00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                    .andExpect(jsonPath("$.valorFinal").value(280.0))
                    .andExpect(jsonPath("$.valorConfirmadoCliente").value(280.0));
            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.cliente))
                    .andExpect(jsonPath("$.valorFinal").value(280.0))
                    .andExpect(jsonPath("$.valorConfirmadoCliente").value(280.0));
            mvc.perform(get("/api/v1/solicitacoes").session(p.cliente))
                    .andExpect(jsonPath(itemDaLista(id) + ".valorFinal", contains(280.0)));
        }
    }
}
