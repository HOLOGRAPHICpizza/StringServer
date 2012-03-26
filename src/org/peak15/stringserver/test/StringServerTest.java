package org.peak15.stringserver.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import org.peak15.stringserver.StringServer;

/**
 * Test plan:
 * Server echos to all when a string starts with ALL, and to all except the sender when string starts with EXCEPT
 * 
 * Main Client						Second Client
 * 									Listen
 * Send ALL
 * Listen
 * Receive ALL						Receive ALL
 * Listen							Send EXCEPT
 * Receive EXCEPT					Listen
 * 
 * Verify that both received ALL, and ONLY Main Client received EXCEPT.
 * 
 */
public class StringServerTest {
	public static StringServer server;
	
	public static String sendALL = "ALL Sent to all.\n";
	public static String sendEXCEPT = "EXCEPT Sent to all but the sender.\n";
	public static String recieved1 = null;
	public static String recieved2 = null;
	
	public static void main(String[] args) {
		//StringServer.debug = true;
		server = new StringServer(new TestListener());
		server.start();
		try {
			server.bind(1337);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Start our second client
		TestClient.start();
		
		// Main Client
		InetSocketAddress isa = new InetSocketAddress("localhost", 1337);
		try(SocketChannel sc = SocketChannel.open()) {
			// Wait for Second Client to ramp up
			Thread.sleep(600);
			
			// Connect
			sc.connect(isa);
			
			// Send ALL
			ByteBuffer buffer = Charset.forName("US-ASCII").encode(sendALL);
			System.out.println("Main - Sending: " + sendALL);
			sc.write(buffer);
			
			// Now lets listen for it to be echoed back to us.
			buffer.clear();
			sc.read(buffer);
			buffer.flip();
			recieved1 = Charset.forName("US-ASCII").decode(buffer).toString();
			System.out.println("Main - Recieved: " + recieved1);
			
			// Listen now for EXCEPT
			buffer = ByteBuffer.allocate(sendEXCEPT.length());
			sc.read(buffer);
			buffer.flip();
			recieved2 = Charset.forName("US-ASCII").decode(buffer).toString();
			System.out.println("Main - Recieved: " + recieved2);
			
			sc.close();
			Thread.sleep(600);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		TestClient.stop();
		server.stop();
		
		if(recieved1.equals(sendALL.trim()) && recieved2.equals(sendEXCEPT.trim())
				&& TestClient.recieved1.equals(sendALL.trim()) && (TestClient.recieved2 == null)) {
			System.out.println("\nTest passed!");
		}
		else {
			System.err.println("\nTest failed!");
			System.exit(1);
		}
	}
}
