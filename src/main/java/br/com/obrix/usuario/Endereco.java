package br.com.obrix.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Objeto de valor. Nao tem identidade propria: vive dentro de quem o contem.
 *
 * <p>Na solicitacao ele e uma copia, nao uma referencia ao cadastro do cliente —
 * se o cliente se mudar, o endereco de um servico ja registrado nao muda junto.</p>
 */
@Embeddable
public class Endereco {

    @Column(length = 9)   private String cep;
    @Column(length = 160) private String logradouro;
    @Column(length = 10)  private String numero;
    @Column(length = 80)  private String bairro;
    @Column(length = 80)  private String cidade;
    @Column(length = 2)   private String estado;

    protected Endereco() { }

    public Endereco(String cep, String logradouro, String numero,
                    String bairro, String cidade, String estado) {
        this.cep = cep;
        this.logradouro = logradouro;
        this.numero = numero;
        this.bairro = bairro;
        this.cidade = cidade;
        this.estado = estado;
    }

    /** Copia defensiva — e o que mantem a composicao do diagrama honesta. */
    public Endereco copia() {
        return new Endereco(cep, logradouro, numero, bairro, cidade, estado);
    }

    public String formatar() {
        return "%s, %s - %s, %s/%s".formatted(logradouro, numero, bairro, cidade, estado);
    }

    public String getCep() { return cep; }
    public String getLogradouro() { return logradouro; }
    public String getNumero() { return numero; }
    public String getBairro() { return bairro; }
    public String getCidade() { return cidade; }
    public String getEstado() { return estado; }
}
