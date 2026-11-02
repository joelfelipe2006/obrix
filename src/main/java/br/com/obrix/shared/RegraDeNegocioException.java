package br.com.obrix.shared;

/** Falha de regra de negocio: vira 422 na API, com codigo e mensagem legiveis. */
public class RegraDeNegocioException extends RuntimeException {
    private final String codigo;

    public RegraDeNegocioException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() { return codigo; }
}
