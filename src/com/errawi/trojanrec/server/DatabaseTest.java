package com.errawi.trojanrec.server;

import com.errawi.trojanrec.utils.User;

import java.sql.SQLException;
import java.util.ArrayList;

public class DatabaseTest {


    public static void main(String[] args) {
        DatabaseMain db = new DatabaseMain();

        /*
         *
         * Testing DatabaseMain authenticateUser() method
         * Returns boolean 'true' if net_id+password combination exists
         *
         */
        boolean authenticate;
        System.out.println("----TESTING USER AUTHENTICATION----");

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
         * Testing DatabaseMain retrieveUser() method
         * Returns populated User object
         *
         */
        User user;
        System.out.println("----TESTING USER POPULATING----");

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
        System.out.println(user.getName() + ", " + user.getStudentID() + ", " + user.getUserPhoto());
        user = db.retrieveUser("");
        System.out.println(user.getName() + ", " + user.getStudentID() + ", " + user.getUserPhoto());

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
         * Testing DatabaseMain getCenterTimeslots() method
         * Returns populated all center timeslots for specified gym
         *
         */
        String result;
        System.out.println("----TESTING GETTING CENTER TIMESLOTS----");

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


        /* NOT DONE TESTING
         *
         * Testing DatabaseMain isCapMax() method
         * Returns true if current reservation capacity and max capacity are the same
         *
         */
        boolean cap;
        System.out.println("----TESTING CAPACITY MAX CHECK ----");

        cap = db.isCapMax(1, "2022-03-25 06:00:00");
        System.out.println("Capacity max (false): " + cap);

        /*
         * Testing DatabaseMain clearWaitlist() method
         *  removes all reservations corresponding with a center+datetime
         */
        System.out.println("----TESTING REMOVING USERS FROM WAITLIST ----");
        db.clearWaitlist(1, "2022-03-25 06:00:00");

        // perform a test

        /*
         * Testing DatabaseMain addToWaitlist() method
         * Return value void, adds user to the waitlist
         *
         */
        System.out.println("----TESTING ADDING USERS TO WAITLIST ----");

        db.addToWaitlist(1, "2022-03-25 06:00:00", db.retrieveUser("shreya"));
        db.addToWaitlist(1, "2022-03-25 06:00:00", db.retrieveUser("karan"));

        /*
         * Testing DatabaseMain getWaitlist() method
         * Returns ArrayList of Users in the waitlist for given time/center
         *
         */
        ArrayList<User> users_waiting = new ArrayList<>();
        System.out.println("----TESTING PRINTING OUT WAITLIST ----");

        users_waiting = db.getWaitlist(1, "2022-03-25 06:00:00");
        for(int i=0; i<users_waiting.size(); i++){
            User w = users_waiting.get(i);
            System.out.print(w.getName() + " " + w.getStudentID() + " " + w.getUserPhoto());
        }







        /* close pool after database calls are complete
           do not make any database calls using DatabaseMain db after it is closed */
        db.datasource.close();
    }
}