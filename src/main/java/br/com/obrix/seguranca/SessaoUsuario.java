package br.com.obrix.seguranca;

import java.io.Serializable;

/** O que fica na sessao: identidade minima, nunca a entidade inteira. */
public record SessaoUsuario(Long id, String nome, String email, String perfil) implements Serializable { }
