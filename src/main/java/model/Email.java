package model;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import static core.Main.appProps;

/**
 * Created by Michael Plath on 3/8/2018.
 */
public class Email implements Callable {

    String recipient;
    String sender; //static?
    InetAddress host; //static?
    String attachmentName; //static? TODO: remove and use attachmentFile instead
    File attachentFile;
    String emailSubject; //pass emailSubject from FXML in constructor. If no emailSubject in FXML use default
    String bodyText; //pass bodytext from FXML in constructor. If no bodytext in FXML use default



    Properties properties = System.getProperties();
    //Session session = Session.getDefaultInstance(properties);
    Session session = Session.getInstance(properties); //static?

    public Email(String recipient, String attachmentName, String emailSubject, String bodyText ) throws IOException {
        this.recipient = recipient;
        this.attachmentName = attachmentName;
        this.emailSubject = emailSubject;
        this.bodyText = bodyText;
        try{
            host = InetAddress.getLocalHost();
        }catch(Exception e){
            System.out.println(e.getCause().toString());
        }
        //Session session = Session.getDefaultInstance(properties);


        sender = appProps.getProperty("sender");
        String fromAddress = properties.getProperty("mail.smtp.from");
        String mailHost = properties.getProperty("mail.smtp.host");
        String port = properties.getProperty("mail.smtp.port");
        String starttlsEnable = properties.getProperty("mail.smtp.starttls.enable");
    }

    public void setAttachentFile(File attachentFile) {
        this.attachentFile = attachentFile;
    }

    @Override
    public Object call() throws Exception {
        return sendTheEmail();
    }
    //add try and connection timeout
    public boolean sendTheEmail(){
        try{
            MimeMessage message = new MimeMessage(session);
            message.setFrom(sender);
            //This if/else statement checks if email contains semi-colons. If so it breaks apart the String
            //and adds each String as a recipient
            System.out.println("Inside SendTheEmail function");
            if(recipient.contains(";")){
                String[] listOfRecipients=splitRecipients(recipient);
                for (String theRecipient : listOfRecipients){
                    if(isValid(theRecipient)){ //TODO: can be eliminated as addresses are validated in customerClass
                        message.addRecipient(Message.RecipientType.TO,new InternetAddress(theRecipient));
                    }else if(!isValid(theRecipient)){
                        MimeMessage invalid = new MimeMessage(session);
                        invalid.setFrom("mplath@usawineimports.com"); //USE CONFIG FOR ALL OF THIS
                        invalid.addRecipient(Message.RecipientType.TO,new InternetAddress("mplath@usawineimports.com"));
                        invalid.setSubject("Undeliverable: Prenotification of Pending NYSLA COD Status from USA Wine Imports");
                        BodyPart invalidBodyPart = new MimeBodyPart();
                        invalidBodyPart.setText("Unable to send message to " + theRecipient.toString());
                        BodyPart invalidAttachment = new MimeBodyPart();
                        MimeMultipart invalidMultiPart = new MimeMultipart();
//                        String filename = attachmentName;
//                        DataSource source = new FileDataSource(filename);
                        DataSource source = new FileDataSource(attachentFile);
                        invalidAttachment.setDataHandler(new DataHandler(source));
                        invalidAttachment.setFileName(attachmentName);

                        invalidMultiPart.addBodyPart(invalidBodyPart);
                        invalidMultiPart.addBodyPart(invalidAttachment);
                        invalid.setContent(invalidMultiPart);
                        try {
                            Transport.send(invalid);
                        }catch(Exception e){
                            System.out.println("Exception thrown by " + theRecipient.toString());
                            e.printStackTrace();
                        }
                    }
                }
            }else{
                if(isValid(recipient)){ //TODO: can be eliminated as addresses are validated in customerClass
                    recipient =checkForBrackets(recipient);
                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
                }else if(!isValid(recipient)){ //TODO: can be eliminated as addresses are validated in customerClass
                    MimeMessage invalid = new MimeMessage(session);
                    invalid.setFrom("mplath@usawineimports.com");
                    invalid.addRecipient(Message.RecipientType.TO,new InternetAddress("mplath@usawineimports.com"));
                    invalid.setSubject("Undeliverable: " + emailSubject);
                    BodyPart invalidBodyPart = new MimeBodyPart();
                    invalidBodyPart.setText("Unable to send message to " + recipient);
                    BodyPart invalidAttachment = new MimeBodyPart();
                    MimeMultipart invalidMultiPart = new MimeMultipart();
//                    String filename = attachmentName;
//                    DataSource source = new FileDataSource(filename);
                    DataSource source = new FileDataSource(attachentFile);
                    invalidAttachment.setDataHandler(new DataHandler(source));
                    invalidAttachment.setFileName(attachmentName);

                    invalidMultiPart.addBodyPart(invalidBodyPart);
                    invalidMultiPart.addBodyPart(invalidAttachment);
                    invalid.setContent(invalidMultiPart);
                    try {
                        Transport.send(invalid);
                    }catch(Exception e){
                        System.out.println("Exception thrown by " + recipient.toString());
                        e.printStackTrace();
                    }}

            }
            //next lines set the subject of the email, create a body and fills it with the bodyText variable
            //message.setSubject("Prenotification of Pending NYSLA COD Status from USA Wine Imports");
            message.setSubject(emailSubject);
            message.addRecipient(Message.RecipientType.BCC,new InternetAddress("Prenotifications@usawineimports.com"));
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(bodyText);
            BodyPart attachmentBodyPart = new MimeBodyPart();
            //next lines will select the file to be attached and set attach to the email
            String filename = attachmentName;
//            DataSource source = new FileDataSource(filename);
//            attachmentBodyPart.setDataHandler(new DataHandler(source));
//            attachmentBodyPart.setFileName("Prenotification of Pending NYSLA COD Status from USA Wine Imports.xls");
            String filePath = "src/resources/AttachmentTemplates/temp.xlsx"; //TODO: use dependency injection
//            DataSource source = new FileDataSource(filePath);
            DataSource source = new FileDataSource(attachentFile);
            attachmentBodyPart.setDataHandler(new DataHandler(source));
            attachmentBodyPart.setFileName(filename);

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            multipart.addBodyPart(attachmentBodyPart);

            message.setContent(multipart);
            System.out.println("ALL OTHER EMAIL PARTS SET");
            try{
                System.out.println("TRYING TO SEND EMAIL WITH ATTACHMENT:" + source.getName() + " SENT AS " + filename);
                Transport.send(message);
                System.out.println("EMAIL NOW SENT");
                //set emailAddressMap entry in Customer class to true
                return true;
            }catch(SendFailedException e){
                //this section catches emails that can't be sent (bounce backs) and constructs a new email that is sent
                //to my inbox so that I'm aware the email was unsuccessful
                System.out.println("EXCEPTION- SEND FAILED");
                MimeMessage invalid = new MimeMessage(session);
                invalid.setFrom("mplath@usawineimports.com");
                invalid.addRecipient(Message.RecipientType.TO,new InternetAddress("mplath@usawineimports.com"));
                invalid.setSubject("Undeliverable: " + emailSubject);
                BodyPart invalidBodyPart = new MimeBodyPart();
                invalidBodyPart.setText("Unable to send message to one or more addresses");
                BodyPart invalidAttachment = new MimeBodyPart();
                MimeMultipart invalidMultiPart = new MimeMultipart();
                invalidAttachment.setDataHandler(new DataHandler(source));
                invalidAttachment.setFileName(attachmentName);
                //set emailAddressMap entry in Customer class to false

                invalidMultiPart.addBodyPart(invalidBodyPart);
                invalidMultiPart.addBodyPart(invalidAttachment);
                invalid.setContent(invalidMultiPart);
                Transport.send(invalid);
            }


        }catch(MessagingException mex){
            mex.printStackTrace();
        }
        return false;
    }

