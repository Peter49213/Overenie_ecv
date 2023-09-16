package com.example.overenie_ecv;

public class Employees {
    private String name;
    private String plate;
    private String surname;
    private String key;

    public Employees(String name, String plate, String surname, String key) {
        this.name = name;
        this.plate = plate;
        this.surname = surname;
        this.key = key;
    }

    public Employees() {

    }


    public void setName(String name) {
        this.name = name;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }


    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getPlate() {
        return plate;
    }

    public String getSurname() {
        return surname;
    }
}
