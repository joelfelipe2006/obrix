package br.com.obrix.seguranca;

import br.com.obrix.usuario.dto.Dtos.LoginRequest;
import br.com.obrix.usuario.dto.Dtos.SessaoResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AutenticacaoService servico;

    public AuthController(AutenticacaoService servico) {
        this.servico = servico;
    }

    @PostMapping("/login")
    public SessaoResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        SessaoUsuario usuario = servico.autenticar(req.email(), req.senha());
        // Sessao nova a cada login, para nao reaproveitar o identificador anterior.
        HttpSession antiga = http.getSession(false);
        if (antiga != null) antiga.invalidate();
        http.getSession(true).setAttribute(AutenticacaoInterceptor.CHAVE_SESSAO, usuario);
        return new SessaoResponse(usuario.id(), usuario.nome(), usuario.email(), usuario.perfil());
    }

    @GetMapping("/sessao")
    public ResponseEntity<SessaoResponse> sessao(HttpServletRequest http) {
        HttpSession sessao = http.getSession(false);
        if (sessao == null) return ResponseEntity.noContent().build();
        Object u = sessao.getAttribute(AutenticacaoInterceptor.CHAVE_SESSAO);
        if (!(u instanceof SessaoUsuario usuario)) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(new SessaoResponse(
                usuario.id(), usuario.nome(), usuario.email(), usuario.perfil()));
    }

    @PostMapping("/sair")
    public ResponseEntity<Void> sair(HttpServletRequest http) {
        HttpSession sessao = http.getSession(false);
        if (sessao != null) sessao.invalidate();
        return ResponseEntity.noContent().build();
    }
}
