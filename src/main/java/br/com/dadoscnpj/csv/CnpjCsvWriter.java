package br.com.dadoscnpj.csv;

import br.com.dadoscnpj.dto.CnpjResult;
import com.opencsv.CSVWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class CnpjCsvWriter {

    private static final String[] CABECALHO = {
            "razao_social_informada", "cnpj", "razao_social", "nome_fantasia", "situacao_cadastral",
            "telefone_1", "telefone_2", "email", "logradouro", "numero",
            "complemento", "bairro", "cidade", "uf", "cep", "cnae_principal",
            "observacao", "status_consulta", "erro"
    };

    public byte[] escrever(List<CnpjResult> resultados) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(
                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8),
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.DEFAULT_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {

            writer.writeNext(CABECALHO);

            for (CnpjResult r : resultados) {
                writer.writeNext(new String[]{
                        r.getRazaoSocialInformada(),
                        r.getCnpj(),
                        r.getRazaoSocial(),
                        r.getNomeFantasia(),
                        r.getSituacaoCadastral(),
                        r.getTelefone1(),
                        r.getTelefone2(),
                        r.getEmail(),
                        r.getLogradouro(),
                        r.getNumero(),
                        r.getComplemento(),
                        r.getBairro(),
                        r.getCidade(),
                        r.getUf(),
                        r.getCep(),
                        r.getCnaePrincipal(),
                        r.getObservacao(),
                        r.getStatusConsulta(),
                        r.getErro()
                });
            }
        }
        return outputStream.toByteArray();
    }
}
