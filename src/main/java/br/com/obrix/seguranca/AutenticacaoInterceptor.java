package br.com.obrix.seguranca;

import br.com.obrix.shared.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * Exige sessao autenticada nas rotas protegidas. Autenticar nao basta: a
 * autorizacao por objeto acontece nos services, que verificam se o usuario e
 * o cliente dono ou o prestador destinatario do registro.
 */
@Component
public class AutenticacaoInterceptor implements HandlerInterceptor {

    public static final String CHAVE_SESSAO = "obrix.usuario";

    private final ObjectMapper json = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler)
            throws Exception {
        HttpSession sessao = req.getSession(false);
        if (sessao != null && sessao.getAttribute(CHAVE_SESSAO) != null) {
            return true;
        }
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write(json.writeValueAsString(
                new ApiError("NAO_AUTENTICADO", "Faca login para continuar.")));
        return false;
    }
}
