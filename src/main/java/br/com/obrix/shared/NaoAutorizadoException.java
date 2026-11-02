package br.com.obrix.shared;

/**
 * Autorizacao por objeto: nao basta estar autenticado, e preciso ser o cliente
 * dono ou o prestador destinatario daquele registro.
 */
public class NaoAutorizadoException extends RuntimeException {
    public NaoAutorizadoException(String mensagem) { super(mensagem); }
}
