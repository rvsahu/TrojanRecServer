package com.errawi.trojanrec.testing;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class TestRunner {
	public static void main(String[] args) {
		testClientHandler();
	}
	
	public static void testClientHandler() {
		System.out.println("Testing Client Handler class...");
		JUnitCore junit = new JUnitCore();
		junit.addListener(new TextListener(System.out));
		
		Result clResult = junit.run(TestClientHandler.class);
		
		for (Failure failure : clResult.getFailures()) {
			System.out.println(failure.toString());
		}
		
		System.out.print("ClientHandler class ");
		if (clResult.wasSuccessful()) {
			System.out.println("passes all tests.");
		} else {
			System.out.println("fails!");
		}
	}
}
