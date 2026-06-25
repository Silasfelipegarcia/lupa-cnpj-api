package br.com.lupainsights.csv;

import br.com.lupainsights.dto.ImportRow;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Component
public class CnpjPlanilhaReader {

    private final CnpjCsvReader csvReader;
    private final CnpjExcelReader excelReader;

    public CnpjPlanilhaReader(CnpjCsvReader csvReader, CnpjExcelReader excelReader) {
        this.csvReader = csvReader;
        this.excelReader = excelReader;
    }

    public List<ImportRow> ler(MultipartFile file) throws IOException {
        String nome = file.getOriginalFilename();
        if (nome != null && (nome.toLowerCase().endsWith(".xlsx") || nome.toLowerCase().endsWith(".xls"))) {
            return excelReader.ler(file);
        }
        return csvReader.lerLinhas(file);
    }
}
