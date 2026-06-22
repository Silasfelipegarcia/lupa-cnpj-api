package br.com.dadoscnpj.service;

import br.com.dadoscnpj.dto.CnpjResponse;

public interface CnpjConsultaPort {

    CnpjResponse consultar(String cnpj) throws InterruptedException;
}
