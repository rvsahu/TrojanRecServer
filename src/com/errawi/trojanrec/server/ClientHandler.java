package com.errawi.trojanrec.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

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
	 * (Synchronised) notification bank that all ClientHandlers can check
	 * to see if the client they service has wait list entries that they can
	 * now make bookings for.
	 */
	private NotificationBank notifBank;

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
	
	public ClientHandler(Socket socket, DatabaseHandler dbHandler, NotificationBank notifBank, int id) {
		this.id = id;
		this.socket = socket;
		userAuthenticated = false;
		this.dbHandler = dbHandler;
		this.notifBank = notifBank;
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
			} else {
				System.out.println(id + " - Connect bad"); //TODO: log this to a file
				//received another request when it should have been CONNECT
				//send a CLOSED type server response and close connection
				currResp = createClosedResponse();
				//exit run() (which ends thread)
				return;
			}
			oos.writeObject(currResp);
			if (currResp.responseType() == ResponseType.CLOSED) {
				socket.close();
				return;
			}
		} catch (ClassCastException cce) {
			//object sent was not a ClientRequest
			//send a CLOSED type server response and close connection
			System.out.println(id + " - Connect bad, bad casting"); //TODO: log this to a file
			try {
				oos.writeObject(createClosedResponse()); //send CLOSED response
				socket.close(); //close socket
			} catch (IOException inner_ioe) {
				System.out.println("Additional IO Exception while handling exception");
			}
			return; //exit run() (which ends thread)
		} catch (ClassNotFoundException cnfe) {
			//not quite sure how we would get here but compiler says need to check for it
			//send a CLOSED type server response and close connection
			System.out.println(id + " - Connect bad, class nonexistent"); //TODO: log this to a file
			try {
				oos.writeObject(createClosedResponse()); //send CLOSED response
				socket.close(); //close socket
			} catch (IOException inner_ioe) {
				System.out.println("Additional IO Exception while handling exception");
			}
			return; //exit run() (which ends thread)
		} catch (EOFException eofe) {
			System.out.println(id + " - Client disconnected"); //TODO: log this to a file
			//eofe.printStackTrace();
			try {
				socket.close(); //close socket
			} catch (IOException inner_ioe) {
				System.out.println("Additional IO Exception while handling exception");
			}
			return; //exit run() (which ends thread)
		} catch (IOException ioe) {
			System.out.println(id + " - Connect bad"); //TODO: log this to a file
			ioe.printStackTrace();
			try {
				socket.close(); //close socket
			} catch (IOException inner_ioe) {
				System.out.println("Additional IO Exception while handling exception");
			}
			return; //exit run() (which ends thread)
		} 
		
		//connection successful. now thread enters main loop where it handles
		//all requests
		while (true) {
			try {
				currReq = (ClientRequest)ois.readObject();
				System.out.print(id + " - new request recieved: ");
				if (currReq == null) {
					System.out.println("Null request, send fail response");
					currResp = createFailResponse();
					oos.writeObject(currResp);
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
					} else if (currReq.getUser() == null) { //check user is non-null
						//user is null, send UNAUTHENTICATED response
						System.out.println(id + " - Login bad - null user"); //TODO: log this to a file
						currResp = createUnauthenticatedResponse();
					} else {
						//authenticate user
						userAuthenticated = dbHandler.authenticateUser(currReq.getUser().getNetID(), currReq.getUserPassword());
						//check if successful or not
						if (userAuthenticated) {
							System.out.println(id + " - Login good"); //TODO: log this to a file
							//send AUTHENTICATED response
							currResp = new ServerResponse(ResponseType.AUTHENTICATED);
						} else {
							//send UNAUTHENTICATED response
							System.out.println(id + " - Login bad"); //TODO: log this to a file
							currResp = createUnauthenticatedResponse();
						}
					}
				} else if (currFunc == ServerFunction.CLOSE) {
					System.out.println("Close request"); //TODO: log this to a file
					currResp = createClosedResponse(); //kill thread once client connection it handles is closed
					System.out.println(id + " - Close request good"); //TODO: log this to a file
					return; //exit thread once socket closed
				} else if (currFunc == ServerFunction.CHECK_IF_LOGGED_IN) {
					System.out.println("Check login attempt"); //TODO: log this to a file
					if (userAuthenticated) {
						System.out.println(id + " - Check login good"); //TODO: log this to a file
						currResp = new ServerResponse(ResponseType.AUTHENTICATED);
					} else {
						System.out.println(id + " - Check login bad"); //TODO: log this to a file
						currResp = createUnauthenticatedResponse();
					}
				} else if (!userAuthenticated) {
					//return UNAUTHENTICATED response, meaning the server
					//will not perform the client request because the User is not logged in
					currResp = createUnauthenticatedResponse();
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
					} else {
						System.out.println(id + " - Profile bad"); //TODO: log this to a file
						//send fail response back to client
						currResp = createFailResponse();
					}
				} else if (currFunc == ServerFunction.GET_CURRENT_BOOKINGS) {
					System.out.println("Current bookings attempt"); //TODO: log this to a file
					ArrayList<Reservation> reservations = dbHandler.getFutureBookings(currReq.getUser());
					if(reservations != null) {
						System.out.println(id + " - Current bookings good"); //TODO: log this to a file
						currResp = new ServerResponse(ResponseType.SUCCESS);
						currResp.setBookings(reservations);	
					} else {
						System.out.println(id + " - Current bookings bad"); //TODO: log this to a file
						currResp = createFailResponse();
					}									
				} else if (currFunc == ServerFunction.GET_PREVIOUS_BOOKINGS) {
					System.out.println("Previous bookings attempt"); //TODO: log this to a file
					ArrayList<Reservation> reservations = dbHandler.getPastBookings(currReq.getUser());
					if(reservations != null) {
						System.out.println(id + " - Previous bookings good"); //TODO: log this to a file
						currResp = new ServerResponse(ResponseType.SUCCESS);
						currResp.setBookings(reservations);	
					} else {
						System.out.println(id + " - Previous bookings bad"); //TODO: log this to a file
						currResp = createFailResponse();
					}	
				} else if (currFunc == ServerFunction.GET_WAIT_LIST) {
					System.out.println("Wait list attempt"); //TODO: log this to a file
					ArrayList<Reservation> waitlist_reservations = dbHandler.getWaitlistForUser(currReq.getUser());
					if(waitlist_reservations != null) {
						System.out.println(id + " - Wait list good"); //TODO: log this to a file
						currResp = new ServerResponse(ResponseType.SUCCESS);
						currResp.setBookings(waitlist_reservations);
					} else {
						System.out.println(id + " - Wait list bad"); //TODO: log this to a file
						currResp = createFailResponse();
					}
				} else if (currFunc == ServerFunction.GET_CENTRE_TIME_SLOTS) {
					System.out.println("Get slots attempt"); //TODO: log this to a file
					ArrayList<String> timeslots = dbHandler.getFutureCenterTimeslots(currReq.getRecCentre());
					if(timeslots != null) {
						System.out.println(id + " - Get slots good"); //TODO: log this to a file
						currResp = new ServerResponse(ResponseType.SUCCESS);
						currResp.setTimeslots(timeslots);
					} else {
						System.out.println(id + " - Get slots bad"); //TODO: log this to a file
						currResp = createFailResponse();
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
						currResp = createNoActionResponse();
					} else if (dbHandler.isCapMax(res)) { //then check if slot is full
						System.out.println(id + " - Make bookings bad, slot full, send fail response"); //TODO: log this to a file
						currResp = createFailResponse();
					} else {
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
							currResp = new ServerResponse(ResponseType.SUCCESS);
						} else {
							System.out.println("bad, send fail response");
							currResp = createFailResponse();
						}
					}
				} else if (currFunc == ServerFunction.CANCEL_BOOKING) {
					System.out.println("Cancel bookings attempt"); //TODO: log this to a file
					Reservation res = new Reservation();
					res.setRecCentre(currReq.getRecCentre());
					res.setTimedate(currReq.getTimeslot());
					dbHandler.removeBooking(res, currReq.getUser());
					currResp = new ServerResponse(ResponseType.SUCCESS);
					System.out.println(id + " - Current bookings good (in theory)"); //TODO: log this to a file
				} else if (currFunc == ServerFunction.JOIN_WAIT_LIST) {
					System.out.println("Join waitlist attempt"); //TODO: log this to a file
					//TODO: modify client side code to send a reservation rather than construct one here
					Reservation res = new Reservation();
					res.setRecCentre(currReq.getRecCentre());
					res.setTimedate(currReq.getTimeslot());
					User currUser = currReq.getUser();
					//first check if the wait list already exists
					if (dbHandler.waitlistEntryExists(res, currUser)) {
						//wait list entry already exists, send a NO_ACTION back
						System.out.println(id + " - Join wait list bad, wait lest entry exists, send no action response");
						currResp = createNoActionResponse();
						continue; //await next message
					} else if (dbHandler.bookingEntryExists(res, currUser)) { //then check if a booking already exists (no reason to add to waitlist if a booking is already made)
						//wait list entry already exists, send a NO_ACTION back
						System.out.println(id + " - Join wait list bad, booking for user exists, send no action response");
						currResp = createNoActionResponse();
					} else {
						//then join wait list
						boolean success = dbHandler.addToWaitlist(res, currUser);
						System.out.println(id + " - Join wait list good"); //TODO: log this to a file
						if (success) {
							currResp = new ServerResponse(ResponseType.SUCCESS); //create success response to send back
						} else {
							currResp = createFailResponse(); //create fail response to send back
						}
					}
				} else if (currFunc == ServerFunction.CANCEL_WAIT_LIST) {
					System.out.println("Cancel waitlist attempt"); //TODO: log this to a file
					Reservation res = new Reservation();
					res.setRecCentre(currReq.getRecCentre());
					res.setTimedate(currReq.getTimeslot());
					//check whether the wait list entry we're trying to remove actually exists
					boolean entryExists = dbHandler.waitlistEntryExists(res, currReq.getUser());
					if (entryExists) {
						//entry exists, remove it
						boolean removed = dbHandler.removeWaitlistEntry(res, currReq.getUser());
						if (removed) {
							currResp = new ServerResponse(ResponseType.SUCCESS);
						} else {
							currResp = createFailResponse();
						}
					} else {
						//entry doesn't exist, send response to client saying no action was taken
						currResp = createNoActionResponse();
					}
					
				}
				
				//check notification bank for user's notifs, given they're authenticated
				if (userAuthenticated && currReq.getUser() != null) {
					List<Reservation> userOpenedSlots = notifBank.getUserNotifs(currReq.getUser().getNetID());
					if (userOpenedSlots != null && userOpenedSlots.size() != 0) {
						//user is wait listed for slots that had opened, put in response before sending it back
						currResp.setOpenedSlots(userOpenedSlots);
					}
				}
				//send back current response
				oos.writeObject(currResp);
				//close socket if need to
				if (currResp.responseType() == ResponseType.CLOSED) {
					socket.close(); //close socket
					return; //exit run() (which ends thread)
				}
			} catch (ClassCastException cce) {
				//object sent was not a ClientRequest
				//send a CLOSED type server response and close connection
				try {
					oos.writeObject(createClosedResponse()); //send CLOSED response
					socket.close(); //close socket
				} catch (IOException inner_ioe) {
					System.out.println("Additional IO Exception while handling exception");
				}
				return; //exit run() (which ends thread)
			} catch (ClassNotFoundException cnfe) {
				//not quite sure how we would get here but compiler says need to check for it
				//send a CLOSED type server response and close connection
				try {
					oos.writeObject(createClosedResponse()); //send CLOSED response
					socket.close(); //close socket
				} catch (IOException inner_ioe) {
					System.out.println("Additional IO Exception while handling exception");
				}
				return; //exit run() (which ends thread)
			} catch (EOFException eofe) {
				System.out.println(id + " - Client disconnected"); //TODO: log this to a file
				try {
					socket.close(); //close socket
				} catch (IOException inner_ioe) {
					System.out.println("Additional IO Exception while handling exception");
				}
				return; //exit run() (which ends thread)
			} catch (SocketException se) {
				System.out.println(id + " - Connection reset"); //TODO log to a file
				se.printStackTrace();
				try {
					socket.close(); //close socket
				} catch (IOException inner_ioe) {
					System.out.println("Additional IO Exception while handling exception");
				}
				return; //exit run() (which ends thread)
			} catch (IOException ioe) {
				//some error reading from input stream. errors sending back would be handled
				//further in. we should print stack trace and then end this thread.
				ioe.printStackTrace();
				try {
					socket.close(); //close socket
				} catch (IOException inner_ioe) {
					System.out.println("Additional IO Exception while handling exception");
				}
				return; //exit run() (which ends thread)
			}
		}
	}
	
	/**
	 * Below are generators for cookie-cutter response that may need to be sent, specifically
	 * FAIL, CLOSED, NO_ACTION, and UNAUTHENTICATED. This may not be as useful for some of
	 * the other response types as extra information would be added to the
	 * ServerResponse objects. 
	 */
	
	/**
	 * Creates a UNAUTHENTICATED response to be sent back to the client.
	 * 
	 * @return     A new UNAUTHENTICATED response
	 */
	private ServerResponse createUnauthenticatedResponse() {
		return new ServerResponse(ResponseType.UNAUTHENTICATED);
	}
	
	/**
	 * Creates a CLOSED response to be sent back to the client.
	 * 
	 * @return     A new CLOSED response
	 */
	private ServerResponse createClosedResponse() {
		return new ServerResponse(ResponseType.CLOSED);
		
	}
	
	/**
	 * Creates a FAIL response to be sent back to the client.
	 * 
	 * @return     A new FAIL response
	 */
	private ServerResponse createFailResponse() {
		return new ServerResponse(ResponseType.FAIL);
	}
	
	/**
	 * Creates a NO_ACTION response to be sent back to the client.
	 * 
	 * @return     A new NO_ACTION response
	 */
	private ServerResponse createNoActionResponse() {
		return new ServerResponse(ResponseType.NO_ACTION);
	}
}
