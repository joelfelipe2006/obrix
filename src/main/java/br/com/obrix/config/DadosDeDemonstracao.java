package br.com.obrix.config;

import br.com.obrix.catalogo.CategoriaServico;
import br.com.obrix.catalogo.CategoriaServicoRepository;
import br.com.obrix.catalogo.RegiaoAtuacao;
import br.com.obrix.catalogo.RegiaoAtuacaoRepository;
import br.com.obrix.solicitacao.Solicitacao;
import br.com.obrix.solicitacao.SolicitacaoService;
import br.com.obrix.usuario.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Massa de demonstracao dos perfis dev e demo.
 *
 * <p>Reproduz os mesmos servicos dos mockups do documento, para que a aplicacao
 * suba com o painel ja populado e cada status do ciclo visivel.</p>
 *
 * <p>O perfil demo combina com prod para popular um banco persistente. Por isso a
 * carga e idempotente (so roda se a cliente de exemplo ainda nao existe) e atomica:
 * uma falha no meio desfaz tudo, em vez de deixar meia massa que bloquearia a
 * proxima tentativa. O perfil prod sozinho nunca ativa esta classe.</p>
 */
@Configuration
@Profile({"dev", "demo"})
public class DadosDeDemonstracao {

    private static final Logger log = LoggerFactory.getLogger(DadosDeDemonstracao.class);
    private static final String SENHA = "obrix123";
    private static final String EMAIL_SENTINELA = "ana@exemplo.com";

    @Bean
    ApplicationRunner semear(UsuarioRepository usuarios,
                             ClienteRepository clientes,
                             PrestadorRepository prestadores,
                             CategoriaServicoRepository categorias,
                             RegiaoAtuacaoRepository regioes,
                             SolicitacaoService servico,
                             PasswordEncoder encoder,
                             PlatformTransactionManager transacoes) {
        return args -> {
            if (usuarios.existsByEmailIgnoreCase(EMAIL_SENTINELA)) {
                log.info("Dados de demonstracao ja existem; nada foi inserido.");
                return;
            }
            new TransactionTemplate(transacoes).executeWithoutResult(status ->
                    popular(clientes, prestadores, categorias, regioes, servico, encoder));
        };
    }

