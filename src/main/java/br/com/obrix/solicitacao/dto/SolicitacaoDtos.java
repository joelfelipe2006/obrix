package br.com.obrix.solicitacao.dto;

import br.com.obrix.usuario.dto.Dtos.EnderecoDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class SolicitacaoDtos {

    private SolicitacaoDtos() { }

    public record CriarRequest(
            @NotNull Long prestadorId,
            @NotNull Long categoriaId,
            @NotNull Long regiaoId,
            @NotBlank @Size(max = 1000) String descricao,
            @NotNull @FutureOrPresent(message = "A data desejada nao pode estar no passado")
            LocalDate dataDesejada,
            @NotNull @Valid EnderecoDto enderecoExecucao) { }

    public record MotivoRequest(
            @NotBlank(message = "O motivo e obrigatorio") @Size(max = 500) String motivo) { }

    public record ConclusaoRequest(
            @NotNull @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
            BigDecimal valorFinal) { }

    public record AvaliarRequest(
            @Min(1) @Max(5) int nota,
            @Size(max = 500) String comentario) { }

    public record HistoricoResponse(
            String statusAnterior, String statusNovo,
            String dataHora, String motivo, String autor) { }

    public record ContatoResponse(String nome, String telefone, String email) { }

    public record CobrancaResponse(
            Long id, BigDecimal valorServico, BigDecimal percentualComissao,
            BigDecimal valorComissao, String dataGeracao) { }

    public record SolicitacaoResponse(
            Long id,
            String descricao,
            String categoria,
            String regiao,
            String status,
            String statusRotulo,
            String dataDesejada,
            String dataCriacao,
            BigDecimal valorFinal,
            String motivo,
            String dataRegistroConclusao,
            String prazoConfirmacao,
            boolean confirmacaoAutomatica,
            boolean foiContestada,
            boolean podeSerCancelada,
            String enderecoExecucao,
            ContatoResponse cliente,
            ContatoResponse prestador,
            CobrancaResponse cobranca,
            Integer notaAvaliacao,
            List<HistoricoResponse> historico,
            List<String> acoesDisponiveis) { }

    public record ExtratoResponse(
            List<ItemExtrato> itens,
            BigDecimal totalComissao,
            BigDecimal totalServicos) { }

    public record ItemExtrato(
            Long solicitacaoId, String servico, String cliente, String data,
            BigDecimal valorServico, BigDecimal valorComissao) { }
}
