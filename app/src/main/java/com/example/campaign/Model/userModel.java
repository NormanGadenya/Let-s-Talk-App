package com.example.campaign.Model;

public class userModel {
    private String userName,userId;
    private String about;
    private String phoneNumber;
    private String profileUrI;

    public userModel(){

    }
    public userModel(String userName, String about, String phoneNumber, String profileUrI, String userId){
        this.userName = userName;
        this.about= about;
        this.phoneNumber=phoneNumber;
        this.profileUrI=profileUrI;
        this.userId=userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String name) {
        this.userName = name;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setProfileUrI(String profileUrI) {
        this.profileUrI = profileUrI;
    }

    public String getProfileUrI() {
        return profileUrI;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}