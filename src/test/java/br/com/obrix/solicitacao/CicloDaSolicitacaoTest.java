package br.com.obrix.solicitacao;

import br.com.obrix.cobranca.Cobranca;
import br.com.obrix.shared.RegraDeNegocioException;
import br.com.obrix.usuario.Endereco;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes da maquina de estados e do calculo da comissao — os dois pontos que o
 * plano de testes marca como obrigatorios, porque sao os unicos que produzem
 * obrigacao financeira.
 */
class CicloDaSolicitacaoTest {

    private static final BigDecimal DEZ_POR_CENTO = new BigDecimal("10.00");

    private Solicitacao novaSolicitacao() {
        Endereco endereco = new Endereco("74080-010", "Rua 84", "120",
                "Setor Sul", "Goiania", "GO");
        return new Solicitacao(null, null, null, null,
                "Instalacao de chuveiro eletrico.", LocalDate.now().plusDays(2), endereco);
    }

    /** Leva a solicitacao ate o estado pedido pelo caminho normal. */
    private Solicitacao ate(StatusSolicitacao destino) {
        Solicitacao s = novaSolicitacao();
        if (destino == StatusSolicitacao.SOLICITADA) return s;
        s.aceitar();
        if (destino == StatusSolicitacao.ACEITA) return s;
        s.iniciarExecucao();
        if (destino == StatusSolicitacao.EM_EXECUCAO) return s;
        s.registrarConclusao(new BigDecimal("280.00"));
        if (destino == StatusSolicitacao.AGUARDANDO_CONFIRMACAO) return s;
        if (destino == StatusSolicitacao.CONTESTADA) {
            s.contestar("A instalacao ficou com mau contato.");
            return s;
        }
        s.confirmar(false);
        return s;
    }

    @Nested
    @DisplayName("Caminho normal")
    class CaminhoNormal {

        @Test
        @DisplayName("a solicitacao nasce em SOLICITADA e ja com um registro no historico")
        void nasceComHistorico() {
            Solicitacao s = novaSolicitacao();

            assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.SOLICITADA);
            // Multiplicidade 1..*: criar a solicitacao ja e a primeira transicao.
            assertThat(s.getHistorico()).hasSize(1);
            assertThat(s.getHistorico().get(0).getStatusAnterior()).isNull();
            assertThat(s.getHistorico().get(0).getAutor()).isEqualTo(AutorTransicao.CLIENTE);
        }

        @Test
        @DisplayName("percorre aceite, execucao, conclusao e confirmacao")
        void percorreOCicloInteiro() {
            Solicitacao s = novaSolicitacao();

            s.aceitar();
            assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.ACEITA);

