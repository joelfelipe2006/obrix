package br.com.obrix.solicitacao;

import br.com.obrix.shared.RegraDeNegocioException;

/** Transicao proibida falha explicitamente, nunca deixa estado inconsistente. */
public class TransicaoInvalidaException extends RegraDeNegocioException {
    public TransicaoInvalidaException(StatusSolicitacao de, StatusSolicitacao para) {
        super("TRANSICAO_INVALIDA",
              "Nao e possivel passar de %s para %s.".formatted(de, para));
    }
}
