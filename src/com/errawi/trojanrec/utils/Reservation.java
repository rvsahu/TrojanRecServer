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

}
