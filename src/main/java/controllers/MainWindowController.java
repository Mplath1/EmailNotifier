package controllers;

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
import model.Attachment;
import model.Customer;
import model.Email;
import model.Line;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static core.Main.appProps;

public class MainWindowController {
    @FXML
    GridPane grid;

    @FXML
    Button listSelect;
    @FXML
    TextField listFile;
    @FXML
    Button prenotificationSelect;
    @FXML
    TextField prenotificationFile;
    @FXML
    ComboBox emailSubjectComboBox;
    @FXML
    ToggleGroup messageToggleGroup;
    @FXML
    RadioButton defaultMessageButton;
    @FXML
    RadioButton customMessageButton; //if toggled show label & text field.
    @FXML
    TextField customMessageTextField; //if active deactivate runEverything button if blank
    @FXML
    Button runEverything;
    @FXML
    Label progressLabel;
    @FXML
    ProgressBar progressBar;

    static ArrayList<String> previouslySentFiles;

    static {
        try {
            previouslySentFiles = loadPreviouslySentFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static int noEmailsSent;
    static int noPrinted;
    ArrayList<String> testNonSendable;

    public void initialize(){

        noEmailsSent = 0;
        noPrinted = 0;
        testNonSendable = new ArrayList<>();
        progressBar.setProgress(0);
        progressBar.setMinSize(250,10);
        //progressBar.setVisible(false);
        progressLabel.setText("READY");
        progressLabel.setMinWidth(250);
        progressLabel.setAlignment(Pos.CENTER);


        customMessageButton.selectedProperty().bindBidirectional(customMessageTextField.visibleProperty());
        customMessageButton.selectedProperty().bindBidirectional(customMessageTextField.editableProperty());
        BooleanBinding runButtonBinding = customMessageButton.selectedProperty()
                .and(customMessageTextField.textProperty().isEmpty())
                .or(messageToggleGroup.selectedToggleProperty().isNull())
                .or(prenotificationFile.textProperty().isEmpty())
                .or(listFile.textProperty().isEmpty())
                .or(emailSubjectComboBox.getSelectionModel().selectedItemProperty().isNull());

        runEverything.disableProperty().bind(runButtonBinding);
        progressLabel.visibleProperty().bind(runEverything.disableProperty().not());
        progressBar.visibleProperty().bind(runEverything.disableProperty().not());

        emailSubjectComboBox.getItems().add("NYSLA Prenotification"); //load default names from resources folder
        emailSubjectComboBox.getItems().add("Final Notice");
        emailSubjectComboBox.setEditable(true);



    }

   /* @FXML
    protected void masterSelect(ActionEvent event) { //REMOVE
        //Possible update: Modify line in prenotification list and set text to bold if successfully sent
        //Possible update: Group customer lines so that one email can be sent for multiple lines instead of
        //seperate emails

        //This section allows the user to select an xls file with all of or customer data
        FileChooser fileChooser = new FileChooser();
        //Next line ensures that only xls Excel file types will be selectable
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Excel File", "*.xls");
        fileChooser.getExtensionFilters().add(extensionFilter);
        //Next two lines set the start folder for the file selection for ease of use
        File id = new File("Q:/");
        fileChooser.setInitialDirectory(id);
        //next line and if/else statement opens a file selection window to choose customer data file
        File file = fileChooser.showOpenDialog(grid.getScene().getWindow());
        if (file != null) {
            customerFile.setText(file.toString());
        } else {
            System.out.println("You must select a file");
        }

    }*/


    @FXML
    protected void listSelect(ActionEvent event) throws IOException {
        //This section allows the user to select an xls file with the current list to be sent
        FileChooser fileChooser = new FileChooser();
        //Next line ensures that only xls Excel file types will be selectable
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Excel File", "*.xls","*.xlsx");
        fileChooser.getExtensionFilters().add(extensionFilter);
        //Next two lines set the start folder for the file selection for ease of use
        File id = new File(appProps.getProperty("defaultListLoadDirectory")); //SET THIS AS A VARIABLE
        fileChooser.setInitialDirectory(id);
        //next line and if/else statement opens a file selection window to choose current list
        File file = fileChooser.showOpenDialog(grid.getScene().getWindow());
        if (file != null) {
            listFile.setText(file.toString());
        } else {
            System.out.println("You must select a file");
        }
    }

    @FXML
    protected void prenotificationSelect(ActionEvent event) throws IOException {
        //This section allows the user to select an xls file with our standard prenotification message
        FileChooser fileChooser = new FileChooser();
        //Next line ensures that only xls Excel file types will be selectable
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Excel File", "*.xls","*.xlsx");
        fileChooser.getExtensionFilters().add(extensionFilter);
        //Next two lines set the start folder for the file selection for ease of use
        File id = new File(appProps.getProperty("defaultListLoadDirectory"));//SET THIS AS A VARIABLE
        fileChooser.setInitialDirectory(id);
        //next line and if/else statement opens a file selection window to choose prenotification message
        File file = fileChooser.showOpenDialog(grid.getScene().getWindow());
        if (file != null) {
            prenotificationFile.setText(file.toString());
        } else {
            System.out.println("You must select a file");
        }

    }


    @FXML
    protected void doEverything(ActionEvent event) throws IOException {
        //next lines ensure files are selected for all fields preventing Null Pointer Exception
        //fix this to disable button after first click once it is confirmed it has proper files




//        if (listFile.getText().isEmpty() || prenotificationFile.getText().isEmpty()) {
//            System.out.println("There was a problem. Please make sure both files are selected");
//            return;
//        }

       displayAlreadySentMessage();

        //works with HSSF(.xls) or XSSF(.xlsx)
        Workbook wb = null;
        try {
            wb = WorkbookFactory.create(new File(listFile.getText()));
            System.out.println(listFile.getText() + " loaded as " + wb.getSpreadsheetVersion().toString());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to load selected file:" + listFile.getText());
        }

        ArrayList<Line> testLines = loadListToSend(wb);


        String emailSubject = String.valueOf(emailSubjectComboBox.getSelectionModel().getSelectedItem());
        String emailMessage;
        if(customMessageTextField.getText().isEmpty()){
            emailMessage = appProps.getProperty("bodyText");
        }else{
            emailMessage = customMessageTextField.getText();
        }

        //These are now in process list as they are only present in that function
        //HashMap<String,String> testDatabaseMap = getDatabaseList(testLines);
        //ArrayList<String> testNonSendable = new ArrayList<>();





            ///////start of new method
            processList(testLines,emailSubject,emailMessage);
            ///put into overall cleanup method
            //displayCompletionReport(); //DO NOT DISPLAY UNTIL COMPLETED. POSSIBLY NEW TASK
            //saveCompletedList();
            //updateOriginalFile(); //sync? must wait for process list to complete before running


            ///alter original file to leave notes
            ///


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
         sb.append("******Prenotification Process Complete******\n"+
                 "No. of prenotification emails sent: " + noEmailsSent + "\n"+
                 "No. of prenotifications to be printed: " + noPrinted + "\n" +
                 "Unable to send for:\n"); //SAVE TO ORIGINAL FILE?
        for (String customer : testNonSendable){
             sb.append(customer + "\n");
        }
        String fullMessage = sb.toString();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Results");
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setContentText(fullMessage);
        alert.showAndWait();
    }

    //IN PROGRESS Must wait for processList to complete
    public void updateOriginalFile() throws IOException {
        Workbook wb = null;
        System.out.println("Now running updateOriginalFile method");
        try {
            wb = WorkbookFactory.create(new File(listFile.getText()));
            System.out.println(listFile.getText() + " loaded as " + wb.getSpreadsheetVersion().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Sheet sheet = wb.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();
        while(rowIterator.hasNext()){
            Row currentRow = rowIterator.next();
            while(currentRow != null) {
                Cell licenseCell = currentRow.getCell(5);
                Cell customerNameCell = currentRow.getCell(6);
                //check unsent list otherwise mark as SENT
                Cell notesCell = currentRow.getCell(8);
                //notesCell interpreted as null?
                Cell deliveryDateCell = currentRow.getCell(1);
                deliveryDateCell.setCellValue("1/1/2022");
//                if(!testNonSendable.contains(customerNameCell.toString())) {
//                    notesCell.setCellValue("SENT");
//                }else {
//                    notesCell.setCellValue("UNABLE TO SEND TO 1 OR MORE ADDRESSES");
//                }
            }
           // wb.close();
        }
        OutputStream fileToOverwrite = new FileOutputStream(listFile.getText());
        try {
            wb.write(fileToOverwrite);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            fileToOverwrite.close();
        }

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

    public void processList(ArrayList<Line> testLines, String emailSubject, String emailMessage){
        //HashMap<String,String> testDatabaseMap = getDatabaseList(testLines);
        HashMap<String,String> testDatabaseMap = new HashMap<>();
       testDatabaseMap.put(testLines.get(5).getLicenseNumber(),"mplath@usawineimports.com");

        Task task  = new Task(){ //TASK NO LONGER UPDATING PROGRESS??? TASK IS WRONG AS NEVER RUNS SETONSUCCEED

            @Override
            protected Object call() throws Exception {
                for (int i = 0; i < testLines.size(); i++) {
                    //need to check line licence against customer license
                    String emailAddresses = testDatabaseMap.get(testLines.get(i).getLicenseNumber());

                    if (StringUtils.isBlank(emailAddresses)) {
                        emailAddresses = ""; //seems redundant but had trouble with cust Miss Korea trying to create email with null
                    }
                    System.out.println(testLines.get(i).getCustomerName() + " " + emailAddresses);
                    boolean success = false;
                    if (!emailAddresses.equals(null) && !emailAddresses.isEmpty() && emailAddresses.trim().length() != 0) {
//
//                        Attachment otherAttachment = new Attachment(testLines.get(i), prenotificationFile.getText());
//                        Workbook theAttachment = (Workbook) otherAttachment.call();
//                       Email theEmail = new Email(emailAddresses, prenotificationFile.getText().toString(),emailSubject,emailMessage);
//                       System.out.println("Send email for " +
//                               theAttachment.getSheetAt(0).getRow(22).getCell(6) + " to " + emailAddresses + " Total:$" +
//                                theAttachment.getSheetAt(0).getRow(22).getCell(7));
                        noEmailsSent++;
                        updateMessage("Sending for " + testLines.get(i).getCustomerName());
                        Thread.sleep(500);
//                       success = (boolean) theEmail.call();
//                       System.out.println(success);
                    } else if (emailAddresses.equals(null) || emailAddresses.isEmpty() || emailAddresses.trim().length() == 0) {
                        testNonSendable.add(testLines.get(i).getCustomerName());
//                        System.out.println(testLines.get(i).getCustomerName() + " added to nonsendable");
                        updateMessage("Unable to send for" + testLines.get(i).getCustomerName());
                        Thread.sleep(500);

//                //excel attachment will instead be printed out by default printer here

                        noPrinted++;
                    }

                    updateProgress(i,testLines.size());
                    updateMessage("");

                    Thread.sleep(500);
                }
                updateProgress(testLines.size(),testLines.size());
                //System.out.println("TASK COMPLETE- PROGRESS FINISHED");

                return null;
            }

        };

            Task altTask = new Task() { //WORKS. USE THIS
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

                            Attachment otherAttachment = new Attachment(testLines.get(i), prenotificationFile.getText());
                            Workbook theAttachment = (Workbook) otherAttachment.call();
                            Email theEmail = new Email(emailAddresses, prenotificationFile.getText().toString(), emailSubject, emailMessage);
//                            System.out.println("Send email for " +
//                                    theAttachment.getSheetAt(0).getRow(22).getCell(6) + " to " + emailAddresses + " Total:$" +
//                                    theAttachment.getSheetAt(0).getRow(22).getCell(7));
                            success = (boolean) theEmail.call();
                        }else{
                           testNonSendable.add(testLines.get(i).getCustomerName());
                        }
                        String message = "";
                        if(success){
                            message += "Email sent for:" + testLines.get(i).getCustomerName();
                        }else{
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
                    System.out.println("SUCCESS ON TASK");
                    saveCompletedList();
                    System.out.println("SUCCESS ON SAVE COMPLETED LIST");
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

    //}

    //DEPRECATED
    /*private static HSSFWorkbook readXLSWorkbook(String filename) throws IOException {
        try (FileInputStream fis = new FileInputStream(filename)) {
            return new HSSFWorkbook(fis);
        }

    }*/
    //DEPRECATED
    /*private static XSSFWorkbook readXLXSWorkbook(String filename) throws IOException {
        try (FileInputStream fis = new FileInputStream(filename)) {
            return new XSSFWorkbook(fis);
        }

    }*/

    //DEPRECATED
    /*@FXML  //ADD VARIOUS SCHEMA AND OPTIONS
    public static HSSFWorkbook createXLSAttachement(Line theLine, String theFile) {
        try {
            FileInputStream fis = new FileInputStream(new File(theFile));
            HSSFWorkbook attachmentWorkbook = new HSSFWorkbook(fis);
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

    }*/
    //needs to be part of a class implementing Callable. Or load workbook intially and jsut alter each time.
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

    //DEPRECATED
    //ADD VARIOUS SCHEMA AND OPTIONS
   /* public static ArrayList<Line> loadXLSListToSend(HSSFWorkbook workbook) {
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
    }*/

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


    //building a list of the customers found on the reportable list //USE CONFIG
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

    public static ArrayList<String> loadPreviouslySentFiles() throws IOException {
        File file = new File("Q:\\Previously Sent Emails\\Previously Sent Emails.txt");//USE CONFIG
        file = new File(appProps.getProperty("defaultPreviouslySentFilesDirectory")
                + appProps.getProperty("defaultPreviouslySentFilesPath")
                + appProps.getProperty("defaultPreviouslySentFilesFile") + ".txt");
        ArrayList<String> listToReturn = new ArrayList<>();
        try(Scanner scanner = new Scanner(new FileReader(file))){
            //need to loop through and read each String before returning array of Strings
            while(scanner.hasNextLine()){
                String currentLine = scanner.nextLine();
                listToReturn.add(currentLine);
                // System.out.println(currentLine + " was loaded into program");
//                String[]currentLineBreakdown = currentLine.split(",");
//                listToReturn.add(currentLineBreakdown[0]);
//                listToReturn.add(currentLineBreakdown[1]);
            }
            if (listToReturn.size()>35){
                listToReturn.remove(0);
//              listToReturn.remove(0);
            }

            return listToReturn;

        }catch (IOException e){
            return listToReturn;

        }
    }
}
