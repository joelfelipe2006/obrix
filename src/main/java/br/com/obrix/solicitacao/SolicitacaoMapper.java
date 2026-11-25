package br.com.obrix.solicitacao;

import br.com.obrix.cobranca.Cobranca;
import br.com.obrix.solicitacao.dto.SolicitacaoDtos.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Converte a entidade em resposta da API. Entidade nunca sai do back-end. */
@Component
public class SolicitacaoMapper {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final int prazoDias;

    public SolicitacaoMapper(SolicitacaoService servico) {
        this.prazoDias = servico.getPrazoConfirmacaoDias();
    }

    public SolicitacaoResponse paraResposta(Solicitacao s, Long usuarioQueVe) {
        boolean ehCliente = s.getCliente().getId().equals(usuarioQueVe);
        boolean contato = s.contatoLiberado();
        // Confirmacao as cegas: o cliente so ve o valor do prestador depois de
        // concluida. Antes disso ele copiaria o numero ao confirmar, e a checagem
        // contra a subdeclaracao (informarValorPago) perderia o sentido.
        boolean valoresVisiveis = !ehCliente || s.getStatus() == StatusSolicitacao.CONCLUIDA;

        return new SolicitacaoResponse(
                s.getId(),
                s.getDescricao(),
                s.getCategoria().getNome(),
                s.getRegiao().descrever(),
                s.getStatus().name(),
                rotulo(s.getStatus()),
                fmt(s.getDataDesejada()),
                fmt(s.getDataCriacao()),
                valoresVisiveis ? s.getValorFinal() : null,
                valoresVisiveis ? s.getValorConfirmadoCliente() : null,
                s.getMotivo(),
                fmt(s.getDataRegistroConclusao()),
                fmt(s.getPrazoConfirmacao(prazoDias)),
                s.isConfirmacaoAutomatica(),
                s.isFoiContestada(),
                s.podeSerCancelada(),
                s.getEnderecoExecucao() == null ? null : s.getEnderecoExecucao().formatar(),
                // RF14: os dados de contato so aparecem depois do aceite.
                contato ? new ContatoResponse(s.getCliente().getNome(),
                        s.getCliente().getTelefone(), s.getCliente().getEmail()) : null,
                contato ? new ContatoResponse(s.getPrestador().getNome(),
                        s.getPrestador().getTelefone(), s.getPrestador().getEmail())
                        : new ContatoResponse(s.getPrestador().getNome(), null, null),
                cobranca(s.getCobranca()),
                s.getAvaliacao() == null ? null : s.getAvaliacao().getNota(),
                s.getHistorico().stream()
                        .map(h -> new HistoricoResponse(
                                h.getStatusAnterior() == null ? null : h.getStatusAnterior().name(),
                                h.getStatusNovo().name(),
                                fmt(h.getDataHora()),
                                h.getMotivo(),
                                h.getAutor().name()))
                        .toList(),
                acoes(s, ehCliente));
    }

    /**
     * O front usa esta lista para mostrar, em cada cartao, apenas as acoes
     * permitidas naquele status — a regra fica num lugar so.
     */
    private List<String> acoes(Solicitacao s, boolean ehCliente) {
        List<String> acoes = new ArrayList<>();
        switch (s.getStatus()) {
            case SOLICITADA -> {
                if (ehCliente) acoes.add("cancelar");
                else { acoes.add("aceitar"); acoes.add("recusar"); }
            }
            case ACEITA -> {
                acoes.add("cancelar");
                if (!ehCliente) acoes.add("iniciar");
            }
            case EM_EXECUCAO -> { if (!ehCliente) acoes.add("concluir"); }
            case AGUARDANDO_CONFIRMACAO -> {
                if (ehCliente) { acoes.add("confirmar"); acoes.add("contestar"); }
            }
            case CONTESTADA -> { if (!ehCliente) acoes.add("concluir"); }
            case CONCLUIDA -> {
                if (ehCliente && s.getAvaliacao() == null) acoes.add("avaliar");
            }
            default -> { }
        }
        return acoes;
    }

    private CobrancaResponse cobranca(Cobranca c) {
        if (c == null) return null;
        return new CobrancaResponse(c.getId(), c.getValorServico(),
                c.getPercentualComissao(), c.getValorComissao(), fmt(c.getDataGeracao()));
    }

    public static String rotulo(StatusSolicitacao status) {
        return switch (status) {
            case SOLICITADA -> "Solicitada";
            case ACEITA -> "Aceita";
            case RECUSADA -> "Recusada";
            case EM_EXECUCAO -> "Em execucao";
            case AGUARDANDO_CONFIRMACAO -> "Aguardando confirmacao";
            case CONTESTADA -> "Contestada";
            case CONCLUIDA -> "Concluida";
            case CANCELADA -> "Cancelada";
        };
    }

    private static String fmt(LocalDateTime d) { return d == null ? null : d.format(DATA_HORA); }
    private static String fmt(java.time.LocalDate d) { return d == null ? null : d.format(DATA); }
}
