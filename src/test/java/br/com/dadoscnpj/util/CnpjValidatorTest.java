package br.com.dadoscnpj.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CnpjValidatorTest {

    @Test
    void deveAceitarCnpjValidoComMascara() {
        assertTrue(CnpjValidator.isValido(CnpjValidator.removerMascara("19.131.243/0001-97")));
    }

    @Test
    void deveRejeitarCnpjComDigitosVerificadoresInvalidos() {
        String erro = CnpjValidator.validar("19131243000100");
        assertNotNull(erro);
        assertTrue(erro.contains("dígitos verificadores"));
    }

    @Test
    void deveRejeitarCnpjComTamanhoIncorreto() {
        String erro = CnpjValidator.validar("123");
        assertNotNull(erro);
        assertTrue(erro.contains("14 dígitos"));
    }

    @Test
    void deveRejeitarCnpjComDigitosRepetidos() {
        String erro = CnpjValidator.validar("11111111111111");
        assertNotNull(erro);
        assertTrue(erro.contains("repetidos"));
    }

    @Test
    void deveFormatarCnpj() {
        assertEquals("19.131.243/0001-97", CnpjValidator.formatar("19131243000197"));
    }
}
