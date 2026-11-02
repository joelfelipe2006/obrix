package br.com.obrix.cobranca;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface CobrancaRepository extends JpaRepository<Cobranca, Long> {

    boolean existsBySolicitacaoId(Long solicitacaoId);

    /** O extrato navega Cobranca -> Solicitacao -> Prestador (RF22). */
    @Query("select c from Cobranca c where c.solicitacao.prestador.id = :prestadorId order by c.dataGeracao desc")
    Page<Cobranca> doPrestador(@Param("prestadorId") Long prestadorId, Pageable pageable);

    @Query("select coalesce(sum(c.valorComissao), 0) from Cobranca c where c.solicitacao.prestador.id = :prestadorId")
    BigDecimal totalComissaoDoPrestador(@Param("prestadorId") Long prestadorId);
}
