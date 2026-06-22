package br.com.dadoscnpj.csv;

import br.com.dadoscnpj.dto.ImportRow;
import br.com.dadoscnpj.util.CnpjEntradaNormalizer;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class CnpjExcelReader {

    private static final Logger log = LoggerFactory.getLogger(CnpjExcelReader.class);

    private final CnpjPlanilhaParser parser;
    private final DataFormatter dataFormatter = new DataFormatter();

    public CnpjExcelReader(CnpjPlanilhaParser parser) {
        this.parser = parser;
    }

    public List<ImportRow> ler(MultipartFile file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Planilha Excel vazia");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Cabeçalho não encontrado na planilha");
            }

            String[] cabecalho = lerLinha(headerRow);
            int indiceCnpj = parser.encontrarIndice(cabecalho, "cnpj", "documento", "cnpj_cpf");
            int indiceRazaoSocial = parser.encontrarIndice(cabecalho, "razao_social", "razaosocial", "razao social", "empresa", "nome");

            if (indiceCnpj < 0 && indiceRazaoSocial < 0) {
                throw new IllegalArgumentException(
                        "A planilha deve conter as colunas 'cnpj' e/ou 'razao_social'. Colunas encontradas: "
                                + String.join(", ", cabecalho));
            }

            List<ImportRow> resultado = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String[] linha = lerLinha(row);
                String cnpj = indiceCnpj >= 0 ? lerCelulaCnpj(row.getCell(indiceCnpj)) : "";
                String razaoSocial = parser.lerCampoTexto(linha, indiceRazaoSocial);

                if (parser.isLinhaInstrucao(razaoSocial) || parser.isLinhaInstrucao(cnpj)) {
                    continue;
                }

                ImportRow importRow = parser.montarLinha(cnpj, razaoSocial);
                if (importRow != null) {
                    resultado.add(importRow);
                }
            }

            if (resultado.isEmpty()) {
                throw new IllegalArgumentException("Nenhuma linha com CNPJ ou razão social encontrada na planilha");
            }

            log.info("Lidas {} linha(s) do Excel {}", resultado.size(), file.getOriginalFilename());
            return resultado;
        }
    }

    private String lerCelulaCnpj(Cell cell) {
        if (cell == null) {
            return "";
        }
        return CnpjEntradaNormalizer.normalizar(dataFormatter.formatCellValue(cell));
    }

    private String[] lerLinha(Row row) {
        int lastCell = Math.max(row.getLastCellNum(), 0);
        String[] valores = new String[lastCell];
        for (int i = 0; i < lastCell; i++) {
            Cell cell = row.getCell(i);
            valores[i] = cell != null ? dataFormatter.formatCellValue(cell).trim() : "";
        }
        return valores;
    }
}
