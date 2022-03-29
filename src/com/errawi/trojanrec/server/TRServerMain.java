package com.errawi.trojanrec.server;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class TRServerMain {
	public static final int SOCKET_PORT = 1337;
	public static void main(String[] args) { 
		System.out.println("Initialising data handlers...");
		List<ClientHandler> clientHandlers = new ArrayList<>(); 
		ExecutorService clientExecutor = Executors.newCachedThreadPool(); 
		DatabaseHandler databaseHandler = new DatabaseHandler();
		
		//declare a server socket for this program, will be initialised later
		ServerSocket serverSocket = null;
		
		//first initialise server socket 
		try {
			System.out.println("Initialising server socket...");
			serverSocket = new ServerSocket(SOCKET_PORT); //if this works enter a perpetual loop
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
		System.out.println("Listening for connections..."); //TODO: output this to a log file too
		while (serverSocket != null) { 
			try {
				Socket newSocket = serverSocket.accept();
				ClientHandler newCH = createClientHandler(newSocket, databaseHandler);
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
	
	private static ClientHandler createClientHandler(Socket socket, DatabaseHandler dbHandler) {
		System.out.println("New client connection!"); //TODO: output to a log file too
		return new ClientHandler(socket, dbHandler);
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
	 