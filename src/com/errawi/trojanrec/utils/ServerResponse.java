package com.errawi.trojanrec.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ServerResponse implements Serializable {
	private static final long serialVersionUID = 72002803L;
	
	private ResponseType responseType;
	
	private User user;
	
	private ArrayList<Reservation> bookings;
        
    /**
     * Slots that the user was waitlisted that had opened up.
     * We send this to client so user can be notified of it.
     */
	private List<Reservation> openedSlots;
	
	private ArrayList<String> timeslots;
	
	public ServerResponse(ResponseType responseType) {
		this.responseType = responseType;
	}
	
	public void setResponse(ResponseType responseType) {
		this.responseType = responseType;
	}
	
	public ResponseType responseType() {
		return responseType;
	}
	
	public void setUser(User user) {
		this.user = user;
	}
	
	public User getUser() {
		return user;
	}
	
	public void setBookings(ArrayList<Reservation> bookings) {
		this.bookings = bookings;
	}
	
	public ArrayList<Reservation> getBookings() {
		return bookings;
	}
	
	public void setTimeslots(ArrayList<String> timeslots) {
		this.timeslots = timeslots;
	}

	public ArrayList<String> getTimeslots() {
		return timeslots;
	}
        
	public void setOpenedSlots(List<Reservation> openedSlots) {
		this.openedSlots = openedSlots;
	}
    
	public List<Reservation> getOpenedSlots() {
		return openedSlots;
	}
}