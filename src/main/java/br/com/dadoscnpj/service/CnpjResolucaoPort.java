package br.com.dadoscnpj.service;

public interface CnpjResolucaoPort {

    CnpjResolucaoService.ResolucaoCnpj resolverPorRazaoSocial(String razaoSocial) throws InterruptedException;
}
