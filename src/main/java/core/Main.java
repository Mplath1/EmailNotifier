package core;

import controllers.MainWindowController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Session;

import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;


public class Main extends Application {
    public static PropertyValues appProps;
    public static ArrayList<String> previouslySentFiles;
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) throws Exception{
        bootstrap();
        FXMLLoader loader = new FXMLLoader();
        String rootFilePath = "src/main/java/views/MainWindow.fxml";
        rootFilePath = "src/resources/views/MainWindow.fxml";
        //rootFilePath = "target/classes/views/MainWindow.fxml";
        URL otherView = getClass().getClassLoader().getResource("views/MainWindow.fxml");
        log.debug("FXML file loaded:{}",otherView.toString());
        //InputStream is = getClass().getClassLoader().getResourceAsStream("/views/MainWindow.fxml");
       // FileInputStream fxmlStream = (FileInputStream) is;
        MainWindowController controller = new MainWindowController();
        log.debug("Controller created");
        loader.setController(controller);
        log.debug("Controller set to {}",loader.getController().toString());
        loader.setLocation(otherView);
        log.debug("View set to {}",loader.getLocation().toString());
        //loader.setRoot(otherView.getFile());
        //System.out.println(loader.getRoot().toString());
        log.debug("Loaded view \'{}\' from:{}",loader.getLocation().getFile(),loader.getLocation());
        Parent root = loader.load();
        //loader.setRoot(otherView);
        log.debug("FXMLLoader successfully loaded");
        String stageTitle = "Email Prenotifications";
        primaryStage.setTitle(stageTitle);
        log.debug("PrimaryStage Title set to \'{}\'",primaryStage.getTitle().toString());
        //Scene mainScene = new Scene(root, 350, 350);
        Scene mainScene = new Scene(root, 350, 350);
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    private void bootstrap() throws IOException {
        appProps = new PropertyValues("config.properties");
        //TODO:: load config.properties from outside path and not include with JAR
        Properties systemProperties = System.getProperties();
        Session session = Session.getDefaultInstance(systemProperties);
        InetAddress host;
        try{
            host = InetAddress.getLocalHost();
        }catch(Exception e){
            System.out.println(e.getCause().toString());
        }

        String sender = appProps.getProperty("sender");
        String fromAddress = appProps.getProperty("mail.smtp.from");
        String mailHost = appProps.getProperty("mail.smtp.host");
        String port = appProps.getProperty("mail.smtp.port");
        String starttlsEnable = appProps.getProperty("mail.smtp.starttls.enable");
        systemProperties.put("mail.smtp.port", "25");
        systemProperties.put("mail.smtp.starttls.enable", starttlsEnable);
        systemProperties.put("mail.smtp.host",mailHost);
        systemProperties.put("mail.smtp.port", port);
        systemProperties.put("mail.smtp.from",fromAddress);




        previouslySentFiles = loadPreviouslySentFiles();
    }

    public static ArrayList<String> loadPreviouslySentFiles() throws IOException {
        File file; //TODO:store file in resources folder
        file = new File(appProps.getProperty("defaultPreviouslySentFilesDirectory")
                + appProps.getProperty("defaultPreviouslySentFilesPath")
                + appProps.getProperty("defaultPreviouslySentFilesFile") + ".txt");
        ArrayList<String> listToReturn = new ArrayList<>();
        try(Scanner scanner = new Scanner(new FileReader(file))){
            while(scanner.hasNextLine()){
                String currentLine = scanner.nextLine();
                listToReturn.add(currentLine);
            }
            if (listToReturn.size()>35){
                listToReturn.remove(0);
//              listToReturn.remove(0);
            }

            return listToReturn;

        }catch (IOException e){
            log.debug("Unable to load \'{}\' - {}",file.getName(),e);
            return listToReturn;

        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}

