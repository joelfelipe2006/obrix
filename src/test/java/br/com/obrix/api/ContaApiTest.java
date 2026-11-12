package br.com.obrix.api;

import br.com.obrix.solicitacao.TarefasAgendadas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("API - Minha conta")
class ContaApiTest extends ApiTestBase {

    @Autowired JdbcTemplate jdbc;
    @Autowired TarefasAgendadas tarefas;

    @Nested
    @DisplayName("Ver e editar")
    class VerEEditar {

        @Test
        @DisplayName("cliente ve os proprios dados com endereco, sem CPF")
        void verCliente() throws Exception {
            String email = emailUnico("conta.cliente");
            cadastrarCliente(email);
            mvc.perform(get("/api/v1/conta").session(login(email)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(email))
                    .andExpect(jsonPath("$.perfil").value("CLIENTE"))
                    .andExpect(jsonPath("$.status").value("ATIVA"))
                    .andExpect(jsonPath("$.endereco.logradouro").value("Rua 84"))
                    .andExpect(jsonPath("$.cpfMascarado").value(nullValue()))
                    .andExpect(jsonPath("$.termosVersao").value("1.0"));
        }

        @Test
        @DisplayName("prestador ve CPF mascarado e o catalogo que atende")
        void verPrestador() throws Exception {
            String email = emailUnico("conta.prestador");
            cadastrarPrestador(email);
            mvc.perform(get("/api/v1/conta").session(login(email)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.perfil").value("PRESTADOR"))
                    .andExpect(jsonPath("$.cpfMascarado", startsWith("***.***.***-")))
                    .andExpect(jsonPath("$.categoriaIds", hasItem(categoria("Eletrica").intValue())))
                    .andExpect(jsonPath("$.regiaoIds", hasItem(regiao("Setor Sul").intValue())));
        }

        @Test
        @DisplayName("cliente edita nome, telefone e endereco (corpo igual ao de pagina-minha-conta.js)")
        void editarCliente() throws Exception {
            String email = emailUnico("edita.cliente");
            cadastrarCliente(email);
            MockHttpSession s = login(email);

            Map<String, Object> endereco = endereco();
            endereco.put("numero", "999");
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("nome", "Nome Novo");
            corpo.put("telefone", "(62) 91111-2222");
            corpo.put("endereco", endereco);

            mvc.perform(put("/api/v1/conta").session(s).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpo)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome").value("Nome Novo"))
                    .andExpect(jsonPath("$.telefone").value("(62) 91111-2222"))
                    .andExpect(jsonPath("$.endereco.numero").value("999"));
        }

        @Test
        @DisplayName("prestador troca categorias e regioes")
        void editarPrestador() throws Exception {
            String email = emailUnico("edita.prestador");
            cadastrarPrestador(email);
            MockHttpSession s = login(email);

            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("nome", "Prestador Editado");
            corpo.put("telefone", "(62) 93333-4444");
            corpo.put("descricaoProfissional", "Agora tambem pinto.");
            corpo.put("categoriaIds", List.of(categoria("Pintura")));
            corpo.put("regiaoIds", List.of(regiao("Setor Bueno")));

            mvc.perform(put("/api/v1/conta").session(s).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpo)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.descricaoProfissional").value("Agora tambem pinto."))
                    .andExpect(jsonPath("$.categoriaIds", contains(categoria("Pintura").intValue())))
                    .andExpect(jsonPath("$.regiaoIds", contains(regiao("Setor Bueno").intValue())));
        }

        @Test
        @DisplayName("prestador sem nenhuma categoria: 422 CATEGORIA_OBRIGATORIA")
        void prestadorSemCategoria() throws Exception {
            String email = emailUnico("sem.categoria");
            cadastrarPrestador(email);
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("nome", "X");
            corpo.put("telefone", "1");
            corpo.put("categoriaIds", List.of());
            corpo.put("regiaoIds", List.of(regiao("Setor Sul")));

            mvc.perform(put("/api/v1/conta").session(login(email)).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpo)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(erroPadrao("CATEGORIA_OBRIGATORIA"));
        }

        @Test
        @DisplayName("nome em branco: 400 DADOS_INVALIDOS")
        void nomeEmBranco() throws Exception {
            String email = emailUnico("nome.branco");
            cadastrarCliente(email);
            mvc.perform(put("/api/v1/conta").session(login(email)).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("nome", "", "telefone", "1"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(erroPadrao("DADOS_INVALIDOS"))
                    .andExpect(jsonPath("$.campos", hasItem(startsWith("nome"))));
        }
    }

    @Nested
    @DisplayName("Trocar senha")
    class TrocarSenha {

        @Test
        @DisplayName("com a senha atual certa: 204, e a nova passa a valer")
        void troca() throws Exception {
            String email = emailUnico("troca.senha");
            cadastrarCliente(email);

            mvc.perform(post("/api/v1/conta/senha").session(login(email)).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("senhaAtual", SENHA, "novaSenha", "novaSenha9"))))
                    .andExpect(status().isNoContent());

            login(email, "novaSenha9");
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("email", email, "senha", SENHA))))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("senha atual errada: 422 SENHA_INCORRETA")
        void senhaAtualErrada() throws Exception {
            String email = emailUnico("senha.errada");
            cadastrarCliente(email);
            mvc.perform(post("/api/v1/conta/senha").session(login(email)).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("senhaAtual", "outra123", "novaSenha", "novaSenha9"))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(erroPadrao("SENHA_INCORRETA"));
        }

        @Test
        @DisplayName("nova senha segue a mesma regra do cadastro (letras e numeros): 400")
        void novaSenhaFraca() throws Exception {
            String email = emailUnico("senha.nova.fraca");
            cadastrarCliente(email);
            mvc.perform(post("/api/v1/conta/senha").session(login(email)).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("senhaAtual", SENHA, "novaSenha", "12345678"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(erroPadrao("DADOS_INVALIDOS"));
        }
    }

    @Nested
    @DisplayName("Desativar e excluir")
    class DesativarEExcluir {

        @Test
        @DisplayName("prestador desativado some da busca e nao recebe novas solicitacoes")
        void desativar() throws Exception {
            Partes p = new Partes();

            mvc.perform(post("/api/v1/conta/desativar").session(p.prestador))
                    .andExpect(status().isNoContent());

            mvc.perform(get("/api/v1/prestadores/busca").param("categoriaId", categoria("Eletrica").toString()))
                    .andExpect(jsonPath("$.content[*].id", not(hasItem(p.prestadorId.intValue()))));

            mvc.perform(post("/api/v1/solicitacoes").session(p.cliente)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpoSolicitacao(
                                    p.prestadorId, categoria("Eletrica"), regiao("Setor Sul")))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(erroPadrao("PRESTADOR_INDISPONIVEL"));
        }

        @Test
        @DisplayName("conta desativada nao entra mais: 422 CONTA_INDISPONIVEL (e nao ha rota para reativar)")
        void desativadaNaoEntra() throws Exception {
            String email = emailUnico("desativada");
            cadastrarCliente(email);
            mvc.perform(post("/api/v1/conta/desativar").session(login(email))).andExpect(status().isNoContent());

            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("email", email, "senha", SENHA))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(erroPadrao("CONTA_INDISPONIVEL"));
        }

        @Test
        @DisplayName("excluir encerra a sessao na hora: a conta deixa de autenticar")
        void excluirEncerraSessao() throws Exception {
            String email = emailUnico("excluir.sessao");
            cadastrarCliente(email);
            MockHttpSession s = login(email);

            mvc.perform(post("/api/v1/conta/excluir").session(s)).andExpect(status().isNoContent());

            mvc.perform(get("/api/v1/conta").session(s))
                    .andExpect(status().isUnauthorized())
                    .andExpect(erroPadrao("NAO_AUTENTICADO"));
        }

        @Test
        @DisplayName("depois de excluir, o login e recusado")
        void excluidaNaoEntra() throws Exception {
            String email = emailUnico("excluida");
            cadastrarCliente(email);
            mvc.perform(post("/api/v1/conta/excluir").session(login(email))).andExpect(status().isNoContent());

            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("email", email, "senha", SENHA))))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("anonimizacao apos 30 dias remove nome, e-mail, telefone e CPF do prestador")
        void anonimizaPrestador() throws Exception {
            String email = emailUnico("anon.prestador");
            Long id = cadastrarPrestador(email);
            mvc.perform(post("/api/v1/conta/excluir").session(login(email))).andExpect(status().isNoContent());
            jdbc.update("update usuario set data_solicitacao_exclusao = dateadd('DAY', -31, data_solicitacao_exclusao) where id = ?", id);

            tarefas.anonimizarContasEmExclusao();

            Map<String, Object> u = jdbc.queryForMap("select nome, email, telefone from usuario where id = ?", id);
            assertThat(u.get("nome")).isEqualTo("Usuario removido");
            assertThat((String) u.get("email")).doesNotContain("anon.prestador");
            assertThat(u.get("telefone")).isEqualTo("");

            String cpf = jdbc.queryForObject("select cpf from prestador where id = ?", String.class, id);
            assertThat(cpf).as("CPF depois da anonimizacao").doesNotMatch("\\d{11}");
        }

