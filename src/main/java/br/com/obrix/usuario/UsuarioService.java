package br.com.obrix.usuario;

import br.com.obrix.avaliacao.Avaliacao;
import br.com.obrix.avaliacao.AvaliacaoRepository;
import br.com.obrix.catalogo.CategoriaServico;
import br.com.obrix.catalogo.CategoriaServicoRepository;
import br.com.obrix.catalogo.RegiaoAtuacao;
import br.com.obrix.catalogo.RegiaoAtuacaoRepository;
import br.com.obrix.shared.NaoEncontradoException;
import br.com.obrix.shared.RegraDeNegocioException;
import br.com.obrix.usuario.dto.Dtos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class UsuarioService {

    private static final String VERSAO_TERMOS = "1.0";
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final PrestadorRepository prestadores;
    private final CategoriaServicoRepository categorias;
    private final RegiaoAtuacaoRepository regioes;
    private final AvaliacaoRepository avaliacoes;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarios, ClienteRepository clientes,
                          PrestadorRepository prestadores, CategoriaServicoRepository categorias,
                          RegiaoAtuacaoRepository regioes, AvaliacaoRepository avaliacoes,
                          PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.prestadores = prestadores;
        this.categorias = categorias;
        this.regioes = regioes;
        this.avaliacoes = avaliacoes;
        this.encoder = encoder;
    }

    // ------------------------------------------------------------------
    // Cadastro
    // ------------------------------------------------------------------

    @Transactional
    public Cliente cadastrarCliente(CadastroClienteRequest req) {
        exigirEmailLivre(req.email());

        Cliente cliente = new Cliente(req.nome(), req.email(), req.telefone(),
                encoder.encode(req.senha()), converter(req.endereco()));
        cliente.aceitarTermos(VERSAO_TERMOS);
        // Em producao a conta ficaria PENDENTE ate a validacao do e-mail (RF28).
        // Sem provedor SMTP configurado, a verificacao e dispensada no MVP.
        cliente.verificarEmail();
        return clientes.save(cliente);
    }

    @Transactional
    public Prestador cadastrarPrestador(CadastroPrestadorRequest req) {
        exigirEmailLivre(req.email());

        String cpf = req.cpf().replaceAll("\\D", "");
        if (prestadores.existsByCpf(cpf)) {
            throw new RegraDeNegocioException("CPF_EM_USO", "Este CPF ja esta cadastrado.");
        }

        Prestador prestador = new Prestador(req.nome(), req.email(), req.telefone(),
                encoder.encode(req.senha()), cpf, req.descricaoProfissional());

        // Multiplicidade 1..* do diagrama validada no cadastro, nao so no desenho.
        aplicarCatalogo(prestador, req.categoriaIds(), req.regiaoIds());
        prestador.aceitarTermos(VERSAO_TERMOS);
        prestador.verificarEmail();
        return prestadores.save(prestador);
    }

    private void exigirEmailLivre(String email) {
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new RegraDeNegocioException("EMAIL_EM_USO", "Este e-mail ja esta cadastrado.");
        }
    }

    private void aplicarCatalogo(Prestador prestador, List<Long> categoriaIds, List<Long> regiaoIds) {
        if (categoriaIds == null || categoriaIds.isEmpty()) {
            throw new RegraDeNegocioException("CATEGORIA_OBRIGATORIA",
                    "Mantenha ao menos uma categoria de atuacao.");
        }
        if (regiaoIds == null || regiaoIds.isEmpty()) {
            throw new RegraDeNegocioException("REGIAO_OBRIGATORIA",
                    "Mantenha ao menos uma regiao de atuacao.");
        }
        prestador.getCategorias().clear();
        prestador.getRegioes().clear();
        categoriaIds.forEach(id -> prestador.adicionarCategoria(categorias.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Categoria", id))));
        regiaoIds.forEach(id -> prestador.adicionarRegiao(regioes.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Regiao", id))));
    }

    // ------------------------------------------------------------------
    // Busca e perfil publico
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<PrestadorResumoResponse> buscar(Long categoriaId, Long regiaoId, Pageable pageable) {
        return prestadores.buscar(categoriaId, regiaoId, pageable).map(this::resumo);
    }

    @Transactional(readOnly = true)
    public PrestadorPublicoResponse perfilPublico(Long id) {
        Prestador p = prestadores.buscarComCatalogo(id)
                .orElseThrow(() -> new NaoEncontradoException("Prestador", id));

        List<AvaliacaoResponse> notas = avaliacoes.doPrestador(id).stream()
                .map(a -> new AvaliacaoResponse(a.getNota(), a.getComentario(),
                        a.getDataHora().format(DATA_HORA),
                        a.getSolicitacao().getCategoria().getNome()))
                .toList();

        return new PrestadorPublicoResponse(p.getId(), p.getNome(), p.getDescricaoProfissional(),
                p.getNotaMedia(), notas.size(), nomesCategorias(p), nomesRegioes(p), notas);
    }

    private PrestadorResumoResponse resumo(Prestador p) {
        return new PrestadorResumoResponse(p.getId(), p.getNome(), p.getDescricaoProfissional(),
                p.getNotaMedia(), avaliacoes.quantidadeDoPrestador(p.getId()),
                nomesCategorias(p), nomesRegioes(p));
    }

    private List<String> nomesCategorias(Prestador p) {
        return p.getCategorias().stream().map(CategoriaServico::getNome).sorted().toList();
    }

    private List<String> nomesRegioes(Prestador p) {
        return p.getRegioes().stream().map(RegiaoAtuacao::descrever)
                .sorted(Comparator.naturalOrder()).toList();
    }

    // ------------------------------------------------------------------
    // Minha conta
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PerfilResponse perfil(Long usuarioId) {
        Usuario u = usuarios.findById(usuarioId)
                .orElseThrow(() -> new NaoEncontradoException("Usuario", usuarioId));

        if (u instanceof Prestador p) {
            Prestador comCatalogo = prestadores.buscarComCatalogo(p.getId()).orElse(p);
            return new PerfilResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(),
                    u.getPerfil(), u.getStatus().name(), u.isEmailVerificado(),
                    comCatalogo.getCpfMascarado(), comCatalogo.getDescricaoProfissional(),
                    comCatalogo.getCategorias().stream().map(CategoriaServico::getId).toList(),
                    comCatalogo.getRegioes().stream().map(RegiaoAtuacao::getId).toList(),
                    null, "1.0");
        }

        Cliente c = (Cliente) u;
        return new PerfilResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(),
                u.getPerfil(), u.getStatus().name(), u.isEmailVerificado(),
                null, null, List.of(), List.of(), converter(c.getEndereco()), "1.0");
    }

    @Transactional
    public PerfilResponse atualizarPerfil(Long usuarioId, AtualizarPerfilRequest req) {
        Usuario u = usuarios.findById(usuarioId)
                .orElseThrow(() -> new NaoEncontradoException("Usuario", usuarioId));
        u.setNome(req.nome());
        u.setTelefone(req.telefone());

        if (u instanceof Prestador p) {
            Prestador comCatalogo = prestadores.buscarComCatalogo(p.getId()).orElse(p);
            comCatalogo.setDescricaoProfissional(req.descricaoProfissional());
            aplicarCatalogo(comCatalogo, req.categoriaIds(), req.regiaoIds());
        } else if (u instanceof Cliente c && req.endereco() != null) {
            c.setEndereco(converter(req.endereco()));
        }
        return perfil(usuarioId);
    }

    @Transactional
    public void alterarSenha(Long usuarioId, AlterarSenhaRequest req) {
        Usuario u = usuarios.findById(usuarioId)
                .orElseThrow(() -> new NaoEncontradoException("Usuario", usuarioId));
        if (!encoder.matches(req.senhaAtual(), u.getSenhaHash())) {
            throw new RegraDeNegocioException("SENHA_INCORRETA", "A senha atual nao confere.");
        }
        u.setSenhaHash(encoder.encode(req.novaSenha()));
    }

    @Transactional
    public void desativarConta(Long usuarioId) {
        usuarios.findById(usuarioId)
                .orElseThrow(() -> new NaoEncontradoException("Usuario", usuarioId))
                .desativarConta();
    }

    /** Exclusao e anonimizacao em ate 30 dias; o historico do servico permanece. */
    @Transactional
    public void solicitarExclusao(Long usuarioId) {
        usuarios.findById(usuarioId)
                .orElseThrow(() -> new NaoEncontradoException("Usuario", usuarioId))
                .solicitarExclusao();
    }

    private Endereco converter(EnderecoDto dto) {
        if (dto == null) return null;
        return new Endereco(dto.cep(), dto.logradouro(), dto.numero(),
                dto.bairro(), dto.cidade(), dto.estado());
    }

    private EnderecoDto converter(Endereco e) {
        if (e == null) return null;
        return new EnderecoDto(e.getCep(), e.getLogradouro(), e.getNumero(),
                e.getBairro(), e.getCidade(), e.getEstado());
    }
}
