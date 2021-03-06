package com.errawi.trojanrec.utils;

import java.io.Serializable;


public class Reservation implements Serializable {
	
	private static final long serialVersionUID = 38932022L;
	
    /**
     * time and date of the booking, stored as YYYY-MM-DD hh:mm:ss
     */
	private String timedate;
	
	
    /**
     * Integer id representing the recreation centre.
     */
    private int recCentre;
    
    public Reservation() {
    	timedate = "";
    	recCentre = -1;
    }
    
    public Reservation(int recCentre, String timedate) {
    	this.timedate = timedate;
    	this.recCentre = recCentre;
    }
    
    public void setRecCentre(int recCentre) {
        this.recCentre = recCentre;
    }
    
    public int getRecCentre() {
        return recCentre;
    }
    
    public void setTimedate(String timedate) {
    	this.timedate = timedate;
    }
    
    public String getTimedate() {
    	return timedate;
    }
    
    @Override public boolean equals(Object other) {
    	if (!(other instanceof Reservation)) {
    		return false;
    	}
    	Reservation rOther = (Reservation)(other);
    	if (rOther.getRecCentre() == recCentre && rOther.getTimedate().equals(timedate)) {
    		return true;
    	}
    	return false;
    }
}