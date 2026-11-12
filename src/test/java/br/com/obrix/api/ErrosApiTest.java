package br.com.obrix.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("API - casos de erro e formato { codigo, mensagem, campos }")
class ErrosApiTest extends ApiTestBase {

    @Nested
    @DisplayName("Sem login")
    class SemLogin {

        @ParameterizedTest(name = "{0} {1} -> 401")
        @CsvSource({
                "GET,  /api/v1/solicitacoes",
                "POST, /api/v1/solicitacoes",
                "GET,  /api/v1/solicitacoes/1",
                "POST, /api/v1/solicitacoes/1/aceitar",
                "POST, /api/v1/solicitacoes/1/recusar",
                "POST, /api/v1/solicitacoes/1/iniciar",
                "POST, /api/v1/solicitacoes/1/concluir",
                "POST, /api/v1/solicitacoes/1/confirmar",
                "POST, /api/v1/solicitacoes/1/contestar",
                "POST, /api/v1/solicitacoes/1/cancelar",
                "POST, /api/v1/solicitacoes/1/avaliar",
                "GET,  /api/v1/solicitacoes/extrato",
                "GET,  /api/v1/conta",
                "PUT,  /api/v1/conta",
                "POST, /api/v1/conta/senha",
                "POST, /api/v1/conta/desativar",
                "POST, /api/v1/conta/excluir"
        })
        void exigeLogin(String metodo, String url) throws Exception {
            mvc.perform(request(HttpMethod.valueOf(metodo), url)
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(erroPadrao("NAO_AUTENTICADO"))
                    .andExpect(jsonPath("$.campos", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("Perfil errado e solicitacao de outra pessoa")
    class Autorizacao {

        @Test
        @DisplayName("cliente tentando aceitar: 403 NAO_AUTORIZADO")
        void clienteNaoAceita() throws Exception {
            Partes p = new Partes();
            Long id = criarSolicitacao(p);
            acao(p.cliente, id, "aceitar")
                    .andExpect(status().isForbidden())
                    .andExpect(erroPadrao("NAO_AUTORIZADO"));
        }

        @Test
        @DisplayName("prestador tentando confirmar: 403 NAO_AUTORIZADO")
        void prestadorNaoConfirma() throws Exception {
            Partes p = new Partes();
            Long id = ateAguardandoConfirmacao(p, "100.00");
            acao(p.prestador, id, "confirmar")
                    .andExpect(status().isForbidden())
                    .andExpect(erroPadrao("NAO_AUTORIZADO"));
        }

        @Test
        @DisplayName("outro prestador nao aceita solicitacao que nao e dele")
        void outroPrestador() throws Exception {
            Partes p = new Partes();
            Partes intruso = new Partes();
            Long id = criarSolicitacao(p);
            acao(intruso.prestador, id, "aceitar")
                    .andExpect(status().isForbidden())
                    .andExpect(erroPadrao("NAO_AUTORIZADO"));
        }

        @Test
        @DisplayName("outro cliente nao ve, nao cancela, nao confirma e nao avalia")
        void outroCliente() throws Exception {
            Partes p = new Partes();
            Partes intruso = new Partes();
            Long id = ateAguardandoConfirmacao(p, "100.00");

            mvc.perform(get("/api/v1/solicitacoes/" + id).session(intruso.cliente))
                    .andExpect(status().isForbidden())
                    .andExpect(erroPadrao("NAO_AUTORIZADO"));
            acao(intruso.cliente, id, "confirmar").andExpect(status().isForbidden());
            acao(intruso.cliente, id, "contestar", motivo("x")).andExpect(status().isForbidden());
            acao(intruso.cliente, id, "cancelar", motivo("x")).andExpect(status().isForbidden());
            acao(intruso.cliente, id, "avaliar", Map.of("nota", 1, "comentario", ""))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("solicitacao inexistente: 404 NAO_ENCONTRADO")
        void inexistente() throws Exception {
            Partes p = new Partes();
            mvc.perform(get("/api/v1/solicitacoes/999999").session(p.cliente))
                    .andExpect(status().isNotFound())
                    .andExpect(erroPadrao("NAO_ENCONTRADO"));
            acao(p.prestador, 999999L, "aceitar")
                    .andExpect(status().isNotFound())
                    .andExpect(erroPadrao("NAO_ENCONTRADO"));
        }
    }

    @Nested
    @DisplayName("Transicao proibida")
    class TransicaoProibida {

        @Test
        @DisplayName("confirmar o que nem foi aceito: 422 TRANSICAO_INVALIDA")
        void confirmarSolicitada() throws Exception {
            Partes p = new Partes();
            Long id = criarSolicitacao(p);
            acao(p.cliente, id, "confirmar")
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(erroPadrao("TRANSICAO_INVALIDA"))
                    .andExpect(jsonPath("$.mensagem", containsString("SOLICITADA")));
        }

        @Test
        @DisplayName("concluir sem ter iniciado: 422 TRANSICAO_INVALIDA")
        void concluirSemIniciar() throws Exception {
            Partes p = new Partes();
            Long id = criarSolicitacao(p);
            acao(p.prestador, id, "aceitar");
            acao(p.prestador, id, "concluir", valor("100.00"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(erroPadrao("TRANSICAO_INVALIDA"));
        }

        @Test
        @DisplayName("aceitar uma recusada: 422 TRANSICAO_INVALIDA")
        void aceitarRecusada() throws Exception {
            Partes p = new Partes();
            Long id = criarSolicitacao(p);
            acao(p.prestador, id, "recusar", motivo("Sem agenda."));
            acao(p.prestador, id, "aceitar")
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(erroPadrao("TRANSICAO_INVALIDA"));
        }
    }

    @Nested
    @DisplayName("Dados invalidos")
    class DadosInvalidos {

        @Test
        @DisplayName("solicitacao sem descricao: 400 com o campo listado")
        void semDescricao() throws Exception {
            Partes p = new Partes();
            var corpo = corpoSolicitacao(p.prestadorId, categoria("Eletrica"), regiao("Setor Sul"));
            corpo.put("descricao", "");
            mvc.perform(post("/api/v1/solicitacoes").session(p.cliente)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(erroPadrao("DADOS_INVALIDOS"))
                    .andExpect(jsonPath("$.campos", hasItem(startsWith("descricao"))));
        }

        @Test
        @DisplayName("solicitacao com CEP vazio (o campo nao e obrigatorio na tela): 400 com o campo listado")
        void cepVazio() throws Exception {
            Partes p = new Partes();
            var corpo = corpoSolicitacao(p.prestadorId, categoria("Eletrica"), regiao("Setor Sul"));
            var endereco = endereco();
            endereco.put("cep", "");
            corpo.put("enderecoExecucao", endereco);
            mvc.perform(post("/api/v1/solicitacoes").session(p.cliente)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos", hasItem(startsWith("enderecoExecucao.cep"))));
        }

        @Test
        @DisplayName("valor final zero: 400 DADOS_INVALIDOS")
        void valorZero() throws Exception {
            Partes p = new Partes();
            Long id = criarSolicitacao(p);
            acao(p.prestador, id, "aceitar");
            acao(p.prestador, id, "iniciar");
            acao(p.prestador, id, "concluir", valor("0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(erroPadrao("DADOS_INVALIDOS"))
                    .andExpect(jsonPath("$.campos", hasItem(startsWith("valorFinal"))));
        }

        @Test
        @DisplayName("valor final com texto: 400 DADOS_INVALIDOS, nao erro interno")
        void valorTexto() throws Exception {
            Partes p = new Partes();
            Long id = criarSolicitacao(p);
            acao(p.prestador, id, "aceitar");
            acao(p.prestador, id, "iniciar");
            acao(p.prestador, id, "concluir", Map.of("valorFinal", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(erroPadrao("DADOS_INVALIDOS"));
        }

        @Test
        @DisplayName("JSON malformado: 400 DADOS_INVALIDOS, nao erro interno")
        void jsonMalformado() throws Exception {
            Partes p = new Partes();
            mvc.perform(post("/api/v1/solicitacoes").session(p.cliente)
                            .contentType(MediaType.APPLICATION_JSON).content("{ isto nao e json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(erroPadrao("DADOS_INVALIDOS"));
        }

        @Test
        @DisplayName("acao com motivo sem corpo nenhum: 400 DADOS_INVALIDOS, nao erro interno")
        void semCorpo() throws Exception {
            Partes p = new Partes();
            Long id = criarSolicitacao(p);
            mvc.perform(post("/api/v1/solicitacoes/" + id + "/recusar").session(p.prestador)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(erroPadrao("DADOS_INVALIDOS"));
        }

        @Test
        @DisplayName("data em formato brasileiro: 400 DADOS_INVALIDOS, nao erro interno")
        void dataFormatoErrado() throws Exception {
            Partes p = new Partes();
            var corpo = corpoSolicitacao(p.prestadorId, categoria("Eletrica"), regiao("Setor Sul"));
            corpo.put("dataDesejada", "25/12/2030");
            mvc.perform(post("/api/v1/solicitacoes").session(p.cliente)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(erroPadrao("DADOS_INVALIDOS"));
        }

        @Test
        @DisplayName("id nao numerico na URL: 400, nao erro interno")
        void idNaoNumerico() throws Exception {
            Partes p = new Partes();
            mvc.perform(get("/api/v1/solicitacoes/abc").session(p.cliente))
                    .andExpect(status().isBadRequest())
                    .andExpect(erroPadrao("DADOS_INVALIDOS"));
        }
    }

    @Nested
    @DisplayName("Rotas e metodos")
    class RotasEMetodos {

        @Test
        @DisplayName("metodo errado (GET numa acao): 405, nao erro interno")
        void metodoErrado() throws Exception {
            Partes p = new Partes();
            Long id = criarSolicitacao(p);
            mvc.perform(get("/api/v1/solicitacoes/" + id + "/aceitar").session(p.prestador))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.codigo").isString());
        }

        @Test
        @DisplayName("rota inexistente com login: 404, nao erro interno")
        void rotaInexistente() throws Exception {
            Partes p = new Partes();
            mvc.perform(get("/api/v1/nao-existe").session(p.cliente))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").isString());
        }
    }
}
