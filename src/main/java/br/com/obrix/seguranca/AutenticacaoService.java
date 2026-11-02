package br.com.obrix.seguranca;

import br.com.obrix.shared.RegraDeNegocioException;
import br.com.obrix.usuario.Usuario;
import br.com.obrix.usuario.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacaoService {

    private static final int LIMITE_TENTATIVAS = 5;
    private static final int MINUTOS_BLOQUEIO = 15;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public AutenticacaoService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Transactional
    public SessaoUsuario autenticar(String email, String senha) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElse(null);

        // Resposta generica: e-mail inexistente e senha errada dizem a mesma
        // coisa, para nao permitir descobrir quem tem conta na plataforma.
        if (usuario == null) {
            throw new RegraDeNegocioException("CREDENCIAIS_INVALIDAS",
                    "E-mail ou senha incorretos.");
        }
        if (usuario.estaBloqueado()) {
            throw new RegraDeNegocioException("CONTA_BLOQUEADA",
                    "Conta bloqueada temporariamente. Tente novamente em alguns minutos.");
        }
        if (!encoder.matches(senha, usuario.getSenhaHash())) {
            usuario.registrarFalhaDeLogin(LIMITE_TENTATIVAS, MINUTOS_BLOQUEIO);
            throw new RegraDeNegocioException("CREDENCIAIS_INVALIDAS",
                    "E-mail ou senha incorretos.");
        }
        if (!usuario.podeOperar()) {
            throw new RegraDeNegocioException("CONTA_INDISPONIVEL",
                    "Esta conta nao esta ativa.");
        }

        usuario.registrarLoginBemSucedido();
        return new SessaoUsuario(usuario.getId(), usuario.getNome(),
                usuario.getEmail(), usuario.getPerfil());
    }
}
