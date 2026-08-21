package com.alshuibi.grocery.model;

public class Customer {
    private long localId;
    private String globalId;
    private String name;
    private String phone;
    private String address;
    private long creditLimit;
    private long balance;
    private boolean active = true;
    private String createdAt;

    public Customer() {}

    public Customer(String globalId, String name, String phone, String address, long creditLimit) {
        this.globalId = globalId;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.creditLimit = creditLimit;
    }

    public long getLocalId() { return localId; }
    public void setLocalId(long localId) { this.localId = localId; }
    public String getGlobalId() { return globalId == null ? "" : globalId; }
    public void setGlobalId(String globalId) { this.globalId = globalId; }
    public String getName() { return name == null ? "" : name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone == null ? "" : phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address == null ? "" : address; }
    public void setAddress(String address) { this.address = address; }
    public long getCreditLimit() { return creditLimit; }
    public void setCreditLimit(long creditLimit) { this.creditLimit = Math.max(0, creditLimit); }
    public long getBalance() { return balance; }
    public void setBalance(long balance) { this.balance = balance; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getCreatedAt() { return createdAt == null ? "" : createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
