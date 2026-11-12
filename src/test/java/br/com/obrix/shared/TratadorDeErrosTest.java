package br.com.obrix.shared;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Conflitos de concorrencia e de restricao do banco sao dificeis de provocar
 * pela API de verdade. Aqui um controller de teste lanca as excecoes que o
 * Spring entregaria nesses casos, e o tratador real responde.
 */
@DisplayName("TratadorDeErros - conflitos viram 409")
class TratadorDeErrosTest {

    @RestController
    static class ControllerDeTeste {
        @GetMapping("/concorrencia")
        void concorrencia() {
            throw new ObjectOptimisticLockingFailureException("Solicitacao", 1L);
        }

        @GetMapping("/restricao")
        void restricao() {
            throw new DataIntegrityViolationException("Unique index or primary key violation: CPF 123");
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void montar() {
        mvc = MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
                .setControllerAdvice(new TratadorDeErros())
                .build();
    }

    @Test
    @DisplayName("conflito de versao (@Version): 409 CONFLITO")
    void concorrencia() throws Exception {
        mvc.perform(get("/concorrencia"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO"))
                .andExpect(jsonPath("$.mensagem").isString())
                .andExpect(jsonPath("$.campos").isArray());
    }

    @Test
    @DisplayName("violacao de restricao do banco: 409 CONFLITO, sem vazar o detalhe do SQL")
    void restricao() throws Exception {
        mvc.perform(get("/restricao"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO"))
                .andExpect(jsonPath("$.mensagem", not(containsString("CPF"))))
                .andExpect(jsonPath("$.campos").isArray());
    }
}
