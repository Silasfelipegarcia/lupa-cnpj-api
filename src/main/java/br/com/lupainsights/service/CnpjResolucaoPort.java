package br.com.lupainsights.service;

public interface CnpjResolucaoPort {

    CnpjResolucaoService.ResolucaoCnpj resolverPorRazaoSocial(String razaoSocial) throws InterruptedException;
}
