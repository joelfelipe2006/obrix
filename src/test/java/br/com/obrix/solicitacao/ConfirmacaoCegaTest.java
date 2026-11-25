package br.com.obrix.solicitacao;

import br.com.obrix.usuario.Endereco;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O motivo automatico da divergencia aparece no historico que o cliente ve.
 * Se trouxesse o valor do prestador, bastaria errar de proposito para descobri-lo.
 */
@DisplayName("Confirmacao as cegas na entidade")
class ConfirmacaoCegaTest {

    @Test
    @DisplayName("o motivo da divergencia traz o valor do cliente e nao o do prestador")
    void motivoNaoRevelaValorDoPrestador() {
        Endereco endereco = new Endereco("74080-010", "Rua 84", "120",
                "Setor Sul", "Goiania", "GO");
        Solicitacao s = new Solicitacao(null, null, null, null,
                "Instalacao de chuveiro eletrico.", LocalDate.now().plusDays(2), endereco);
        s.aceitar();
        s.iniciarExecucao();
        s.registrarConclusao(new BigDecimal("280.00"));

        s.informarValorPago(new BigDecimal("300.00"));

        assertThat(s.getMotivo())
                .isEqualTo("Divergencia de valor: o valor informado pelo cliente (R$ 300,00)"
                        + " nao confere com o registrado pelo profissional.")
                .doesNotContain("280");
    }
}
