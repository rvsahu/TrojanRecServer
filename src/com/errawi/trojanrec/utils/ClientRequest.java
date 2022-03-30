package com.errawi.trojanrec.utils;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * ClientRequest is the framework for client-to-server communication. It contains
 * a specific function that the client wishes the server to perform, and the
 * relevant information that the server needs to be able to perform it.
 */
public class ClientRequest implements Serializable {
    private static final long serialVersionUID = 20220328L;
    /**
     * The function the server needs to perform, as enumerated by ServerFunctions.
     */
    private ServerFunction function;

    /**
     * The user invoking the server function (i.e user trying to retrieve their current bookings)
     */
    private User user;

    /**
     * String containing the user password, for login requests.
     *
     * TODO: determine how to store this in an encrypted fashion
     */
    private String userPassword;

    /**
     * Integer id representing the recreation centre that is being sent to the server.
     * This would be used by the server in the cases of making a booking, cancelling one,
     * and likewise for the wait list.
     */
    private int recCentre;

    /**
     * ArrayList of reservation times/dates for retrieving past and future bookings.
     */
    private ArrayList<Reservation> bookings;


    /**
     * Reservation object with timedate and center for the reservation.
     * useful for creating a booking, retrieving waitlist for specific reservation
     */
    private Reservation reservation;

    /**
     * String representing a time slot in a recreation centre (and the specific one is
     * defined by the recCentre field). This would be used by the server in the cases of
     * making a booking, cancelling one, and likewise for the wait list.
     */
    private String timeslot;

    public ClientRequest(ServerFunction function) {
        this.function = function;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public void setRecCentre(int recCentre) {
        this.recCentre = recCentre;
    }

    public void setTimeslot(String timeslot) {
        this.timeslot = timeslot;
    }

    public void setBookings(ArrayList<Reservation> bookings) {
        this.bookings = bookings;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public ServerFunction getFunction() {
        return function;
    }

    public User getUser() {
        return user;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public int getRecCentre() {
        return recCentre;
    }

    public String getTimeslot() {
        return timeslot;
    }

    public ArrayList<Reservation> getBookings() {
        return bookings;
    }

    public Reservation getReservation() {
        return reservation;
    }
}
