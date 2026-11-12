package br.com.obrix.usuario;

import br.com.obrix.seguranca.UsuarioLogado;
import br.com.obrix.usuario.dto.Dtos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/conta")
public class ContaController {

    private final UsuarioService servico;
    private final UsuarioLogado logado;

    public ContaController(UsuarioService servico, UsuarioLogado logado) {
        this.servico = servico;
        this.logado = logado;
    }

    @GetMapping
    public PerfilResponse perfil(HttpServletRequest http) {
        return servico.perfil(logado.id(http));
    }

    @PutMapping
    public PerfilResponse atualizar(@Valid @RequestBody AtualizarPerfilRequest req,
                                    HttpServletRequest http) {
        return servico.atualizarPerfil(logado.id(http), req);
    }

    @PostMapping("/senha")
    public ResponseEntity<Void> alterarSenha(@Valid @RequestBody AlterarSenhaRequest req,
                                             HttpServletRequest http) {
        servico.alterarSenha(logado.id(http), req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/desativar")
    public ResponseEntity<Void> desativar(HttpServletRequest http) {
        servico.desativarConta(logado.id(http));
        return ResponseEntity.noContent().build();
    }

    /** RF30 — exclusao por anonimizacao, preservando o historico de servicos. */
    @PostMapping("/excluir")
    public ResponseEntity<Void> excluir(HttpServletRequest http) {
        servico.solicitarExclusao(logado.id(http));
        // A conta deixa de autenticar na hora, nao so no proximo login.
        HttpSession sessao = http.getSession(false);
        if (sessao != null) sessao.invalidate();
        return ResponseEntity.noContent().build();
    }
}
