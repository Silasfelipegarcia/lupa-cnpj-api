package br.com.dadoscnpj.csv;

import br.com.dadoscnpj.dto.CnpjResult;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class CnpjExcelResultWriter {

    private static final String[] CABECALHO = {
            "razao_social_informada", "cnpj", "razao_social", "nome_fantasia", "situacao_cadastral",
            "telefone_1", "telefone_2", "email", "logradouro", "numero",
            "complemento", "bairro", "cidade", "uf", "cep", "cnae_principal",
            "observacao", "status_consulta", "erro"
    };

    public byte[] escrever(List<CnpjResult> resultados) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("resultados");
            Row header = sheet.createRow(0);
            for (int i = 0; i < CABECALHO.length; i++) {
                header.createCell(i).setCellValue(CABECALHO[i]);
            }

            int rowIndex = 1;
            for (CnpjResult r : resultados) {
                Row row = sheet.createRow(rowIndex++);
                int col = 0;
                row.createCell(col++).setCellValue(valor(r.getRazaoSocialInformada()));
                row.createCell(col++).setCellValue(valor(r.getCnpj()));
                row.createCell(col++).setCellValue(valor(r.getRazaoSocial()));
                row.createCell(col++).setCellValue(valor(r.getNomeFantasia()));
                row.createCell(col++).setCellValue(valor(r.getSituacaoCadastral()));
                row.createCell(col++).setCellValue(valor(r.getTelefone1()));
                row.createCell(col++).setCellValue(valor(r.getTelefone2()));
                row.createCell(col++).setCellValue(valor(r.getEmail()));
                row.createCell(col++).setCellValue(valor(r.getLogradouro()));
                row.createCell(col++).setCellValue(valor(r.getNumero()));
                row.createCell(col++).setCellValue(valor(r.getComplemento()));
                row.createCell(col++).setCellValue(valor(r.getBairro()));
                row.createCell(col++).setCellValue(valor(r.getCidade()));
                row.createCell(col++).setCellValue(valor(r.getUf()));
                row.createCell(col++).setCellValue(valor(r.getCep()));
                row.createCell(col++).setCellValue(valor(r.getCnaePrincipal()));
                row.createCell(col++).setCellValue(valor(r.getObservacao()));
                row.createCell(col++).setCellValue(valor(r.getStatusConsulta()));
                row.createCell(col).setCellValue(valor(r.getErro()));
            }

            for (int i = 0; i < CABECALHO.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private String valor(String texto) {
        return texto == null ? "" : texto;
    }
}
