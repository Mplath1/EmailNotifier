package model;

public class Invoice {
    private String orderNumber;
    private String deliveryDate;
    private String notificationDate;
    private String reportingDate;
    private String portfolioCode;
    private String dollarValue;

    public Invoice(String orderNumber, String deliveryDate, String notificationDate,
                   String reportingDate, String portfolioCode, String dollarValue){
        this.orderNumber = orderNumber;
        this.deliveryDate = deliveryDate;
        this.notificationDate = notificationDate;
        this.reportingDate = reportingDate;
        this.portfolioCode = portfolioCode;
        this.dollarValue = dollarValue;
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

    public String getDollarValue() {
        return dollarValue;
    }
}
