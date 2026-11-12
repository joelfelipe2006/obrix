package br.com.obrix.api;

import br.com.obrix.solicitacao.TarefasAgendadas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("API - ciclo da solicitacao, cobranca, avaliacao e extrato")
class CicloApiTest extends ApiTestBase {

    @Autowired JdbcTemplate jdbc;
    @Autowired TarefasAgendadas tarefas;

    private int cobrancasDa(Long solicitacaoId) {
        return jdbc.queryForObject("select count(*) from cobranca where solicitacao_id = ?",
                Integer.class, solicitacaoId);
    }

    @Nested
    @DisplayName("Caminho normal")
    class CaminhoNormal {

        @Test
        @DisplayName("criar -> aceitar -> iniciar -> concluir -> confirmar -> avaliar")
        void cicloCompleto() throws Exception {
            Partes p = new Partes();

            // Criar
            Long id = idDe(mvc.perform(post("/api/v1/solicitacoes").session(p.cliente)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpoSolicitacao(
                                    p.prestadorId, categoria("Eletrica"), regiao("Setor Sul")))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SOLICITADA"))
                    .andExpect(jsonPath("$.statusRotulo").value("Solicitada"))
                    .andExpect(jsonPath("$.acoesDisponiveis", contains("cancelar")))
                    .andExpect(jsonPath("$.prestador.nome").isString())
                    .andExpect(jsonPath("$.prestador.telefone").value(nullValue()))
                    .andExpect(jsonPath("$.cliente").value(nullValue()))
                    .andExpect(jsonPath("$.historico", hasSize(1)))
                    .andReturn());

            // O prestador ve a mesma solicitacao com as acoes dele, sem o contato do cliente.
            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.prestador))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.acoesDisponiveis", contains("aceitar", "recusar")))
                    .andExpect(jsonPath("$.cliente").value(nullValue()));

