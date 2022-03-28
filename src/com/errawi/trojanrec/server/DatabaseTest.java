package com.errawi.trojanrec.server;

import com.errawi.trojanrec.utils.User;

import java.util.ArrayList;

public class DatabaseTest {


    public static void main(String[] args) {
        DatabaseHandler db = new DatabaseHandler();

        /*
         *
         * Testing DatabaseHandler authenticateUser() method
         * Returns boolean 'true' if net_id+password combination exists
         *
         */
        boolean authenticate;
        System.out.println("\n----TESTING USER AUTHENTICATION----");

        /* RETURNS TRUE */
        // correct net_id and correct password
        authenticate = db.authenticateUser("erin", "2xAqf9tG47EXhSKA");
        System.out.println("Authenticate user (true): " + authenticate);
        authenticate = db.authenticateUser("rahul", "T5ykcGEwEGBK2ae3");
        System.out.println("Authenticate user (true): " + authenticate);
        authenticate = db.authenticateUser("will", "LunhHJwWW3yVRpWq");
        System.out.println("Authenticate user (true): " + authenticate);

        /* RETURNS FALSE */
        // empty inputs
        authenticate = db.authenticateUser("", "");
        System.out.println("Authenticate user (false): " + authenticate);
        authenticate = db.authenticateUser("helloworld", "");
        System.out.println("Authenticate user (false): " + authenticate);
        authenticate = db.authenticateUser("", "helloworld");
        System.out.println("Authenticate user (false): " + authenticate);
        // correct net_id and incorrect password
        authenticate = db.authenticateUser("erin", "2xAqf9tG47EXhSK");
        System.out.println("Authenticate user (false): " + authenticate);
        // incorrect net_id and correct password
        authenticate = db.authenticateUser("erinnnn", "2xAqf9tG47EXhSKA");
        System.out.println("Authenticate user (false): " + authenticate);

        System.out.println("\n\n");

        /*
         *
         * Testing DatabaseHandler retrieveUser() method
         * Returns populated User object
         *
         */
        User user;
        System.out.println("\n----TESTING USER POPULATING----");

        // should work
        user = db.retrieveUser("erin");
        System.out.println(user.getName() + ", " + user.getStudentID() + ", " + user.getUserPhoto());
        user = db.retrieveUser("rahul");
        System.out.println(user.getName() + ", " + user.getStudentID() + ", " + user.getUserPhoto());
        user = db.retrieveUser("will");
        System.out.println(user.getName() + ", " + user.getStudentID() + ", " + user.getUserPhoto());
        user = db.retrieveUser("moshe");
        System.out.println(user.getName() + ", " + user.getStudentID() + ", " + user.getUserPhoto());

        // should not work
        user = db.retrieveUser("userdoesntexisttest");
        System.out.println(user.getName() + ", " + user.getStudentID() + ", " + user.getUserPhoto() + "[empty test 1]");
        user = db.retrieveUser("");
        System.out.println(user.getName() + ", " + user.getStudentID() + ", " + user.getUserPhoto() + "[empty test 2]");

        // should work
        user = db.retrieveUser("shreya");
        System.out.println(user.getName() + ", " + user.getStudentID() + ", " + user.getUserPhoto());
        user = db.retrieveUser("karan");
        System.out.println(user.getName() + ", " + user.getStudentID() + ", " + user.getUserPhoto());
        user = db.retrieveUser("khanh");
        System.out.println(user.getName() + ", " + user.getStudentID() + ", " + user.getUserPhoto());
        user = db.retrieveUser("avonlea");
        System.out.println(user.getName() + ", " + user.getStudentID() + ", " + user.getUserPhoto());

        System.out.println("\n\n");

        /*
         *
         * Testing DatabaseHandler getCenterTimeslots() method
         * Returns populated all center timeslots for specified gym
         *
         */
        String result;
        System.out.println("\n----TESTING GETTING CENTER TIMESLOTS----");

        // Lyon
        result = db.getCenterTimeslots(1);
        System.out.println("");
        System.out.print("Lyon Center");
        System.out.println(result);
        result = "";

        // Cromwell
        result = db.getCenterTimeslots(2);
        System.out.println("");
        System.out.print("Cromwell");
        System.out.println(result);
        result = "";

        // Village
        result = db.getCenterTimeslots(3);
        System.out.println("");
        System.out.print("Village");
        System.out.println(result);
        result = "";





        /*
         * Testing DatabaseHandler addToWaitlist() method
         * Return value void, adds user to the waitlist
         *
         */
        System.out.println("\n----TESTING ADDING USERS TO WAITLIST ----");

        db.addToWaitlist(1, "2022-03-25 06:00:00", db.retrieveUser("shreya"));
        db.addToWaitlist(1, "2022-03-25 06:00:00", db.retrieveUser("karan"));
        db.addToWaitlist(1, "2022-03-25 06:00:00", db.retrieveUser("rahul"));

        /*
         * Testing DatabaseHandler getWaitlist() method
         * Returns ArrayList of Users in the waitlist for given time/center
         *
         */
        ArrayList<User> users_waiting = new ArrayList<>();
        System.out.println("\nPRINTING OUT WAITLIST");

        System.out.println("Printing users on waitlist for Lyon Center 2022-03-25 06:00:00");
        users_waiting = db.getWaitlist(1, "2022-03-25 06:00:00");
        for(int i=0; i<users_waiting.size(); i++){
            User w = users_waiting.get(i);
            System.out.println(w.getName() + ", " + w.getStudentID() + ", " + w.getUserPhoto());
        }
        
        /*
         * Testing DatabaseHandler clearWaitlist() method
         *  removes all reservations corresponding with a center+datetime
         */
        System.out.println("\n----TESTING REMOVING USERS FROM WAITLIST ----");
        db.clearWaitlist(1, "2022-03-25 06:00:00");

        // perform a test
        
        System.out.println("\nPRINTING OUT WAITLIST (should be empty)");
        
        
        

        /*
         * 
         * Testing DatabaseHandler makeBooking() method
         * clear bookings first (for testing purposes)
         *
         */
        db.clearBookingsTable();
        System.out.println("\n----TESTING ADDING USERS TO BOOKING TABLE ----");

        // Lyon Center - 4 people max for this time slot
        db.makeBooking(1, "2022-03-25 06:00:00", db.retrieveUser("erin"));
        db.makeBooking(1, "2022-03-25 06:00:00", db.retrieveUser("erin")); // should return an error 
        db.makeBooking(1, "2022-03-25 06:00:00", db.retrieveUser("rahul"));
        db.makeBooking(1, "2022-03-25 06:00:00", db.retrieveUser("will"));
        db.makeBooking(1, "2022-03-25 06:00:00", db.retrieveUser("avonlea"));       
        
        // Village - 2 people max for this time slot
        db.makeBooking(3, "2022-03-25 06:00:00", db.retrieveUser("erin"));
        db.makeBooking(3, "2022-03-25 06:00:00", db.retrieveUser("rahul"));
       
        db.makeBooking(1, "2022-03-26 06:00:00", db.retrieveUser("erin"));
                
        

        /* NOT DONE TESTING
        *
        * Testing DatabaseHandler isCapMax() method
        * Returns true if current reservation capacity and max capacity are the same
        *
        */
       boolean cap;
       System.out.println("\n----TESTING CAPACITY MAX CHECK ----");

       // 4/4
       cap = db.isCapMax(1, "2022-03-25 06:00:00");
       System.out.println("Is capacity max (true) 2022-03-25 06:00:00: " + cap);
       
       // 2/2
       cap = db.isCapMax(3, "2022-03-25 06:00:00");
       System.out.println("Is capacity max (true): " + cap);
       
       // only one person signed up for this time slot (cap: 1/4)
       cap = db.isCapMax(1, "2022-03-26 06:00:00");
       System.out.println("Is capacity max (false): " + cap);
       
       // no one is signed up for this time slot (cap: 0/2)
       cap = db.isCapMax(3, "2022-03-26 06:00:00"); 
       System.out.println("Is capacity max (false): " + cap);
       
       
       
       /*
        * 
        * Testing DatabaseHandler getFutureBookings() method
        *
        *
        */     
       db.removeBooking(1, "2022-03-25 06:00:00", db.retrieveUser("avonlea"));
       // check that capacity has gone down
       cap = db.isCapMax(1, "2022-03-25 06:00:00");
       System.out.println("Is capacity max (now false) for 2022-03-25 06:00:00: " + cap);
       


        /*
         * 
         * Testing DatabaseHandler getFutureBookings() method
         *
         *
         */
        User user_booking = new User();
        ArrayList<String> bookings = new ArrayList<>();

        //bookings = db.getFutureBookings(user_booking);
        
        
        

        /*
         * 
         * Testing DatabaseHandler getPastBookings() method
         *
         *
         */
        User user_bookingg = new User();
        ArrayList<String> past_bookings = new ArrayList<>();
        System.out.println("\n----TESTING PRINTING OUT PAST BOOKINGS ----");

        user_bookingg = db.retrieveUser("erin");
        past_bookings = db.getPastBookings(user_bookingg);
        System.out.println(user_bookingg.getName());
        System.out.println(past_bookings);






        System.out.println("\n**** TESTING DONE ****");
        /* close pool after database calls are complete
           do not make any database calls using DatabaseHandler db after it is closed */
        db.datasource.close();
    }
}
