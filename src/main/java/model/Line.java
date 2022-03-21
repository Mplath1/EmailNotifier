package model;

/**
 * Created by Michael Plath on 2/22/2018.
 */
@Deprecated
public class Line {
    String orderNumber;
    String deliveryDate;
    String notificationDate;
    String reportingDate;
    String portfolioCode;
    String licenseNumber;
    String customerName;
    String dollarValue;
    boolean haveEmail;

    ///use comparator or comparable to check int value of licenseNumber against custList value

    public Line(String orderNumber, String deliveryDate, String notificationDate, String reportingDate, String portfolioCode, String licenseNumber, String customerName, String dollarValue) {
        this.orderNumber = orderNumber;
        this.deliveryDate = deliveryDate;
        this.notificationDate = notificationDate;
        this.reportingDate = reportingDate;
        this.portfolioCode = portfolioCode;
        this.licenseNumber = licenseNumber;
        this.customerName = customerName;
        this.dollarValue = dollarValue;
        this.haveEmail=false;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getDeliveryDate() {
        return deliveryDate;
    }

    public String getNotificationDate() {
        return notificationDate;
    }

    public String getReportingDate() {
        return reportingDate;
    }

    public String getPortfolioCode() {
        return portfolioCode;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getDollarValue() {
        return dollarValue;
    }

    public boolean isHaveEmail() {
        return haveEmail;
    }

    public void setHaveEmail(boolean haveEmail) {
        this.haveEmail = haveEmail;
    }

    public boolean compareWith(String testLicense){
        int testingLicense = Integer.parseInt(testLicense);
        int lineLicense = Integer.parseInt(licenseNumber);

        if(lineLicense==testingLicense){
            return true;
        }
        return false;
    }
}
