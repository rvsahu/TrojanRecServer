package com.errawi.trojanrec.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import com.errawi.trojanrec.utils.*;

//TODO: for the debugging portion we'll likely want to implement some
//logging to files
public class ClientHandler extends Thread {
	private Socket socket;

	/**
	 * An output stream to communicate objects to the client over.
	 * Should always be ServerResponse objects.
	 */
	private ObjectOutputStream oos;

	/**
	 * An input stream to read objects from the client.
	 * Should always be ClientRequest objects.
	 */
	private ObjectInputStream ois;
	
	/**
	 * Synchronised database handler to interface between server threads 
	 * and database while preventing race conditions.
	 */
	private DatabaseHandler dbHandler;

	/**
	 * User object representing the User that is logged into the (Android) TrojanRec
	 * client that this thread is handling/communicating with.
	 TODO: determine whether this field is necessary and if not, remove it
	private User user;  
	 */
	
	/**
	 * A boolean representing whether the User of the client this handler is
	 * communicating with is logged in. It is initially false and only becomes
	 * true when the user successfully logs in. If not true, this handler will
	 * not perform many requested functions.
	 */
	private boolean userAuthenticated;
	
	/**
	 * Integer ID to identify this specific client handler (mainly for debug purposes)
	 */
	private int id;
	
	public ClientHandler(Socket socket, DatabaseHandler dbHandler, int id) {
		this.id = id;
		this.socket = socket;
		userAuthenticated = false;
		this.dbHandler = dbHandler;
		try {
			ois = new ObjectInputStream(socket.getInputStream());
			oos = new ObjectOutputStream(socket.getOutputStream());
		} catch (IOException ie) {
			ie.printStackTrace();
		}
	}

	public InetAddress getAddress() {
		return socket.getInetAddress();
	}

