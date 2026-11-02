package br.com.obrix.seguranca;

import br.com.obrix.shared.NaoAutorizadoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/** Acesso ao usuario da sessao corrente, com a checagem de perfil num lugar so. */
@Component
public class UsuarioLogado {

    public SessaoUsuario atual(HttpServletRequest http) {
        HttpSession sessao = http.getSession(false);
        Object u = sessao == null ? null : sessao.getAttribute(AutenticacaoInterceptor.CHAVE_SESSAO);
        if (!(u instanceof SessaoUsuario usuario)) {
            throw new NaoAutorizadoException("Sessao nao encontrada.");
        }
        return usuario;
    }

    public Long idCliente(HttpServletRequest http) {
        SessaoUsuario u = atual(http);
        if (!"CLIENTE".equals(u.perfil())) {
            throw new NaoAutorizadoException("Esta acao e exclusiva de clientes.");
        }
        return u.id();
    }

    public Long idPrestador(HttpServletRequest http) {
        SessaoUsuario u = atual(http);
        if (!"PRESTADOR".equals(u.perfil())) {
            throw new NaoAutorizadoException("Esta acao e exclusiva de profissionais.");
        }
        return u.id();
    }

    public Long id(HttpServletRequest http) { return atual(http).id(); }
}