            s.iniciarExecucao();
            assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.EM_EXECUCAO);

            s.registrarConclusao(new BigDecimal("280.00"));
            assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.AGUARDANDO_CONFIRMACAO);
            assertThat(s.getValorFinal()).isEqualByComparingTo("280.00");
            assertThat(s.getDataRegistroConclusao()).isNotNull();

            s.confirmar(false);
            assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.CONCLUIDA);
            assertThat(s.getHistorico()).hasSize(5);
        }

        @Test
        @DisplayName("cada transicao grava autor e, quando exigido, o motivo")
        void historicoGuardaAutorEMotivo() {
            Solicitacao s = ate(StatusSolicitacao.SOLICITADA);
            s.recusar("Agenda cheia nesta semana.");

            HistoricoStatus ultimo = s.getHistorico().get(s.getHistorico().size() - 1);
            assertThat(ultimo.getStatusAnterior()).isEqualTo(StatusSolicitacao.SOLICITADA);
            assertThat(ultimo.getStatusNovo()).isEqualTo(StatusSolicitacao.RECUSADA);
            assertThat(ultimo.getAutor()).isEqualTo(AutorTransicao.PRESTADOR);
            assertThat(ultimo.getMotivo()).isEqualTo("Agenda cheia nesta semana.");
        }
    }

    @Nested
    @DisplayName("Transicoes proibidas falham explicitamente")
    class TransicoesProibidas {

        @Test
        @DisplayName("nao se conclui um servico que nunca foi aceito")
        void naoConcluiSemAceite() {
            Solicitacao s = novaSolicitacao();

            assertThatThrownBy(() -> s.registrarConclusao(new BigDecimal("100.00")))
                    .isInstanceOf(TransicaoInvalidaException.class);
            assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.SOLICITADA);
        }

        @Test
        @DisplayName("nao se confirma uma solicitacao cancelada")
        void naoConfirmaCancelada() {
            Solicitacao s = novaSolicitacao();
            s.cancelar("Resolvi por conta propria.", AutorTransicao.CLIENTE);

            assertThatThrownBy(() -> s.confirmar(false))
                    .isInstanceOf(TransicaoInvalidaException.class);
        }

        @Test
        @DisplayName("estado final nao se move mais")
        void estadoFinalNaoSeMove() {
            Solicitacao s = ate(StatusSolicitacao.CONCLUIDA);

            assertThatThrownBy(s::aceitar).isInstanceOf(TransicaoInvalidaException.class);
            assertThatThrownBy(() -> s.contestar("depois")).isInstanceOf(TransicaoInvalidaException.class);
            assertThatThrownBy(() -> s.cancelar("depois", AutorTransicao.CLIENTE))
                    .isInstanceOf(TransicaoInvalidaException.class);
        }

        @Test
        @DisplayName("cancelamento so vale enquanto o servico nao comecou")
        void cancelamentoSoAntesDaExecucao() {
            assertThat(ate(StatusSolicitacao.SOLICITADA).podeSerCancelada()).isTrue();
            assertThat(ate(StatusSolicitacao.ACEITA).podeSerCancelada()).isTrue();
            assertThat(ate(StatusSolicitacao.EM_EXECUCAO).podeSerCancelada()).isFalse();

            Solicitacao emExecucao = ate(StatusSolicitacao.EM_EXECUCAO);
            assertThatThrownBy(() -> emExecucao.cancelar("desisti", AutorTransicao.CLIENTE))
                    .isInstanceOf(TransicaoInvalidaException.class);
        }

        @Test
        @DisplayName("recusar, cancelar e contestar exigem motivo")
        void motivoObrigatorio() {
            assertThatThrownBy(() -> ate(StatusSolicitacao.SOLICITADA).recusar("  "))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("motivo");

            assertThatThrownBy(() -> ate(StatusSolicitacao.AGUARDANDO_CONFIRMACAO).contestar(null))
                    .isInstanceOf(RegraDeNegocioException.class);

            assertThatThrownBy(() -> ate(StatusSolicitacao.SOLICITADA)
                    .cancelar("", AutorTransicao.CLIENTE))
                    .isInstanceOf(RegraDeNegocioException.class);
        }

        @Test
        @DisplayName("o valor final precisa ser maior que zero")
        void valorFinalPositivo() {
            Solicitacao s = ate(StatusSolicitacao.EM_EXECUCAO);

            assertThatThrownBy(() -> s.registrarConclusao(BigDecimal.ZERO))
                    .isInstanceOf(RegraDeNegocioException.class);
            assertThatThrownBy(() -> s.registrarConclusao(new BigDecimal("-50.00")))
                    .isInstanceOf(RegraDeNegocioException.class);
        }
    }

    @Nested
    @DisplayName("Contestacao")
    class Contestacao {

        @Test
        @DisplayName("a unica saida e um novo registro de conclusao")
        void unicaSaidaEhNovaConclusao() {
            Solicitacao s = ate(StatusSolicitacao.CONTESTADA);

            assertThatThrownBy(() -> s.confirmar(false))
                    .isInstanceOf(TransicaoInvalidaException.class);
            assertThatThrownBy(s::aceitar).isInstanceOf(TransicaoInvalidaException.class);

            s.registrarConclusao(new BigDecimal("240.00"));
            assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.AGUARDANDO_CONFIRMACAO);
        }

        @Test
        @DisplayName("desliga a confirmacao automatica para sempre naquele servico")
        void desligaPrazoParaSempre() throws Exception {
            Solicitacao s = ate(StatusSolicitacao.CONTESTADA);
            s.registrarConclusao(new BigDecimal("240.00"));

            // Mesmo com o registro antigo, o prazo nao volta a correr.
            recuarRegistroDeConclusao(s, 30);

            assertThat(s.isFoiContestada()).isTrue();
            assertThat(s.prazoConfirmacaoExpirado(7)).isFalse();
            assertThat(s.getPrazoConfirmacao(7)).isNull();
        }

        @Test
        @DisplayName("contestar nao gera cobranca")
        void naoGeraCobranca() {
            Solicitacao s = ate(StatusSolicitacao.CONTESTADA);
            assertThat(s.getCobranca()).isNull();
        }
    }

    @Nested
    @DisplayName("Confirmacao automatica por decurso de prazo")
    class ConfirmacaoAutomatica {

        @Test
        @DisplayName("o prazo nao venceu antes de sete dias")
        void prazoAindaCorrendo() {
            Solicitacao s = ate(StatusSolicitacao.AGUARDANDO_CONFIRMACAO);
            assertThat(s.prazoConfirmacaoExpirado(7)).isFalse();
        }

        @Test
        @DisplayName("vence depois de sete dias do registro do prestador")
        void prazoVencido() throws Exception {
            Solicitacao s = ate(StatusSolicitacao.AGUARDANDO_CONFIRMACAO);
            recuarRegistroDeConclusao(s, 8);

            assertThat(s.prazoConfirmacaoExpirado(7)).isTrue();
        }

        @Test
        @DisplayName("a confirmacao automatica fica registrada, com autor SISTEMA")
        void registraQueFoiAutomatica() {
            Solicitacao s = ate(StatusSolicitacao.AGUARDANDO_CONFIRMACAO);
            s.confirmar(true);

            assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.CONCLUIDA);
            assertThat(s.isConfirmacaoAutomatica()).isTrue();

            HistoricoStatus ultimo = s.getHistorico().get(s.getHistorico().size() - 1);
            assertThat(ultimo.getAutor()).isEqualTo(AutorTransicao.SISTEMA);
        }

        @Test
        @DisplayName("a confirmacao do cliente fica marcada como nao automatica")
        void confirmacaoDoCliente() {
            Solicitacao s = ate(StatusSolicitacao.CONCLUIDA);

            assertThat(s.isConfirmacaoAutomatica()).isFalse();
            HistoricoStatus ultimo = s.getHistorico().get(s.getHistorico().size() - 1);
            assertThat(ultimo.getAutor()).isEqualTo(AutorTransicao.CLIENTE);
        }

        @Test
        @DisplayName("so corre quando o servico esta aguardando confirmacao")
        void soValeNoEstadoCerto() {
            assertThat(ate(StatusSolicitacao.EM_EXECUCAO).prazoConfirmacaoExpirado(7)).isFalse();
            assertThat(ate(StatusSolicitacao.CONCLUIDA).prazoConfirmacaoExpirado(7)).isFalse();
        }
    }

    @Nested
    @DisplayName("Contato liberado somente apos o aceite")
    class Contato {

        @Test
        void naoLiberadoAntesDoAceite() {
            assertThat(ate(StatusSolicitacao.SOLICITADA).contatoLiberado()).isFalse();
        }

        @Test
        void liberadoDepoisDoAceite() {
            assertThat(ate(StatusSolicitacao.ACEITA).contatoLiberado()).isTrue();
            assertThat(ate(StatusSolicitacao.CONCLUIDA).contatoLiberado()).isTrue();
        }

        @Test
        void naoLiberadoSeRecusada() {
            Solicitacao s = novaSolicitacao();
            s.recusar("Nao atendo esse tipo de servico.");
            assertThat(s.contatoLiberado()).isFalse();
        }
    }

    @Nested
    @DisplayName("Calculo da comissao")
    class CalculoDaComissao {

        @Test
        @DisplayName("10% de R$ 280,00 sao R$ 28,00 — o caso dos mockups")
        void casoDeReferencia() {
            Cobranca c = new Cobranca(null, new BigDecimal("280.00"), DEZ_POR_CENTO);

            assertThat(c.getValorServico()).isEqualByComparingTo("280.00");
            assertThat(c.getPercentualComissao()).isEqualByComparingTo("10.00");
            assertThat(c.getValorComissao()).isEqualByComparingTo("28.00");
        }

        @Test
        @DisplayName("arredonda meio para cima, com duas casas")
        void arredondamento() {
            // 10% de 99,99 = 9,999 -> 10,00
            assertThat(Cobranca.calcularComissao(new BigDecimal("99.99"), DEZ_POR_CENTO))
                    .isEqualByComparingTo("10.00");
            // 10% de 0,05 = 0,005 -> 0,01
            assertThat(Cobranca.calcularComissao(new BigDecimal("0.05"), DEZ_POR_CENTO))
                    .isEqualByComparingTo("0.01");
            // 10% de 150,00 = 15,00
            assertThat(Cobranca.calcularComissao(new BigDecimal("150.00"), DEZ_POR_CENTO))
                    .isEqualByComparingTo("15.00");
        }

        @Test
        @DisplayName("o resultado tem sempre duas casas decimais")
        void escalaDois() {
            assertThat(Cobranca.calcularComissao(new BigDecimal("1000.00"), DEZ_POR_CENTO).scale())
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("valores altos nao perdem precisao")
        void valorAlto() {
            assertThat(Cobranca.calcularComissao(new BigDecimal("1234567.89"), DEZ_POR_CENTO))
                    .isEqualByComparingTo("123456.79");
        }

        @Test
        @DisplayName("o percentual fica gravado na cobranca, nao numa configuracao global")
        void percentualEhSnapshot() {
            Cobranca antiga = new Cobranca(null, new BigDecimal("280.00"), DEZ_POR_CENTO);
            Cobranca nova = new Cobranca(null, new BigDecimal("280.00"), new BigDecimal("12.00"));

            // Mudar a politica nao altera a cobranca ja emitida.
            assertThat(antiga.getPercentualComissao()).isEqualByComparingTo("10.00");
            assertThat(antiga.getValorComissao()).isEqualByComparingTo("28.00");
            assertThat(nova.getValorComissao()).isEqualByComparingTo("33.60");
        }
    }

    /** Recua o marco de inicio do prazo, para testar o vencimento sem esperar dias. */
    private void recuarRegistroDeConclusao(Solicitacao s, int dias) throws Exception {
        Field campo = Solicitacao.class.getDeclaredField("dataRegistroConclusao");
        campo.setAccessible(true);
        campo.set(s, LocalDateTime.now().minusDays(dias));
    }
}
