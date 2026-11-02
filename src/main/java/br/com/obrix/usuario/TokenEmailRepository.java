package br.com.obrix.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TokenEmailRepository extends JpaRepository<TokenEmail, Long> {
    Optional<TokenEmail> findByTokenHashAndTipo(String tokenHash, TipoToken tipo);
}
