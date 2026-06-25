package br.com.lupainsights.csv;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CnpjCsvReaderTest {

    private final CnpjCsvReader reader = new CnpjCsvReader(new CnpjPlanilhaParser());

    @Test
    void deveLerCsvSomenteComColunaCnpj() throws Exception {
        String csv = "cnpj\n19.131.243/0001-97\n00.000.000/0001-91\n";
        var file = multipart("teste.csv", csv);

        var linhas = reader.lerLinhas(file);

        assertEquals(2, linhas.size());
        assertTrue(linhas.get(0).temCnpj());
        assertFalse(linhas.get(0).temRazaoSocial());
        assertEquals("19131243000197", linhas.get(0).getCnpj());
    }

    @Test
    void deveLerCsvComBomDoExcel() throws Exception {
        String csv = "\ufeffcnpj\n19.131.243/0001-97\n";
        var file = multipart("teste.csv", csv);

        var linhas = reader.lerLinhas(file);

        assertEquals(1, linhas.size());
        assertTrue(linhas.get(0).temCnpj());
        assertEquals("19131243000197", linhas.get(0).getCnpj());
    }

    @Test
    void deveLerCsvComSeparadorPontoVirgula() throws Exception {
        String csv = "cnpj;razao_social\n19.131.243/0001-97;\n;PETROBRAS\n";
        var file = multipart("teste.csv", csv);

        var linhas = reader.lerLinhas(file);

        assertEquals(2, linhas.size());
        assertTrue(linhas.get(0).temCnpj());
        assertFalse(linhas.get(0).temRazaoSocial());
        assertFalse(linhas.get(1).temCnpj());
        assertTrue(linhas.get(1).temRazaoSocial());
    }

    @Test
    void deveIdentificarCnpjNaColunaRazaoSocial() throws Exception {
        String csv = "cnpj,razao_social\n,19.131.243/0001-97\n";
        var file = multipart("teste.csv", csv);

        var linhas = reader.lerLinhas(file);

        assertEquals(1, linhas.size());
        assertTrue(linhas.get(0).temCnpj());
        assertEquals("19131243000197", linhas.get(0).getCnpj());
    }

    private MockMultipartFile multipart(String nome, String conteudo) {
        return new MockMultipartFile("file", nome, "text/csv", conteudo.getBytes(StandardCharsets.UTF_8));
    }
}
