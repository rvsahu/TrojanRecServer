package com.errawi.trojanrec.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class TRServerMain  {
	public static final int SOCKET_PORT = 1337;
	
	public static void main(String[] args) {
		/*
		 *  Thread variables
		 */
		List<ClientHandler> clientHandlers = new ArrayList<>();
		ExecutorService clientExecutor = Executors.newCachedThreadPool();
		DatabaseHandler databaseHandler = new DatabaseHandler();
		
		//declare and initialise a server socket for this program
		ServerSocket ss = new ServerSocket(SOCKET_PORT);
		
		//number of clients connected to the server
		int connectedClients; //number of drivers doing delivery

		DatabaseHandler databaseHandler = new DatabaseHandler();
		//OrderHandler oHandler = new OrderHandler(orderFrom, orderOf, orderTime, HQLocation);
		//OrderHandlerTester.Test(oHandler);
		//initialise dHandlers
		clientHandlers = new ArrayList<ClientHandler>();
		
		try {
			//connect first driver
			ClientHandler ch = new ClientHandler(ss.accept());
			System.out.println("Connection from " + dh.getAddress());
			dh.sendNeededDrivers(totalDrivers - connectedDrivers);
			dHandlers.add(dh);
			
			
			while (connectedDrivers < totalDrivers) {
				//output wait message
				System.out.print("Waiting for " + (totalDrivers - connectedDrivers));
				System.out.println(" more driver(s).");
				//new driver connected
				dh = new DriverHandler(ss.accept(), oHandler);
				connectedDrivers += 1;
				System.out.println("Connection from " + dh.getAddress());
				dHandlers.add(dh);
				sendNeededDrivers(dHandlers, totalDrivers - connectedDrivers);
			}
			System.out.println("Starting service.");
			TimeFormatter.setStartTime();
			oHandler.setStartTime(TimeFormatter.getStartTime());
			//part 3: clients start deliveries
			//set delivery start time
			//add all the DriverHandlers to a thread 
			for (int i = 0; i < dHandlers.size(); i += 1) {
				driverExec.submit(dHandlers.get(i));
			}
			//shutdown executor and then wait until terminated (all deliveries complete)
			driverExec.shutdown();
			while (!(driverExec.isTerminated())) {
				Thread.yield();
			}
		} catch (SocketException se) {
			se.printStackTrace();
		} catch (IOException ie) {
			ie.printStackTrace();
		}
		
		System.out.println(TimeFormatter.getTimeString() + " All orders completed!");
	}
	
	/**
	 * Sends every driver with a handler in dHandlers list the number of drivers needed
	 * before starting delivery as an Integer. When 0 is sent, the drivers start listening
	 * for DeliveryInformation objects
	 * 
	 * @param dHandlers  A list of DriverHandlers handling the connection for each client
	 * @param needed  Number of drivers needed before deliveries 
	 */
	private static void sendNeededDrivers(List<DriverHandler> dHandlers, Integer needed) {
		for (DriverHandler dh : dHandlers) {
			dh.sendNeededDrivers(needed);
		}
	}
}