package org.peak15.stringserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.Selector;

public class UdpConnection {
	public InetSocketAddress connectedAddress;
	
	/**
	 * Binds this connection to the specified port with the specified selector.
	 * @param selector Selector to bind with.
	 * @param udpPort Port to bind to.
	 */
	public void bind(Selector selector, InetSocketAddress udpPort) throws IOException {
		// TODO Auto-generated method stub
	}

	/**
	 * Returns the read from address.
	 * @return The read from address.
	 * @throws IOException
	 */
	public InetSocketAddress readFromAddress() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Reads an object from a connection.
	 * @param fromConnection Connection to read from.
	 * @return
	 */
	public Object readObject(Connection fromConnection) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Close the udp connection.
	 */
	public void close() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Sends an object to an address with a connection.
	 * @param connection Connection to use.
	 * @param object Object to send.
	 * @param address Address to send to.
	 * @return Number of bytes sent.
	 */
	public int send(Connection connection, Object object, SocketAddress address) {
		// TODO Auto-generated method stub
		return -1;
	}

}
