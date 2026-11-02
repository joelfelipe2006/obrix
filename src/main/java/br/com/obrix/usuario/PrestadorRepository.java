package br.com.obrix.usuario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PrestadorRepository extends JpaRepository<Prestador, Long> {

    boolean existsByCpf(String cpf);

    @Query("""
           select distinct p from Prestador p
             join p.categorias c
             join p.regioes r
            where c.id = :categoriaId
              and (:regiaoId is null or r.id = :regiaoId)
              and p.status = br.com.obrix.usuario.StatusConta.ATIVA
            order by p.notaMedia desc, p.dataCadastro asc
           """)
    Page<Prestador> buscar(@Param("categoriaId") Long categoriaId,
                           @Param("regiaoId") Long regiaoId,
                           Pageable pageable);

    @Query("select p from Prestador p left join fetch p.categorias left join fetch p.regioes where p.id = :id")
    Optional<Prestador> buscarComCatalogo(@Param("id") Long id);
}