    private void popular(ClienteRepository clientes, PrestadorRepository prestadores,
                         CategoriaServicoRepository categorias, RegiaoAtuacaoRepository regioes,
                         SolicitacaoService servico, PasswordEncoder encoder) {

        CategoriaServico eletrica   = categoria(categorias, "Eletrica");
        CategoriaServico hidraulica = categoria(categorias, "Hidraulica");
        CategoriaServico pintura    = categoria(categorias, "Pintura");
        CategoriaServico jardinagem = categoria(categorias, "Jardinagem");

        RegiaoAtuacao setorSul    = regiao(regioes, "Setor Sul");
        RegiaoAtuacao setorBueno  = regiao(regioes, "Setor Bueno");
        RegiaoAtuacao jdAmerica   = regiao(regioes, "Jardim America");

        String hash = encoder.encode(SENHA);

        Endereco enderecoAna = new Endereco("74080-010", "Rua 84", "120",
                "Setor Sul", "Goiania", "GO");
        Cliente ana = new Cliente("Ana Carolina Souza", "ana@exemplo.com",
                "(62) 98111-4512", hash, enderecoAna);
        ana.aceitarTermos("1.0");
        ana.verificarEmail();
        clientes.save(ana);

        Prestador carlos = prestador(prestadores, "Carlos Mendes", "carlos@exemplo.com",
                "(62) 98222-1180", hash, "12345678901",
                "Eletricista com 12 anos de experiencia em instalacoes residenciais.",
                List.of(eletrica), List.of(setorSul, setorBueno));

        Prestador rita = prestador(prestadores, "Rita Alves", "rita@exemplo.com",
                "(62) 98333-2244", hash, "23456789012",
                "Pintora residencial. Trabalho limpo e prazo combinado.",
                List.of(pintura), List.of(setorBueno, setorSul));

        Prestador joao = prestador(prestadores, "Joao Ferreira", "joao@exemplo.com",
                "(62) 98444-7788", hash, "34567890123",
                "Jardinagem, poda e manutencao de areas verdes.",
                List.of(jardinagem), List.of(jdAmerica, setorSul));

        Prestador marcos = prestador(prestadores, "Marcos Pereira", "marcos@exemplo.com",
                "(62) 98555-9090", hash, "45678901234",
                "Encanador. Vazamentos, caixa d'agua e instalacoes.",
                List.of(hidraulica), List.of(setorSul));

        Long anaId = ana.getId();

        // 1. Aguardando confirmacao — o caso dos mockups: R$ 280,00, comissao R$ 28,00.
        Solicitacao chuveiro = servico.criar(anaId, carlos.getId(), eletrica.getId(),
                setorSul.getId(), "Instalacao de chuveiro eletrico 220v no banheiro social.",
                LocalDate.now().plusDays(2), enderecoAna);
        servico.aceitar(chuveiro.getId(), carlos.getId());
        servico.iniciarExecucao(chuveiro.getId(), carlos.getId());
        servico.registrarConclusao(chuveiro.getId(), carlos.getId(), new BigDecimal("280.00"));

        // 2. Em execucao.
        Solicitacao pinturaSala = servico.criar(anaId, rita.getId(), pintura.getId(),
                setorSul.getId(), "Pintura da sala e do corredor, tinta ja comprada.",
                LocalDate.now().plusDays(5), enderecoAna);
        servico.aceitar(pinturaSala.getId(), rita.getId());
        servico.iniciarExecucao(pinturaSala.getId(), rita.getId());

        // 3. Concluida e avaliada — gerou cobranca de 10%.
        Solicitacao tomadas = servico.criar(anaId, carlos.getId(), eletrica.getId(),
                setorSul.getId(), "Troca de quatro tomadas da cozinha.",
                LocalDate.now().minusDays(10), enderecoAna);
        servico.aceitar(tomadas.getId(), carlos.getId());
        servico.iniciarExecucao(tomadas.getId(), carlos.getId());
        servico.registrarConclusao(tomadas.getId(), carlos.getId(), new BigDecimal("150.00"));
        servico.confirmar(tomadas.getId(), anaId, new BigDecimal("150.00"));
        servico.avaliar(tomadas.getId(), anaId, 5, "Pontual, explicou tudo e deixou limpo.");

        // 4. Solicitada — aguardando resposta do profissional.
        servico.criar(anaId, joao.getId(), jardinagem.getId(), setorSul.getId(),
                "Poda de duas arvores no quintal dos fundos.",
                LocalDate.now().plusDays(7), enderecoAna);

        // 5. Contestada — nenhuma cobranca foi gerada.
        Solicitacao torneira = servico.criar(anaId, marcos.getId(), hidraulica.getId(),
                setorSul.getId(), "Conserto de vazamento na torneira da pia.",
                LocalDate.now().minusDays(3), enderecoAna);
        servico.aceitar(torneira.getId(), marcos.getId());
        servico.iniciarExecucao(torneira.getId(), marcos.getId());
        servico.registrarConclusao(torneira.getId(), marcos.getId(), new BigDecimal("190.00"));
        servico.contestar(torneira.getId(), anaId, "A torneira voltou a pingar no dia seguinte.");

        log.info("Dados de demonstracao criados. Entre com ana@exemplo.com ou carlos@exemplo.com, senha {}", SENHA);
    }

    private CategoriaServico categoria(CategoriaServicoRepository repo, String nome) {
        return repo.findAllByOrderByNomeAsc().stream()
                .filter(c -> c.getNome().equalsIgnoreCase(nome))
                .findFirst().orElseThrow(() -> new IllegalStateException("Categoria ausente: " + nome));
    }

    private RegiaoAtuacao regiao(RegiaoAtuacaoRepository repo, String bairro) {
        return repo.findAllByOrderByCidadeAscBairroAsc().stream()
                .filter(r -> r.getBairro().equalsIgnoreCase(bairro))
                .findFirst().orElseThrow(() -> new IllegalStateException("Regiao ausente: " + bairro));
    }

    private Prestador prestador(PrestadorRepository repo, String nome, String email,
                                String telefone, String hash, String cpf, String descricao,
                                List<CategoriaServico> categorias, List<RegiaoAtuacao> regioes) {
        Prestador p = new Prestador(nome, email, telefone, hash, cpf, descricao);
        categorias.forEach(p::adicionarCategoria);
        regioes.forEach(p::adicionarRegiao);
        p.aceitarTermos("1.0");
        p.verificarEmail();
        return repo.save(p);
    }
}
