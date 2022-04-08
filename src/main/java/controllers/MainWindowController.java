package controllers;

import core.Main;
import javafx.beans.binding.BooleanBinding;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import model.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static core.Main.appProps;
import static core.Main.previouslySentFiles;
import static org.apache.commons.io.FilenameUtils.removeExtension;

public class MainWindowController {
    private static final Logger log = LoggerFactory.getLogger(MainWindowController.class);

    @FXML
    GridPane grid;
    @FXML
    Button listSelect;
    @FXML
    TextField listFile;
    @FXML
    ComboBox attachmentFileComboBox;
//    @FXML
//    Button attachmentSelectButton;
//    @FXML
//    TextField attachmentFile;
    @FXML
    ComboBox emailSubjectComboBox;
    @FXML
    ToggleGroup messageToggleGroup;
    @FXML
    RadioButton defaultMessageButton;
    @FXML
    RadioButton customMessageButton;
    @FXML
    TextArea customMessageTextArea;
    @FXML
    Button runEverything;
    @FXML
    Label progressLabel;
    @FXML
    ProgressBar progressBar;

    static int noEmailsSent;
    static int noPrinted;
    ArrayList<String> testNonSendable; //may need to make static
    ArrayList<Customer> nonSendableCustomers; //may need to make static

    public void initialize() throws IOException {
        noEmailsSent = 0;
        noPrinted = 0;
        testNonSendable = new ArrayList<>();
        nonSendableCustomers = new ArrayList<>();
        progressBar.setProgress(0);
        progressBar.setMinSize(250,10);
        progressLabel.setText("READY");
        progressLabel.setMinWidth(250);
        progressLabel.setAlignment(Pos.CENTER);
        loadTemplateOptions();
        setBindings();
    }

    protected void setBindings(){
        customMessageButton.selectedProperty().bindBidirectional(customMessageTextArea.visibleProperty());
        customMessageButton.selectedProperty().bindBidirectional(customMessageTextArea.editableProperty());
        BooleanBinding runButtonBinding = customMessageButton.selectedProperty()
                .and(customMessageTextArea.textProperty().isEmpty())
                .or(messageToggleGroup.selectedToggleProperty().isNull())
                //.or(attachmentFile.textProperty().isEmpty())
                .or(listFile.textProperty().isEmpty())
                .or(attachmentFileComboBox.getSelectionModel().selectedItemProperty().isNull())
                .or(emailSubjectComboBox.getSelectionModel().selectedItemProperty().isNull());

        runEverything.disableProperty().bind(runButtonBinding);
        progressLabel.visibleProperty().bind(runEverything.disableProperty().not());
        progressBar.visibleProperty().bind(runEverything.disableProperty().not());
        emailSubjectComboBox.setEditable(true);
    }

