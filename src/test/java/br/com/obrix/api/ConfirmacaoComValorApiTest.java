package br.com.obrix.api;

import br.com.obrix.solicitacao.TarefasAgendadas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Brecha de subdeclaracao: quem paga a comissao e o prestador, entao o valor
 * dele sozinho nao gera cobranca. O cliente informa quanto pagou ao confirmar,
 * e a cobranca so nasce quando os dois valores batem.
 */
@DisplayName("API - confirmacao com o valor pago pelo cliente")
class ConfirmacaoComValorApiTest extends ApiTestBase {

    @Autowired JdbcTemplate jdbc;
    @Autowired TarefasAgendadas tarefas;

    private int cobrancasDa(Long solicitacaoId) {
        return jdbc.queryForObject("select count(*) from cobranca where solicitacao_id = ?",
                Integer.class, solicitacaoId);
    }

    private static Map<String, Object> pago(String valor) {
        return Map.of("valorPago", new BigDecimal(valor));
    }

    @Nested
    @DisplayName("Valor igual ao registrado pelo profissional")
    class ValorIgual {

        @Test
        @DisplayName("conclui, grava o valor do cliente e gera a cobranca sobre o valor final")
        void concluiEGeraCobranca() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "280.00");

            acao(p.cliente, id, "confirmar", pago("280.00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                    .andExpect(jsonPath("$.valorConfirmadoCliente").value(280.0))
                    .andExpect(jsonPath("$.confirmacaoAutomatica").value(false))
                    .andExpect(jsonPath("$.cobranca.valorServico").value(280.0))
                    .andExpect(jsonPath("$.cobranca.valorComissao").value(28.0))
                    .andExpect(jsonPath("$.historico[-1].autor").value("CLIENTE"));
            assertThat(cobrancasDa(id)).isEqualTo(1);
        }

        @Test
        @DisplayName("280 e 280.00 sao o mesmo valor")
        void escalaNaoImporta() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "280.00");

            acao(p.cliente, id, "confirmar", pago("280"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONCLUIDA"));
            assertThat(cobrancasDa(id)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Valor diferente do registrado pelo profissional")
    class ValorDiferente {

        @Test
        @DisplayName("vai para CONTESTADA sem cobranca, com o motivo automatico no historico")
        void contestaSemCobranca() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "280.00");
            String motivo = "Divergencia de valor: o valor informado pelo cliente (R$ 300,00)"
                    + " nao confere com o registrado pelo profissional.";

            acao(p.cliente, id, "confirmar", pago("300.00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONTESTADA"))
                    .andExpect(jsonPath("$.foiContestada").value(true))
                    .andExpect(jsonPath("$.valorConfirmadoCliente").value(nullValue()))
                    .andExpect(jsonPath("$.cobranca").value(nullValue()))
                    .andExpect(jsonPath("$.motivo").value(motivo))
                    .andExpect(jsonPath("$.historico[-1].statusAnterior").value("AGUARDANDO_CONFIRMACAO"))
                    .andExpect(jsonPath("$.historico[-1].statusNovo").value("CONTESTADA"))
                    .andExpect(jsonPath("$.historico[-1].autor").value("CLIENTE"))
                    .andExpect(jsonPath("$.historico[-1].motivo").value(motivo));
            assertThat(cobrancasDa(id)).isZero();
        }

        @Test
        @DisplayName("o profissional e notificado da divergencia")
        void notificaPrestador() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "280.00");

            acao(p.cliente, id, "confirmar", pago("250.00")).andExpect(status().isOk());

            Integer avisos = jdbc.queryForObject(
                    "select count(*) from notificacao where usuario_id = ? and mensagem like ?",
                    Integer.class, p.prestadorId, "%Divergencia de valor%");
            assertThat(avisos).isEqualTo(1);
        }

        @Test
        @DisplayName("o profissional revisa, registra de novo e o cliente confirma o valor certo")
        void revisaoEConfirmacao() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "280.00");
            acao(p.cliente, id, "confirmar", pago("250.00"));

            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.prestador))
                    .andExpect(jsonPath("$.acoesDisponiveis", contains("concluir")));
            acao(p.prestador, id, "concluir", valor("250.00")).andExpect(status().isOk());

            acao(p.cliente, id, "confirmar", pago("250.00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                    .andExpect(jsonPath("$.cobranca.valorComissao").value(25.0));
            assertThat(cobrancasDa(id)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Valor pago invalido")
    class ValorInvalido {

        @Test
        @DisplayName("sem valorPago: 400 DADOS_INVALIDOS e nada muda")
        void semValor() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "280.00");

            acao(p.cliente, id, "confirmar")
                    .andExpect(status().isBadRequest())
                    .andExpect(erroPadrao("DADOS_INVALIDOS"))
                    .andExpect(jsonPath("$.campos", hasItem(startsWith("valorPago"))));
            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.cliente))
                    .andExpect(jsonPath("$.status").value("AGUARDANDO_CONFIRMACAO"));
            assertThat(cobrancasDa(id)).isZero();
        }

        @ParameterizedTest(name = "valorPago = {0}: 400")
        @ValueSource(strings = {"0", "0.00", "-10.00"})
        @DisplayName("valorPago zero ou negativo: 400 DADOS_INVALIDOS")
        void zeroOuNegativo(String valor) throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "280.00");

            acao(p.cliente, id, "confirmar", pago(valor))
                    .andExpect(status().isBadRequest())
                    .andExpect(erroPadrao("DADOS_INVALIDOS"));
            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.cliente))
                    .andExpect(jsonPath("$.status").value("AGUARDANDO_CONFIRMACAO"));
            assertThat(cobrancasDa(id)).isZero();
        }
    }

    @Nested
    @DisplayName("Confirmacao automatica")
    class Automatica {

        @Test
        @DisplayName("usa o valor final e deixa valorConfirmadoCliente nulo")
        void usaValorFinal() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "150.00");
            jdbc.update("update solicitacao set data_registro_conclusao = dateadd('DAY', -8, data_registro_conclusao) where id = ?", id);

            tarefas.confirmarPorDecursoDePrazo();

            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.cliente))
                    .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                    .andExpect(jsonPath("$.confirmacaoAutomatica").value(true))
                    .andExpect(jsonPath("$.valorConfirmadoCliente").value(nullValue()))
                    .andExpect(jsonPath("$.cobranca.valorServico").value(150.0))
                    .andExpect(jsonPath("$.cobranca.valorComissao").value(15.0));
        }
    }
}
