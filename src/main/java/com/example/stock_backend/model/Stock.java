package com.example.stock_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "stocks")
public class Stock {
    @Id
    private String id;

    private String code;
    private String name;
    private double price;
    private double change;

    public Stock() {}

    public Stock(String code, String name, double price, double change) {
        this.code = code;
        this.name = name;
        this.price = price;
        this.change = change;
    }

    // getter & setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getChange() { return change; }
    public void setChange(double change) { this.change = change; }
}
