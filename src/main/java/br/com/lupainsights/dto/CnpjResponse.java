package br.com.lupainsights.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CnpjResponse {

    @JsonProperty("razao_social")
    private String razaoSocial;

    @JsonProperty("capital_social")
    private String capitalSocial;

    private Porte porte;

    @JsonProperty("natureza_juridica")
    private NaturezaJuridica naturezaJuridica;

    private java.util.List<Socio> socios;

    private Simples simples;

    private Estabelecimento estabelecimento;

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public Estabelecimento getEstabelecimento() {
        return estabelecimento;
    }

    public void setEstabelecimento(Estabelecimento estabelecimento) {
        this.estabelecimento = estabelecimento;
    }

    public String getCapitalSocial() {
        return capitalSocial;
    }

    public void setCapitalSocial(String capitalSocial) {
        this.capitalSocial = capitalSocial;
    }

    public Porte getPorte() {
        return porte;
    }

    public void setPorte(Porte porte) {
        this.porte = porte;
    }

    public NaturezaJuridica getNaturezaJuridica() {
        return naturezaJuridica;
    }

    public void setNaturezaJuridica(NaturezaJuridica naturezaJuridica) {
        this.naturezaJuridica = naturezaJuridica;
    }

    public java.util.List<Socio> getSocios() {
        return socios;
    }

    public void setSocios(java.util.List<Socio> socios) {
        this.socios = socios;
    }

    public Simples getSimples() {
        return simples;
    }

    public void setSimples(Simples simples) {
        this.simples = simples;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Porte {

        private String descricao;

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NaturezaJuridica {

        private String descricao;

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Socio {
        // Apenas para contagem; demais campos ignorados via @JsonIgnoreProperties
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Simples {

        private String mei;

        public String getMei() {
            return mei;
        }

        public void setMei(String mei) {
            this.mei = mei;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Estabelecimento {

        private String cnpj;

        @JsonProperty("tipo_logradouro")
        private String tipoLogradouro;

        @JsonProperty("nome_fantasia")
        private String nomeFantasia;

        @JsonProperty("situacao_cadastral")
        private String situacaoCadastral;

        @JsonProperty("data_inicio_atividade")
        private String dataInicioAtividade;

        private String logradouro;
        private String numero;
        private String complemento;
        private String bairro;
        private String cep;
        private String ddd1;
        private String telefone1;
        private String ddd2;
        private String telefone2;
        private String email;
        private Estado estado;
        private Cidade cidade;

        @JsonProperty("atividade_principal")
        private AtividadePrincipal atividadePrincipal;

        public String getCnpj() {
            return cnpj;
        }

        public void setCnpj(String cnpj) {
            this.cnpj = cnpj;
        }

        public String getTipoLogradouro() {
            return tipoLogradouro;
        }

        public void setTipoLogradouro(String tipoLogradouro) {
            this.tipoLogradouro = tipoLogradouro;
        }

        public String getNomeFantasia() {
            return nomeFantasia;
        }

        public void setNomeFantasia(String nomeFantasia) {
            this.nomeFantasia = nomeFantasia;
        }

        public String getSituacaoCadastral() {
            return situacaoCadastral;
        }

        public void setSituacaoCadastral(String situacaoCadastral) {
            this.situacaoCadastral = situacaoCadastral;
        }

        public String getDataInicioAtividade() {
            return dataInicioAtividade;
        }

        public void setDataInicioAtividade(String dataInicioAtividade) {
            this.dataInicioAtividade = dataInicioAtividade;
        }

        public String getLogradouro() {
            return logradouro;
        }

        public void setLogradouro(String logradouro) {
            this.logradouro = logradouro;
        }

        public String getNumero() {
            return numero;
        }

        public void setNumero(String numero) {
            this.numero = numero;
        }

        public String getComplemento() {
            return complemento;
        }

        public void setComplemento(String complemento) {
            this.complemento = complemento;
        }

        public String getBairro() {
            return bairro;
        }

        public void setBairro(String bairro) {
            this.bairro = bairro;
        }

        public String getCep() {
            return cep;
        }

        public void setCep(String cep) {
            this.cep = cep;
        }

        public String getDdd1() {
            return ddd1;
        }

        public void setDdd1(String ddd1) {
            this.ddd1 = ddd1;
        }

        public String getTelefone1() {
            return telefone1;
        }

        public void setTelefone1(String telefone1) {
            this.telefone1 = telefone1;
        }

        public String getDdd2() {
            return ddd2;
        }

        public void setDdd2(String ddd2) {
            this.ddd2 = ddd2;
        }

        public String getTelefone2() {
            return telefone2;
        }

        public void setTelefone2(String telefone2) {
            this.telefone2 = telefone2;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Estado getEstado() {
            return estado;
        }

        public void setEstado(Estado estado) {
            this.estado = estado;
        }

        public Cidade getCidade() {
            return cidade;
        }

        public void setCidade(Cidade cidade) {
            this.cidade = cidade;
        }

        public AtividadePrincipal getAtividadePrincipal() {
            return atividadePrincipal;
        }

        public void setAtividadePrincipal(AtividadePrincipal atividadePrincipal) {
            this.atividadePrincipal = atividadePrincipal;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Estado {

        private String sigla;

        public String getSigla() {
            return sigla;
        }

        public void setSigla(String sigla) {
            this.sigla = sigla;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cidade {

        private String nome;

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AtividadePrincipal {

        private String id;
        private String descricao;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }
    }
}
