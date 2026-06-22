package br.com.dadoscnpj.dto;

public class ImportRow {

    private String cnpj;
    private String razaoSocial;

    public ImportRow() {
    }

    public ImportRow(String cnpj, String razaoSocial) {
        this.cnpj = cnpj;
        this.razaoSocial = razaoSocial;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public boolean temCnpj() {
        return cnpj != null && !cnpj.isBlank();
    }

    public boolean temRazaoSocial() {
        return razaoSocial != null && !razaoSocial.isBlank();
    }
}
