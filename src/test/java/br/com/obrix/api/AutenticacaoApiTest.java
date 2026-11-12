package br.com.obrix.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("API - cadastro, login, sessao e bloqueio")
class AutenticacaoApiTest extends ApiTestBase {

    @Nested
    @DisplayName("Cadastro")
    class Cadastro {

        @Test
        @DisplayName("cliente: 201 com id, nome, e-mail e perfil CLIENTE")
        void cadastraCliente() throws Exception {
            String email = emailUnico("cad.cliente");
            mvc.perform(post("/api/v1/cadastro/cliente").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpoCadastroCliente(email))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.email").value(email))
                    .andExpect(jsonPath("$.perfil").value("CLIENTE"));
        }

        @Test
        @DisplayName("prestador: 201 com perfil PRESTADOR")
        void cadastraPrestador() throws Exception {
            String email = emailUnico("cad.prestador");
            mvc.perform(post("/api/v1/cadastro/prestador").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpoCadastroPrestador(email,
                                    List.of(categoria("Eletrica")), List.of(regiao("Setor Sul"))))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.perfil").value("PRESTADOR"));
        }

        @Test
        @DisplayName("cadastro nao autentica: a sessao continua vazia")
        void cadastroNaoAutentica() throws Exception {
            var r = mvc.perform(post("/api/v1/cadastro/cliente").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpoCadastroCliente(emailUnico("cad.sem.sessao")))))
                    .andExpect(status().isCreated()).andReturn();
            assertThat(r.getRequest().getSession(false)).isNull();
        }

        @Test
        @DisplayName("e-mail repetido, mesmo com outra caixa, e recusado com EMAIL_EM_USO")
        void emailRepetido() throws Exception {
            String email = emailUnico("repetido");
            cadastrarCliente(email);
            mvc.perform(post("/api/v1/cadastro/cliente").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpoCadastroCliente(email.toUpperCase()))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(erroPadrao("EMAIL_EM_USO"));
        }

        @Test
        @DisplayName("CPF repetido e recusado com CPF_EM_USO")
        void cpfRepetido() throws Exception {
            var primeiro = corpoCadastroPrestador(emailUnico("cpf.a"),
                    List.of(categoria("Eletrica")), List.of(regiao("Setor Sul")));
            mvc.perform(post("/api/v1/cadastro/prestador").contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(primeiro))).andExpect(status().isCreated());

            var segundo = corpoCadastroPrestador(emailUnico("cpf.b"),
                    List.of(categoria("Eletrica")), List.of(regiao("Setor Sul")));
            segundo.put("cpf", primeiro.get("cpf"));
            mvc.perform(post("/api/v1/cadastro/prestador").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(segundo)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(erroPadrao("CPF_EM_USO"));
        }

        @Test
        @DisplayName("sem aceite dos termos: 400 DADOS_INVALIDOS apontando o campo")
        void semAceite() throws Exception {
            var corpo = corpoCadastroCliente(emailUnico("sem.termos"));
            corpo.put("aceiteTermos", false);
            mvc.perform(post("/api/v1/cadastro/cliente").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(erroPadrao("DADOS_INVALIDOS"))
                    .andExpect(jsonPath("$.campos", hasItem(startsWith("aceiteTermos"))));
        }

        @Test
        @DisplayName("senha sem numeros: 400 DADOS_INVALIDOS")
        void senhaFraca() throws Exception {
            var corpo = corpoCadastroCliente(emailUnico("senha.fraca"));
            corpo.put("senha", "somenteletras");
            mvc.perform(post("/api/v1/cadastro/cliente").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos", hasItem(startsWith("senha"))));
        }

        @Test
        @DisplayName("prestador sem categoria: 400 DADOS_INVALIDOS")
        void prestadorSemCategoria() throws Exception {
            var corpo = corpoCadastroPrestador(emailUnico("sem.cat"), List.of(), List.of(regiao("Setor Sul")));
            mvc.perform(post("/api/v1/cadastro/prestador").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos", hasItem(startsWith("categoriaIds"))));
        }

        @Test
        @DisplayName("cliente com endereco em branco, como o formulario permite enviar: 400 apontando os campos")
        void enderecoEmBranco() throws Exception {
            // pagina-criar-conta.js manda '' quando o usuario deixa o endereco vazio,
            // e os campos nao sao obrigatorios no HTML.
            var corpo = corpoCadastroCliente(emailUnico("end.branco"));
            corpo.put("endereco", Map.of("cep", "", "logradouro", "", "numero", "",
                    "bairro", "", "cidade", "Goiania", "estado", "GO"));
            mvc.perform(post("/api/v1/cadastro/cliente").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(erroPadrao("DADOS_INVALIDOS"))
                    .andExpect(jsonPath("$.campos", hasItem(startsWith("endereco.cep"))))
                    .andExpect(jsonPath("$.campos", hasItem(startsWith("endereco.logradouro"))));
        }
    }

    @Nested
    @DisplayName("Login, sessao e sair")
    class LoginESessao {

        @Test
        @DisplayName("login devolve a sessao e GET /auth/sessao passa a responder 200")
        void loginAbreSessao() throws Exception {
            String email = emailUnico("login");
            Long id = cadastrarCliente(email);

            MockHttpSession sessao = login(email);

            mvc.perform(get("/api/v1/auth/sessao").session(sessao))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.perfil").value("CLIENTE"));
        }

        @Test
        @DisplayName("sem sessao, GET /auth/sessao responde 204")
        void semSessao() throws Exception {
            mvc.perform(get("/api/v1/auth/sessao")).andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("login troca o identificador da sessao (evita fixacao de sessao)")
        void loginTrocaSessao() throws Exception {
            String email = emailUnico("fixacao");
            cadastrarCliente(email);
            MockHttpSession anterior = new MockHttpSession();

            var r = mvc.perform(post("/api/v1/auth/login").session(anterior)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("email", email, "senha", SENHA))))
                    .andExpect(status().isOk()).andReturn();

            assertThat(anterior.isInvalid()).isTrue();
            assertThat(r.getRequest().getSession(false).getId()).isNotEqualTo(anterior.getId());
        }

        @Test
        @DisplayName("sair encerra a sessao: rotas protegidas voltam a responder 401")
        void sair() throws Exception {
            String email = emailUnico("sair");
            cadastrarCliente(email);
            MockHttpSession sessao = login(email);

            mvc.perform(post("/api/v1/auth/sair").session(sessao)).andExpect(status().isNoContent());

            mvc.perform(get("/api/v1/auth/sessao").session(sessao)).andExpect(status().isNoContent());
            mvc.perform(get("/api/v1/solicitacoes").session(sessao))
                    .andExpect(status().isUnauthorized())
                    .andExpect(erroPadrao("NAO_AUTENTICADO"));
        }

        @Test
        @DisplayName("senha errada e e-mail inexistente dao a mesma resposta")
        void respostaGenerica() throws Exception {
            String email = emailUnico("generica");
            cadastrarCliente(email);

            String senhaErrada = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("email", email, "senha", "errada123"))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(erroPadrao("CREDENCIAIS_INVALIDAS"))
                    .andReturn().getResponse().getContentAsString();

            String inexistente = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("email", "ninguem@teste.com", "senha", "errada123"))))
                    .andExpect(status().isUnprocessableEntity())
                    .andReturn().getResponse().getContentAsString();

            assertThat(senhaErrada).isEqualTo(inexistente);
        }

        @Test
        @DisplayName("e-mail em formato invalido: 400 DADOS_INVALIDOS")
        void emailInvalido() throws Exception {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("email", "sem-arroba", "senha", "x"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(erroPadrao("DADOS_INVALIDOS"));
        }
    }

    @Nested
    @DisplayName("Bloqueio apos 5 senhas erradas")
    class Bloqueio {

        private void errar(String email) throws Exception {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("email", email, "senha", "errada123"))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"));
        }

        @Test
        @DisplayName("depois da 5a falha, nem a senha certa entra: CONTA_BLOQUEADA")
        void bloqueia() throws Exception {
            String email = emailUnico("bloqueio");
            cadastrarCliente(email);

            for (int i = 0; i < 5; i++) errar(email);

            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("email", email, "senha", SENHA))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(erroPadrao("CONTA_BLOQUEADA"));
        }

        @Test
        @DisplayName("4 falhas e um acerto zeram a contagem")
        void acertoZera() throws Exception {
            String email = emailUnico("zera");
            cadastrarCliente(email);

            for (int i = 0; i < 4; i++) errar(email);
            login(email);
            for (int i = 0; i < 4; i++) errar(email);
            login(email);
        }
    }
}
