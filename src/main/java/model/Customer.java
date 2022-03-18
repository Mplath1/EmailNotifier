package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Michael Plath on 8/8/2018.
 */
public class Customer {
    private String licenseNumber;
    private String customerName;
    private String customerEmail;
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

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public boolean addInvoice(Invoice invoice){
        return invoiceList.add(invoice);
    }

    public List<Invoice> getInvoiceList(){
        return invoiceList;
    }
}