    protected void loadTemplateOptions() throws IOException {
        emailSubjectComboBox.getItems().add("NYSLA Prenotification");
        emailSubjectComboBox.getItems().add("Final Notice");
        //Load All Available Templates
        //ClassLoader loader =  getClass().getClassLoader();
        //URL resource = loader.getResource("AttachmentTemplates"); works but makes resources uneditable when building jar
        String attachmentDirectoryPath = appProps.getProperty("defaultAttachmentTemplateDirectory");

        List<File> collect = null;
        try {
            collect = Files.walk(Paths.get(attachmentDirectoryPath)).filter(Files::isRegularFile).map(x -> x.toFile())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(File currentFile: collect){
            emailSubjectComboBox.getItems().add(removeExtension(currentFile.getName()));
            attachmentFileComboBox.getItems().add(currentFile.getName());
            log.debug("Options added to comboboxes:{}",removeExtension(currentFile.getName()));
        }
    }


    @FXML
    protected void selectList(ActionEvent event) throws IOException {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Excel File", "*.xls","*.xlsx");
        fileChooser.getExtensionFilters().add(extensionFilter);

        File id = new File(appProps.getProperty("defaultListLoadDirectory")); //SET THIS AS A VARIABLE
        fileChooser.setInitialDirectory(id);

        File file = fileChooser.showOpenDialog(grid.getScene().getWindow());
        if (file != null) {
            listFile.setText(file.toString());
        } else {
            System.out.println("You must select a file");
        }
    }

    @FXML @Deprecated
    protected void selectAttachment(ActionEvent event) throws IOException {
//        FileChooser fileChooser = new FileChooser();
//        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Excel File", "*.xls","*.xlsx");
//        fileChooser.getExtensionFilters().add(extensionFilter);
//
//        File id = new File(appProps.getProperty("defaultListLoadDirectory"));//SET THIS AS A VARIABLE
//        fileChooser.setInitialDirectory(id);
//
//        File file = fileChooser.showOpenDialog(grid.getScene().getWindow());
//        if (file != null) {
//            attachmentFile.setText(file.toString());
//        } else {
//            System.out.println("You must select a file");
//        }
    }


    @FXML
    protected void doEverything(ActionEvent event) throws IOException {
       displayAlreadySentMessage();

        //works with HSSF(.xls) or XSSF(.xlsx)
        Workbook wb = null;
        try {
            wb = WorkbookFactory.create(new File(listFile.getText()));
            System.out.println(listFile.getText() + " loaded as " + wb.getSpreadsheetVersion().toString());
            log.debug("Loaded \'{}\' as \'{}\'",listFile.getText(),wb.getSpreadsheetVersion());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to load selected file:" + listFile.getText());
            log.error("Unable to load:{} - {}",listFile.getText(),e);
        }

        ArrayList<Customer> customerListToSend = loadListToSend2(wb);


        String emailSubject = String.valueOf(emailSubjectComboBox.getSelectionModel().getSelectedItem());
        String emailMessage;
        if(customMessageTextArea.getText().isEmpty()){
            emailMessage = appProps.getProperty("bodyText");
        }else{
            emailMessage = customMessageTextArea.getText();
        }
            processList2(customerListToSend,emailSubject,emailMessage);
            ///put tasks after process into overall cleanup method

        };



    public void displayAlreadySentMessage() {
        String hasBeenSent = checkPreviouslySaved(listFile.getText());
        //alert needs serious work but is functional
        if (hasBeenSent != null) {
            String[] splitString = hasBeenSent.split(",");
            String[] dateAndTime = splitString[1].split(" ");
            Alert alreadySent = new Alert(Alert.AlertType.CONFIRMATION);
            alreadySent.setHeaderText("ATTENTION!\nFile has been sent before!");
            alreadySent.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

            alreadySent.setContentText(splitString[0] + " has already been sent previously on " +
                    dateAndTime[0] + " at approximately " + dateAndTime[1] + ".\nAre you sure you still want to send this file?");
            Optional<ButtonType> result = alreadySent.showAndWait();
            if (result.isPresent() && result.get() != ButtonType.OK) {
                System.exit(0);
            }

        }
    }

    public void displayCompletionReport(){
        StringBuilder sb = new StringBuilder();
         sb.append("******Process Complete******\n"+
                 "Emails sent: " + noEmailsSent + "\n"+
                 "To be printed: " + noPrinted + "\n" +
                 "Unable to send for:\n"); //SAVE TO ORIGINAL FILE?
//        for (String customer : testNonSendable){
//             sb.append(customer + "\n");
//        }
        for (Customer current : nonSendableCustomers){
            sb.append(current.getCustomerName() + "\n");
        }

        String fullMessage = sb.toString();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Results");
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setContentText(fullMessage);
        double parentWindowTopRightCorner = grid.getScene().getWindow().getX() + grid.getScene().getWindow().getWidth();
        alert.setX(parentWindowTopRightCorner + 100);
        alert.setResizable(true);
        alert.showAndWait();
    }

    //IN PROGRESS can update but issues with output
    public void updateOriginalFile() throws IOException {

        List<String> nonSendableLicenses = new ArrayList<>();
        for(Customer currentCustomer: nonSendableCustomers){
            System.out.println(currentCustomer.getLicenseNumber());
            nonSendableLicenses.add(currentCustomer.getLicenseNumber());
        }


        Workbook wb = null;
        File fileToOverwrite = new File(listFile.getText());
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            fileInputStream = new FileInputStream(fileToOverwrite);
            wb = WorkbookFactory.create(fileInputStream);

            System.out.println(listFile.getText() + " loaded as " + wb.getSpreadsheetVersion().toString());

            Sheet sheet = wb.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.rowIterator();
            while(rowIterator.hasNext()) {
                Row currentRow = rowIterator.next();
                if (currentRow != null) {
                    Cell licenseCell = currentRow.getCell(5);
                    Cell customerNameCell = currentRow.getCell(6);
                    currentRow.createCell(8);
                    Cell notesCell = currentRow.getCell(8);
                  if(!nonSendableLicenses.contains(licenseCell.toString())) {
                      notesCell.setCellValue("SENT");
                  }else{
                      notesCell.setCellValue("UNABLE TO SEND TO 1 OR MORE ADDRESSES");
                  }
                }
            }

            fileInputStream.close();
           String someOtherFile = "C:\\Users\\Michael\\Downloads\\outputFile.xlsx";
            fileOutputStream = new FileOutputStream(someOtherFile); //IO Exception here
            wb.write(fileOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            wb.close();
            fileOutputStream.close();
        }
        System.out.println("DONE!");

    }

    public void saveCompletedList() throws IOException {
        //need to create saved static string of file name just sent. save array of 30-35 entries
        //entries load upon start up preventing you from selecting a a previously sent file as the listFile
        previouslySentFiles.add(listFile.getText().toString());
        boolean successfulSave = savePreviouslySentFiles();
        if (successfulSave) {
            System.out.println("File successfully saved.");
        } else {
            System.out.println("There was an error saving!");
        }
    }

    @Deprecated
    public void processList(ArrayList<Line> testLines, String emailSubject, String emailMessage){
        //HashMap<String,String> testDatabaseMap = getDatabaseList(testLines);
        HashMap<String,String> testDatabaseMap = new HashMap<>();
       testDatabaseMap.put(testLines.get(5).getLicenseNumber(),"mplath@usawineimports.com");

            Task altTask = new Task() {
                @Override
                protected Object call() throws Exception {
                    for(int i = 1; i<testLines.size(); i++){
                        boolean success = false;
                        String emailAddresses = testDatabaseMap.get(testLines.get(i).getLicenseNumber());
                        if (StringUtils.isBlank(emailAddresses)) {
                            emailAddresses = ""; //seems redundant but had trouble with cust Miss Korea trying to create email with null
                        }
                        System.out.println(emailAddresses);
                        if(!emailAddresses.equals(null) && !emailAddresses.isEmpty() && emailAddresses.trim().length() != 0) {

                            //Attachment otherAttachment = new Attachment(testLines.get(i), attachmentFile.getText());
                            //Workbook theAttachment = (Workbook) otherAttachment.call(); //TODO: call correct POI here
                            //Email theEmail = new Email(emailAddresses, attachmentFile.getText().toString(), emailSubject, emailMessage);
//                            System.out.println("Send email for " +
//                                    theAttachment.getSheetAt(0).getRow(22).getCell(6) + " to " + emailAddresses + " Total:$" +
//                                    theAttachment.getSheetAt(0).getRow(22).getCell(7));
                            //success = (boolean) theEmail.call();
                        }else{
                           testNonSendable.add(testLines.get(i).getCustomerName());
                        }
                        String message = "";
                        if(success){
                            noEmailsSent++;
                            message += "Email sent for:" + testLines.get(i).getCustomerName();
                        }else{
                            noPrinted++;
                            message += "Unable to send for:" + testLines.get(i).getCustomerName();
                        }
                        message += "\n" + i + "/" + testLines.size();

                        System.out.println(i + "/" + testLines.size() +"\t" + success + "\n");
                        updateProgress(i,testLines.size());
                        updateMessage(message);

                        Thread.sleep(100);
                    }
                    return null;
                }
            };

            progressBar.progressProperty().bind(altTask.progressProperty());
            progressLabel.textProperty().bind(altTask.messageProperty());

        altTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                displayCompletionReport();
                try {
                    saveCompletedList();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        altTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                //alert?
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("ERROR");
                alert.setContentText("There was an error!");
            }
        });

