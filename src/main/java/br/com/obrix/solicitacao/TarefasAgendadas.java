package br.com.obrix.solicitacao;

import br.com.obrix.usuario.StatusConta;
import br.com.obrix.usuario.Usuario;
import br.com.obrix.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * As duas rotinas que rodam sozinhas, sem ninguem clicar em nada.
 *
 * <p>Sao elas que exigem uma instancia que nao hiberna por inatividade: se a
 * aplicacao dorme, a confirmacao automatica e a anonimizacao nao acontecem.</p>
 */
@Component
public class TarefasAgendadas {

    private static final Logger log = LoggerFactory.getLogger(TarefasAgendadas.class);
    private static final int PRAZO_ANONIMIZACAO_DIAS = 30;

    private final SolicitacaoRepository solicitacoes;
    private final SolicitacaoService servico;
    private final UsuarioRepository usuarios;

    public TarefasAgendadas(SolicitacaoRepository solicitacoes,
                            SolicitacaoService servico,
                            UsuarioRepository usuarios) {
        this.solicitacoes = solicitacoes;
        this.servico = servico;
        this.usuarios = usuarios;
    }

    /** RF19 — confirmacao automatica por decurso de prazo. De hora em hora. */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void confirmarPorDecursoDePrazo() {
        List<Solicitacao> pendentes =
                solicitacoes.findByStatus(StatusSolicitacao.AGUARDANDO_CONFIRMACAO);
        int confirmadas = 0;
        for (Solicitacao s : pendentes) {
            if (s.prazoConfirmacaoExpirado(servico.getPrazoConfirmacaoDias())) {
                servico.confirmarAutomaticamente(s.getId());
                confirmadas++;
            }
        }
        if (confirmadas > 0) {
            log.info("Confirmacao automatica: {} solicitacoes concluidas por prazo.", confirmadas);
        }
    }

    /** RNF27 — anonimizacao das contas em exclusao, em ate 30 dias. Uma vez ao dia. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void anonimizarContasEmExclusao() {
        LocalDateTime limite = LocalDateTime.now().minusDays(PRAZO_ANONIMIZACAO_DIAS);
        List<Usuario> alvos = usuarios
                .findByStatusAndDataSolicitacaoExclusaoBefore(StatusConta.EM_EXCLUSAO, limite);
        alvos.forEach(Usuario::anonimizar);
        if (!alvos.isEmpty()) {
            log.info("Anonimizacao LGPD: {} contas anonimizadas.", alvos.size());
        }
    }
}
