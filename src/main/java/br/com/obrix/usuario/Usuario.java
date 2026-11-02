package br.com.obrix.usuario;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Classe abstrata: ninguem e "usuario generico" no Obrix — ou e cliente, ou e
 * prestador. Guarda apenas o que os dois tem em comum.
 *
 * <p>Heranca JOINED: o CPF fica fisicamente restrito a tabela do prestador,
 * que e quem recebe cobranca (RNF04, minimizacao de dados).</p>
 */
@Entity
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 160, unique = true)
    private String email;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusConta status = StatusConta.PENDENTE;

    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado = false;

    @Column(name = "data_cadastro", nullable = false)
    private LocalDateTime dataCadastro = LocalDateTime.now();

    @Column(name = "termos_versao", length = 10)
    private String termosVersao;

    @Column(name = "termos_aceitos_em")
    private LocalDateTime termosAceitosEm;

    @Column(name = "tentativas_login", nullable = false)
    private int tentativasLogin = 0;

    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;

    @Column(name = "data_solicitacao_exclusao")
    private LocalDateTime dataSolicitacaoExclusao;

    protected Usuario() { }

    protected Usuario(String nome, String email, String telefone, String senhaHash) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.senhaHash = senhaHash;
    }

    /** Qual papel esta pessoa exerce. Usado pelo front para escolher o painel. */
    public abstract String getPerfil();

    public boolean estaBloqueado() {
        return bloqueadoAte != null && bloqueadoAte.isAfter(LocalDateTime.now());
    }

    public boolean podeOperar() {
        return status == StatusConta.ATIVA;
    }

    public void registrarFalhaDeLogin(int limite, int minutosBloqueio) {
        this.tentativasLogin++;
        if (this.tentativasLogin >= limite) {
            this.bloqueadoAte = LocalDateTime.now().plusMinutes(minutosBloqueio);
            this.tentativasLogin = 0;
        }
    }

    public void registrarLoginBemSucedido() {
        this.tentativasLogin = 0;
        this.bloqueadoAte = null;
    }

    public void aceitarTermos(String versao) {
        this.termosVersao = versao;
        this.termosAceitosEm = LocalDateTime.now();
    }

    public void verificarEmail() {
        this.emailVerificado = true;
        if (this.status == StatusConta.PENDENTE) {
            this.status = StatusConta.ATIVA;
        }
    }

    public void desativarConta() { this.status = StatusConta.DESATIVADA; }
    public void reativarConta()  { this.status = StatusConta.ATIVA; }

    /** Exclusao e anonimizacao: o historico do servico sobrevive (RNF27). */
    public void solicitarExclusao() {
        this.status = StatusConta.EM_EXCLUSAO;
        this.dataSolicitacaoExclusao = LocalDateTime.now();
    }

    public void anonimizar() {
        this.nome = "Usuario removido";
        this.email = "removido+" + id + "@obrix.invalid";
        this.telefone = "";
        this.senhaHash = "";
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getTelefone() { return telefone; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public StatusConta getStatus() { return status; }
    public void setStatus(StatusConta status) { this.status = status; }
    public boolean isEmailVerificado() { return emailVerificado; }
    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public LocalDateTime getDataSolicitacaoExclusao() { return dataSolicitacaoExclusao; }
    public void setNome(String nome) { this.nome = nome; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
}
