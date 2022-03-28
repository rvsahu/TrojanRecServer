package com.errawi.trojanrec.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;

import com.errawi.trojanrec.utils.User;

public class ClientHandler extends Thread {
	private Socket socket;

	/**
	 * An output stream to communicate objects to the client over.
	 */
	private ObjectOutputStream oos;

	/**
	 * An input stream to read strings from the client.
	 */
	private BufferedReader br;
	private DatabaseHandler databaseHandler;

	/**
	 * User object representing the User that is logged into the (Android) TrojanRec
	 * client that this thread is handling/communicating with.
	 */
	private User user;
	

    /**
     * Enumerates the functions that the server could need to perform for the client
     */
    private enum ServerFunction {
        CONNECT, LOGIN, GET_PROFILE_INFO, GET_CURRENT_BOOKINGS,
        GET_PREVIOUS_BOOKINGS, GET_WAIT_LIST, GET_CENTRE_TIME_SLOTS, MAKE_BOOKING,
        CANCEL_BOOKING, CANCEL_WAIT_LIST, POLL_FOR_NOTIFICATIONS, CLOSE
    }

    /**
     * Enumerates conditions with which the server could respond to the
     */
    enum ResponseType {
        CONNECTED, SUCCESS, FAIL, CLOSED
    }

    /**
     * ClientRequest is the framework for client-to-server communication. It contains
     * a specific function that the client wishes the server to perform, and the
     * relevant information that the server needs to be able to perform it. This
     * exists as a nest private class because for security reasons we don't really
     * want another part of the program to be able to access/create these.
     */
    private class ClientRequest implements Serializable {
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
    }

    private class ServerResponse implements Serializable {

    }
	
	public ClientHandler(Socket socket, DatabaseHandler databaseHandler) {
		this.socket = socket;
		this.databaseHandler = databaseHandler;
		try {
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			oos = new ObjectOutputStream(socket.getOutputStream());
		} catch (IOException ie) {
			ie.printStackTrace();
		}
	}

	public InetAddress getAddress() {
		return socket.getInetAddress();
	}

	public void run() {
		//accept the first communication
		
		
		while (true) {
			try {
				
				// DeliveryInformation info = oHandler.getReadyOrders();
				// send orders to driver
				// System.out.println(TimeFormatter.getTimeString() + " sent driver delivery
				// instructions");
				// oos.writeObject(info);
				// check if info was null
				// if (info == null) {
				// it was, all orders are complete, break
				// break;
				// }
				// wait for driver to return
				String response = br.readLine();
				if (response.equals("done")) {
					continue;
				} else {
					break;
				}
			} catch (Exception e) {
				System.err.println("General error sending driver deliveries!");
				e.printStackTrace();
				break;
			}
		}
	}
}
