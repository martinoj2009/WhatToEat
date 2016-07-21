package com.martinojones.whattoeat;

/**
 * Created by marti on 7/11/2016.
 */
public class Resturant {

    private String name;
    private String address;
    private String city;
    private String postalcode;
    private String website;

    Resturant(String na, String streetAddress, String resCity, String post, String web)
    {
        this.name = na;
        this.address = streetAddress;
        this.city = resCity;
        this.postalcode = post;
        this.website = web;
    }



    //Getter and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalcode() {
        return postalcode;
    }

    public void setPostalcode(String postalcode) {
        this.postalcode = postalcode;
    }

    public String getWebsite() { return website; }

    public void setWebsite(String website) { this.website = website; }

    public String toString()
    {
        return this.name + "\n" + this.address + "\n" + this.city;
    }

}
