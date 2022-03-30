package com.errawi.trojanrec.utils;

import java.io.Serializable;
import java.util.ArrayList;

public class ServerResponse implements Serializable {
        private static final long serialVersionUID = 72002803L;

        private ResponseType responseType;

        private User user;

        private ArrayList<Reservation> bookings;

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
    }