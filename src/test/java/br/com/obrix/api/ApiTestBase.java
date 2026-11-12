package br.com.obrix.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Base dos testes de integracao da API.
 *
 * <p>Os helpers montam os corpos exatamente como o front-end monta
 * (pagina-criar-conta.js, pagina-solicitar.js, paineis), e a autenticacao passa
 * pelo POST /auth/login de verdade: a sessao devolvida e reaproveitada nas
 * chamadas seguintes, como o cookie faz no navegador.</p>
 *
 * <p>O contexto Spring e o banco sao compartilhados entre as classes de teste.
 * Por isso cada teste cria os proprios usuarios, com e-mail e CPF unicos.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestBase {

    protected static final String SENHA = "obrix123";
    private static final AtomicInteger SEQUENCIA = new AtomicInteger(1);

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;

    // ------------------------------------------------------------------
    // Catalogo (vem da migration V2)
    // ------------------------------------------------------------------

    protected Long categoria(String nome) throws Exception {
        List<Integer> ids = JsonPath.read(get200("/api/v1/catalogo"),
                "$.categorias[?(@.nome == '" + nome + "')].id");
        return ids.get(0).longValue();
    }

    protected Long regiao(String bairro) throws Exception {
        List<Integer> ids = JsonPath.read(get200("/api/v1/catalogo"),
                "$.regioes[?(@.bairro == '" + bairro + "')].id");
        return ids.get(0).longValue();
    }

    private String get200(String url) throws Exception {
        return mvc.perform(get(url)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    // ------------------------------------------------------------------
    // Usuarios
    // ------------------------------------------------------------------

    protected static String emailUnico(String prefixo) {
        return prefixo + "." + SEQUENCIA.getAndIncrement() + "@teste.com";
    }

    /** CPF de 11 digitos, diferente a cada chamada (o formato e o unico validado). */
    protected static String cpfUnico() {
        return String.format("%011d", 10_000_000_000L + SEQUENCIA.getAndIncrement());
    }

    /** Corpo igual ao montado por pagina-criar-conta.js para o perfil cliente. */
    protected Map<String, Object> corpoCadastroCliente(String email) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("nome", "Cliente Teste");
        c.put("email", email);
        c.put("telefone", "(62) 99999-0000");
        c.put("senha", SENHA);
        c.put("aceiteTermos", true);
        c.put("endereco", endereco());
        return c;
    }

    /** Corpo igual ao montado por pagina-criar-conta.js para o perfil prestador. */
    protected Map<String, Object> corpoCadastroPrestador(String email, List<Long> categorias,
                                                         List<Long> regioes) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("nome", "Prestador Teste");
        c.put("email", email);
        c.put("telefone", "(62) 98888-0000");
        c.put("senha", SENHA);
        c.put("aceiteTermos", true);
        c.put("cpf", cpfUnico());
        c.put("descricaoProfissional", "Eletricista residencial.");
        c.put("categoriaIds", categorias);
        c.put("regiaoIds", regioes);
        return c;
    }

    protected Map<String, Object> endereco() {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("cep", "74080-010");
        e.put("logradouro", "Rua 84");
        e.put("numero", "120");
        e.put("bairro", "Setor Sul");
        e.put("cidade", "Goiania");
        e.put("estado", "GO");
        return e;
    }

    protected Long cadastrarCliente(String email) throws Exception {
        return idDe(mvc.perform(post("/api/v1/cadastro/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(corpoCadastroCliente(email))))
                .andExpect(status().isCreated()).andReturn());
    }

    protected Long cadastrarPrestador(String email) throws Exception {
        return cadastrarPrestador(email, List.of(categoria("Eletrica")), List.of(regiao("Setor Sul")));
    }

    protected Long cadastrarPrestador(String email, List<Long> categorias, List<Long> regioes)
            throws Exception {
        return idDe(mvc.perform(post("/api/v1/cadastro/prestador")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                corpoCadastroPrestador(email, categorias, regioes))))
                .andExpect(status().isCreated()).andReturn());
    }

    /** Login de verdade. A sessao devolvida faz o papel do cookie JSESSIONID. */
    protected MockHttpSession login(String email) throws Exception {
        return login(email, SENHA);
    }

    protected MockHttpSession login(String email, String senha) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "senha", senha))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    // ------------------------------------------------------------------
    // Cenario pronto: um cliente e um prestador logados
    // ------------------------------------------------------------------

    protected class Partes {
        public final String emailCliente = emailUnico("cliente");
        public final String emailPrestador = emailUnico("prestador");
        public final Long clienteId;
        public final Long prestadorId;
        public final MockHttpSession cliente;
        public final MockHttpSession prestador;

        public Partes() throws Exception {
            clienteId = cadastrarCliente(emailCliente);
            prestadorId = cadastrarPrestador(emailPrestador);
            cliente = login(emailCliente);
            prestador = login(emailPrestador);
        }
    }

    /** Corpo igual ao montado por pagina-solicitar.js. */
    protected Map<String, Object> corpoSolicitacao(Long prestadorId, Long categoriaId, Long regiaoId) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("prestadorId", prestadorId);
        c.put("categoriaId", categoriaId);
        c.put("regiaoId", regiaoId);
        c.put("descricao", "Instalacao de chuveiro eletrico.");
        c.put("dataDesejada", LocalDate.now().plusDays(2).toString());
        c.put("enderecoExecucao", endereco());
        return c;
    }

    protected Long criarSolicitacao(Partes p) throws Exception {
        return idDe(mvc.perform(post("/api/v1/solicitacoes").session(p.cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(corpoSolicitacao(
                                p.prestadorId, categoria("Eletrica"), regiao("Setor Sul")))))
                .andExpect(status().isCreated()).andReturn());
    }

    /** POST /solicitacoes/{id}/{acao}, com corpo {} quando nao ha corpo, como o api.js faz. */
    protected ResultActions acao(MockHttpSession sessao, Long id, String acao, Object corpo)
            throws Exception {
        return mvc.perform(post("/api/v1/solicitacoes/" + id + "/" + acao).session(sessao)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(corpo == null ? Map.of() : corpo)));
    }

    protected ResultActions acao(MockHttpSession sessao, Long id, String acao) throws Exception {
        return acao(sessao, id, acao, null);
    }

    protected static Map<String, Object> motivo(String texto) {
        return Map.of("motivo", texto);
    }

    protected static Map<String, Object> valor(String valorFinal) {
        // O painel do prestador envia Number(...), nao string.
        return Map.of("valorFinal", new BigDecimal(valorFinal));
    }

    /** Leva uma solicitacao nova ate AGUARDANDO_CONFIRMACAO. */
    protected Long ateAguardandoConfirmacao(Partes p, String valorFinal) throws Exception {
        Long id = criarSolicitacao(p);
        acao(p.prestador, id, "aceitar").andExpect(status().isOk());
        acao(p.prestador, id, "iniciar").andExpect(status().isOk());
        acao(p.prestador, id, "concluir", valor(valorFinal)).andExpect(status().isOk());
        return id;
    }

    // ------------------------------------------------------------------
    // Utilitarios
    // ------------------------------------------------------------------

    protected static Long idDe(MvcResult r) throws Exception {
        Number id = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    /** O corpo de erro padronizado: { codigo, mensagem, campos }. */
    protected static ResultMatcher erroPadrao(String codigo) {
        return ResultMatcher.matchAll(
                content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                jsonPath("$.codigo").value(codigo),
                jsonPath("$.mensagem").isString(),
                jsonPath("$.campos").isArray());
    }
}
