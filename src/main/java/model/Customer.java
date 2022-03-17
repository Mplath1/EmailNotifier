package model;

/**
 * Created by Michael Plath on 8/8/2018.
 */
public class Customer {
    private String licenseNumber;
    private String customerName;
    private String customerEmail;

    public Customer(String licenseNumber, String customerName, String customerEmail) {
        this.licenseNumber = licenseNumber;
        this.customerName = customerName;
        if(customerEmail == null){
         this.customerEmail = "";
        }else {
            this.customerEmail = customerEmail;
        }
    }


    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }
}
