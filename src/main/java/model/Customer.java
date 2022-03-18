package model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by Michael Plath on 8/8/2018.
 */
public class Customer {
    private String licenseNumber;
    private String customerName;
    private String customerEmail; //TODO: implement customerEmailListMap<address, boolean(sent)> and later portfolio/salesRepEmailList
    private List<Invoice> invoiceList;

    public Customer(String licenseNumber, String customerName, String customerEmail) {
        this.licenseNumber = licenseNumber;
        this.customerName = customerName;
        if(customerEmail == null){
         this.customerEmail = "";
        }else {
            this.customerEmail = customerEmail;
        }
        invoiceList = new ArrayList<>();
    }

    //TODO: Use this constructor movving forward and set customerEmail upon database retrieval
    public Customer(String licenseNumber, String customerName) {
        this.licenseNumber = licenseNumber;
        this.customerName = customerName;
        invoiceList = new ArrayList<>();
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    //TODO: validate and clean email address when setting
    public void setCustomerEmail(String customerEmail) {
        boolean valid = validateEmail(customerEmail);
        if(valid){
            this.customerEmail = customerEmail;
            System.out.println("Valid Email!");
        }else{
            System.out.println("Invalid Email!");
        }
    }

    public boolean addInvoice(Invoice invoice){
        return invoiceList.add(invoice);
    }

    public List<Invoice> getInvoiceList(){
        return invoiceList;
    }

    public boolean validateEmail(String email)
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Customer customer = (Customer) o;

        return licenseNumber.equals(customer.licenseNumber);
    }

    @Override
    public int hashCode() {
        return licenseNumber.hashCode();
    }
}
