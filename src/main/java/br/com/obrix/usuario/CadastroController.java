package br.com.obrix.usuario;

import br.com.obrix.usuario.dto.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cadastro")
public class CadastroController {

    private final UsuarioService servico;

    public CadastroController(UsuarioService servico) { this.servico = servico; }

    @PostMapping("/cliente")
    public ResponseEntity<SessaoResponse> cliente(@Valid @RequestBody CadastroClienteRequest req) {
        Cliente c = servico.cadastrarCliente(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SessaoResponse(c.getId(), c.getNome(), c.getEmail(), c.getPerfil()));
    }

    @PostMapping("/prestador")
    public ResponseEntity<SessaoResponse> prestador(@Valid @RequestBody CadastroPrestadorRequest req) {
        Prestador p = servico.cadastrarPrestador(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SessaoResponse(p.getId(), p.getNome(), p.getEmail(), p.getPerfil()));
    }
}
