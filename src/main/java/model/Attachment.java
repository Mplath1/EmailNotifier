package model;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

public class Attachment implements Callable {

    private Line theLine;
    private String theFile;
    public Workbook theWorkbook;

    public Attachment(Line theLine,String theFile){
        this.theLine = theLine;
        this.theFile = theFile;
        theWorkbook = null;
    }

    @Override
    public Object call() throws Exception {
        try {
            theWorkbook = createAttachment(theLine,theFile);

        } catch(Exception e){
            e.printStackTrace();
        }
        return theWorkbook;
    }

    Workbook createAttachment(Line theLine, String theFile) {
        try {
            FileInputStream fis = new FileInputStream(new File(theFile));
            Workbook attachmentWorkbook = new XSSFWorkbook(fis);
            Sheet currentSheet = attachmentWorkbook.getSheetAt(0);
            Row currentRow = currentSheet.getRow(22);
            Cell currentCell = currentRow.getCell(0);
            currentCell.setCellValue(theLine.orderNumber);
            currentCell = currentRow.getCell(1);
            currentCell.setCellValue(theLine.deliveryDate);
            currentCell = currentRow.getCell(2);
            currentCell.setCellValue(theLine.notificationDate);
            currentCell = currentRow.getCell(3);
            currentCell.setCellValue(theLine.reportingDate);
            currentCell = currentRow.getCell(4);
            currentCell.setCellValue(theLine.portfolioCode);
            currentCell = currentRow.getCell(5);
            currentCell.setCellValue(theLine.licenseNumber);
            currentCell = currentRow.getCell(6);
            currentCell.setCellValue(theLine.customerName);
            currentCell = currentRow.getCell(7);
            currentCell.setCellValue("$" + theLine.dollarValue);

            FileOutputStream fileout = new FileOutputStream(new File(theFile));
            attachmentWorkbook.write(fileout);
            attachmentWorkbook.close();
            fileout.close();

            return attachmentWorkbook;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
