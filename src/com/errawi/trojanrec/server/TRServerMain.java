package com.errawi.trojanrec.server;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executors;

import com.errawi.trojanrec.utils.NotificationBank;

import java.util.concurrent.ExecutorService;

public class TRServerMain {
	public static final int SOCKET_PORT = 4000;
	
	/**
	 * CL code to kill anything listening on the port
	 * @param args
	 */
	private static final String KILL_COMMAND = "kill -9 $(lsof -t -i:" + SOCKET_PORT + ")";
	
	public static void main(String[] args) {
		System.out.println("Killing any existing processes on port " + SOCKET_PORT + ".");
		/*
		try {
			Process p = Runtime.getRuntime().exec(KILL_COMMAND);
			p.waitFor();
			System.out.println("Processes killed!");
			p.destroy();
		} catch (IOException ioe) {
			System.err.println("IO exception killing existing processes on port " + SOCKET_PORT +"!");
			ioe.printStackTrace();
			System.exit(-1);
		} catch (InterruptedException ie) {
			System.err.println("Interrupted while killing existing processes on port " + SOCKET_PORT +"!");
		}
		*/
		System.out.println("Creating notification bank...");
		NotificationBank notifBank = new NotificationBank();
		System.out.println("Initialising data handlers...");
		List<ClientHandler> clientHandlers = new ArrayList<>(); 
		ExecutorService clientExecutor = Executors.newCachedThreadPool(); 
		DatabaseHandler databaseHandler = new DatabaseHandler(notifBank);
		
		//declare a server socket for this program, will be initialised later
		ServerSocket serverSocket = null;
		
		//first initialise server socket 
		try {
			System.out.println("Initialising server socket...");
			serverSocket = new ServerSocket(SOCKET_PORT); //if this works enter a perpetual loop
			//add a shutdown hook for this program to make sure socket is always closed
			Runtime.getRuntime().addShutdownHook(new SocketCloser(serverSocket));
		} catch (SocketException se) {
			System.err.println("Socket exception while initialising server socket!");
			se.printStackTrace();
			closeSocket(serverSocket);
			System.exit(-1);
			//TODO: log this to a file
		} catch (IOException ioe) {
			System.err.println("IO exception while initialising server socket!");
			ioe.printStackTrace();
			closeSocket(serverSocket);
			System.exit(-1);
			//TODO: log this to a file
		} 
		int connections = 0; //keep track of connection account, used to assign a debug id
		System.out.println("Listening for connections..."); //TODO: output this to a log file too
		while (serverSocket != null) { 
			try {
				Socket newSocket = serverSocket.accept();
				ClientHandler newCH = createClientHandler(newSocket, databaseHandler, notifBank, ++connections);
				clientHandlers.add(newCH);
				clientExecutor.submit(newCH); 
			} catch (IOException ioe) {
				System.err.println("IO Exception accepting new socket connection!");
				ioe.printStackTrace();
				closeSocket(serverSocket);
				System.exit(-1);
			}
		}
		
		System.out.println("Shouldn't ever really get here, but closing socket");
		closeSocket(serverSocket);
	}
	
	private static ClientHandler createClientHandler(Socket socket, DatabaseHandler dbHandler, NotificationBank notifBank, int id) {
		System.out.println("New client connection!"); //TODO: output to a log file too
		return new ClientHandler(socket, dbHandler, notifBank, id);
	}
	
	private static void closeSocket(ServerSocket serverSocket) {
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException ioe) {
				System.err.println("IO Exception closing server socket!");
				ioe.printStackTrace();
			}
		} else {
			System.err.println("Attempting to close null socket!");
		}
	}
}
	 