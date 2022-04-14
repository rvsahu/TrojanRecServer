package com.errawi.trojanrec.testing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;

import static org.junit.Assert.assertEquals;

import com.errawi.trojanrec.server.DatabaseHandler;
import com.errawi.trojanrec.utils.ClientRequest;
import com.errawi.trojanrec.utils.Reservation;
import com.errawi.trojanrec.utils.ResponseType;
import com.errawi.trojanrec.utils.ServerFunction;
import com.errawi.trojanrec.utils.ServerResponse;
import com.errawi.trojanrec.utils.User;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * A class to test the outputs of ClientHandler and each of the requests it may get
 * 
 * @author RSahu
 */
public class TestClientHandler {
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
	 * Constructor for tester
	 */
	public TestClientHandler() throws SocketException, UnknownHostException, ClassNotFoundException, IOException {
		clearDBAndConnect();
	}
	
	/**
	 * Creates a database handler that we can use to clear tables between each test
	 */
	@BeforeAll public static void setUpDatabase() {
		testDBH = new DatabaseHandler();
	}
	
	/**
	 * Sets up a new connection to the 'server' 
	 */
	@BeforeEach public void clearDBAndConnect() throws SocketException, UnknownHostException, IOException, ClassNotFoundException {
		//clears DB of tables
		clearDB();
		//establishes a connection and creates input and output streams
		connect();
	}
	
	/**
	 * Specifically clears DB bookings table and wait list
	 */
	private void clearDB() {
		testDBH.clearBookingsWaitlistsTables();
	}
	
