package org.peak15.stringserver.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import org.peak15.stringserver.StringServer;

public class StringServerTest {
	public static StringServer server;
	
	public static void main(String[] args) {
		String send = "Test String\n";
		
		//StringServer.debug = true;
		server = new StringServer(new TestListener());
		server.start();
		try {
			server.bind(1337);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Connect to ourself to test.
		InetSocketAddress isa = new InetSocketAddress("localhost", 1337);
		String recieved = null;
		try(SocketChannel sc = SocketChannel.open()) {
			sc.connect(isa);
			ByteBuffer buffer = Charset.forName("US-ASCII").encode(send);
			System.out.println("Sending: " + send);
			sc.write(buffer);
			
			// Now lets listen for it to be echoed back to us.
			buffer.clear();
			sc.read(buffer);
			buffer.flip();
			recieved = Charset.forName("US-ASCII").decode(buffer).toString();
			System.out.println("Recieved: " + recieved);
			
			sc.close();
			Thread.sleep(600);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		server.stop();
		
		if(recieved.equals(send.trim())) {
			System.out.println("\nTest passed!");
		}
		else {
			System.err.println("\nTest failed!");
			System.exit(1);
		}
	}
}
