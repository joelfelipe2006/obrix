package br.com.obrix.shared;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Converte toda excecao em {@link ApiError}: { codigo, mensagem, campos }.
 *
 * <p>Erros de quem chama (entrada malformada, rota ou metodo errado, conflito)
 * saem com o status certo e geram so uma linha de aviso no log, sem stack
 * trace. Stack trace fica reservado para o que e de fato inesperado.</p>
 */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    // ------------------------------------------------------------------
    // Regras de negocio e recursos
    // ------------------------------------------------------------------

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ApiError> regra(RegraDeNegocioException e) {
        return ResponseEntity.unprocessableEntity()
                .body(new ApiError(e.getCodigo(), e.getMessage()));
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<ApiError> naoEncontrado(NaoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("NAO_ENCONTRADO", e.getMessage()));
    }

    @ExceptionHandler(NaoAutorizadoException.class)
    public ResponseEntity<ApiError> naoAutorizado(NaoAutorizadoException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError("NAO_AUTORIZADO", e.getMessage()));
    }

    // ------------------------------------------------------------------
    // Entrada invalida -> 400
    // ------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacao(MethodArgumentNotValidException e) {
        List<String> campos = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(new ApiError("DADOS_INVALIDOS", "Verifique os campos informados.", campos));
    }

    /** JSON quebrado, corpo ausente ou valor de tipo errado no corpo (ex.: data em outro formato). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> corpoIlegivel(HttpMessageNotReadableException e, HttpServletRequest req) {
        avisar(req, e, 400);
        return ResponseEntity.badRequest()
                .body(new ApiError("DADOS_INVALIDOS",
                        "O corpo da requisicao esta ausente ou em formato invalido."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> parametroAusente(MissingServletRequestParameterException e,
                                                     HttpServletRequest req) {
        avisar(req, e, 400);
        return ResponseEntity.badRequest()
                .body(new ApiError("DADOS_INVALIDOS", "Parametro obrigatorio ausente.",
                        List.of(e.getParameterName() + ": obrigatorio")));
    }

    /** Parametro de URL ou de consulta com tipo errado (ex.: /solicitacoes/abc). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> tipoInvalido(MethodArgumentTypeMismatchException e,
                                                 HttpServletRequest req) {
        avisar(req, e, 400);
        return ResponseEntity.badRequest()
                .body(new ApiError("DADOS_INVALIDOS", "Parametro com valor invalido.",
                        List.of(e.getName() + ": valor invalido")));
    }

    // ------------------------------------------------------------------
    // Rota e metodo -> 404 e 405
    // ------------------------------------------------------------------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> metodo(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        avisar(req, e, 405);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ApiError("METODO_NAO_PERMITIDO",
                        "O metodo " + e.getMethod() + " nao e aceito nesta rota."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> rotaInexistente(NoResourceFoundException e, HttpServletRequest req) {
        avisar(req, e, 404);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("NAO_ENCONTRADO", "Rota inexistente."));
    }

    // ------------------------------------------------------------------
    // Conflitos -> 409
    // ------------------------------------------------------------------

    /** Duas acoes ao mesmo tempo sobre o mesmo registro (@Version). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> concorrencia(OptimisticLockingFailureException e, HttpServletRequest req) {
        avisar(req, e, 409);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("CONFLITO",
                        "Este registro foi alterado por outra acao. Atualize a pagina e tente de novo."));
    }

    /** A mensagem do banco traz SQL e valores: nunca vai para a resposta nem para o log. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> restricao(DataIntegrityViolationException e, HttpServletRequest req) {
        avisar(req, e, 409);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("CONFLITO",
                        "A operacao conflita com dados ja existentes."));
    }

    // ------------------------------------------------------------------
    // Inesperado -> 500
    // ------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> inesperado(Exception e, HttpServletRequest req) {
        log.error("Erro inesperado em {} {}", req.getMethod(), req.getRequestURI(), e);
        return ResponseEntity.internalServerError()
                .body(new ApiError("ERRO_INTERNO", "Ocorreu um erro ao processar a requisicao."));
    }

    /**
     * Uma linha de aviso, sem stack trace. Registra so o tipo da excecao: as
     * mensagens podem conter o corpo enviado ou valores do banco.
     */
    private static void avisar(HttpServletRequest req, Exception e, int status) {
        log.warn("{} {} -> {} ({})", req.getMethod(), req.getRequestURI(), status,
                e.getClass().getSimpleName());
    }
}
