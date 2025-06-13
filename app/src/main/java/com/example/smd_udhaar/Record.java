package com.example.smd_udhaar;

import java.io.Serializable;

public class Record implements Serializable {
    private String BorrowerName;
    private String LenderName;
//    private String lenderId;
    private String Date;
    private int Amount;
    private String status;
    private String recordId;
    private String lenderPhoneNumber;
    private String borrowerPhoneNumber;


    public Record() {}

    public Record(String borrowerName, String lenderName, int amount, String date, String status) {
        BorrowerName = borrowerName;
        LenderName = lenderName;
        Date = date;
        Amount = amount;
        this.status = status;
    }

    public String getBorrowerPhoneNumber() {
        return borrowerPhoneNumber;
    }

    public void setBorrowerPhoneNumber(String borrowerPhoneNumber) {
        this.borrowerPhoneNumber = borrowerPhoneNumber;
    }

    public String getLenderPhoneNumber() {
        return lenderPhoneNumber;
    }

    public void setLenderPhoneNumber(String lenderPhoneNumber) {
        this.lenderPhoneNumber = lenderPhoneNumber;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBorrowerName() {
        return BorrowerName;
    }

    public void setBorrowerName(String borrowerName) {
        BorrowerName = borrowerName;
    }

    public String getLenderName() {
        return LenderName;
    }

    public void setLenderName(String lenderName) {
        LenderName = lenderName;
    }

    public String getDate() {
        return Date;
    }

    public void setDate(String date) {
        Date = date;
    }

    public int getAmount() {
        return Amount;
    }

    public void setAmount(int amount) {
        Amount = amount;
    }
}
