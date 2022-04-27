package com.errawi.trojanrec.testing;

import com.errawi.trojanrec.utils.User;
import com.errawi.trojanrec.server.DatabaseHandler;
import com.errawi.trojanrec.utils.NotificationBank;
import com.errawi.trojanrec.utils.Reservation;
import java.util.ArrayList;

import org.junit.Test;
import org.junit.AfterClass;
import org.junit.Before;
import static org.junit.Assert.*;

public class TestDatabaseHandler {
	
	public static DatabaseHandler db = new DatabaseHandler(new NotificationBank()); 
	public static boolean isInitialized = false;
	
	@Before
	public void populateDatabase() {
		if(!isInitialized) {
			db.clearBookingsWaitlistsTables();
			Reservation res_test = new Reservation();
			System.out.println("\n-POPULATING DATABASE WITH BOOKINGS-");
			
			// PAST BOOKINGS
			// Cromwell - 3 people max for this time slot
			res_test.setRecCentre(2);
			res_test.setTimedate("2022-04-03 07:00:00");
			db.makeBooking(res_test, db.retrieveUser("erinbris"));
			
			// Village - 2 people max for this time slot
			res_test.setRecCentre(3);
			res_test.setTimedate("2022-04-07 08:30:00");
			db.makeBooking(res_test, db.retrieveUser("rahuls"));
			db.makeBooking(res_test, db.retrieveUser("willw"));
			

			// FUTURE BOOKINGS
			// Lyon Center - 4 people max for this time slot
			res_test.setRecCentre(1);
			res_test.setTimedate("2022-05-27 06:00:00");
			db.makeBooking(res_test, db.retrieveUser("erinbris"));		
			db.makeBooking(res_test, db.retrieveUser("willw"));
			db.makeBooking(res_test, db.retrieveUser("rahuls"));

			// Village - 2 people max for this time slot
			res_test.setRecCentre(3);
			res_test.setTimedate("2022-05-28 13:30:00");
			db.makeBooking(res_test, db.retrieveUser("erinbris"));
			db.makeBooking(res_test, db.retrieveUser("rahuls"));	
			
			db.addToWaitlist(res_test, db.retrieveUser("shreyac"));
			db.addToWaitlist(res_test, db.retrieveUser("avonleav"));	
			
			isInitialized = true;
		}		
	}
	
	@Test
	public void testAuthentication() {
		boolean authenticate;
		System.out.println("\n----TESTING USER AUTHENTICATION----");
		
		/* RETURNS TRUE */
		// correct net_id and correct password
		authenticate = db.authenticateUser("erinbris", "1234");
		assertEquals(authenticate, true);
		authenticate = db.authenticateUser("rahuls", "4321");
		assertEquals(authenticate, true);
		authenticate = db.authenticateUser("willw", "3456");
		assertEquals(authenticate, true);

		/* RETURNS FALSE */
		// empty inputs
		authenticate = db.authenticateUser("", "");
		assertEquals(authenticate, false);
		authenticate = db.authenticateUser("erinbris", "");
		assertEquals(authenticate, false);
		authenticate = db.authenticateUser("", "1234");
		assertEquals(authenticate, false);
		// correct net_id and incorrect password
		authenticate = db.authenticateUser("erinbris", "12345");
		assertEquals(authenticate, false);
		// incorrect net_id and correct password
		authenticate = db.authenticateUser("erinbrissss", "1234");
		assertEquals(authenticate, false);
		// null inputs
		authenticate = db.authenticateUser(null, null);
		assertEquals(authenticate, false);
	}
	
	@Test
	public void testRetrieveUser() {		
		User user;
		System.out.println("\n----TESTING USER RETRIEVAL----");

		// should work
		user = db.retrieveUser("erinbris");
		assertEquals(user.getName(), "Erin Bristow");
		assertEquals(user.getStudentID(), 1000000001);
		assertEquals(user.getUserPhoto(), "erin");
		assertEquals(user.getNetID(), "erinbris");
		user = db.retrieveUser("rahuls");
		assertEquals(user.getName(), "Rahul Sahu");
		assertEquals(user.getStudentID(), 2000000002);
		assertEquals(user.getUserPhoto(), "rahul");
		assertEquals(user.getNetID(), "rahuls");

		// should not work
		user = db.retrieveUser("userdoesntexisttest");
		assertEquals(user.getName(), "");
		assertEquals(user.getStudentID(), -1);
		assertEquals(user.getUserPhoto(), "");
		assertEquals(user.getNetID(), "");
		user = db.retrieveUser("");
		assertEquals(user.getName(), "");
		assertEquals(user.getStudentID(), -1);
		assertEquals(user.getUserPhoto(), "");
		assertEquals(user.getNetID(), "");
		user = db.retrieveUser(null);	
		assertEquals(user.getName(), "");
		assertEquals(user.getStudentID(), -1);
		assertEquals(user.getUserPhoto(), "");
		assertEquals(user.getNetID(), "");
	}
	
