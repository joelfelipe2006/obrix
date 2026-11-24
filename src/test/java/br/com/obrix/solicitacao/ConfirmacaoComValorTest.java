package br.com.obrix.solicitacao;

import br.com.obrix.shared.RegraDeNegocioException;
import br.com.obrix.usuario.Endereco;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Regra do valor pago na entidade: comparar o valor do cliente com o do
 * prestador e, na divergencia, contestar pelo caminho de transicao normal.
 */
@DisplayName("Confirmacao com o valor pago pelo cliente")
class ConfirmacaoComValorTest {

    private Solicitacao aguardandoConfirmacao(String valorFinal) {
        Endereco endereco = new Endereco("74080-010", "Rua 84", "120",
                "Setor Sul", "Goiania", "GO");
        Solicitacao s = new Solicitacao(null, null, null, null,
                "Instalacao de chuveiro eletrico.", LocalDate.now().plusDays(2), endereco);
        s.aceitar();
        s.iniciarExecucao();
        s.registrarConclusao(new BigDecimal(valorFinal));
        return s;
    }

    @Nested
    @DisplayName("Valor igual")
    class ValorIgual {

        @Test
        @DisplayName("libera a confirmacao, grava o valor e nao muda o status sozinho")
        void liberaConfirmacao() {
            Solicitacao s = aguardandoConfirmacao("280.00");

            assertThat(s.informarValorPago(new BigDecimal("280.00"))).isTrue();

            assertThat(s.getValorConfirmadoCliente()).isEqualByComparingTo("280.00");
            // Quem conclui e gera a cobranca e o service, no ponto unico.
            assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.AGUARDANDO_CONFIRMACAO);
        }

        @Test
        @DisplayName("280 e 280.00 sao iguais (compareTo, nao equals)")
        void escalaNaoImporta() {
            Solicitacao s = aguardandoConfirmacao("280.00");
            assertThat(s.informarValorPago(new BigDecimal("280"))).isTrue();
        }
    }

    @Nested
    @DisplayName("Valor diferente")
    class ValorDiferente {

        @Test
        @DisplayName("contesta com autor CLIENTE e motivo automatico, sem gravar o valor")
        void contesta() {
            Solicitacao s = aguardandoConfirmacao("280.00");

            assertThat(s.informarValorPago(new BigDecimal("1500.5"))).isFalse();

            assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.CONTESTADA);
            assertThat(s.isFoiContestada()).isTrue();
            assertThat(s.getValorConfirmadoCliente()).isNull();
            assertThat(s.getCobranca()).isNull();
            HistoricoStatus ultimo = s.getHistorico().get(s.getHistorico().size() - 1);
            assertThat(ultimo.getAutor()).isEqualTo(AutorTransicao.CLIENTE);
            assertThat(ultimo.getMotivo()).isEqualTo(
                    "Divergencia de valor: o cliente informou R$ 1.500,50"
                            + " e o profissional registrou R$ 280,00");
        }

        @Test
        @DisplayName("depois da divergencia, o prazo automatico deixa de correr")
        void desligaPrazoAutomatico() {
            Solicitacao s = aguardandoConfirmacao("280.00");
            s.informarValorPago(new BigDecimal("200.00"));
            s.registrarConclusao(new BigDecimal("200.00"));

            assertThat(s.getPrazoConfirmacao(7)).isNull();
            assertThat(s.prazoConfirmacaoExpirado(0)).isFalse();
        }
    }

    @Nested
    @DisplayName("Valor invalido ou status errado")
    class Invalido {

        @Test
        @DisplayName("valor nulo, zero ou negativo: VALOR_INVALIDO e nada muda")
        void valorInvalido() {
            Solicitacao s = aguardandoConfirmacao("280.00");
            for (BigDecimal v : new BigDecimal[] {null, BigDecimal.ZERO, new BigDecimal("-1")}) {
                assertThatThrownBy(() -> s.informarValorPago(v))
                        .isInstanceOf(RegraDeNegocioException.class)
                        .extracting("codigo").isEqualTo("VALOR_INVALIDO");
            }
            assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.AGUARDANDO_CONFIRMACAO);
        }

        @Test
        @DisplayName("fora de AGUARDANDO_CONFIRMACAO: transicao invalida, sem contestar")
        void foraDoStatus() {
            Solicitacao s = aguardandoConfirmacao("280.00");
            s.contestar("Nao terminou.");
            int registros = s.getHistorico().size();

            assertThatThrownBy(() -> s.informarValorPago(new BigDecimal("999.00")))
                    .isInstanceOf(TransicaoInvalidaException.class);
            assertThat(s.getHistorico()).hasSize(registros);
        }
    }
}
