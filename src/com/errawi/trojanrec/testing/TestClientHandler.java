package com.errawi.trojanrec.testing;

import org.junit.Test;

import com.errawi.trojanrec.server.DatabaseHandler;
import com.errawi.trojanrec.utils.ClientRequest;
import com.errawi.trojanrec.utils.ResponseType;
import com.errawi.trojanrec.utils.ServerFunction;
import com.errawi.trojanrec.utils.ServerResponse;
import com.errawi.trojanrec.utils.User;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * A class to test the outputs of 
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
	
	private Socket testSocket;
	
	/**
	 * Need a copy of this to be able to clear bookings table and wait list once done with a test group
	 */
	private static DatabaseHandler testDBH;
	
	/**
	 * Creates a database handler that we can use to clear tables between each test
	 */
	@BeforeClass public static void setUpDatabase() {
		testDBH = new DatabaseHandler();
	}
	
	/**
	 * Sets up a new connection to the 'server' 
	 */
	@Before public void clearDBAndConnect() throws SocketException, UnknownHostException, IOException, ClassNotFoundException {
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
	 * Test each login works with specified usernames and passwords
	 */
	@Test public void testLogins() throws SocketException, UnknownHostException, ClassNotFoundException, IOException {
		//declare test instances of users in db
		User erin = new User("erinbris");
		String erinPassword = "1234";
		User rahul = new User("rahuls");
		String rahulPassword = "4321";
		User will = new User("willw");
		String willPassword = "3456";
		User moshe = new User("mosheheletz");
		String moshePassword = "7890";
		User karan = new User("karanm");
		String karanPassword = "9876";
		User shreya = new User("shreyac");
		String shreyaPassword = "7654";
		User khanh = new User("khanhpham");
		String khanhPassword = "2345";
		User avonlea = new User("avonleav");
		String avonleaPassword = "6543";
		
		assertTrue("Testing login for erin", testLogin(erin, erinPassword));
		assertTrue("Testing login for rahul", testLogin(rahul, rahulPassword));
		assertTrue("Testing login for will", testLogin(will, willPassword));
		assertTrue("Testing login for moshe", testLogin(moshe, moshePassword));
		assertTrue("Testing login for karan", testLogin(karan, karanPassword));
		assertTrue("Testing login for shreya", testLogin(shreya, shreyaPassword));
		assertTrue("Testing login for khanh", testLogin(khanh, khanhPassword));
		assertTrue("Testing login for avonlea", testLogin(avonlea, avonleaPassword));
	}
	
	/**
	 * Test a specific login 
	 */
	private boolean testLogin(User user, String userPassword) throws SocketException, UnknownHostException, ClassNotFoundException, IOException {
		connect(); //connect to server
		ClientRequest loginRequest = new ClientRequest(ServerFunction.LOGIN);
		loginRequest.setUser(user);
		loginRequest.setUserPassword(userPassword);
		ServerResponse response = sendRequest(loginRequest);
		return response.responseType() == ResponseType.AUTHENTICATED;
	}
	
	/**
	 * Clears database of bookings and wait list entries one last time.
	 */
	@AfterClass public static void clearDatabase() {
		testDBH.clearBookingsWaitlistsTables();
	}
}
