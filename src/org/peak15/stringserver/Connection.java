package org.peak15.stringserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
/**
 * Represents a connection to the server.
 */
public class Connection {
	public TcpConnection tcp;
	public StringServer server;
	public int id;
	
	private boolean isConnected;
	private Listener listener;
	
	/**
	 * Creates a new connection with the given listener.
	 * @param listener Listener to use for the new connection.
	 */
	public Connection(Listener listener) {
		this.listener = listener;
		tcp = new TcpConnection();
	}
	
	/**
	 * Sends the object over the network.
	 * @param object Object to send.
	 * @return Number of bytes sent.
	 */
	public int send(Object object) {
		if(object == null) throw new IllegalArgumentException("object cannot be null.");
		try {
			int length = tcp.send(this, object);
			StringServer.printDbg(this + " sent: " +
					(object == null ? "null" : object.getClass().getSimpleName()) + "(" + length + ")");
			return length;
		} catch(IOException e) {
			StringServer.printDbg(this + " unable to send: " + e.getMessage());
			close();
			return 0;
		}
	}
	
	/**
     * Returns true if this connection is connected to the remote end.
     * Note that a connection can become disconnected at any time.
     * @return True if connected, false otherwise.
     */
    public boolean isConnected () {
    	return isConnected;
    }
    
    /**
     * Set the connection status.
     * @param connected Connection status.
     */
    public void setConnected(boolean connected) {
    	isConnected = connected;
    }
	
    /**
     * Get the remote address.
     * @return the IP address and port of the remote end of the connection, or null if this connection is not connected.
     */
    public InetSocketAddress getRemoteAddress() {
    	SocketChannel socketChannel = tcp.socketChannel;
    	if(socketChannel != null) {
    		Socket socket = tcp.socketChannel.socket();
    		if(socket != null) {
    			return (InetSocketAddress) socket.getRemoteSocketAddress();
    		}
    	}
    	return null;
    }
    
	/**
	 * Closes the connection.
	 */
	public void close() {
		boolean wasConnected = isConnected;
		isConnected = false;
		tcp.close();
		if(wasConnected) {
			listener.disconnected(this);
			StringServer.print(this + " disconnected.");
		}
		setConnected(false);
	}
	
	/**
	 * Notify the listener of the received object.
	 * @param object Object to pass along.
	 */
	public void notifyReceived(Object object) {
		listener.received(this, object);
	}
	
	/**
	 * Notify the listener of the connection.
	 */
	public void notifyConnected() {
		listener.connected(this);
	}
	
	public String toString() {
		return "Connection " + Integer.toString(id);
	}
}
