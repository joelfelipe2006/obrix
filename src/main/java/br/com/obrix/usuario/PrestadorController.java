package br.com.obrix.usuario;

import br.com.obrix.usuario.dto.Dtos.*;
import br.com.obrix.shared.Pagina;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/prestadores")
public class PrestadorController {

    private static final int TAMANHO_PAGINA = 20;

    private final UsuarioService servico;

    public PrestadorController(UsuarioService servico) { this.servico = servico; }

    /** RF08/RF09/RF10 — busca por categoria, filtro por regiao, ordenada por nota. */
    @GetMapping("/busca")
    public Pagina<PrestadorResumoResponse> buscar(
            @RequestParam Long categoriaId,
            @RequestParam(required = false) Long regiaoId,
            @RequestParam(defaultValue = "0") int pagina) {
        return Pagina.de(servico.buscar(categoriaId, regiaoId,
                PageRequest.of(Math.max(pagina, 0), TAMANHO_PAGINA)));
    }

    @GetMapping("/{id}/publico")
    public PrestadorPublicoResponse perfilPublico(@PathVariable Long id) {
        return servico.perfilPublico(id);
    }
}