	public void run() {
		//accept the first communication, basically a handshake.
		//once this is done successfully, enter infinite loop.
		ClientRequest currReq; //used to reference current client request
		ServerFunction currFunc; //the function currently requested of the server
		ServerResponse currResp; //references response to current request, may be sent or unsent	
		try {
			currReq = (ClientRequest)ois.readObject();
			currFunc = currReq.getFunction();
			System.out.println(id + " - Connection attempt"); //TODO: log this to a file
			if (currFunc == ServerFunction.CONNECT) {
				//expected input, send back connection successful to
				//complete handshake
				System.out.println(id + " - Connect good"); //TODO: log this to a file
				currResp = new ServerResponse(ResponseType.CONNECTED);
				oos.writeObject(currResp);
			} else {
				System.out.println(id + " - Connect bad"); //TODO: log this to a file
				//received another request when it should have been CONNECT
				//send a CLOSED type server response and close connection
				sendClosedResponse();
				//exit run() (which ends thread)
				return;
			}
		} catch (ClassCastException cce) {
			//object sent was not a ClientRequest
			//send a CLOSED type server response and close connection
			System.out.println(id + " - Connect bad"); //TODO: log this to a file
			sendClosedResponse();
			//exit run() (which ends thread)
			return;
		} catch (ClassNotFoundException cnfe) {
			//not quite sure how we would get here but compiler says need to check for it
			//send a CLOSED type server response and close connection
			System.out.println(id + " - Connect bad"); //TODO: log this to a file
			sendClosedResponse();
			//exit run() (which ends thread)
			return;
		} catch (EOFException eofe) {
			System.out.println(id + " - Client disconnected"); //TODO: log this to a file
			//eofe.printStackTrace();
		} catch (IOException ioe) {
			System.out.println(id + " - Connect bad"); //TODO: log this to a file
			ioe.printStackTrace();
			return;
		} 
		
		//connection successful. now thread enters main loop where it handles
		//all requests
		while (true) {
			try {
				currReq = (ClientRequest)ois.readObject();
				System.out.print(id + " - new request recieved: ");
				if (currReq == null) {
					System.out.println("Null request, send fail response");
					sendFailResponse();
					continue;
				}
				currFunc = currReq.getFunction();
				if (currFunc == ServerFunction.LOGIN) {
					System.out.println("Login attempt"); //TODO: log this to a file
					//check if user already authenticated
					if (userAuthenticated) {
						System.out.println(id + " - Already logged in"); //TODO: log this to a file
						//send AUTHENTICATED response
						currResp = new ServerResponse(ResponseType.AUTHENTICATED);
						oos.writeObject(currResp);
						continue;
					}
					//check user is non-null
					if (currReq.getUser() == null) {
						//user is null, send UNAUTHENTICATED response
						System.out.println(id + " - Login bad - null user"); //TODO: log this to a file
						sendUnauthenticatedResponse();
						continue;
					}
					//authenticate user
					userAuthenticated = dbHandler.authenticateUser(currReq.getUser().getNetID(), currReq.getUserPassword());
					//check if successful or not
					if (userAuthenticated) {
						System.out.println(id + " - Login good"); //TODO: log this to a file
						//send AUTHENTICATED response
						currResp = new ServerResponse(ResponseType.AUTHENTICATED);
						oos.writeObject(currResp);		
					} else {
						//send UNAUTHENTICATED response
						System.out.println(id + " - Login bad"); //TODO: log this to a file
						sendUnauthenticatedResponse();
					}
				} else if (currFunc == ServerFunction.CLOSE) {
					System.out.println("Close request"); //TODO: log this to a file
					sendClosedResponse(); //kill thread once client connection it handles is closed
					System.out.println(id + " - Close request good"); //TODO: log this to a file
					return; //exit thread once socket closed
				} else if (currFunc == ServerFunction.CHECK_IF_LOGGED_IN) {
					System.out.println("Check login attempt"); //TODO: log this to a file
					if (userAuthenticated) {
						System.out.println(id + " - Check login good"); //TODO: log this to a file
						currResp = new ServerResponse(ResponseType.AUTHENTICATED);
						oos.writeObject(currResp);		
					} else {
						System.out.println(id + " - Check login bad"); //TODO: log this to a file
						sendUnauthenticatedResponse();
					}
				} else if (!userAuthenticated) {
					//return UNAUTHENTICATED response, meaning the server
					//will not perform the client request because the User is not logged in
					sendUnauthenticatedResponse();
				} else if (currFunc == ServerFunction.GET_PROFILE_INFO) {
					//get server side User object from client side user's net ID
					System.out.println("Profile attempt"); //TODO: log this to a file
					User serverSideUser = dbHandler.retrieveUser(currReq.getUser().getNetID());
					//check if serverSideUser actually exists (studentID should not be -1)
					if (serverSideUser.getStudentID() != -1) {
						System.out.println(id + " - Profile good"); //TODO: log this to a file
						//send server side user back to client
						currResp = new ServerResponse(ResponseType.SUCCESS); //create response with SUCCESS type
						currResp.setUser(serverSideUser); //add server-side user to response
						oos.writeObject(currResp); //send response
					} else {
						System.out.println(id + " - Profile bad"); //TODO: log this to a file
						//send fail response back to client
						sendFailResponse();
					}
				} else if (currFunc == ServerFunction.GET_CURRENT_BOOKINGS) {
					System.out.println("Current bookings attempt"); //TODO: log this to a file
					ArrayList<Reservation> reservations = dbHandler.getFutureBookings(currReq.getUser());
					if(reservations != null) {
						System.out.println(id + " - Current bookings good"); //TODO: log this to a file
						currResp = new ServerResponse(ResponseType.SUCCESS);
						currResp.setBookings(reservations);	
						oos.writeObject(currResp);					
					} else {
						System.out.println(id + " - Current bookings bad"); //TODO: log this to a file
						sendFailResponse();
					}									
				} else if (currFunc == ServerFunction.GET_PREVIOUS_BOOKINGS) {
					System.out.println("Previous bookings attempt"); //TODO: log this to a file
					ArrayList<Reservation> reservations = dbHandler.getPastBookings(currReq.getUser());
					if(reservations != null) {
						System.out.println(id + " - Previous bookings good"); //TODO: log this to a file
						currResp = new ServerResponse(ResponseType.SUCCESS);
						currResp.setBookings(reservations);	
						oos.writeObject(currResp);	
					} else {
						System.out.println(id + " - Previous bookings bad"); //TODO: log this to a file
						sendFailResponse();
					}	
				} else if (currFunc == ServerFunction.GET_WAIT_LIST) {
					System.out.println("Wait list attempt"); //TODO: log this to a file
					ArrayList<Reservation> waitlist_reservations = dbHandler.getWaitlistForUser(currReq.getUser());
					if(waitlist_reservations != null) {
						System.out.println(id + " - Wait list good"); //TODO: log this to a file
						currResp = new ServerResponse(ResponseType.SUCCESS);
						currResp.setBookings(waitlist_reservations);
						oos.writeObject(currResp);	
					} else {
						System.out.println(id + " - Wait list bad"); //TODO: log this to a file
						sendFailResponse();
					}
				} else if (currFunc == ServerFunction.GET_CENTRE_TIME_SLOTS) {
					System.out.println("Get slots attempt"); //TODO: log this to a file
					ArrayList<String> timeslots = dbHandler.getFutureCenterTimeslots(currReq.getRecCentre());
					if(timeslots != null) {
						System.out.println(id + " - Get slots good"); //TODO: log this to a file
						currResp = new ServerResponse(ResponseType.SUCCESS);
						currResp.setTimeslots(timeslots);
						oos.writeObject(currResp);		
					} else {
						System.out.println(id + " - Get slots bad"); //TODO: log this to a file
						sendFailResponse();
					}
				} else if (currFunc == ServerFunction.MAKE_BOOKING) {
					System.out.print("Make booking attempt "); //TODO: log this to a file
					//TODO: modify client side code to send a reservation rather than construct one here
					Reservation res = new Reservation();
					res.setRecCentre(currReq.getRecCentre());
					res.setTimedate(currReq.getTimeslot());
					User currUser = currReq.getUser();
					System.out.print("centre: " + res.getRecCentre());
					System.out.println(", time slot: " + res.getTimedate());
					//first check if the booking already exists
					if (dbHandler.bookingEntryExists(res, currUser)) {
						//booking already exists, send a NO_ACTION back
						System.out.println(id + " - Make bookings bad, booking exists, send no action response");
						sendNoActionResponse();
						continue; //await next message
					}
					//then check if slot is full
					if (dbHandler.isCapMax(res)) {
						System.out.println(id + " - Make bookings bad, slot full, send fail response"); //TODO: log this to a file
						sendFailResponse();
						continue; //await next message
					}
					//then try and make booking
					boolean success = dbHandler.makeBooking(res, currUser);
					System.out.print(id + " - Make bookings "); //TODO: log this to a file
					if (success) {
						//booking was successful, we should remove a wait list entry for this reservation if it exists
						if (dbHandler.waitlistEntryExists(res, currUser)) {
							//a wait list entry exists for this reservation (and this user), now remove it since booking was made
							dbHandler.removeWaitlistEntry(res, currUser);
						}
						//should send success response in either case
						System.out.println("good, send success response");
						oos.writeObject(new ServerResponse(ResponseType.SUCCESS));
					} else {
						System.out.println("bad, send fail response");
						sendFailResponse();
					}
				} else if (currFunc == ServerFunction.CANCEL_BOOKING) {
					System.out.println("Cancel bookings attempt"); //TODO: log this to a file
					Reservation res = new Reservation();
					res.setRecCentre(currReq.getRecCentre());
					res.setTimedate(currReq.getTimeslot());
					dbHandler.removeBooking(res, currReq.getUser());
					currResp = new ServerResponse(ResponseType.SUCCESS);
					oos.writeObject(currResp);
					System.out.println(id + " - Current bookings good (in theory)"); //TODO: log this to a file
				} else if (currFunc == ServerFunction.CANCEL_WAIT_LIST) {
					System.out.println("Cancel waitlist attempt"); //TODO: log this to a file
					Reservation res = new Reservation();
					res.setRecCentre(currReq.getRecCentre());
					res.setTimedate(currReq.getTimeslot());
					//check whether the waitlist entry we're trying to remove actually exists
					boolean entryExists = dbHandler.waitlistEntryExists(res, currReq.getUser());
					if (entryExists) {
						//entry exists, remove it
						boolean removed = dbHandler.removeWaitlistEntry(res, currReq.getUser());
						if (removed) {
							currResp = new ServerResponse(ResponseType.SUCCESS);
							oos.writeObject(currResp);
						} else {
							sendFailResponse();
						}
					} else {
						//entry doesn't exist, send response to client saying no action was taken
						sendNoActionResponse();
					}
					
				}
			} catch (ClassCastException cce) {
				//object sent was not a ClientRequest
				//send a CLOSED type server response and close connection
				sendClosedResponse();
				//exit run() (which ends thread)
				return;
			} catch (ClassNotFoundException cnfe) {
				//not quite sure how we would get here but compiler says need to check for it
				//send a CLOSED type server response and close connection
				sendClosedResponse();
				//exit run() (which ends thread)
				return;
			} catch (EOFException eofe) {
				System.out.println(id + " - Client disconnected"); //TODO: log this to a file
				//eofe.printStackTrace();
				return;
			} catch (SocketException se) {
				System.out.println(id + " - connection reset"); //TODO log to a file
				return;
			} catch (IOException ioe) {
				//some error reading from input stream. errors sending back would be handled
				//further in. we should print stack trace and then end this thread.
				ioe.printStackTrace();
				return;
			}
		}
	}
	
