package br.com.obrix.shared;

public class NaoEncontradoException extends RuntimeException {
    public NaoEncontradoException(String recurso, Object id) {
        super("%s nao encontrado: %s".formatted(recurso, id));
    }
}
