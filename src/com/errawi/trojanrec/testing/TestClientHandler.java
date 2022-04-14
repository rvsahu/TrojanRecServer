package com.errawi.trojanrec.testing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.errawi.trojanrec.server.DatabaseHandler;
import com.errawi.trojanrec.utils.ClientRequest;
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
	 * Creates a database handler that we can use to clear tables between each test
	 */
	@BeforeAll public static void setUpDatabase() {
		testDBH = new DatabaseHandler();
	}
	
	/**
	 * Sets up a new connection to the 'server' 
	 */
	@BeforeEach public void clearDBAndConnect() throws SocketException, UnknownHostException, IOException, ClassNotFoundException {
		//clear DB
		clearDB();
		//establishes a connection and creates input and output streams
		connect();
	}
	
	/**
	 * Specifically clears DB bookings table and wait list
	 */
	@AfterAll private static void clearDB() {
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
		assertEquals(expected, response.responseType(), "broken login for " + userNetID); //check response is AUTHENTICATED
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
		assertEquals(ResponseType.AUTHENTICATED, loginResponse.responseType(), "testUserInfo: broken login for " + testUser.getNetID()); //check response is AUTHENTICATED
		//build profile info request with testUser
		ClientRequest infoRequest = new ClientRequest(ServerFunction.GET_PROFILE_INFO);
		infoRequest.setUser(testUser);
		//send info request, save server response
		ServerResponse infoResponse = sendRequest(infoRequest);
		//check response type is what's expected (SUCCESSFUL)
		assertEquals(ResponseType.SUCCESS, infoResponse.responseType(), "testUserInfo: broken profile retrieval for "+ testUser.getNetID()); //check response is SUCCESS
		//get resulting user from infoResponse
		User userResult = infoResponse.getUser();
		//check userResult details against userExpected
		assertEquals(userExpected.getName(), userResult.getName(), "testUserInfo: bad user name");
		assertEquals(userExpected.getNetID(), userResult.getNetID(), "testUserInfo: bad net id");
		assertEquals(userExpected.getStudentID(), userResult.getStudentID(), "testUserInfo: bad student id");
		assertEquals(userExpected.getUserPhoto(), userResult.getUserPhoto(), "testUserInfo: bad user photo");
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
	
	//TODO: test retrieve bookings 
}
