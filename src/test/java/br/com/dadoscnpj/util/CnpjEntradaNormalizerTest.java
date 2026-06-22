package br.com.dadoscnpj.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CnpjEntradaNormalizerTest {

    @Test
    void devePreservarCnpjFormatado() {
        assertEquals("19131243000197", CnpjEntradaNormalizer.normalizar("19.131.243/0001-97"));
    }

    @Test
    void deveCompletarZerosAEsquerda() {
        assertEquals("07526557000100", CnpjEntradaNormalizer.normalizar("7526557000100"));
        assertEquals("07526557000100", CnpjEntradaNormalizer.normalizar("07526557000100"));
    }

    @Test
    void deveConverterNotacaoCientificaParaQuatorzeDigitos() {
        String normalizado = CnpjEntradaNormalizer.normalizar("3.30001670001E13");
        assertEquals(14, normalizado.length());
        assertTrue(normalizado.matches("\\d{14}"));
    }
}
