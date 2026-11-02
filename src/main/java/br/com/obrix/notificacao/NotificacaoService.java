package br.com.obrix.notificacao;

import br.com.obrix.usuario.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacaoService {

    private final NotificacaoRepository repositorio;

    public NotificacaoService(NotificacaoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public void criar(Usuario destinatario, String mensagem) {
        repositorio.save(new Notificacao(destinatario, mensagem));
    }

    @Transactional(readOnly = true)
    public Page<Notificacao> doUsuario(Long usuarioId, Pageable pageable) {
        return repositorio.findByUsuarioIdOrderByDataHoraDesc(usuarioId, pageable);
    }

    @Transactional(readOnly = true)
    public long naoLidas(Long usuarioId) {
        return repositorio.countByUsuarioIdAndLidaFalse(usuarioId);
    }

    @Transactional
    public void marcarComoLida(Long id) {
        repositorio.findById(id).ifPresent(Notificacao::marcarComoLida);
    }
}
