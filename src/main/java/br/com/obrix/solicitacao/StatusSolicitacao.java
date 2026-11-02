package br.com.obrix.solicitacao;

/**
 * Os oito estados do ciclo de vida da solicitacao (RF16).
 *
 * <p>Lista fechada de proposito: com texto livre, "concluido", "Concluida" e
 * "CONCLUIDO" seriam tres valores diferentes para o sistema.</p>
 */
public enum StatusSolicitacao {
    SOLICITADA,
    ACEITA,
    RECUSADA,
    EM_EXECUCAO,
    AGUARDANDO_CONFIRMACAO,
    CONTESTADA,
    CONCLUIDA,
    CANCELADA;

    /** Estados a partir dos quais a solicitacao nao se move mais. */
    public boolean ehFinal() {
        return this == RECUSADA || this == CANCELADA || this == CONCLUIDA;
    }
}
