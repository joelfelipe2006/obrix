package br.com.obrix.notificacao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    Page<Notificacao> findByUsuarioIdOrderByDataHoraDesc(Long usuarioId, Pageable pageable);
    long countByUsuarioIdAndLidaFalse(Long usuarioId);
}
