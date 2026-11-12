package br.com.obrix.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("API - catalogo, busca e perfil publico (sem login)")
class PublicoApiTest extends ApiTestBase {

    @Test
    @DisplayName("catalogo: categorias e regioes com os campos que o front usa")
    void catalogo() throws Exception {
        mvc.perform(get("/api/v1/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorias", hasSize(8)))
                .andExpect(jsonPath("$.categorias[0].id").isNumber())
                .andExpect(jsonPath("$.categorias[0].nome").isString())
                .andExpect(jsonPath("$.regioes[0].id").isNumber())
                .andExpect(jsonPath("$.regioes[0].cidade").isString())
                .andExpect(jsonPath("$.regioes[0].bairro").isString())
                .andExpect(jsonPath("$.regioes[0].descricao").isString());
    }

    @Test
    @DisplayName("busca: devolve pagina com content, sem telefone, e-mail ou CPF")
    void busca() throws Exception {
        Long id = cadastrarPrestador(emailUnico("busca"),
                List.of(categoria("Marcenaria")), List.of(regiao("Setor Marista")));

        mvc.perform(get("/api/v1/prestadores/busca").param("categoriaId", categoria("Marcenaria").toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[*].id", hasItem(id.intValue())))
                .andExpect(jsonPath("$.itens[0].nome").isString())
                .andExpect(jsonPath("$.itens[0].notaMedia").isNumber())
                .andExpect(jsonPath("$.itens[0].quantidadeAvaliacoes").isNumber())
                .andExpect(jsonPath("$.itens[0].categorias").isArray())
                .andExpect(jsonPath("$.itens[0].regioes").isArray())
                .andExpect(jsonPath("$.itens[0].telefone").doesNotExist())
                .andExpect(jsonPath("$.itens[0].email").doesNotExist())
                .andExpect(jsonPath("$.itens[0].cpf").doesNotExist());
    }

    @Test
    @DisplayName("busca com regiao: so quem atende aquela regiao")
    void buscaPorRegiao() throws Exception {
        Long noMarista = cadastrarPrestador(emailUnico("regiao.a"),
                List.of(categoria("Refrigeracao")), List.of(regiao("Setor Marista")));
        Long noOeste = cadastrarPrestador(emailUnico("regiao.b"),
                List.of(categoria("Refrigeracao")), List.of(regiao("Setor Oeste")));

        mvc.perform(get("/api/v1/prestadores/busca")
                        .param("categoriaId", categoria("Refrigeracao").toString())
                        .param("regiaoId", regiao("Setor Marista").toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[*].id", hasItem(noMarista.intValue())))
                .andExpect(jsonPath("$.itens[*].id", not(hasItem(noOeste.intValue()))));
    }

    @Test
    @DisplayName("busca sem categoriaId: 400 DADOS_INVALIDOS, nao erro interno")
    void buscaSemCategoria() throws Exception {
        mvc.perform(get("/api/v1/prestadores/busca"))
                .andExpect(status().isBadRequest())
                .andExpect(erroPadrao("DADOS_INVALIDOS"));
    }

    @Test
    @DisplayName("busca com categoriaId nao numerico: 400 DADOS_INVALIDOS, nao erro interno")
    void buscaCategoriaInvalida() throws Exception {
        mvc.perform(get("/api/v1/prestadores/busca").param("categoriaId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(erroPadrao("DADOS_INVALIDOS"));
    }

    @Test
    @DisplayName("perfil publico: dados e avaliacoes, sem contato nem CPF")
    void perfilPublico() throws Exception {
        Long id = cadastrarPrestador(emailUnico("perfil"));

        mvc.perform(get("/api/v1/prestadores/" + id + "/publico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nome").isString())
                .andExpect(jsonPath("$.categorias", hasItem("Eletrica")))
                .andExpect(jsonPath("$.regioes").isArray())
                .andExpect(jsonPath("$.avaliacoes", hasSize(0)))
                .andExpect(jsonPath("$.telefone").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.cpf").doesNotExist());
    }

    @Test
    @DisplayName("perfil publico inexistente: 404 NAO_ENCONTRADO")
    void perfilInexistente() throws Exception {
        mvc.perform(get("/api/v1/prestadores/999999/publico"))
                .andExpect(status().isNotFound())
                .andExpect(erroPadrao("NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("id de um cliente nao abre perfil publico: 404")
    void idDeCliente() throws Exception {
        Long cliente = cadastrarCliente(emailUnico("nao.prestador"));
        mvc.perform(get("/api/v1/prestadores/" + cliente + "/publico"))
                .andExpect(status().isNotFound())
                .andExpect(erroPadrao("NAO_ENCONTRADO"));
    }
}
