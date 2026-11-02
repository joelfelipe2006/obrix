package br.com.obrix.solicitacao;

/**
 * Quem provocou a mudanca de status.
 *
 * <p>SISTEMA cobre a confirmacao automatica por decurso de prazo (RF19) — e e
 * o que permite provar depois que aquela confirmacao nao partiu do cliente.</p>
 */
public enum AutorTransicao {
    CLIENTE, PRESTADOR, SISTEMA
}
