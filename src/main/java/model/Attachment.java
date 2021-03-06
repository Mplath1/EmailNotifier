package model;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

public class Attachment implements Callable {
    private static final Logger log = LoggerFactory.getLogger(Attachment.class);

    private Line theLine;
    private String templateFileName;
    public Workbook theWorkbook;
    private Customer customer;

    public Attachment(Line theLine,String templateFileName){
        this.theLine = theLine;
        this.templateFileName = templateFileName;
        theWorkbook = null;
    }

    public Attachment(Customer customer, String templateFileName){
        this.customer = customer;
        this.templateFileName = templateFileName;
    }

    @Override
    public Object call() throws Exception {
        File outputFile = null;
        try {
            //theWorkbook = createAttachment(theLine,theFile);
            outputFile = createAttachment();
        } catch(Exception e){
            e.printStackTrace();
        }
        return outputFile;
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

    File createAttachment() throws InvalidFormatException {
        try {
            String filePath = "src/resources/AttachmentTemplates/" + templateFileName;
            System.out.println("Opening " + filePath);
            File fis = new File(filePath);
            Workbook attachmentWorkbook = new XSSFWorkbook(fis);
            Sheet currentSheet = attachmentWorkbook.getSheetAt(0);
            int startingRow = 22;
            Row currentRow = currentSheet.getRow(startingRow);

            for (int i = 0; i < customer.getInvoiceList().size(); i++) {
            Invoice currentInvoice = customer.getInvoiceList().get(i);
            currentRow = currentSheet.getRow(startingRow);
            Cell currentCell = currentRow.getCell(0);
            currentCell.setCellValue(currentInvoice.getOrderNumber());
            currentCell = currentRow.getCell(1);
            currentCell.setCellValue(currentInvoice.getDeliveryDate());
            currentCell = currentRow.getCell(2);
            currentCell.setCellValue(currentInvoice.getNotificationDate());
            currentCell = currentRow.getCell(3);
            currentCell.setCellValue(currentInvoice.getReportingDate());
            currentCell = currentRow.getCell(4);
            currentCell.setCellValue(currentInvoice.getPortfolioCode());
            currentCell = currentRow.getCell(5);
            currentCell.setCellValue(customer.getLicenseNumber()); //TODO: being set as number instead of text
            currentCell = currentRow.getCell(6);
            currentCell.setCellValue(customer.getCustomerName());
            currentCell = currentRow.getCell(7);
            currentCell.setCellValue("$" + currentInvoice.getDollarValue());
            startingRow++;
            };
            //TODO: seperate into new private method
            String tempOutFile = "src/resources/AttachmentTemplates";
            File outputFile = new File(tempOutFile + "/temp.xlsx");
            FileOutputStream fileout = new FileOutputStream(outputFile);
            attachmentWorkbook.write(fileout);
            log.debug("temp file written:{}",outputFile);
            attachmentWorkbook.close();
            log.debug("workbook closed");
            fileout.close();
            log.debug("temp file closed");

            return outputFile;
        } catch (IOException e) {
            e.printStackTrace();
            log.debug("Error creating attachment - {}",e);
        }
        return null;
    }
















}
