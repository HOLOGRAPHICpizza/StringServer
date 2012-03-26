package org.peak15.stringserver.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class TestClient implements Runnable {
	private static Thread thread;
	
	public static String recieved1 = null;
	public static String recieved2 = null;
	
	/**
	 * Starts a new thread that calls run().
	 */
	public static void start() {
		thread = new Thread(new TestClient(), "Test Client");
		thread.start();
	}
	
	@SuppressWarnings("deprecation")
	public static void stop() {
		thread.stop();
	}
	
	@Override
	public void run() {
		InetSocketAddress isa = new InetSocketAddress("localhost", 1337);
		try(SocketChannel sc = SocketChannel.open()) {
			// Connect
			sc.connect(isa);
			
			// Listen for ALL
			ByteBuffer buffer = ByteBuffer.allocate(StringServerTest.sendALL.length());
			sc.read(buffer);
			buffer.flip();
			recieved1 = Charset.forName("US-ASCII").decode(buffer).toString();
			System.out.println("Second - Recieved: " + recieved1);
			
			// Send EXCEPT
			buffer = Charset.forName("US-ASCII").encode(StringServerTest.sendEXCEPT);
			System.out.println("Second - Sending: " + StringServerTest.sendEXCEPT);
			sc.write(buffer);
			
			// Listen for nothing
			buffer.clear();
			sc.read(buffer);
			buffer.flip();
			recieved2 = Charset.forName("US-ASCII").decode(buffer).toString();
			System.out.println("Second - Recieved: " + recieved2);
			
			sc.close();
		} catch (IOException e) {
			//e.printStackTrace();
		}
	}

}
