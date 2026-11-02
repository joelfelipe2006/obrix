package br.com.obrix.avaliacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    boolean existsBySolicitacaoId(Long solicitacaoId);

    @Query("select a from Avaliacao a where a.solicitacao.prestador.id = :prestadorId order by a.dataHora desc")
    List<Avaliacao> doPrestador(@Param("prestadorId") Long prestadorId);

    @Query("select coalesce(avg(a.nota), 0) from Avaliacao a where a.solicitacao.prestador.id = :prestadorId")
    double mediaDoPrestador(@Param("prestadorId") Long prestadorId);

    @Query("select count(a) from Avaliacao a where a.solicitacao.prestador.id = :prestadorId")
    long quantidadeDoPrestador(@Param("prestadorId") Long prestadorId);
}
