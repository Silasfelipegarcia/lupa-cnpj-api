package br.com.lupainsights.service;

import br.com.lupainsights.dto.CnpjResponse;

public interface CnpjConsultaPort {

    CnpjResponse consultar(String cnpj) throws InterruptedException;
}
