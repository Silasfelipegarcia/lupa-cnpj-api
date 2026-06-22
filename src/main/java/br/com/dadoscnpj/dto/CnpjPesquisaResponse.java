package br.com.dadoscnpj.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CnpjPesquisaResponse {

    private Paginacao paginacao;
    private List<String> data;

    public Paginacao getPaginacao() {
        return paginacao;
    }

    public void setPaginacao(Paginacao paginacao) {
        this.paginacao = paginacao;
    }

    public List<String> getData() {
        return data;
    }

    public void setData(List<String> data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Paginacao {

        private int total;

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }
}
