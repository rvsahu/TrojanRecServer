package com.errawi.trojanrec.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationBank {
	/**
	 * Maps user (by their string Net ID) to a list of slots which they wait listed
	 * for and then 
	 */
	private Map<String, List<Reservation> > allNotifs;
	
	public NotificationBank() {
		allNotifs = new HashMap<String, List<Reservation> >();
	}
	
	/**
	 * Given a list of Users and a timeslot, adds the given timeslot to the notif list of each user.
	 * 
	 * @param listOfUsers  List of users who were on the wait list for the time slot
	 * @param timeslot  The time slot for which users were wait listed (and now a spot has opened up)
	 */
	public synchronized void addUserNotifs(List<User> listOfUsers, Reservation timeslot) {
		for (User user : listOfUsers) {
			String netID = user.getNetID();
			addUserNotifs(netID, timeslot);
		}
	}
	
	/**
	 * Helper for public addUserNotifs, add time slots a specific user should be notified about
	 * 
	 * Given a user's net ID, and a time slot they were wait listed for, adds that time slot
	 * to that user's list in the notification bank
	 * @param netID
	 * @param timeslot
	 */
	private void addUserNotifs(String netID, Reservation timeslot) {
		//check if user is in notif bank
		if (allNotifs.containsKey(netID)) {
			//user in notif bank
			allNotifs.get(netID).add(timeslot); //add time slot user should be notified for to existing list
		} else {
			//not in notif bank
			List<Reservation> newNotifList = new ArrayList<Reservation>(); //create new list for the user
			newNotifList.add(timeslot); //add the first time slot they should be notified for to new list
			allNotifs.put(netID, newNotifList); //add the new list to the bank
		}
	}
	
	/**
	 * Given the net ID for a user, returns their notif list, or null if the user doesn't have a list
	 * 
	 * @param netID  The user's net ID
	 * @return     The user's notif list, null if the user doesn't exist
	 */
	public synchronized List<Reservation> getUserNotifs(String netID) {
		if (netID == null)
			return null;
		//user exists, check if their notif list does
		if (allNotifs.containsKey(netID)) {
			//notif list exists
			List<Reservation> userNotifList = allNotifs.get(netID); //get users notif list
			//once they've been gotten they aren't needed in bank anymore
			allNotifs.replace(netID, new ArrayList<Reservation>()); //replace old list with new blank one
			return userNotifList; //return user's notif list
		} else {
			//notif list doesn't exist 
			return new ArrayList<Reservation>(); //return a blank list
		}
	}
}
