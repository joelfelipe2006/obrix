package br.com.obrix.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoriaServicoRepository extends JpaRepository<CategoriaServico, Long> {
    List<CategoriaServico> findAllByOrderByNomeAsc();
}
