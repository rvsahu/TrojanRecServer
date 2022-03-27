package com.errawi.trojanrec.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ClientHandler extends Thread {
	private Socket socket;
	private ObjectOutputStream oos;
	private BufferedReader br;
	
	
	public ClientHandler(Socket socket) {
		this.socket = socket;
		try {
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			oos = new ObjectOutputStream(socket.getOutputStream());
		} catch (IOException ie) {
			ie.printStackTrace();
		}
	}
	
	/**
	 * Sends an integer over oos representing the number of drivers needed before
	 * delivery can start. When 0 is sent, the client starts listening for
	 * DeliveryInformation objects
	 * @param needed
	 */
	public void sendNeededDrivers(Integer needed) {
		try {
			oos.writeObject(needed);
		} catch (IOException ie) {
			System.err.println("Error sending needed driver count");
			ie.printStackTrace();
		}
	}
	
	public InetAddress getAddress() {
		return socket.getInetAddress();
	}
	
	public void run() {
		while (true) {
			//get available orders if any, wait until there are
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
			} catch (IOException ie) {
				System.err.println("IO error sending driver deliveries!");
				ie.printStackTrace();
				break;
			} catch (Exception e) {
				System.err.println("General error sending driver deliveries!");
				e.printStackTrace();
				break;
			}
		}
	} 
}
