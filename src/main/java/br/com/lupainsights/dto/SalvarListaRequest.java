package br.com.lupainsights.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SalvarListaRequest {

    @NotBlank
    @Size(max = 200)
    private String nomeLista;

    public String getNomeLista() {
        return nomeLista;
    }

    public void setNomeLista(String nomeLista) {
        this.nomeLista = nomeLista;
    }
}
