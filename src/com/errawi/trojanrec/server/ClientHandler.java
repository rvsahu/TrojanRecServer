package com.errawi.trojanrec.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

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
	 * The integer userId of the user that is logged into the (Android) TrojanRec
	 * client that this thread is handling/communicating with. 
	 */
	private int userId;
	
	/**
	 * 
	 */
	private boolean active;
	
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
		while (true) {
			//
			try {
				//DeliveryInformation info = oHandler.getReadyOrders();
				//send orders to driver
				//System.out.println(TimeFormatter.getTimeString() + " sent driver delivery instructions");
				//oos.writeObject(info);
				//check if info was null
				//if (info == null) {
					//it was, all orders are complete, break
				//	break;
				//}
				//wait for driver to return
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
