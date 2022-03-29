package com.errawi.trojanrec.server;

import java.io.IOException;
import java.net.ServerSocket;

public class SocketCloser extends java.lang.Thread {
	private ServerSocket ss;
	
	public SocketCloser(ServerSocket ss) {
		this.ss = ss;
	}
	
	@Override public void run() {
		if (ss != null) {
			try {
				ss.close();
			} catch (IOException ioe) {
				System.err.println("IO Exception closing server socket!");
				ioe.printStackTrace();
			}
		} else {
			System.err.println("Attempting to close null socket!");
		}
	}
}
