package com.errawi.trojanrec.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationBank {
	/**
	 * Maps user (by their string Net ID) to a list of slots which they waitlisted
	 * for and then 
	 */
	private Map<String, List<Reservation> > userNotifs;
	
	public NotificationBank() {
		userNotifs = new HashMap<String, List<Reservation> >();
	}
	
	public synchronized boolean addUserNotifs(List<User> listOfUsers, Reservation timeslot) {
		return false;
	}
	
	private boolean addUserNotifs(String netID, Reservation timeslot) {
		return false;
	}
	
	public synchronized List<Reservation> getUserNotifs(String netID) {
		if (netID == null)
			return null;
		return userNotifs.get(netID);	
	}
}
