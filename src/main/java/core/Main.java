package core;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.mail.Session;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;


public class Main extends Application {
    public static PropertyValues appProps = new PropertyValues("config.properties");

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader();
        String rootFilePath = "src/main/java/views/MainWindow.fxml";
        FileInputStream fxmlStream = new FileInputStream(rootFilePath);
        Parent root = loader.load(fxmlStream);

        Properties properties = System.getProperties();
        Session session = Session.getDefaultInstance(properties);
        InetAddress host;
        try{
            host = InetAddress.getLocalHost();
        }catch(Exception e){
            System.out.println(e.getCause().toString());
        }
        try {
            appProps.getPropValues();
            System.out.println(appProps.getProperty("defaultListLoadDirectory"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String sender = appProps.getProperty("sender");
        String fromAddress = appProps.getProperty("mail.stmp.from");
        String mailHost = appProps.getProperty("mail.stmp.host");
        String port = appProps.getProperty("mail.stmp.port");
        String starttlsEnable = appProps.getProperty("mail.smtp.starttls.enable");
        properties.put("mail.smtp.port", "25");
        properties.put("mail.smtp.starttls.enable", starttlsEnable);
        properties.put("mail.smtp.host",mailHost);
        //properties.put("mail.smtp.port", port); //PORT LOADS AS NULL?
        properties.put("mail.smtp.from",fromAddress);



        primaryStage.setTitle("Email Prenotifications");
        primaryStage.setScene(new Scene(root, 300, 350));
        primaryStage.show();
    }

    private void loadConfigurations(){

    }


    public static void main(String[] args) {
        launch(args);
    }
}