	@Test
	public void testGetCenterTimeslots() {
		ArrayList<String> result = new ArrayList<>();
		System.out.println("\n----TESTING GETTING CENTER TIMESLOTS----");
		
		// just checking to make sure resulting ArrayList is correct length
		// Lyon
		result = db.getCenterTimeslots(1);
		assertEquals(result.size(), 16);

		// Cromwell
		result = db.getCenterTimeslots(2);
		assertEquals(result.size(), 20);

		// Village
		result = db.getCenterTimeslots(3);
		assertEquals(result.size(), 12);	
		
		// invalid center_id test
		result = db.getCenterTimeslots(0);
		assertEquals(result, null);	
		result = db.getCenterTimeslots(4);
		assertEquals(result, null);	
		
		// get future timeslots
		result = db.getFutureCenterTimeslots(1);
		assertEquals(result.size(), 8);
		
		result = db.getFutureCenterTimeslots(2);
		assertEquals(result.size(), 10);
		
		result = db.getFutureCenterTimeslots(3);
		assertEquals(result.size(), 6);
	}
	
	@Test 
	public void testGetFutureBookings() {
		// populated in populateDatabase()
		
		User user;
		ArrayList<Reservation> future_bookings = new ArrayList<>();
		Reservation reservation = new Reservation();
		Reservation db_res;
		System.out.println("\n----TESTING GETTING FUTURE BOOKINGS----");

		// for user erinbris
		user = db.retrieveUser("erinbris");
		future_bookings = db.getFutureBookings(user);
		assertEquals(future_bookings.size(), 2); // correct number of reservations
		reservation.setTimedate("2022-05-27 06:00:00");
		reservation.setRecCentre(1);
		db_res = future_bookings.get(0);
		assertEquals(db_res.getRecCentre(), reservation.getRecCentre());
		assertEquals(db_res.getTimedate(), reservation.getTimedate());
		reservation.setTimedate("2022-05-28 13:30:00");
		reservation.setRecCentre(3);
		db_res = future_bookings.get(1);
		assertEquals(db_res.getRecCentre(), reservation.getRecCentre());
		assertEquals(db_res.getTimedate(), reservation.getTimedate());
		
		// for user rahuls
		user = db.retrieveUser("rahuls");
		future_bookings = db.getFutureBookings(user);
		assertEquals(future_bookings.size(), 2); // correct number of reservations
		reservation.setTimedate("2022-05-27 06:00:00");
		reservation.setRecCentre(1);
		db_res = future_bookings.get(0);
		assertEquals(db_res.getRecCentre(), reservation.getRecCentre());
		assertEquals(db_res.getTimedate(), reservation.getTimedate());
		reservation.setTimedate("2022-05-28 13:30:00");
		reservation.setRecCentre(3);
		db_res = future_bookings.get(1);
		assertEquals(db_res.getRecCentre(), reservation.getRecCentre());
		assertEquals(db_res.getTimedate(), reservation.getTimedate());
		
		// for user willw
		user = db.retrieveUser("willw");
		future_bookings = db.getFutureBookings(user);
		assertEquals(future_bookings.size(), 1); // correct number of reservations
		reservation.setTimedate("2022-05-27 06:00:00");
		reservation.setRecCentre(1);
		db_res = future_bookings.get(0);
		assertEquals(db_res.getRecCentre(), reservation.getRecCentre());
		assertEquals(db_res.getTimedate(), reservation.getTimedate());
	}
	
	@Test
	public void testGetPastBookings() {
		// populated in populateDatabase()
		
		User user;
		ArrayList<Reservation> past_bookings = new ArrayList<>();
		Reservation reservation = new Reservation();
		Reservation db_res;
		System.out.println("\n----TESTING GETTING PAST BOOKINGS----");
		
		user = db.retrieveUser("erinbris");
		past_bookings = db.getPastBookings(user);
		assertEquals(past_bookings.size(), 1); // correct number of reservations
		reservation.setTimedate("2022-04-03 07:00:00");
		reservation.setRecCentre(2);
		db_res = past_bookings.get(0);
		assertEquals(db_res.getRecCentre(), reservation.getRecCentre());
		assertEquals(db_res.getTimedate(), reservation.getTimedate());
		
		user = db.retrieveUser("rahuls");
		past_bookings = db.getPastBookings(user);
		assertEquals(past_bookings.size(), 1); // correct number of reservations
		reservation.setTimedate("2022-04-07 08:30:00");
		reservation.setRecCentre(3);
		db_res = past_bookings.get(0);
		assertEquals(db_res.getRecCentre(), reservation.getRecCentre());
		assertEquals(db_res.getTimedate(), reservation.getTimedate());
		
		user = db.retrieveUser("willw");
		past_bookings = db.getPastBookings(user);
		assertEquals(past_bookings.size(), 1); // correct number of reservations
		reservation.setTimedate("2022-04-07 08:30:00");
		reservation.setRecCentre(3);
		db_res = past_bookings.get(0);
		assertEquals(db_res.getRecCentre(), reservation.getRecCentre());
		assertEquals(db_res.getTimedate(), reservation.getTimedate());	
	}
	
