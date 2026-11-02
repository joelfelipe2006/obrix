package br.com.obrix.usuario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/** DTOs de entrada e saida. Entidades nunca sao expostas na API. */
public final class Dtos {

    private Dtos() { }

    public record EnderecoDto(
            @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP invalido") String cep,
            @NotBlank String logradouro,
            @NotBlank String numero,
            @NotBlank String bairro,
            @NotBlank String cidade,
            @NotBlank @Size(min = 2, max = 2) String estado) { }

    public record CadastroClienteRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email String email,
            @NotBlank @Size(max = 20) String telefone,
            @NotBlank @Size(min = 8, message = "A senha deve ter ao menos 8 caracteres")
            @Pattern(regexp = ".*[A-Za-z].*", message = "A senha deve conter letras")
            @Pattern(regexp = ".*\\d.*", message = "A senha deve conter numeros")
            String senha,
            @NotNull @Valid EnderecoDto endereco,
            @AssertTrue(message = "E preciso aceitar os termos de uso") boolean aceiteTermos) { }

    public record CadastroPrestadorRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email String email,
            @NotBlank @Size(max = 20) String telefone,
            @NotBlank @Size(min = 8, message = "A senha deve ter ao menos 8 caracteres")
            @Pattern(regexp = ".*[A-Za-z].*", message = "A senha deve conter letras")
            @Pattern(regexp = ".*\\d.*", message = "A senha deve conter numeros")
            String senha,
            @NotBlank @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}", message = "CPF invalido")
            String cpf,
            @Size(max = 500) String descricaoProfissional,
            @NotEmpty(message = "Selecione ao menos uma categoria") List<Long> categoriaIds,
            @NotEmpty(message = "Selecione ao menos uma regiao") List<Long> regiaoIds,
            @AssertTrue(message = "E preciso aceitar os termos de uso") boolean aceiteTermos) { }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) { }

    public record SessaoResponse(Long id, String nome, String email, String perfil) { }

    public record PrestadorResumoResponse(
            Long id, String nome, String descricaoProfissional,
            BigDecimal notaMedia, long quantidadeAvaliacoes,
            List<String> categorias, List<String> regioes) { }

    public record PrestadorPublicoResponse(
            Long id, String nome, String descricaoProfissional,
            BigDecimal notaMedia, long quantidadeAvaliacoes,
            List<String> categorias, List<String> regioes,
            List<AvaliacaoResponse> avaliacoes) { }

    public record AvaliacaoResponse(int nota, String comentario, String dataHora, String servico) { }

    public record PerfilResponse(
            Long id, String nome, String email, String telefone, String perfil,
            String status, boolean emailVerificado,
            String cpfMascarado, String descricaoProfissional,
            List<Long> categoriaIds, List<Long> regiaoIds,
            EnderecoDto endereco, String termosVersao) { }

    public record AtualizarPerfilRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Size(max = 20) String telefone,
            @Size(max = 500) String descricaoProfissional,
            List<Long> categoriaIds,
            List<Long> regiaoIds,
            @Valid EnderecoDto endereco) { }

    public record AlterarSenhaRequest(
            @NotBlank String senhaAtual,
            @NotBlank @Size(min = 8) String novaSenha) { }
}