        @Test
        @DisplayName("anonimizacao apos 30 dias remove o endereco do cliente")
        void anonimizaCliente() throws Exception {
            String email = emailUnico("anon.cliente");
            Long id = cadastrarCliente(email);
            mvc.perform(post("/api/v1/conta/excluir").session(login(email))).andExpect(status().isNoContent());
            jdbc.update("update usuario set data_solicitacao_exclusao = dateadd('DAY', -31, data_solicitacao_exclusao) where id = ?", id);

            tarefas.anonimizarContasEmExclusao();

            Map<String, Object> c = jdbc.queryForMap("select logradouro, numero, cep from cliente where id = ?", id);
            assertThat(c.get("logradouro")).as("logradouro depois da anonimizacao").isNotEqualTo("Rua 84");
            assertThat(c.get("cep")).as("CEP depois da anonimizacao").isNotEqualTo("74080-010");
        }

        @Test
        @DisplayName("anonimizacao apaga rua, numero e CEP das solicitacoes do cliente e mantem bairro e cidade")
        void anonimizaEnderecoDasSolicitacoes() throws Exception {
            Partes p = new Partes();
            Long solicitacao = criarSolicitacao(p);
            mvc.perform(post("/api/v1/conta/excluir").session(p.cliente)).andExpect(status().isNoContent());
            jdbc.update("update usuario set data_solicitacao_exclusao = dateadd('DAY', -31, data_solicitacao_exclusao) where id = ?",
                    p.clienteId);

            tarefas.anonimizarContasEmExclusao();

            Map<String, Object> e = jdbc.queryForMap(
                    "select exec_cep, exec_logradouro, exec_numero, exec_bairro, exec_cidade from solicitacao where id = ?",
                    solicitacao);
            assertThat(e.get("exec_cep")).isNull();
            assertThat(e.get("exec_logradouro")).isNull();
            assertThat(e.get("exec_numero")).isNull();
            assertThat(e.get("exec_bairro")).isEqualTo("Setor Sul");
            assertThat(e.get("exec_cidade")).isEqualTo("Goiania");
        }
    }
}
