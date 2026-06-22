package br.com.dadoscnpj.csv;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class CnpjExcelTemplateWriter {

    public byte[] gerarModelo() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Modelo");
            CellStyle headerStyle = criarEstiloCabecalho(workbook);
            CellStyle hintStyle = criarEstiloDica(workbook);

            String[] cabecalho = {"cnpj", "razao_social"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < cabecalho.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(cabecalho[i]);
                cell.setCellStyle(headerStyle);
                sheet.setDefaultColumnStyle(i, criarEstiloTexto(workbook));
            }

            Object[][] exemplos = {
                    {"19.131.243/0001-97", ""},
                    {"", "PETROLEO BRASILEIRO S A PETROBRAS"},
                    {"00.000.000/0001-91", ""},
                    {"33.592.510/0001-54", ""}
            };

            for (int i = 0; i < exemplos.length; i++) {
                Row row = sheet.createRow(i + 1);
                Cell cnpjCell = row.createCell(0);
                cnpjCell.setCellValue(String.valueOf(exemplos[i][0]));
                cnpjCell.setCellStyle(criarEstiloTexto(workbook));
                row.createCell(1).setCellValue(String.valueOf(exemplos[i][1]));
            }

            Row instrucoes = sheet.createRow(6);
            Cell instrucaoCell = instrucoes.createCell(0);
            instrucaoCell.setCellValue(
                    "Preencha a coluna cnpj em cada linha. A busca por razão social pode ser habilitada futuramente.");
            instrucaoCell.setCellStyle(hintStyle);

            sheet.setColumnWidth(0, 22 * 256);
            sheet.setColumnWidth(1, 45 * 256);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle criarEstiloCabecalho(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle criarEstiloTexto(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("@"));
        return style;
    }

    private CellStyle criarEstiloDica(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setWrapText(true);
        return style;
    }
}
