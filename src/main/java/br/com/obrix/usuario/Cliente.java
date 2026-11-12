package br.com.obrix.usuario;

import jakarta.persistence.*;

/** Quem contrata. Nao tem CPF: nenhuma funcionalidade desta versao precisa dele. */
@Entity
@Table(name = "cliente")
@PrimaryKeyJoinColumn(name = "id")
public class Cliente extends Usuario {

    @Embedded
    private Endereco endereco;

    protected Cliente() { }

    public Cliente(String nome, String email, String telefone, String senhaHash, Endereco endereco) {
        super(nome, email, telefone, senhaHash);
        this.endereco = endereco;
    }

    @Override
    public String getPerfil() { return "CLIENTE"; }

    /** Alem dos dados comuns, remove o endereco do cadastro. */
    @Override
    public void anonimizar() {
        super.anonimizar();
        this.endereco = null;
    }

    public Endereco getEndereco() { return endereco; }
    public void setEndereco(Endereco endereco) { this.endereco = endereco; }
}
