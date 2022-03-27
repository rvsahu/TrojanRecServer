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
	public static void main(String[] args) {
		/*
		 *  Thread variables
		 */
		List<ClientHandler> cHandlers;
		ExecutorService clientExec;
		
		
		/*
		 *  Server variables
		 */
		ServerSocket ss;
		
		/*
		 * Program Variables
		 */
		//three concurrent-index lists for the schedule
		List<Integer> orderTime; //the time the order takes place
		List<String> orderFrom; //the restaurant being ordered from
		List<String> orderOf; //the item being ordered
		
		int totalDrivers; //number of drivers doing delivery
		
		//read and validate schedule info
		while (true) { //loop until valid file given
			//open a scanner for the schedule file
			try {
				//output message to console
				System.out.println("What is the name of the schedule file?");
				//get the filename from user input to console
				String filename = console.next();
				//create a scanner from the file name
				scheInfo = new Scanner(new File(filename));
			} catch (FileNotFoundException fe) {
				System.out.print("That file does not exist. ");
				continue;
			}
			//initialise lists (and clear them if the previous input was bad)
			orderTime = new ArrayList<Integer>();
			orderFrom = new ArrayList<String>();
			orderOf = new ArrayList<String>();
			boolean scheduleFail = false;
			//parse the scanner and validate input
			while (scheInfo.hasNext()) {
				String order = scheInfo.nextLine();
				String[] orderSplit = order.split(", ");
				if (orderSplit.length != 3) {
					System.out.print("That file is not properly formatted. ");
					scheduleFail = true;
					break;
				}
				//get and validate the ready time
				String timeStr = orderSplit[0];
				int time;
				try {
					time = Integer.parseInt(timeStr);
				} catch (NumberFormatException ne) {
					System.out.print("That file is not properly formatted. ");
					scheduleFail = true;
					break;
				}
				//add to orderTime
				orderTime.add(time);
				//no validation necessary for the other parts
				//get restaurant, add to orderFrom
				orderFrom.add(orderSplit[1]);
				//get menu item, add to orderOf
				orderOf.add(orderSplit[2]);
			}
			if (!scheduleFail) {
				break;
			}
		}
		
		//initialise user location

		//create database handler
		DatabaseHandler databaseHandler = new DatabaseHandler();
		//OrderHandler oHandler = new OrderHandler(orderFrom, orderOf, orderTime, HQLocation);
		//OrderHandlerTester.Test(oHandler);
		//initialise dHandlers
		clientHandlers = new ArrayList<ClientHandler>();		
		
		try {
			//part 2: open socket, wait for enough drivers
			ss = new ServerSocket(3456);
			int connectedDrivers = 0;
			
			//output wait message
			//
			System.out.println("Waiting for drivers...");
			//connect first driver
			DriverHandler dh = new DriverHandler(ss.accept(), oHandler);
			connectedDrivers += 1;
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
			//add all the DriverHandlers to a thread executor
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