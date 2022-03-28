package com.errawi.trojanrec.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class TRServerMain  {
	public static final int SOCKET_PORT = 1337;
	
	public static void main(String[] args) {
		/*
		 * 
		 */
		List<ClientHandler> clientHandlers = new ArrayList<>();
		ExecutorService clientExecutor = Executors.newCachedThreadPool();
		DatabaseHandler databaseHandler = new DatabaseHandler();
		
		//declare a server socket for this program, will be initialised later
		ServerSocket serverSocket;
		
		//number of clients connected to the server
		int connectedClients = 0; 
		
		
		//first initialise server socket
		try {
			serverSocket = new ServerSocket(SOCKET_PORT); //if this works enter a perpetual loop
			
			while (true) {
				ClientHandler newClientHandler = new ClientHandler(serverSocket.accept(), databaseHandler);
				clientHandlers.add(newClientHandler);
				clientExecutor.submit(newClientHandler);
				connectedClients += 1;
			}
			//shutdown executor and then wait until terminated
			//clientExecutor.shutdown();
			/*
			while (!(clientExecutor.isTerminated())) {
				Thread.yield();
			}
			*/
		} catch (SocketException se) {
			//TODO: log the exception, printStackTrace() but to a file, etc.
		} catch (IOException ie) {
			//TODO: log the exception, printStackTrace() but to a file, etc.
		}
	}
}