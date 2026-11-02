package br.com.obrix.shared;

import java.util.List;

/** Corpo de erro padronizado, sem stack trace e sem detalhe interno. */
public record ApiError(String codigo, String mensagem, List<String> campos) {
    public ApiError(String codigo, String mensagem) { this(codigo, mensagem, List.of()); }
}