            // Aceitar libera o contato para os dois lados
            acao(p.prestador, id, "aceitar")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACEITA"))
                    .andExpect(jsonPath("$.cliente.telefone").isString())
                    .andExpect(jsonPath("$.acoesDisponiveis", containsInAnyOrder("cancelar", "iniciar")));
            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.cliente))
                    .andExpect(jsonPath("$.prestador.telefone").isString())
                    .andExpect(jsonPath("$.prestador.email").value(p.emailPrestador));

            // Iniciar
            acao(p.prestador, id, "iniciar")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("EM_EXECUCAO"))
                    .andExpect(jsonPath("$.acoesDisponiveis", contains("concluir")));

            // Concluir com valor: ainda nao cobra
            acao(p.prestador, id, "concluir", valor("280.00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("AGUARDANDO_CONFIRMACAO"))
                    .andExpect(jsonPath("$.valorFinal").value(280.0))
                    .andExpect(jsonPath("$.prazoConfirmacao").isString())
                    .andExpect(jsonPath("$.cobranca").value(nullValue()));
            assertThat(cobrancasDa(id)).isZero();

            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.cliente))
                    .andExpect(jsonPath("$.acoesDisponiveis", containsInAnyOrder("confirmar", "contestar")));

            // Confirmar: unico ponto que gera cobranca
            acao(p.cliente, id, "confirmar")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                    .andExpect(jsonPath("$.confirmacaoAutomatica").value(false));
            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.cliente))
                    .andExpect(jsonPath("$.cobranca.valorServico").value(280.0))
                    .andExpect(jsonPath("$.cobranca.percentualComissao").value(10.0))
                    .andExpect(jsonPath("$.cobranca.valorComissao").value(28.0))
                    .andExpect(jsonPath("$.acoesDisponiveis", contains("avaliar")));
            assertThat(cobrancasDa(id)).isEqualTo(1);

            // Avaliar
            acao(p.cliente, id, "avaliar", Map.of("nota", 4, "comentario", "Rapido e limpo."))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notaAvaliacao").value(4))
                    .andExpect(jsonPath("$.acoesDisponiveis", hasSize(0)));

            mvc.perform(get("/api/v1/prestadores/" + p.prestadorId + "/publico"))
                    .andExpect(jsonPath("$.notaMedia").value(4.0))
                    .andExpect(jsonPath("$.quantidadeAvaliacoes").value(1))
                    .andExpect(jsonPath("$.avaliacoes[0].comentario").value("Rapido e limpo."));

            // Historico: uma linha por transicao, com o autor certo
            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.cliente))
                    .andExpect(jsonPath("$.historico[*].statusNovo", contains(
                            "SOLICITADA", "ACEITA", "EM_EXECUCAO", "AGUARDANDO_CONFIRMACAO", "CONCLUIDA")))
                    .andExpect(jsonPath("$.historico[*].autor", contains(
                            "CLIENTE", "PRESTADOR", "PRESTADOR", "PRESTADOR", "CLIENTE")));

            // Extrato do prestador
            mvc.perform(get("/api/v1/solicitacoes/extrato").session(p.prestador))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pagina.itens", hasSize(1)))
                    .andExpect(jsonPath("$.pagina.itens[0].solicitacaoId").value(id))
                    .andExpect(jsonPath("$.pagina.itens[0].servico").value("Eletrica"))
                    .andExpect(jsonPath("$.pagina.itens[0].cliente").isString())
                    .andExpect(jsonPath("$.pagina.itens[0].data").isString())
                    .andExpect(jsonPath("$.pagina.itens[0].valorComissao").value(28.0))
                    .andExpect(jsonPath("$.totalComissao").value(28.0))
                    .andExpect(jsonPath("$.totalServicos").value(280.0));
        }

        @Test
        @DisplayName("GET /solicitacoes lista o painel de quem esta logado")
        void listaDoPainel() throws Exception {
            Partes p = new Partes();
            Long id = criarSolicitacao(p);

            mvc.perform(get("/api/v1/solicitacoes").session(p.cliente))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.itens[*].id", contains(id.intValue())));
            mvc.perform(get("/api/v1/solicitacoes").session(p.prestador))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.itens[*].id", contains(id.intValue())));
        }

        @Test
        @DisplayName("comissao arredonda meio para cima: 10% de 99,95 = 10,00")
        void arredondamento() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "99.95");
            acao(p.cliente, id, "confirmar").andExpect(status().isOk());
            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.cliente))
                    .andExpect(jsonPath("$.cobranca.valorComissao").value(10.0));
        }

        @Test
        @DisplayName("a resposta do proprio /confirmar ja traz a cobranca gerada")
        void respostaDoConfirmarTrazCobranca() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "280.00");
            acao(p.cliente, id, "confirmar")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cobranca.valorComissao").value(28.0));
        }
    }

    @Nested
    @DisplayName("Recusa e cancelamento")
    class RecusaECancelamento {

        @Test
        @DisplayName("prestador recusa com motivo")
        void recusa() throws Exception {
            Partes p = new Partes();
            Long id = criarSolicitacao(p);
            acao(p.prestador, id, "recusar", motivo("Sem agenda nesta semana."))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RECUSADA"))
                    .andExpect(jsonPath("$.motivo").value("Sem agenda nesta semana."))
                    .andExpect(jsonPath("$.prestador.telefone").value(nullValue()));
        }

        @Test
        @DisplayName("recusar sem motivo: 400 DADOS_INVALIDOS")
        void recusaSemMotivo() throws Exception {
            Partes p = new Partes();
            Long id = criarSolicitacao(p);
            acao(p.prestador, id, "recusar", motivo(" "))
                    .andExpect(status().isBadRequest())
                    .andExpect(erroPadrao("DADOS_INVALIDOS"))
                    .andExpect(jsonPath("$.campos", hasItem(startsWith("motivo"))));
        }

        @Test
        @DisplayName("cliente cancela antes do aceite")
        void clienteCancela() throws Exception {
            Partes p = new Partes();
            Long id = criarSolicitacao(p);
            acao(p.cliente, id, "cancelar", motivo("Resolvi de outra forma."))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADA"))
                    .andExpect(jsonPath("$.historico[1].autor").value("CLIENTE"));
        }

        @Test
        @DisplayName("prestador cancela depois do aceite, com autor PRESTADOR")
        void prestadorCancela() throws Exception {
            Partes p = new Partes();
            Long id = criarSolicitacao(p);
            acao(p.prestador, id, "aceitar").andExpect(status().isOk());
            acao(p.prestador, id, "cancelar", motivo("Imprevisto."))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADA"))
                    .andExpect(jsonPath("$.historico[2].autor").value("PRESTADOR"));
        }

        @Test
        @DisplayName("nao se cancela depois de iniciado: 422 TRANSICAO_INVALIDA")
        void naoCancelaEmExecucao() throws Exception {
            Partes p = new Partes();
            Long id = criarSolicitacao(p);
            acao(p.prestador, id, "aceitar");
            acao(p.prestador, id, "iniciar");
            acao(p.cliente, id, "cancelar", motivo("Desisti."))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(erroPadrao("TRANSICAO_INVALIDA"));
        }
    }

    @Nested
    @DisplayName("Contestacao e nova conclusao")
    class Contestacao {

        @Test
        @DisplayName("contestar nao cobra; nova conclusao volta a aguardar, sem prazo automatico")
        void contestaENovaConclusao() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "300.00");

            acao(p.cliente, id, "contestar", motivo("Ficou com mau contato."))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONTESTADA"))
                    .andExpect(jsonPath("$.foiContestada").value(true))
                    .andExpect(jsonPath("$.cobranca").value(nullValue()))
                    .andExpect(jsonPath("$.acoesDisponiveis", hasSize(0)));
            assertThat(cobrancasDa(id)).isZero();

            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.prestador))
                    .andExpect(jsonPath("$.acoesDisponiveis", contains("concluir")));

            acao(p.prestador, id, "concluir", valor("250.00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("AGUARDANDO_CONFIRMACAO"))
                    .andExpect(jsonPath("$.valorFinal").value(250.0))
                    .andExpect(jsonPath("$.prazoConfirmacao").value(nullValue()));

            acao(p.cliente, id, "confirmar")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONCLUIDA"));
            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.cliente))
                    .andExpect(jsonPath("$.cobranca.valorComissao").value(25.0));
            assertThat(cobrancasDa(id)).isEqualTo(1);
        }

        @Test
        @DisplayName("contestar sem motivo: 400 DADOS_INVALIDOS")
        void contestarSemMotivo() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "100.00");
            acao(p.cliente, id, "contestar", motivo(""))
                    .andExpect(status().isBadRequest())
                    .andExpect(erroPadrao("DADOS_INVALIDOS"));
        }
    }

    @Nested
    @DisplayName("Cobranca unica")
    class CobrancaUnica {

        @Test
        @DisplayName("confirmar duas vezes: a segunda falha e continua existindo uma cobranca")
        void confirmarDuasVezes() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "200.00");
            acao(p.cliente, id, "confirmar").andExpect(status().isOk());
            acao(p.cliente, id, "confirmar")
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(erroPadrao("TRANSICAO_INVALIDA"));
            assertThat(cobrancasDa(id)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Avaliacao")
    class Avaliacao {

        @Test
        @DisplayName("antes da conclusao: 422 AVALIACAO_NAO_LIBERADA")
        void antesDaConclusao() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "100.00");
            acao(p.cliente, id, "avaliar", Map.of("nota", 5, "comentario", ""))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(erroPadrao("AVALIACAO_NAO_LIBERADA"));
        }

        @Test
        @DisplayName("segunda avaliacao do mesmo servico: 422 AVALIACAO_DUPLICADA")
        void duplicada() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "100.00");
            acao(p.cliente, id, "confirmar");
            acao(p.cliente, id, "avaliar", Map.of("nota", 5, "comentario", "")).andExpect(status().isOk());
            acao(p.cliente, id, "avaliar", Map.of("nota", 1, "comentario", ""))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(erroPadrao("AVALIACAO_DUPLICADA"));
        }

        @Test
        @DisplayName("nota fora de 1 a 5: 400 DADOS_INVALIDOS")
        void notaInvalida() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "100.00");
            acao(p.cliente, id, "confirmar");
            acao(p.cliente, id, "avaliar", Map.of("nota", 6, "comentario", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(erroPadrao("DADOS_INVALIDOS"));
        }
    }

    @Nested
    @DisplayName("Criacao com dados que o back-end recusa")
    class CriacaoInvalida {

        @Test
        @DisplayName("categoria que o prestador nao atende: 422 FORA_DE_ATUACAO")
        void foraDeAtuacao() throws Exception {
            Partes p = new Partes();
            mvc.perform(post("/api/v1/solicitacoes").session(p.cliente)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpoSolicitacao(
                                    p.prestadorId, categoria("Pintura"), regiao("Setor Sul")))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(erroPadrao("FORA_DE_ATUACAO"));
        }

        @Test
        @DisplayName("data desejada no passado: 400 DADOS_INVALIDOS")
        void dataPassada() throws Exception {
            Partes p = new Partes();
            var corpo = corpoSolicitacao(p.prestadorId, categoria("Eletrica"), regiao("Setor Sul"));
            corpo.put("dataDesejada", LocalDate.now().minusDays(1).toString());
            mvc.perform(post("/api/v1/solicitacoes").session(p.cliente)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos", hasItem(startsWith("dataDesejada"))));
        }

        @Test
        @DisplayName("prestador inexistente: 404 NAO_ENCONTRADO")
        void prestadorInexistente() throws Exception {
            Partes p = new Partes();
            mvc.perform(post("/api/v1/solicitacoes").session(p.cliente)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpoSolicitacao(
                                    999999L, categoria("Eletrica"), regiao("Setor Sul")))))
                    .andExpect(status().isNotFound())
                    .andExpect(erroPadrao("NAO_ENCONTRADO"));
        }

        @Test
        @DisplayName("prestador nao pode criar solicitacao: 403 NAO_AUTORIZADO")
        void prestadorNaoCria() throws Exception {
            Partes p = new Partes();
            mvc.perform(post("/api/v1/solicitacoes").session(p.prestador)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpoSolicitacao(
                                    p.prestadorId, categoria("Eletrica"), regiao("Setor Sul")))))
                    .andExpect(status().isForbidden())
                    .andExpect(erroPadrao("NAO_AUTORIZADO"));
        }
    }

    @Nested
    @DisplayName("Confirmacao automatica (tarefa agendada)")
    class ConfirmacaoAutomatica {

        private void recuarConclusao(Long id, int dias) {
            jdbc.update("update solicitacao set data_registro_conclusao = dateadd('DAY', ?, data_registro_conclusao) where id = ?",
                    -dias, id);
        }

        @Test
        @DisplayName("depois de 7 dias sem resposta, conclui com autor SISTEMA e gera a cobranca")
        void confirmaPorPrazo() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "150.00");
            recuarConclusao(id, 8);

            tarefas.confirmarPorDecursoDePrazo();

            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.cliente))
                    .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                    .andExpect(jsonPath("$.confirmacaoAutomatica").value(true))
                    .andExpect(jsonPath("$.cobranca.valorComissao").value(15.0))
                    .andExpect(jsonPath("$.historico[-1].autor").value("SISTEMA"));
        }

        @Test
        @DisplayName("antes dos 7 dias, nada muda")
        void prazoCorrendo() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "150.00");
            recuarConclusao(id, 6);

            tarefas.confirmarPorDecursoDePrazo();

            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.cliente))
                    .andExpect(jsonPath("$.status").value("AGUARDANDO_CONFIRMACAO"));
        }

        @Test
        @DisplayName("servico que ja foi contestado nunca e confirmado sozinho")
        void contestadaNaoConfirmaSozinha() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "150.00");
            acao(p.cliente, id, "contestar", motivo("Nao terminou."));
            acao(p.prestador, id, "concluir", valor("150.00"));
            recuarConclusao(id, 30);

            tarefas.confirmarPorDecursoDePrazo();

            mvc.perform(get("/api/v1/solicitacoes/" + id).session(p.cliente))
                    .andExpect(jsonPath("$.status").value("AGUARDANDO_CONFIRMACAO"));
            assertThat(cobrancasDa(id)).isZero();
        }
    }

    @Nested
    @DisplayName("Extrato do prestador")
    class Extrato {

        @Test
        @DisplayName("os totais somam todas as comissoes, nao so a primeira pagina (21 servicos)")
        void totaisConsideramTudo() throws Exception {
            Partes p = new Partes();
            for (int i = 0; i < 21; i++) {
                Long id = ateAguardandoConfirmacao(p, "100.00");
                acao(p.cliente, id, "confirmar").andExpect(status().isOk());
            }

            mvc.perform(get("/api/v1/solicitacoes/extrato").session(p.prestador))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalComissao").value(210.0))
                    .andExpect(jsonPath("$.totalServicos").value(2100.0));
        }

        @Test
        @DisplayName("cliente nao acessa o extrato: 403 NAO_AUTORIZADO")
        void clienteNaoVe() throws Exception {
            Partes p = new Partes();
            mvc.perform(get("/api/v1/solicitacoes/extrato").session(p.cliente))
                    .andExpect(status().isForbidden())
                    .andExpect(erroPadrao("NAO_AUTORIZADO"));
        }
    }
}
