package core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.Properties;

import static core.Main.appProps;

public class PropertyValues {
    private static final Logger log = LoggerFactory.getLogger(PropertyValues.class);

    String values = "";
    InputStream inputStream;
    String propertiesFileName; //TODO: create properties filename and directory fields
    Properties properties;


    public PropertyValues(String fileName){
        propertiesFileName = fileName;
        properties = new Properties();
        String propertiesdirectoryPath = "src/resources/config.properties";
        String propertiesConfigFile = "./config.properties";
        try {
            inputStream = new FileInputStream(new File(propertiesdirectoryPath));
            properties.load(inputStream);
            log.debug("Config file loaded:{}",propertiesdirectoryPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            log.error("ERROR:{}",e);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("ERROR:{}",e);
        }
        log.debug("property file\'{}\' successfully loaded", propertiesFileName);
    }

    public String getPropertiesFileName() {
        return propertiesFileName;
    }


    public String getPropValues() throws IOException {

        try{
            Properties properties = new Properties();
            String propertiesFileName = "resources/config.properties";
            URL otherFileName = getClass().getClassLoader().getResource("/config.properties");
            inputStream = new FileInputStream(String.valueOf(otherFileName.getFile()));
            //inputStream.getClass().getClassLoader().getResourceAsStream(propertiesFileName);

            if(inputStream != null){
                properties.load(inputStream);
            }else{
                throw new FileNotFoundException("Property file " + propertiesFileName + " could not be located!");
            }

        }catch (Exception e){

        }finally {
            inputStream.close();
        }
        return values;
    }

    public String getProperty(String propertyName) throws IOException {
        String property = "";
        try{
//            String propertiesdirectoryPath = "src/resources/config.properties";
//            URL otherFileName = getClass().getClassLoader().getResource("config.properties");
//            //inputStream = getClass().getResourceAsStream("/config.properties"); //works but makes config file uneditable when building jar
//            inputStream = new FileInputStream(new File(propertiesdirectoryPath));
//
//            if(inputStream != null){
//                properties.load(inputStream);
//            }else{
//                throw new FileNotFoundException("Property file " + propertiesFileName + " could not be located!");
//            }

            Date time = new Date(System.currentTimeMillis());

            property = properties.getProperty(propertyName);
            log.debug("property:\'{}\' retrieved as:{}",propertyName,property);
        }catch (Exception e){
            System.err.println(e);
            log.error("ERROR:{}",e);
        }finally {
            inputStream.close();
        }

        return property;
    }


}