	/**
	 * Specifically (re)connects to server and creates IO streams
	 */
	private void connect() throws SocketException, UnknownHostException, IOException, ClassNotFoundException {
		testSocket = new Socket("localhost", 1337);
		testOOS = new ObjectOutputStream(testSocket.getOutputStream());
		testOIS = new ObjectInputStream(testSocket.getInputStream());
		ClientRequest connectUser = new ClientRequest(ServerFunction.CONNECT);
		ServerResponse response = sendRequest(connectUser);
		assertEquals("Response from server upon connection", response.responseType(), ResponseType.CONNECTED);
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
	 * Clears database of bookings and wait list entries one last time.
	 */
	@AfterAll public static void clearDatabase() {
		testDBH.clearBookingsWaitlistsTables(); //clear database
	}
	
	/**
	 * Closes the socket connection to the server appropriately, called after each test
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
		assertEquals("Response from server upon closing connection", sendRequest(closeRequest).responseType(), ResponseType.CLOSED);
		testSocket.close();
		goodClosed = true;
	}
	
	/**
	 * Test a specific login 
	 */
	@ParameterizedTest
	@MethodSource("testLoginUsers")
	@Timeout(1)
	public void testLogins(User user, String userPassword, ResponseType expected) throws SocketException, UnknownHostException, ClassNotFoundException, IOException {
		//build login request
		ClientRequest loginRequest = new ClientRequest(ServerFunction.LOGIN);
		loginRequest.setUser(user);
		loginRequest.setUserPassword(userPassword);
		ServerResponse response = sendRequest(loginRequest); //send login request and get response
		String userNetID;
		if (user != null) {
			userNetID = user.getNetID();
		} else {
			userNetID = "null user object";
		}
		assertEquals("Testing login for " + userNetID, expected, response.responseType()); //check response is AUTHENTICATED
	}
	
	private static Stream<Arguments> testLoginUsers() {
		User erin = new User("erinbris");
		User rahul = new User("rahuls");
		User will = new User("willw");
		User moshe = new User("mosheheletz");
		User karan = new User("karanm");
		User shreya = new User("shreyac");
		User khanh = new User("khanhpham");
		User avonlea = new User("avonleav");
		User fake = new User("fakeguy");
		User nullid = new User(null);
		
		return Stream.of(
				//good users
				Arguments.of(erin, "1234", ResponseType.AUTHENTICATED),
				Arguments.of(rahul, "4321", ResponseType.AUTHENTICATED),
				Arguments.of(will, "3456", ResponseType.AUTHENTICATED),
				Arguments.of(moshe, "7890", ResponseType.AUTHENTICATED),
				Arguments.of(karan, "9876", ResponseType.AUTHENTICATED),
				Arguments.of(shreya, "7654", ResponseType.AUTHENTICATED),
				Arguments.of(khanh, "2345", ResponseType.AUTHENTICATED),
				Arguments.of(avonlea, "6543", ResponseType.AUTHENTICATED),
				//existing user with bad password
				Arguments.of(avonlea, "1111", ResponseType.UNAUTHENTICATED),
				//existing user with password of another user
				Arguments.of(moshe, "4321", ResponseType.UNAUTHENTICATED),
				//non-extant user with real user's password
				Arguments.of(fake, "1234", ResponseType.UNAUTHENTICATED),
				//null user id
				Arguments.of(nullid, "1234", ResponseType.UNAUTHENTICATED),
				//null user
				Arguments.of(null, "1234", ResponseType.UNAUTHENTICATED)
				);
	}
	
	/**
	 * Tests retrieval of user information for separate users through separate connections.
	 * 
	 */
	@ParameterizedTest
	@MethodSource("testUserInfoUsers")
	public void testUserInfo(User testUser, String userPassword, User userExpected) throws ClassNotFoundException, IOException {
		//build login request with testUser
		ClientRequest loginRequest = new ClientRequest(ServerFunction.LOGIN);
		loginRequest.setUser(testUser);
		loginRequest.setUserPassword(userPassword);
		//send login request
		ServerResponse loginResponse = sendRequest(loginRequest); //send login request and get response
		//check response (really this should be good because the two users we're using we just tested)
		//but we'll do the assert anyway
		assertEquals("testUserInfo: login of " + testUser.getNetID(), ResponseType.AUTHENTICATED, loginResponse.responseType()); //check response is AUTHENTICATED
		//build profile info request with testUser
		ClientRequest infoRequest = new ClientRequest(ServerFunction.GET_PROFILE_INFO);
		infoRequest.setUser(testUser);
		//send info request, save server response
		ServerResponse infoResponse = sendRequest(infoRequest);
		//check response type is what's expected (SUCCESSFUL)
		assertEquals("testUserInfo: info getting operation of "+ testUser.getNetID(), ResponseType.SUCCESS, infoResponse.responseType()); //check response is SUCCESS
		//get resulting user from infoResponse
		User userResult = infoResponse.getUser();
		//check userResult details against userExpected
		assertEquals("testUserInfo: user name: ", userExpected.getName(), userResult.getName());
		assertEquals("testUserInfo: net ID: ", userExpected.getNetID(), userResult.getNetID());
		assertEquals("testUserInfo: student ID: ", userExpected.getStudentID(), userResult.getStudentID());
		assertEquals("testUserInfo: user photo: ", userExpected.getUserPhoto(), userResult.getUserPhoto());
	}
	
	
	private static Stream<Arguments> testUserInfoUsers() {
		User will = new User("willw");
		User moshe = new User("mosheheletz");
		
		User willExpected = new User("willw");
		willExpected.setName("Will Wei");
		willExpected.setStudentID(3000000003L);
		willExpected.setUserPhoto("will");
		User mosheExpected = new User("mosheheletz");
		mosheExpected.setName("Moshe Heletz");
		mosheExpected.setStudentID(4000000004L);
		mosheExpected.setUserPhoto("moshe");
		
		return Stream.of(
				Arguments.of(will, "3456", willExpected),
				Arguments.of(moshe, "7890", mosheExpected)
				);
	}
	
	//TODO: test make booking
	
	@ParameterizedTest
	@MethodSource("testMakeBookingArgs")
	public void testMakeBooking(User testUser, String userPassword, List<Reservation> userBookings, List<ResponseType> opResponses) throws ClassNotFoundException, IOException {
		//build login request with testUser
		ClientRequest loginRequest = new ClientRequest(ServerFunction.LOGIN);
		loginRequest.setUser(testUser);
		loginRequest.setUserPassword(userPassword);
		//send login request
		ServerResponse loginResponse = sendRequest(loginRequest); //send login request and get response
		//check login works (this shouldn't be an issue because we tested these users previously)
		assertEquals("testMakeBooking: login " + testUser.getNetID(), loginResponse.responseType(), ResponseType.AUTHENTICATED); //check response is AUTHENTICATED
		//make bookings and check we're getting the successes/fails expected
		for (int i = 0; i < userBookings.size(); i += 1) {
			ClientRequest bookingRequest = new ClientRequest(ServerFunction.MAKE_BOOKING);
			bookingRequest.setUser(testUser);
			Reservation userBooking = userBookings.get(i);
			bookingRequest.setRecCentre(userBooking.getRecCentre());
			bookingRequest.setTimeslot(userBooking.getTimedate());
			//send make booking request, save server response
			ServerResponse bookingResponse = sendRequest(bookingRequest);
			//check response type is what's expected (SUCCESSFUL)
			assertEquals("testMakeBooking: make booking " + testUser.getNetID(), opResponses.get(i), bookingResponse.responseType()); //check response is SUCCESS
		}
		//check database is populated as it should be
		List<Reservation> serverBookings = testDBH.getFutureBookings(testUser); //get booking list directly from DB
		assertEquals("testMakeBooking: lists are equal", userBookings, serverBookings);
	}
	
	private static Stream<Arguments> testMakeBookingArgs() {
		User shreya = new User("shreyac");
		User khanh = new User("khanhpham");
		User avonlea = new User("avonleav");
		
		//test 1: three separate bookings at three separate time slots
		
		//create reservation list
		Reservation sb_1 = new Reservation(1, "2022-05-28 10:00:00"); //lyon centre
		Reservation sb_2 = new Reservation(2, "2022-05-27 20:00:00"); //cromwell track
		Reservation sb_3 = new Reservation(3, "2022-05-28 13:30:00"); //usc village
		List<Reservation> shreyaBookings = new ArrayList<>();
		shreyaBookings.add(sb_1);
		shreyaBookings.add(sb_2);
		shreyaBookings.add(sb_3);
		//create expected operation response list
		List<ResponseType> shreyaResponses = new ArrayList<>();
		shreyaResponses.add(ResponseType.SUCCESS);
		shreyaResponses.add(ResponseType.SUCCESS);
		shreyaResponses.add(ResponseType.SUCCESS);
		
		
		//test 2: two bookings at same centre different timeslots
		//create reservation list
		Reservation kb_1 = new Reservation(2, "2022-05-27 18:00:00"); //cromwell track
		Reservation kb_2 = new Reservation(2, "2022-05-27 20:00:00"); //cromwell track
		List<Reservation> khanhBookings = new ArrayList<>();
		khanhBookings.add(kb_1);
		khanhBookings.add(kb_2);
		//create expected operation response list
		List<ResponseType> khanhResponses = new ArrayList<>();
		khanhResponses.add(ResponseType.SUCCESS);
		khanhResponses.add(ResponseType.SUCCESS);		
		
		//test 3: two bookings at same centre same timeslots
		//should allow only ONE entry in bookings table
		//create reservation list
		Reservation rb_1 = new Reservation(3, "2022-05-27 13:30:00"); //usc village
		Reservation rb_2 = new Reservation(3, "2022-05-27 13:30:00"); //usc village
		List<Reservation> avonleaBookings = new ArrayList<>();		
		avonleaBookings.add(rb_1);
		avonleaBookings.add(rb_2);
		//create expected operation response list
		List<ResponseType> avonleaResponses = new ArrayList<>();
		avonleaResponses.add(ResponseType.SUCCESS);
		avonleaResponses.add(ResponseType.FAIL);
		
		return Stream.of( 
				Arguments.of(shreya, "7654", shreyaBookings, shreyaResponses),
				Arguments.of(khanh, "2345", khanhBookings, khanhResponses),
				Arguments.of(avonlea, "6543", avonleaBookings, avonleaResponses)
				);
	}
	//TODO: test retrieve bookings
	
	//TODO: 
}
