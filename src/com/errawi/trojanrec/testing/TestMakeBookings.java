package com.errawi.trojanrec.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.errawi.trojanrec.server.DatabaseHandler;
import com.errawi.trojanrec.utils.ClientRequest;
import com.errawi.trojanrec.utils.Reservation;
import com.errawi.trojanrec.utils.ResponseType;
import com.errawi.trojanrec.utils.ServerFunction;
import com.errawi.trojanrec.utils.ServerResponse;
import com.errawi.trojanrec.utils.User;

public class TestMakeBookings {
	/**
     * Stream to send objects (ClientRequest objects specifically) to the server over.
     */
    private ObjectOutputStream testOOS;

    /**
     * Stream to receive objects from the Server over. Most likely to be Strings.
     */
    private ObjectInputStream testOIS;
	
    /**
     * Socket which streams are created from and connects to the server
     */
	private Socket testSocket;
	
	/**
	 * Need a copy of this to be able to clear bookings table and wait list once done with a test group
	 */
	private static DatabaseHandler testDBH;
	
	
	/**
	 * Whether the socket is closed properly or not.
	 */
	private boolean goodClosed;
	
	/**
	 * Creates a database handler that we can use to clear tables between each test
	 */
	@BeforeAll public static void setUpDatabase() {
		testDBH = new DatabaseHandler();
		clearDB();
	}
	
	/**
	 * Specifically clears DB bookings table and wait list
	 */
	@AfterAll private static void clearDB() {
		testDBH.clearBookingsWaitlistsTables();
	}
	
	/**
	 *  Connects (or reconnects) to server and creates IO streams
	 */
	@BeforeEach public void connect() throws SocketException, UnknownHostException, IOException, ClassNotFoundException {
		testSocket = new Socket("18.144.84.223", 1337);
		testOOS = new ObjectOutputStream(testSocket.getOutputStream());
		testOIS = new ObjectInputStream(testSocket.getInputStream());
		ClientRequest connectUser = new ClientRequest(ServerFunction.CONNECT);
		ServerResponse response = sendRequest(connectUser);
		assertEquals(response.responseType(), ResponseType.CONNECTED, "bad response from server upon connection");
		goodClosed = false;
	}
	
	/**
	 * Sends a request to the server.
	 * @param request  The request to be sent
	 * @return     The response from the server.
	 */
	private ServerResponse sendRequest(ClientRequest request) throws IOException, ClassNotFoundException {
		testOOS.writeObject(request);
		return (ServerResponse)(testOIS.readObject());
	}
	
	/**
	 * Closes the socket connection to the server appropriately, MANUALLY called after each test
	 */
	@AfterEach public void closeConnection() throws ClassNotFoundException, SocketException, IOException {
		if (testSocket.isClosed()) {
			if (goodClosed) {
				return; //don't do anything if connection was already closed and *should be*
			} else {
				throw new SocketException("Socket closed when it shouldn't be.");
			}
		}
		ClientRequest closeRequest = new ClientRequest(ServerFunction.CLOSE);
		assertEquals(sendRequest(closeRequest).responseType(), ResponseType.CLOSED, "bad response from server upon closing connection");
		testSocket.close();
		goodClosed = true;
	}
	
	@ParameterizedTest
	@MethodSource("testMakeBookingArgs")
	public void testMakeBooking(User testUser, String userPassword, Reservation userBooking, ResponseType opExpected) throws ClassNotFoundException, IOException {
		//build login request with testUser
		ClientRequest loginRequest = new ClientRequest(ServerFunction.LOGIN);
		loginRequest.setUser(testUser);
		loginRequest.setUserPassword(userPassword);
		//send login request
		ServerResponse loginResponse = sendRequest(loginRequest); //send login request and get response
		//check login works (this shouldn't be an issue because we tested these users previously)
		assertEquals(loginResponse.responseType(), ResponseType.AUTHENTICATED, "testMakeBooking: bad login for " + testUser.getNetID()); //check response is AUTHENTICATED
		//make booking and check we're getting the expected response
		ClientRequest bookingRequest = new ClientRequest(ServerFunction.MAKE_BOOKING);
		bookingRequest.setUser(testUser);
		bookingRequest.setRecCentre(userBooking.getRecCentre());
		bookingRequest.setTimeslot(userBooking.getTimedate());
		//send make booking request, save server response
		ServerResponse bookingResponse = sendRequest(bookingRequest);
		//check response type is what's expected
		assertEquals(opExpected, bookingResponse.responseType(), "testMakeBooking: unexpected make booking response" + testUser.getNetID());
		//check database is populated as it should be
		List<Reservation> serverBookings = testDBH.getFutureBookings(testUser); //get booking list directly from DB
		assertTrue(serverBookings.contains(userBooking), "testMakeBooking: booking (not) present in table");
	}
	
	private static Stream<Arguments> testMakeBookingArgs() {
		User shreya = new User("shreyac");
		User khanh = new User("khanhpham");
		User avonlea = new User("avonleav");
		
		//test 1: three separate bookings at three separate time slots
		//create shreya's reservations
		Reservation sb_1 = new Reservation(1, "2022-05-28 10:00:00"); //lyon centre
		Reservation sb_2 = new Reservation(2, "2022-05-27 20:00:00"); //cromwell track
		Reservation sb_3 = new Reservation(3, "2022-05-28 13:30:00"); //usc village
		
		//test 2: two bookings at same centre different timeslots
		//create khanh's reservations
		Reservation kb_1 = new Reservation(2, "2022-05-27 18:00:00"); //cromwell track
		Reservation kb_2 = new Reservation(2, "2022-05-27 20:00:00"); //cromwell track
		
		//test 3: two bookings at same centre same timeslots
		Reservation rb_1 = new Reservation(3, "2022-05-27 13:30:00"); //usc village
		Reservation rb_2 = new Reservation(3, "2022-05-27 13:30:00"); //usc village
		
		
		return Stream.of( 
				Arguments.of(shreya, "7654", sb_1, ResponseType.SUCCESS),
				Arguments.of(shreya, "7654", sb_2, ResponseType.SUCCESS),
				Arguments.of(shreya, "7654", sb_3, ResponseType.SUCCESS),
				Arguments.of(khanh, "2345", kb_1, ResponseType.SUCCESS),
				Arguments.of(khanh, "2345", kb_2, ResponseType.SUCCESS),
				Arguments.of(avonlea, "6543", rb_1, ResponseType.SUCCESS),
				Arguments.of(avonlea, "6543", rb_2,	ResponseType.FAIL)
				);
	}
}
