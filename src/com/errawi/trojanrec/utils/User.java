package com.errawi.trojanrec.utils;

public class User {
	/**
	 * The name of the user, first name followed by last name (separated with a
	 * space).
	 */
	private String name;

	/**
	 * User's USC net ID. This is what users initially use to log in, and this will
	 * be used for communicating with the server.
	 */
	private String netID;

	/**
	 * User's USC student ID. Needs to be a long as an int will not be able to store
	 * many USC student IDs (Integer.MAX_VALUE is smaller than a lot of them, mine
	 * for example).
	 */
	private long studentID;

	/**
	 * User's photo. Not quite sure how to implement this yet, for now is a String
	 * that is meant to be a URL to that image.
	 */
	private String userPhoto;

	public User() {
		name = "";
		netID = "";
		studentID = -1;
		userPhoto = "";
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

	public String getNetID() {
		return this.netID;
	}

	public void setNetID(String netID) {
		this.netID = netID;
	}

	/**
	 * Sets a URI to the user's photo.
	 * 
	 * @param userPhoto A URI to the user's photo.
	 */
	public void setUserPhoto(String userPhoto) {
		this.userPhoto = userPhoto;
	}

	/**
	 * Returns a URI to the user's photo.
	 * 
	 * @return A URI to the user's photo.
	 */
	public String getUserPhoto() {
		return this.userPhoto;
	}

	/**
	 * Returns a boolean representing if the user is logged in or not. True if they
	 * are, false otherwise.
	 *
	 * @return Whether the user is logged in or not.
	 */
	public boolean isLoggedIn() {
		if (studentID == -1) {
			return false;
		}
		return true;
	}
}