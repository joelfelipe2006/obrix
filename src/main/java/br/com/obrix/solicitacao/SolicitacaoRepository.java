package br.com.obrix.solicitacao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {

    Page<Solicitacao> findByClienteIdOrderByDataCriacaoDesc(Long clienteId, Pageable pageable);

    Page<Solicitacao> findByPrestadorIdOrderByDataCriacaoDesc(Long prestadorId, Pageable pageable);

    @Query("select s from Solicitacao s left join fetch s.historico where s.id = :id")
    Optional<Solicitacao> buscarComHistorico(@Param("id") Long id);

    List<Solicitacao> findByStatus(StatusSolicitacao status);
}
