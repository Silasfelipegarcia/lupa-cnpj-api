package br.com.lupainsights.dto;

import br.com.lupainsights.util.CnpjFormatter;
import br.com.lupainsights.util.CnpjValidator;

public class CnpjResult {

    private String cnpj;
    private String razaoSocialInformada;
    private String razaoSocial;
    private String nomeFantasia;
    private String situacaoCadastral;
    private String telefone1;
    private String telefone2;
    private String email;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
    private String cep;
    private String cnaePrincipal;
    private String observacao;
    private String statusConsulta;
    private String erro;

    public static CnpjResult erro(ImportRow linha, String mensagem) {
        CnpjResult result = new CnpjResult();
        result.setRazaoSocialInformada(nullSafe(linha.getRazaoSocial()));
        if (linha.temCnpj()) {
            result.setCnpj(CnpjValidator.formatar(CnpjValidator.normalizarParaApi(linha.getCnpj())));
        }
        result.setStatusConsulta("ERRO");
        result.setErro(mensagem);
        return result;
    }

    public static CnpjResult sucesso(ImportRow linha, String cnpjConsultado, CnpjResponse response, String observacao) {
        CnpjResult result = new CnpjResult();
        result.setRazaoSocialInformada(nullSafe(linha.getRazaoSocial()));
        result.setStatusConsulta("SUCESSO");
        result.setObservacao(nullSafe(observacao));

        if (response != null) {
            result.setRazaoSocial(nullSafe(response.getRazaoSocial()));

            CnpjResponse.Estabelecimento est = response.getEstabelecimento();
            if (est != null) {
                String cnpjApi = est.getCnpj() != null && !est.getCnpj().isBlank()
                        ? est.getCnpj()
                        : cnpjConsultado;
                result.setCnpj(CnpjValidator.formatar(cnpjApi));

                result.setNomeFantasia(nullSafe(est.getNomeFantasia()));
                result.setSituacaoCadastral(nullSafe(est.getSituacaoCadastral()));
                result.setTelefone1(CnpjFormatter.formatarTelefone(est.getDdd1(), est.getTelefone1()));
                result.setTelefone2(CnpjFormatter.formatarTelefone(est.getDdd2(), est.getTelefone2()));
                result.setEmail(nullSafe(est.getEmail()));
                result.setLogradouro(CnpjFormatter.formatarLogradouro(est.getTipoLogradouro(), est.getLogradouro()));
                result.setNumero(nullSafe(est.getNumero()));
                result.setComplemento(nullSafe(est.getComplemento()));
                result.setBairro(nullSafe(est.getBairro()));
                result.setCep(CnpjFormatter.formatarCep(est.getCep()));

                if (est.getCidade() != null) {
                    result.setCidade(nullSafe(est.getCidade().getNome()));
                }
                if (est.getEstado() != null) {
                    result.setUf(nullSafe(est.getEstado().getSigla()));
                }
                if (est.getAtividadePrincipal() != null) {
                    result.setCnaePrincipal(CnpjFormatter.formatarCnae(
                            est.getAtividadePrincipal().getId(),
                            est.getAtividadePrincipal().getDescricao()));
                }
            } else {
                result.setCnpj(CnpjValidator.formatar(cnpjConsultado));
            }
        } else {
            result.setCnpj(CnpjValidator.formatar(cnpjConsultado));
        }

        return result;
    }

    public static CnpjResult reutilizar(ImportRow linha, CnpjResult original) {
        CnpjResult copia = new CnpjResult();
        copia.setCnpj(original.getCnpj());
        copia.setRazaoSocialInformada(nullSafe(linha.getRazaoSocial()));
        copia.setRazaoSocial(original.getRazaoSocial());
        copia.setNomeFantasia(original.getNomeFantasia());
        copia.setSituacaoCadastral(original.getSituacaoCadastral());
        copia.setTelefone1(original.getTelefone1());
        copia.setTelefone2(original.getTelefone2());
        copia.setEmail(original.getEmail());
        copia.setLogradouro(original.getLogradouro());
        copia.setNumero(original.getNumero());
        copia.setComplemento(original.getComplemento());
        copia.setBairro(original.getBairro());
        copia.setCidade(original.getCidade());
        copia.setUf(original.getUf());
        copia.setCep(original.getCep());
        copia.setCnaePrincipal(original.getCnaePrincipal());
        copia.setStatusConsulta(original.getStatusConsulta());
        copia.setErro(original.getErro());
        copia.setObservacao(nullSafe(original.getObservacao()));
        return copia;
    }

    private static String nullSafe(String value) {
        return value != null ? value : "";
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getRazaoSocialInformada() {
        return razaoSocialInformada;
    }

    public void setRazaoSocialInformada(String razaoSocialInformada) {
        this.razaoSocialInformada = razaoSocialInformada;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
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

    public String getTelefone1() {
        return telefone1;
    }

    public void setTelefone1(String telefone1) {
        this.telefone1 = telefone1;
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

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getCnaePrincipal() {
        return cnaePrincipal;
    }

    public void setCnaePrincipal(String cnaePrincipal) {
        this.cnaePrincipal = cnaePrincipal;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public String getStatusConsulta() {
        return statusConsulta;
    }

    public void setStatusConsulta(String statusConsulta) {
        this.statusConsulta = statusConsulta;
    }

    public String getErro() {
        return erro;
    }

    public void setErro(String erro) {
        this.erro = erro;
    }
}
