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
		List<ClientHandler> clientHandlers = new ArrayList<>(); 
		ExecutorService clientExecutor = Executors.newCachedThreadPool(); 
		DatabaseHandler databaseHandler = new DatabaseHandler();
		
		//declare a server socket for this program, will be initialised later
		ServerSocket serverSocket;
		
		//first initialise server socket 
		try { 
			serverSocket = new ServerSocket(SOCKET_PORT); //if this works enter a perpetual loop
			while (true) { 
				try { 
					Socket newSocket = serverSocket.accept();
					ClientHandler newCH = createClientHandler(newSocket, databaseHandler);
					clientHandlers.add(newCH);
					clientExecutor.submit(newCH); 
				} catch (Exception e) { 
					//TODO: log the exception, printStackTrace() but to a file, etc. 
				}
			} 
		//shutdown executor and then wait until terminated
		//clientExecutor.shutdown(); /* //while (!(clientExecutor.isTerminated())) {
		// Thread.yield();
		} catch (SocketException se) { 
			//TOO: log the exception, printStackTrace() but to a file, etc. 
		} catch (IOException ie) { 
			//TOO: log the exception, printStackTrace() but to a file, etc. 
		} 
	}
	
	private static ClientHandler createClientHandler(Socket socket, DatabaseHandler dbHandler) {
		return new ClientHandler(socket, dbHandler);
	}
}
	 