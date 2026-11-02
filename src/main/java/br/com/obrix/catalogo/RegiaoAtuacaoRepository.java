package br.com.obrix.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RegiaoAtuacaoRepository extends JpaRepository<RegiaoAtuacao, Long> {
    List<RegiaoAtuacao> findAllByOrderByCidadeAscBairroAsc();
    List<RegiaoAtuacao> findByCidadeOrderByBairroAsc(String cidade);
}
