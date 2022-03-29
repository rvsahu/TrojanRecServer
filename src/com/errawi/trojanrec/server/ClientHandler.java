package com.errawi.trojanrec.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

import com.errawi.trojanrec.utils.User;
import com.errawi.trojanrec.utils.Reservation;

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
     * Enumerates the functions that the server could need to perform for the client
     */
    private enum ServerFunction {
        CONNECT, LOGIN, GET_PROFILE_INFO, GET_CURRENT_BOOKINGS,
        GET_PREVIOUS_BOOKINGS, GET_WAIT_LIST, GET_CENTRE_TIME_SLOTS, MAKE_BOOKING,
        CANCEL_BOOKING, CANCEL_WAIT_LIST, POLL_FOR_NOTIFICATIONS, CLOSE
    }
    
    //TODO: current bookings, previous bookings, get waitlist, 
    //TODO: get centre time slots, make booking+join waitlist,
    //TODO: cancel booking, 

    /**
     * Enumerates conditions with which the server could respond to the
     */
    private enum ResponseType {
        CONNECTED, AUTHENTICATED, UNAUTHENTICATED, SUCCESS, FAIL, CLOSED
    }

    /**
     * ClientRequest is the framework for client-to-server communication. It contains
     * a specific function that the client wishes the server to perform, and the
     * relevant information that the server needs to be able to perform it. This
     * exists as a nest private class because for security reasons we don't really
     * want another part of the program to be able to access/create these.
     */
    private static class ClientRequest implements Serializable {
        private static final long serialVersionUID = 20220328L;
        /**
         * The function the server needs to perform, as enumerated by ServerFunctions.
         */
        private ServerFunction function;

        /**
         * The user invoking the server function (i.e user trying to retrieve their current bookings)
         */
        private User user;

        /**
         * String containing the user password, for login requests.
         */
        private String userPassword;

        /**
         * Integer id representing the recreation centre that is being sent to the server.
         * This would be used by the server in the cases of making a booking, cancelling one,
         * and likewise for the wait list.
         */
        private int recCentre;
        
        /**
         * ArrayList of reservation times/dates for retrieving past and future bookings.
         */
        private ArrayList<Reservation> bookings;
        
        
        /**
         * Reservation object with timedate and center for the reservation.
         * useful for creating a booking, retrieving waitlist for specific reservation
         */
        private Reservation reservation;

        /**
         * String representing a time slot in a recreation centre (and the specific one is
         * defined by the recCentre field). This would be used by the server in the cases of
         * making a booking, cancelling one, and likewise for the wait list.
         */
        private String timeslot;    

        public ClientRequest(ServerFunction function) {
            this.function = function;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public void setUserPassword(String userPassword) {
            this.userPassword = userPassword;
        }

        public void setRecCentre(int recCentre) {
            this.recCentre = recCentre;
        }

        public void setTimeslot(String timeslot) {
            this.timeslot = timeslot;
        }
        
        public void setBookings(ArrayList<Reservation> bookings) {
        	this.bookings = bookings;
        }
        
        public void setReservation(Reservation reservation) {
        	this.reservation = reservation;
        }
        
        public ServerFunction getFunction() {
            return function;
        }

        public User getUser() {
            return user;
        }

        public String getUserPassword() {
            return userPassword;
        }

        public int getRecCentre() {
            return recCentre;
        }

        public String getTimeslot() {
            return timeslot;
        }
        
        public ArrayList<Reservation> getBookings() {
        	return bookings;
        }
        
        public Reservation getReservation() {
        	return reservation;
        }
    }

    private static class ServerResponse implements Serializable {
        private static final long serialVersionUID = 72002803L;

        private ResponseType responseType;

        private User user;
        
        private ArrayList<Reservation> bookings;
        
        /**
         * Sending a list of users. Useful for sending a group of users on the waitlist
         */
        private ArrayList<User> send_users;
        
        private ArrayList<String> timeslots;

        public ServerResponse(ResponseType responseType) {
            this.responseType = responseType;
        }

        public void setResponse(ResponseType responseType) {
            this.responseType = responseType;
        }

        public ResponseType responseType() {
            return responseType;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public User getUser() {
            return user;
        }
        
        public void setBookings(ArrayList<Reservation> bookings) {
        	this.bookings = bookings;
        }
        
        public void setSendUsers(ArrayList<User> send_users) {
        	this.send_users = send_users;
        }
        
        public void setTimeslots(ArrayList<String> timeslots) {
        	this.timeslots = timeslots;
        }

    }
	
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
			if (currReq.getFunction() == ServerFunction.CONNECT) {
				//expected input, send back connection successful to
				//complete handshake
				currResp = new ServerResponse(ResponseType.CONNECTED);
				oos.writeObject(currResp);
			} else {
				//received another request when it should have been CONNECT
				//send a CLOSED type server response and close connection
				sendClosedResponse();
				//exit run() (which ends thread)
				return;
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
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return;
		}
		
		//connection successful. now thread enters main loop where it handles
		//all requests
		while (true) {
			try {
				currReq = (ClientRequest)ois.readObject();
				if (currReq.getFunction() == ServerFunction.LOGIN) {
					//authenticate user
					userAuthenticated = dbHandler.authenticateUser(currReq.getUser().getNetID(), currReq.getUserPassword());
					
					if (userAuthenticated) {
						//send AUTHENTICATED response
						currResp = new ServerResponse(ResponseType.AUTHENTICATED);
						oos.writeObject(currResp);		
					} else {
						//send UNAUTHENTICATED response
						sendUnauthenticatedResponse();
					}
				} else if (currReq.getFunction() == ServerFunction.CLOSE) {
					sendClosedResponse();
				} else if (!userAuthenticated) {
					//return UNAUTHENTICATED response, meaning the server
					//will not perform the client request because the User is not logged in
					sendUnauthenticatedResponse();
				} else if (currReq.getFunction() == ServerFunction.GET_PROFILE_INFO) {
					//get server side User object from client side user's net ID
					User serverSideUser = dbHandler.retrieveUser(currReq.getUser().getNetID());
					//check if serverSideUser actually exists (studentID should not be -1)
					if (serverSideUser.getStudentID() != -1) {
						//send server side user back to client
						currResp = new ServerResponse(ResponseType.SUCCESS); //create response with SUCCESS type
						currResp.setUser(serverSideUser); //add server-side user to response
						oos.writeObject(currResp); //send response
					} else {
						//send fail response back to client
						sendFailResponse();
					}
				} else if (currReq.getFunction() == ServerFunction.GET_CURRENT_BOOKINGS) {					
					ArrayList<Reservation> reservations = dbHandler.getFutureBookings(currReq.getUser());
					currResp = new ServerResponse(ResponseType.SUCCESS);
					currResp.setBookings(reservations);	
					oos.writeObject(currResp);					
				}
				else if (currReq.getFunction() == ServerFunction.GET_PREVIOUS_BOOKINGS) {
					ArrayList<Reservation> reservations = dbHandler.getPastBookings(currReq.getUser());
					currResp = new ServerResponse(ResponseType.SUCCESS);
					currResp.setBookings(reservations);	
					oos.writeObject(currResp);					
				}
				else if (currReq.getFunction() == ServerFunction.GET_WAIT_LIST) {
					ArrayList<User> waitlist_users = dbHandler.getWaitlist(currReq.getReservation());
					currResp = new ServerResponse(ResponseType.SUCCESS);
					currResp.setSendUsers(waitlist_users);
					oos.writeObject(currResp);			
				}
				else if (currReq.getFunction() == ServerFunction.GET_CENTRE_TIME_SLOTS) {
					ArrayList<String> timeslots = dbHandler.getCenterTimeslots(currReq.getRecCentre());
					currResp = new ServerResponse(ResponseType.SUCCESS);
					currResp.setTimeslots(timeslots);
					oos.writeObject(currResp);			
				}
				else if (currReq.getFunction() == ServerFunction.MAKE_BOOKING) {
					
					boolean max_cap = dbHandler.isCapMax(currReq.getReservation());
					if(max_cap) {
						// add user to waitlist
						
					}
					else {
						// make booking
						dbHandler.makeBooking(currReq.getReservation(), currReq.getUser());			

					}									
				}
				else if (currReq.getFunction() == ServerFunction.CANCEL_BOOKING) {
					
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