	@Test
	public void testGetWaitlist() {
		//found issue using this test. getWaitlistForUser had an error with a ResultSet.
		User user;
		ArrayList<User> users_on_waitlist = new ArrayList<>();
		ArrayList<Reservation> waitlist = new ArrayList<>();
		Reservation reservation = new Reservation();
		System.out.println("\n----TESTING GETTING USERS FROM WAITLIST----");
		
		// for individual users
		user = db.retrieveUser("shreyac");
		waitlist = db.getWaitlistForUser(user);
		reservation = waitlist.get(0);
		
		assertEquals(reservation.getRecCentre(), 3);
		assertEquals(reservation.getTimedate(), "2022-05-28 13:30:00");		
		assertEquals(waitlist.size(), 1); 
		
		user = db.retrieveUser("avonleav");
		waitlist = db.getWaitlistForUser(user);
		reservation = waitlist.get(0);
		
		assertEquals(reservation.getRecCentre(), 3);
		assertEquals(reservation.getTimedate(), "2022-05-28 13:30:00");
		assertEquals(waitlist.size(), 1); 
		
		// get whole waitlist for a reservation
		reservation.setRecCentre(3);
		reservation.setTimedate("2022-05-28 13:30:00");
		users_on_waitlist = db.getWaitlist(reservation);
		assertEquals(users_on_waitlist.size(), 2); // shreya and avonlea are both on it		
		
		reservation.setRecCentre(1);
		reservation.setTimedate("2022-10-28 13:30:00"); // reservation does not exist
		users_on_waitlist = db.getWaitlist(reservation);
		assertEquals(users_on_waitlist, null);
		
		reservation.setRecCentre(0);
		reservation.setTimedate("2022-10-28 13:30:00"); // reservation does not exist
		users_on_waitlist = db.getWaitlist(reservation);
		assertEquals(users_on_waitlist, null);
		
		reservation.setRecCentre(0);
		reservation.setTimedate(null); // reservation does not exist
		users_on_waitlist = db.getWaitlist(reservation);
		assertEquals(users_on_waitlist, null);
	
	}
	
	@Test
	public void testIsCapMax() {
		boolean cap;
		Reservation r = new Reservation();
		System.out.println("\n----TESTING ISCAPMAX----");
		
		// Village is full for this timeslot
		r.setRecCentre(3);
		r.setTimedate("2022-05-28 13:30:00");
		cap = db.isCapMax(r);
		assertEquals(cap, true);
		
		r.setRecCentre(1);
		r.setTimedate("2022-05-27 06:00:00");
		cap = db.isCapMax(r);
		assertEquals(cap, false);
		
		r.setRecCentre(3);
		r.setTimedate("2022-04-07 08:30:00");
		cap = db.isCapMax(r);
		assertEquals(cap, true);
		
		r.setRecCentre(2);
		r.setTimedate("2022-04-03 07:00:00");
		cap = db.isCapMax(r);
		assertEquals(cap, false);
		
		r.setRecCentre(2);
		r.setTimedate("2022-04-03 07:00:00");
		cap = db.isCapMax(r);
		assertEquals(cap, false);
		
		// reservation doesn't exist
		r.setRecCentre(2);
		r.setTimedate("2022-10-03 07:00:00");
		cap = db.isCapMax(r);
		assertEquals(cap, false);
		
	}
	
	@Test
	public void testRemoveMakeBooking() {
		boolean cap;
		Reservation r = new Reservation();
		System.out.println("\n----TESTING MAKING AND REMOVING BOOKINGS----");
		
		// Village is full for this timeslot
		r.setRecCentre(3);
		r.setTimedate("2022-05-28 13:30:00");
		cap = db.isCapMax(r);
		assertEquals(cap, true);
		
		db.removeBooking(r, db.retrieveUser("erinbris"));
		cap = db.isCapMax(r);
		// no longer at cap max
		assertEquals(cap, false);
		
		// re-add booking (for testing purposes)
		db.makeBooking(r, db.retrieveUser("erinbris"));
		cap = db.isCapMax(r);
		// is cap max again
		assertEquals(cap, true);				
	}
	
	@AfterClass
	public static void cleanUp() {
		System.out.println("\n-CLEANING UP DATABASE BEFORE EXITING-\n");
		db.clearBookingsWaitlistsTables();
	}	
}
