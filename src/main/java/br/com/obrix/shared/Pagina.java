package br.com.obrix.shared;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Formato de pagina da API, igual em todas as listas paginadas.
 *
 * <p>Existe para nao expor o Page do Spring, cujo JSON muda entre versoes.
 * {@code pagina} comeca em 0; {@code temMais} diz se vale pedir a proxima.</p>
 */
public record Pagina<T>(List<T> itens, int pagina, int tamanho, long total, boolean temMais) {

    public static <E, T> Pagina<T> de(Page<E> page, Function<E, T> conversor) {
        return new Pagina<>(
                page.getContent().stream().map(conversor).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.hasNext());
    }

    public static <T> Pagina<T> de(Page<T> page) {
        return de(page, Function.identity());
    }
}