    //TODO: eliminate once emailAddressMap implemented in Customer class
    public static String[] splitRecipients(String listOfRecipients){
        //push changes by 6/18 if there are no issues
        String[] theRawList=listOfRecipients.split(";");
        ArrayList<String> workingList = new ArrayList<>();
        for(String recipient : theRawList){
            //System.out.println("Original: " + recipient);
            if(recipient.contains("<") || recipient.contains(">")){
                recipient =checkForBrackets(recipient);
//                String toKeep;
//                if(recipient.contains(">")) {
//                    int realStart = recipient.indexOf("<");
//                    int realEnd = recipient.indexOf(">");
//                    toKeep = recipient.substring(realStart + 1, realEnd);
//                }else{
//                    int realStart = recipient.indexOf("<");
//                    toKeep = recipient.substring(realStart +1, recipient.length());
//                }
//                recipient = toKeep;
            }
            workingList.add(recipient.trim());

        }
        String[] theList = new String[workingList.size()];
        for(int i=0;i<workingList.size();i++){
            theList[i] = workingList.get(i);
        }
        return theList;
    }


    public static String checkForBrackets(String recipient){
        //place into split recipients and unsplit main email method
        //possibly better with a map in functional programming as it's modifying and returning
        String toKeep =recipient;
        if(recipient.contains("<")){
            if(recipient.contains(">")) {
                int realStart = recipient.indexOf("<");
                int realEnd = recipient.indexOf(">");
                toKeep = recipient.substring(realStart + 1, realEnd);
            }else{
                int realStart = recipient.indexOf("<");
                toKeep = recipient.substring(realStart +1, recipient.length());
            }
        }
        return toKeep;
    }

    //TODO: can be eliminated as addresses are validated in customerClass
    public static boolean isValid(String email)
    {

        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\."+
                "[a-zA-Z0-9_+&*-]+)*@" +
                "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
                "A-Z]{2,7}$";

        Pattern pat = Pattern.compile(emailRegex);
        if (email == null)
            return false;
        return pat.matcher(email).matches();
    }
}
