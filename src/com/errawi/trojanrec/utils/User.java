package com.errawi.trojanrec.utils;

import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable {
    /**
     * The name of the user, first name followed by last name (separated with a space).
     */
    private String name;

    /**
     * User's USC student ID. Needs to be a long as an int will not be able to store many
     * USC student IDs (Integer.MAX_VALUE is smaller than a lot of them, mine for example).
     */
    private long studentID;

    /**
     * User's photo. Not quite sure how to implement this yet, for now is a String that
     * is meant to be a URL to that image.
     */
    private String userPhoto;

    public User() {
        name = "";
        userPhoto = "";
        studentID = -1;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setStudentID(long studentID) {
        this.studentID = studentID;
    }

    public long getStudentID() {
        return this.studentID;
    }

    public void setUserPhoto(String userPhoto) {
        this.userPhoto = userPhoto;
    }

    public String getUserPhoto() {
        return this.userPhoto;
    }

    /**
     * Returns a boolean representing if the user is logged in or not. True if they are,
     * false otherwise.
     *
     * @return     Whether the user is logged in or not.
     */
    public boolean isLoggedIn() {
        if (studentID == -1) {
            return false;
        }
        return true;
    }

    /**
     *
     * @param username  The user's username.
     * @param password  The user's password.
     * @return     True if the login worked, false otherwise.
     */
    public boolean login(String username, String password) {
        //do server stuff
        return false;
    }

    @Override public int describeContents() {
        return 0;
    }

    @Override public void writeToParcel(Parcel dest, int flags) {

    }
}