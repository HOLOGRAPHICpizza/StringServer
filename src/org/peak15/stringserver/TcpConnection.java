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

/**
 * Handles the actual TCP transactions of a Connection.
 */
public class TcpConnection {
	private static final CharsetEncoder charEncoder = Charset.forName("US-ASCII").newEncoder();
	
	private final ByteBuffer readBuffer, writeBuffer;
	private final CharBuffer charBuffer;
	private SelectionKey selectionKey;
	private final Object writeLock = new Object();
	
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
	 * Read a string from a connection.
	 * @param connection Connection to read from.
	 * @return String read.
	 * @throws IOException If string could not be read.
	 */
	public String readString(Connection connection) throws IOException {
		if(socketChannel == null) throw new SocketException("Connection is closed.");
		
		readBuffer.compact();
		// Grab all bytes immediately available up to the limit of the buffer.
		int bytesRead = socketChannel.read(readBuffer);
		readBuffer.flip();
		if(bytesRead == -1) throw new SocketException("Connection is closed.");
		if(bytesRead < 1) return null;
		
		return Charset.forName("US-ASCII").decode(readBuffer).toString().trim();
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