	/**
	 * Below are cookie-cutter responses that may need to be sent, specifically
	 * FAIL, CLOSED, NO_ACTION, and UNAUTHENTICATED. This may not be as useful for some of
	 * the other response types as extra information would be added to the
	 * ServerResponse objects. 
	 */
	
	/**
	 * Sends a server response with type UNAUTHENTICATED back to the client
	 * 
	 * @return     True if it could be sent back successfully, false otherwise.
	 */
	private boolean sendUnauthenticatedResponse() {
		ServerResponse response = new ServerResponse(ResponseType.UNAUTHENTICATED);
		try {
			oos.writeObject(response); //send UNAUTHENTICATED response
			return true;
		} catch (IOException ioe) {
			//couldn't successfully send back response, just return
			ioe.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Sends a CLOSED response back to the client, and then closes the socket
	 */
	private void sendClosedResponse() {
		ServerResponse response = new ServerResponse(ResponseType.CLOSED);
		try {
			oos.writeObject(response); //send CLOSED response
			socket.close(); //close socket
		} catch (IOException ioe) {
			//couldn't successfully send back response, just return
			ioe.printStackTrace();
		}
	}
	
	/**
	 * Sends a FAIL response back to the client, and returns whether sending it was
	 * successful or not
	 * 
	 * @return     True if a FAIL response was sent successfully, false otherwise.
	 */
	private boolean sendFailResponse() {
		ServerResponse response = new ServerResponse(ResponseType.FAIL);
		try {
			oos.writeObject(response); //send FAIL response
			return true;
		} catch (IOException ioe) {
			//couldn't successfully send back response, return false
			ioe.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Sends a NO_ACTION response back to the client, and returns whether sending it was
	 * successful or not
	 * 
	 * @return     True if a NO_ACTION response was sent successfully, false otherwise.
	 */
	private boolean sendNoActionResponse() {
		ServerResponse response = new ServerResponse(ResponseType.NO_ACTION);
		try {
			oos.writeObject(response); //send NO_ACTION response
			return true;
		} catch (IOException ioe) {
			//couldn't successfully send back response, return false
			ioe.printStackTrace();
			return false;
		}
	}
}
