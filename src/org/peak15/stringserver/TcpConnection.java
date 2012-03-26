package org.peak15.stringserver;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Handles the actual TCP transactions of a Connection.
 */
public class TcpConnection {
	private static final CharsetEncoder charEncoder = Charset.forName("US-ASCII").newEncoder();
	
	private final ByteBuffer readBuffer, writeBuffer;
	private final CharBuffer charBuffer;
	private SelectionKey selectionKey;
	private final Object writeLock = new Object();
	
	private String currentString = "";
	private Queue<String> readyStrings = new ArrayDeque<String>();
	
	public SocketChannel socketChannel;
	
	/**
	 * Creates a TcpConnection using the given scheme.
	 * @param scheme SerializationScheme to use.
	 */
	public TcpConnection() {
		this.writeBuffer = ByteBuffer.allocate(2048);
		this.charBuffer = CharBuffer.allocate(2048);
		this.readBuffer = ByteBuffer.allocate(2048);
		this.readBuffer.flip();
	}
	
	/**
	 * Returns the first string in the buffer, or null if the buffer is empty.
	 * @param connection Connection to read from.
	 * @return String read, or null if no string is ready.
	 * @throws IOException If string could not be read.
	 */
	public String readString(Connection connection) throws IOException {
		boolean closed = (socketChannel == null);
		int bytesRead = 0;
		if(!closed) {
			// Read bytes immediately available and add them to our buffer.
			readBuffer.clear();
			bytesRead = socketChannel.read(readBuffer);
			readBuffer.flip();
		}
		
		if(bytesRead > 0) {
			currentString += Charset.forName("US-ASCII").decode(readBuffer).toString();
			
			// Split this by newlines and add to the queue all but the last one
			String[] lines = currentString.split("\n");
			if(lines.length > 1) {
				for(int i=0; i < lines.length - 1; i++) {
					readyStrings.add(lines[i].trim());
				}
				currentString = lines[lines.length - 1];
			}
			else if(lines.length == 1 && currentString.endsWith("\n")) {
				readyStrings.add(lines[0].trim());
				currentString = "";
			}
		}
		
		// Return a string from the queue, or null if the queue is empty.
		return readyStrings.poll();
	}
	
	/**
	 * Accepts a connection.
	 * @param selector Selector to use.
	 * @param socketChannel Socket channel to use.
	 * @return SelectionKey for the connection.
	 * @throws IOException
	 */
	public SelectionKey accept(Selector selector, SocketChannel socketChannel) throws IOException {
		try {
			this.socketChannel = socketChannel;
			this.socketChannel.configureBlocking(false);
			Socket socket = this.socketChannel.socket();
			socket.setTcpNoDelay(true);
			
			selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
			
			StringServer.printDbg("Port " + socketChannel.socket().getLocalPort() + " connected to: "
					+ socketChannel.socket().getRemoteSocketAddress());
			
			return selectionKey;
		} catch(IOException e) {
			close();
			throw e;
		}
	}
	
	/**
	 * Closes the connection.
	 */
	public void close() {
		try {
			if(socketChannel != null) {
				socketChannel.close();
				socketChannel = null;
				if(selectionKey != null) selectionKey.selector().wakeup();
			}
		} catch(IOException e) {
			StringServer.printDbg("Unable to close connection: " + e.getMessage());
		}
	}

	/**
	 * Send a string over a connection.
	 * @param connection Connection to send across.
	 * @param string String to send.
	 * @return Number of bytes sent.
	 */
	public int send(Connection connection, String string) throws IOException {
		if(socketChannel == null) throw new SocketException("Connection is closed.");
		synchronized(writeLock) {
			writeBuffer.clear();
			charBuffer.clear();
			
			// Put the string in the buffer.
			charBuffer.put(string);
			charBuffer.flip();
			charEncoder.encode(charBuffer, writeBuffer, true);
			charEncoder.reset();
			writeBuffer.flip();
			
			return socketChannel.write(writeBuffer);
		}
	}
}
