package com.errawi.trojanrec.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
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
	
	public ClientHandler(Socket socket, DatabaseHandler dbHandler) {
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
		ServerResponse currResp; //references response to current request, may be sent or unsent	
		try {
			currReq = (ClientRequest)ois.readObject();
			System.out.println("Connection attempt"); //TODO: log this to a file
			if (currReq.getFunction() == ServerFunction.CONNECT) {
				//expected input, send back connection successful to
				//complete handshake
				System.out.println("Connect good"); //TODO: log this to a file
				currResp = new ServerResponse(ResponseType.CONNECTED);
				oos.writeObject(currResp);
			} else {
				System.out.println("Connect bad"); //TODO: log this to a file
				//received another request when it should have been CONNECT
				//send a CLOSED type server response and close connection
				sendClosedResponse();
				//exit run() (which ends thread)
				return;
			}
		} catch (ClassCastException cce) {
			//object sent was not a ClientRequest
			//send a CLOSED type server response and close connection
			System.out.println("Connect bad"); //TODO: log this to a file
			sendClosedResponse();
			//exit run() (which ends thread)
			return;
		} catch (ClassNotFoundException cnfe) {
			//not quite sure how we would get here but compiler says need to check for it
			//send a CLOSED type server response and close connection
			System.out.println("Connect bad"); //TODO: log this to a file
			sendClosedResponse();
			//exit run() (which ends thread)
			return;
		} catch (EOFException eofe) {
			System.out.println("Client disconnected"); //TODO: log this to a file
			//eofe.printStackTrace();
		} catch (IOException ioe) {
			System.out.println("Connect bad"); //TODO: log this to a file
			ioe.printStackTrace();
			return;
		} 
		
		//connection successful. now thread enters main loop where it handles
		//all requests
		while (true) {
			try {
				currReq = (ClientRequest)ois.readObject();
				System.out.print("new request recieved: ");
				if (currReq == null) {
					sendFailResponse();
				} else if (currReq.getFunction() == ServerFunction.LOGIN) {
					System.out.println("Login attempt"); //TODO: log this to a file
					//check if user already authenticated
					if (userAuthenticated) {
						System.out.println("Already logged in"); //TODO: log this to a file
						//send AUTHENTICATED response
						currResp = new ServerResponse(ResponseType.AUTHENTICATED);
						oos.writeObject(currResp);
						continue;
					}
					//authenticate user
					userAuthenticated = dbHandler.authenticateUser(currReq.getUser().getNetID(), currReq.getUserPassword());
					//check if successful or not
					if (userAuthenticated) {
						System.out.println("Login good"); //TODO: log this to a file
						//send AUTHENTICATED response
						currResp = new ServerResponse(ResponseType.AUTHENTICATED);
						oos.writeObject(currResp);		
					} else {
						//send UNAUTHENTICATED response
						System.out.println("Login bad"); //TODO: log this to a file
						sendUnauthenticatedResponse();
					}
				} else if (currReq.getFunction() == ServerFunction.CLOSE) {
					System.out.println("Close request"); //TODO: log this to a file
					sendClosedResponse();
				} else if (currReq.getFunction() == ServerFunction.CHECK_IF_LOGGED_IN) {
					System.out.println("Check login attempt"); //TODO: log this to a file
					if (userAuthenticated) {
						System.out.println("Check login good"); //TODO: log this to a file
						currResp = new ServerResponse(ResponseType.AUTHENTICATED);
						oos.writeObject(currResp);		
					} else {
						System.out.println("Check login bad"); //TODO: log this to a file
						sendUnauthenticatedResponse();
					}
				} else if (!userAuthenticated) {
					//return UNAUTHENTICATED response, meaning the server
					//will not perform the client request because the User is not logged in
					sendUnauthenticatedResponse();
				} else if (currReq.getFunction() == ServerFunction.GET_PROFILE_INFO) {
					//get server side User object from client side user's net ID
					System.out.println("Profile attempt"); //TODO: log this to a file
					User serverSideUser = dbHandler.retrieveUser(currReq.getUser().getNetID());
					//check if serverSideUser actually exists (studentID should not be -1)
					if (serverSideUser.getStudentID() != -1) {
						System.out.println("Profile good"); //TODO: log this to a file
						//send server side user back to client
						currResp = new ServerResponse(ResponseType.SUCCESS); //create response with SUCCESS type
						currResp.setUser(serverSideUser); //add server-side user to response
						oos.writeObject(currResp); //send response
					} else {
						System.out.println("Profile bad"); //TODO: log this to a file
						//send fail response back to client
						sendFailResponse();
					}
				} else if (currReq.getFunction() == ServerFunction.GET_CURRENT_BOOKINGS) {
					System.out.println("Current bookings attempt"); //TODO: log this to a file
					ArrayList<Reservation> reservations = dbHandler.getFutureBookings(currReq.getUser());
					if(reservations != null) {
						System.out.println("Current bookings good"); //TODO: log this to a file
						currResp = new ServerResponse(ResponseType.SUCCESS);
						currResp.setBookings(reservations);	
						oos.writeObject(currResp);					
					} else {
						System.out.println("Current bookings bad"); //TODO: log this to a file
						sendFailResponse();
					}									
				}
				else if (currReq.getFunction() == ServerFunction.GET_PREVIOUS_BOOKINGS) {
					System.out.println("Previous bookings attempt"); //TODO: log this to a file
					ArrayList<Reservation> reservations = dbHandler.getPastBookings(currReq.getUser());
					if(reservations != null) {
						System.out.println("Previous bookings good"); //TODO: log this to a file
						currResp = new ServerResponse(ResponseType.SUCCESS);
						currResp.setBookings(reservations);	
						oos.writeObject(currResp);	
					} else {
						System.out.println("Previous bookings bad"); //TODO: log this to a file
						sendFailResponse();
					}	
				}
				else if (currReq.getFunction() == ServerFunction.GET_WAIT_LIST) {
					System.out.println("Wait list attempt"); //TODO: log this to a file
					ArrayList<Reservation> waitlist_reservations = dbHandler.getWaitlistForUser(currReq.getUser());
					if(waitlist_reservations != null) {
						System.out.println("Wait list good"); //TODO: log this to a file
						currResp = new ServerResponse(ResponseType.SUCCESS);
						currResp.setBookings(waitlist_reservations);
						oos.writeObject(currResp);	
					} else {
						System.out.println("Wait list bad"); //TODO: log this to a file
						sendFailResponse();
					}
				}
				else if (currReq.getFunction() == ServerFunction.GET_CENTRE_TIME_SLOTS) {
					System.out.println("Get slots attempt"); //TODO: log this to a file
					ArrayList<String> timeslots = dbHandler.getCenterTimeslots(currReq.getRecCentre());
					if(timeslots != null) {
						System.out.println("Get slots good"); //TODO: log this to a file
						currResp = new ServerResponse(ResponseType.SUCCESS);
						currResp.setTimeslots(timeslots);
						oos.writeObject(currResp);		
					} else {
						System.out.println("Get slots bad"); //TODO: log this to a file
						sendFailResponse();
					}
				}
				else if (currReq.getFunction() == ServerFunction.MAKE_BOOKING) {
					System.out.println("Make booking attempt"); //TODO: log this to a file
					//TODO: modify client side code to send a reservation rather than construct one here
					Reservation res = new Reservation();
					res.setRecCentre(currReq.getRecCentre());
					res.setTimedate(currReq.getTimeslot());
					System.out.println("res rec centre: " + res.getRecCentre());
					System.out.println("res time slot: " + res.getTimedate());
					boolean max_cap = dbHandler.isCapMax(res);
					System.out.println("is cap max done");
					if(max_cap) {
						System.out.println("Make bookings bad, waitlist instead"); //TODO: log this to a file
						// add user to wait list because the bookings are full for that reservation time
						dbHandler.addToWaitlist(res, currReq.getUser());			
					}
					else {
						System.out.println("Make bookings good"); //TODO: log this to a file
						// make booking
						dbHandler.makeBooking(res, currReq.getUser());			
					}	
					currResp = new ServerResponse(ResponseType.SUCCESS);				
				}
				else if (currReq.getFunction() == ServerFunction.CANCEL_BOOKING) {
					System.out.println("Cancel bookings attempt"); //TODO: log this to a file
					Reservation res = new Reservation();
					res.setRecCentre(currReq.getRecCentre());
					res.setTimedate(currReq.getTimeslot());
					dbHandler.removeBooking(res, currReq.getUser());
					currResp = new ServerResponse(ResponseType.SUCCESS);				
					System.out.println("Current bookings good (in theory)"); //TODO: log this to a file
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
				System.out.println("Client disconnected"); //TODO: log this to a file
				//eofe.printStackTrace();
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
	 * FAIL, CLOSED, and UNAUTHENTICATED. This may not be as useful for some of
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
		ServerResponse response = new ServerResponse(ResponseType.CLOSED);
		try {
			oos.writeObject(response); //send FAIL response
			return true;
		} catch (IOException ioe) {
			//couldn't successfully send back response, return false
			ioe.printStackTrace();
			return false;
		}
	}
}
