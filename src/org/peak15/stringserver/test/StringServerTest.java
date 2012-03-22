package org.peak15.stringserver.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import org.peak15.stringserver.StringServer;

public class StringServerTest {
	private static String sent = "Test String\n";
	public static String recieved;
	
	public static void main(String[] args) {
		StringServer server = new StringServer(new TestListener());
		server.start();
		try {
			server.bind(1337);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Connect to ourself to test.
		InetSocketAddress isa = new InetSocketAddress("localhost", 1337);
		try(SocketChannel sc = SocketChannel.open()) {
			sc.connect(isa);
			ByteBuffer buffer = Charset.forName("US-ASCII").encode(sent);
			System.out.println("Sending: " + sent);
			sc.write(buffer);
			sc.close();
			Thread.sleep(600);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		server.stop();
		
		if(recieved.equals(sent.trim())) {
			System.out.println("\nTest passed!");
		}
		else {
			System.err.println("\nTest failed!");
			System.exit(1);
		}
	}
}
