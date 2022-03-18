package core;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

public class PropertyValues {


    String values = "";
    InputStream inputStream;
    String propertiesFileName;
    Properties properties;


    public PropertyValues(String fileName){
        propertiesFileName = fileName;
        properties = new Properties();
    }

    public String getPropertiesFileName() {
        return propertiesFileName;
    }


    public String getPropValues() throws IOException {

        try{
            Properties properties = new Properties();
            String propertiesFileName = "resources/config.properties";
            inputStream = new FileInputStream(propertiesFileName);
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
            inputStream = getClass().getClassLoader().getResourceAsStream(propertiesFileName);

            if(inputStream != null){
                properties.load(inputStream);
            }else{
                throw new FileNotFoundException("Property file " + propertiesFileName + " could not be located!");
            }

            Date time = new Date(System.currentTimeMillis());

            property = properties.getProperty(propertyName);

            System.out.println(propertyName + " = " + property + "\tRetrieved from " + propertiesFileName +
                    " at " + time.toString());

        }catch (Exception e){
            System.err.println(e);
        }finally {
            inputStream.close();
        }

        return property;
    }


}