            new Thread(altTask).start();
        }


    //TODO: eliminate other processList method. possibly pass a callback
    public void processList2(ArrayList<Customer> customerList, String emailSubject, String emailMessage){
//       customerList.get(1).setCustomerEmail("mplath@usawineimports.com");
//       customerList.get(9).setCustomerEmail("map30269@yahoo.com;mplath@usawineimports.com");
//        customerList.get(6).setCustomerEmail(null);
//        customerList.get(7).setCustomerEmail("ArthurDent");

        Task altTask = new Task() {
            @Override
            protected Object call() throws Exception {
                for(int i = 0; i<customerList.size(); i++){
                    boolean success = false;
                    String emailAddresses = customerList.get(i).getCustomerEmail();
                    if (StringUtils.isBlank(emailAddresses)) {
                        emailAddresses = ""; //seems redundant but had trouble with cust Miss Korea trying to create email with null
                    }
                    System.out.println(customerList.get(i).getCustomerName() + " email is " + customerList.get(i).getCustomerEmail());
                    if(!emailAddresses.equals(null) && !emailAddresses.isEmpty() && emailAddresses.trim().length() != 0) {

                        String selectedFileName = attachmentFileComboBox.getSelectionModel().getSelectedItem().toString();

                        Attachment otherAttachment = new Attachment(customerList.get(i), selectedFileName);
                        File fileAttachment = (File) otherAttachment.call(); //TODO:set this into email
                        System.out.println("Attachment made : " + fileAttachment.getName());
                        Email theEmail = new Email(emailAddresses, selectedFileName, emailSubject, emailMessage);
                        theEmail.setAttachentFile(fileAttachment);
//                        String tempFilePathAndName = "src/resources/AttachmentTemplates/temp.xlsx";
//                        Email theEmail = new Email(emailAddresses, tempFilePathAndName, emailSubject, emailMessage);
                        success = (boolean) theEmail.call();
                    }else{
                        System.out.println("Adding " + customerList.get(i).getCustomerName() + " to nonsendable");
                        nonSendableCustomers.add(customerList.get(i));
                    }
                    String message = "";
                    if(success){
                        noEmailsSent++;
                        message += "Email sent for:" + customerList.get(i).getCustomerName();
                    }else{
                        noPrinted++;
                        message += "Unable to send for:" + customerList.get(i).getCustomerName();
                    }
                    message += "\n" + i + "/" + customerList.size();

                    System.out.println(i + "/" + customerList.size() +"\t" + success + "\n");
                    updateProgress(i,customerList.size());
                    updateMessage(message);

                    Thread.sleep(100);
                }
                updateProgress(1,1);
                updateMessage("COMPLETE");
                //delete Temp file no matter what. Possibly move to cleanUp method
                boolean tempFileDeleted = Files.deleteIfExists(Paths.get("src/resources/AttachmentTemplates/temp.xlsx"));
                return null;
            }
        };

        progressBar.progressProperty().bind(altTask.progressProperty());
        progressLabel.textProperty().bind(altTask.messageProperty());

        altTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                displayCompletionReport();
                try {
                    saveCompletedList();
                    //updateOriginalFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        altTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("ERROR");
                alert.setContentText("There was an error!");
            }
        });

        new Thread(altTask).start();
    }

    @Deprecated
    public Workbook createAttachement(Line theLine, String theFile) {
        try {
            FileInputStream fis = new FileInputStream(new File(theFile));
            Workbook attachmentWorkbook = new XSSFWorkbook(fis);
            Sheet currentSheet = attachmentWorkbook.getSheetAt(0);
            Row currentRow = currentSheet.getRow(22);
            Cell currentCell = currentRow.getCell(0);
            currentCell.setCellValue(theLine.getOrderNumber());
            currentCell = currentRow.getCell(1);
            currentCell.setCellValue(theLine.getDeliveryDate());
            currentCell = currentRow.getCell(2);
            currentCell.setCellValue(theLine.getNotificationDate());
            currentCell = currentRow.getCell(3);
            currentCell.setCellValue(theLine.getReportingDate());
            currentCell = currentRow.getCell(4);
            currentCell.setCellValue(theLine.getPortfolioCode());
            currentCell = currentRow.getCell(5);
            currentCell.setCellValue(theLine.getLicenseNumber());
            currentCell = currentRow.getCell(6);
            currentCell.setCellValue(theLine.getCustomerName());
            currentCell = currentRow.getCell(7);
            currentCell.setCellValue("$" + theLine.getDollarValue());

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

    @Deprecated
    public static ArrayList<Line> loadListToSend(Workbook workbook) {
        ArrayList<Line> theLines = new ArrayList<>();

        Iterator<Sheet> sheetIterator = workbook.sheetIterator();

        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (row != null) {
                Iterator<Cell> cellIterator = row.cellIterator();
                ArrayList<String> rowContents = new ArrayList(8);
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    String cellValue = cell.toString();
                    rowContents.add(cellValue);

                }
                System.out.println(rowContents.get(0));
                Line currentLine = new Line(rowContents.get(0),
                        rowContents.get(1).toString(),
                        rowContents.get(2).toString(),
                        rowContents.get(3).toString(),
                        rowContents.get(4).toString(),
                        rowContents.get(5).toString(),
                        rowContents.get(6).toString(),
                        rowContents.get(7).toString()
                );
                theLines.add(currentLine);

            }
        }
        return theLines;
    }

    //TODO: eliminate other loadListToSend method and use this to populate data
    public static ArrayList<Customer> loadListToSend2(Workbook workbook) {
        ArrayList<Customer> customerList = new ArrayList<>();

        Iterator<Sheet> sheetIterator = workbook.sheetIterator();

        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (row != null) {
                Iterator<Cell> cellIterator = row.cellIterator();
                ArrayList<String> rowContents = new ArrayList(8);
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    String cellValue = cell.toString();
                    rowContents.add(cellValue);

                }
                Customer currentCustomer = new Customer(rowContents.get(5),rowContents.get(6));
                Invoice currentInvoice = new Invoice(rowContents.get(0),rowContents.get(1),rowContents.get(2),
                        rowContents.get(3),rowContents.get(4),rowContents.get(7));

                if(!customerList.contains(currentCustomer)){
                    currentCustomer.addInvoice(currentInvoice);
                    customerList.add(currentCustomer);

                }else{
                    currentCustomer = customerList.get(customerList.indexOf(currentCustomer));
                    currentCustomer.addInvoice(currentInvoice);
                }


            }
        }
        return customerList;
    }

    //TODO: needs testing with live database. check for bad data and null values.
    public ArrayList<Customer> populateEmailAddresses(ArrayList<Customer> listOfPrenotifications) {
        //TODO: seperate and create connection factory and DAO patterns
        String dbConnection = null;
        try {
            dbConnection = appProps.getProperty("server") + ";" + appProps.getProperty("instanceName")
                    + ";databasename=" + appProps.getProperty("dbName") + ";user=" + appProps.getProperty("user")
                    + ";password=" + appProps.getProperty("serverPassword");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //TODO: rewrite query to include IN clause reducing result set
        String query = "SELECT strEmail, strCompanyName, memWebSite FROM tblARCustomer";// WHERE strCustomerType = Retail";
        progressLabel.setText("Connecting to database...");
        try (Connection conn = DriverManager.getConnection(dbConnection)) {
            System.out.println("Connection was successful");
            progressLabel.setText("Connected to database");
            PreparedStatement preparedStatement = conn.prepareStatement(query);
            for (Customer custFromInput : listOfPrenotifications) {
                String license = custFromInput.getLicenseNumber();
                System.out.println(license);
                ResultSet rs = preparedStatement.executeQuery();
                while(rs.next()) {
                    if(rs.getString("strmEmail").trim().replaceAll("\\.0*$", "") == custFromInput.getLicenseNumber()){
                        custFromInput.setCustomerEmail(rs.getString("memWebsite")); //check for null too
                        }
                    }
                }
        }catch(SQLException e){
            System.out.println(e);
        }

        return listOfPrenotifications;

    }


    @Deprecated
    public HashMap<String,String> getDatabaseList(ArrayList<Line> listOfPrenotifications) {
        //throws way too many null pointer exceptions when program runs. possible wholeslaers. research and correct
        String dbConnection = null;
        try {
            dbConnection = appProps.getProperty("server") + ";" + appProps.getProperty("instanceName")
                    + ";databasename=" + appProps.getProperty("dbName") + ";user=" + appProps.getProperty("user")
                    + ";password=" + appProps.getProperty("serverPassword");
        } catch (IOException e) {
            e.printStackTrace();
        }


        String query = "SELECT strEmail, strCompanyName, memWebSite FROM tblARCustomer";// WHERE strCustomerType = Retail";
        ArrayList<Customer> databaseList = new ArrayList<>();
        HashMap<String, String> customerEmailMap = new HashMap<>();
        progressLabel.setText("Connecting to database...");
        try (Connection conn = DriverManager.getConnection(dbConnection)) {
            System.out.println("Connection was successful");
            progressLabel.setText("Connected to database");
            PreparedStatement preparedStatement = conn.prepareStatement(query);
            for (Line currentLine : listOfPrenotifications) {
                String license = currentLine.getLicenseNumber();
                System.out.println(license);
                ResultSet rs = preparedStatement.executeQuery();
                while(rs.next()) {
                    Customer currentCustomer;
                    String currentCustomerLicense = "none" ;
                    try {
                        currentCustomerLicense = rs.getString("strEmail").trim();
                        currentCustomerLicense = currentCustomerLicense.replaceAll("\\.0*$", "");
                    } catch (Exception e) {
                        // System.out.println(e + " when adding license for " + rs.getString("strCompanyName"));
                    }
                    String currentCustomerName = rs.getString("strCompanyName");
                    String currentCustomerEmailAddresses = "None";
                    try {
                        currentCustomerEmailAddresses = rs.getString("memWebSite");
                    } catch (Exception e) {
                        //System.out.println(e + " when adding name #" +rs.getString("memWebSite"));
                    }
                    currentCustomer = new Customer(currentCustomerLicense, currentCustomerName, currentCustomerEmailAddresses);
                    if(currentCustomer.getCustomerEmail() != null && currentCustomer.getCustomerEmail() != "None"){
                        try {
                            customerEmailMap.put(currentCustomer.getLicenseNumber(), currentCustomer.getCustomerEmail());
                            //customerEmailMap.put(currentCustomer.getLicenseNumber(),currentCustomer) //NEEDS IMPLEMENTATION
                        }catch (Exception e){
                            //System.out.println(e + " when trying to add license #" + currentCustomer.getLicenseNumber() + " to map.");
                        }

                    }
                    databaseList.add(currentCustomer);
                }

            }
        }catch(SQLException e){
            System.out.println(e);
        }

        return customerEmailMap;

    }

    public String checkPreviouslySaved(String currentList){
        for(String oldFile : previouslySentFiles){
            //System.out.println("This is the old file " + oldFile + "\nIt needs to be compared to " + currentList);
            String[] splitFile = oldFile.split(",");
            //System.out.println( "And this is the split file : " + splitFile[0]);
            if(currentList.equals(splitFile[0])){
                return oldFile;
            }
        }
        return null;
    }

    //TODO:save as binary and update associated methods. relocate file to resources
    public static boolean savePreviouslySentFiles() throws IOException {
        File dir = new File("Q:\\Previously Sent Emails"); //USE CONFIG
        dir = new File(appProps.getProperty("defaultPreviouslySentFilesDirectory")
                + appProps.getProperty("defaultPreviouslySentFilesPath")
                );
        if(!dir.exists()){
            dir.mkdir();
        }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMddyyyy HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.now();
        // fileBuilder = new StringBuilder("Q:\\Previously Sent Emails\\Previously Sent Emails");
        StringBuilder fileBuilder = new StringBuilder(appProps.getProperty("defaultPreviouslySentFilesDirectory")
                + appProps.getProperty("defaultPreviouslySentFilesPath")
                + appProps.getProperty("defaultPreviouslySentFilesFile"));
        File file = new File(fileBuilder + ".txt");
        //files copy correctly. need to fix do everything method and check for substring containing file name.
        if(!file.exists()){
            file.createNewFile();
            System.out.println("New File Created");
        }
        try(FileWriter fr = new FileWriter(file, true)) {
            //txt file looks odd and seems to add an extra line with date and time. works currently but needs investigation
            if (!previouslySentFiles.isEmpty()) {
                for (String current : previouslySentFiles) {
                    if(!current.contains(",")){
                        fr.write("\n");
                        StringBuilder sb = new StringBuilder(current);
                        dtf.format(localDateTime);
                        sb.append("," + dtf.format(localDateTime).toString());
                        fr.write(String.valueOf(sb));
                        System.out.println(sb + " is new and was written");

                    }
                    System.out.println(current + " is in list but was not written");
                }
            }
            return true;
        }catch (IOException e){
            System.out.println(e);
            return false;
        }
    }
}
