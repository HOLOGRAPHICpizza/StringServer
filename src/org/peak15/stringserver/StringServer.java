package org.peak15.stringserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * NIO server wrapper for dealing exclusively in simple strings, i.e. telnet, HTTP, etc...
 * Heavily inspired by the excellent KryoNet (http://code.google.com/p/kryonet/)
 * @author Michael Craft <mcraft@peak15.org>
 */
public class StringServer implements Runnable {
	private boolean running = false;
	private Selector selector;
	private Object updateLock = new Object();
	private ServerSocketChannel serverChannel;
	private UdpConnection udp;
	private Set<Connection> connections = new HashSet<Connection>();
	private int nextConnectionID = 1;
	private Listener listener;
	private Map<Integer, Connection> pendingConnections = new HashMap<Integer, Connection>();
	
	public static boolean debug = false;
	
	private void acceptOperation(SocketChannel socketChannel) {
		Connection connection = new Connection(listener);
		connection.server = this;
		if(udp != null) connection.udp = udp;
		try {
			SelectionKey selectionKey = connection.tcp.accept(selector, socketChannel);
			selectionKey.attach(connection);
			
			int id = nextConnectionID++;
			if(nextConnectionID == -1) nextConnectionID = 1;
			connection.id = id;
			connection.setConnected(true);
			
			if(udp == null)
				connections.add(connection);
			else
				pendingConnections.put(id, connection);
			
			if(udp == null) connection.notifyConnected();
		} catch(IOException e) {
			connection.close();
			printDbg("Unable to accept TCP connection: " + e);
		}
	}
	
	/**
	 * Creates a new server.
	 */
	public StringServer(Listener listener) {
		this.listener = listener;
		
		try {
			this.selector = Selector.open();
		} catch(IOException e) {
			throw new RuntimeException("Error opening selector.", e);
		}
	}
	
	/**
	 * Print to standard output.
	 * @param out Object to print.
	 */
	public static void print(Object obj) {
		System.out.print("StringServer: ");
		System.out.println(obj);
	}
	
	/**
	 * Print an error.
	 * @param out Object to print.
	 */
	public static void printErr(Object obj) {
		System.out.print("StingServer ERROR: ");
		System.err.println(obj);
	}
	
	/**
	 * Print a debug message.
	 * @param out Object to print.
	 */
	public static void printDbg(Object obj) {
		if(debug) {
			System.out.print("StringServer DEBUG: ");
			System.out.println(obj);
		}
	}
	
	/**
	 * Opens a TCP only server on the specified port.
	 * @param tcpPort Port to listen on.
	 * @throws IOException if the port could not be bound to.
	 */
	public void bind(int tcpPort) throws IOException {
		bind(new InetSocketAddress(tcpPort), null);
	}
	
	/**
	 * Opens a TCP and UDP server on the specified ports.
	 * @param tcpPort TCP port to listen on.
	 * @param udpPort UDP port to listen on.
	 * @throws IOException if the ports could not be bound to.
	 */
	public void bind(int tcpPort, int udpPort) throws IOException {
		bind(new InetSocketAddress(tcpPort), new InetSocketAddress(udpPort));
	}
	
	/**
	 * Opens a TCP and UDP server on the specified InetSocketAddesses.
	 * @param tcpPort TCP socket to listen on.
	 * @param udpPort UDP socket to listen on. May be null.
	 * @throws IOException if the ports could not be bound to.
	 */
	public void bind (InetSocketAddress tcpPort, InetSocketAddress udpPort) throws IOException {
		close();
		synchronized(updateLock) {
			selector.wakeup();
			try {
				serverChannel = selector.provider().openServerSocketChannel();
				serverChannel.socket().bind(tcpPort);
				serverChannel.configureBlocking(false);
				serverChannel.register(selector, SelectionKey.OP_ACCEPT);
				printDbg("Accepting connections on port: " + tcpPort + "/TCP");
				if(udpPort != null) {
					udp = new UdpConnection();
					udp.bind(selector, udpPort);
					printDbg("Accepting connections on port: " + tcpPort + "/UDP");
				}
				
			} catch(IOException e) {
				close();
				throw e;
			}
		}
	}
	
	/**
     * Accepts any new connections and reads or writes any pending data for the current connections.
     * @param timeout Wait for up to the specified milliseconds for a connection to be ready to
     * 		process. May be zero to return immediately if there are no connections to process.
     */
	public void update(int timeout) throws IOException {
		// Block to avoid a select while the selector is used to bind the server connection.
		synchronized (updateLock) {}
		
		if(timeout > 0) {
			selector.select(timeout);
		}
		else {
			selector.selectNow();
		}
		
		Set<SelectionKey> keys = selector.selectedKeys();
        synchronized (keys) {
        	Iterator<SelectionKey> iter = keys.iterator();
        	
        	while(iter.hasNext()) {
        		SelectionKey selectionKey = iter.next();
        		iter.remove();
        		try {
        			int ops = selectionKey.readyOps();
        			Connection fromConnection = (Connection) selectionKey.attachment();
        			
        			if(fromConnection != null) {
        				// Must be a TCP read or write operation.
        				if(udp != null && fromConnection.udpRemoteAddress == null) continue;
        				if((ops & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
        					try {
        						// Gobble up all the objects immediately available.
        						while(true) {
        							Object object = fromConnection.tcp.readObject(fromConnection);
        							if(object == null) break;
        							printDbg(fromConnection + " received TCP: " + (object == null ? "null" : object.getClass().getSimpleName()));
        							fromConnection.notifyReceived(object);
        						}
        					} catch(IOException e) {
        						printDbg(fromConnection + " warning: " + e.getMessage());
        						fromConnection.close();
        					}
        				}
        				
        				if ((ops & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
        					try {
        						fromConnection.tcp.writeOperation();
        					} catch(IOException e) {
        						printDbg(fromConnection + " update: " + e.getMessage());
        						fromConnection.close();
        					}
        				}
        				continue;
        			}
        			
        			if((ops & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
        				if(serverChannel == null) continue;
        				try {
        					SocketChannel socketChannel = serverChannel.accept();
        					if(socketChannel != null) acceptOperation(socketChannel);
        				} catch(IOException e) {
        					printDbg("Unable to accept new connection: " + e.getMessage());
        				}
        				continue;
        			}
        			
        			// Must be a UDP read operation.
        			if(udp == null) continue;
        			InetSocketAddress fromAddress = null;
        			try {
        				fromAddress = udp.readFromAddress();
        			} catch(IOException e) {
        				printErr("Error reading UDP data: " + e.getMessage());
        				continue;
        			}
        			if(fromAddress == null) continue;
        			
        			for(Connection connection : connections) {
        				if(fromAddress.equals(connection.udpRemoteAddress)) {
        					fromConnection = connection;
        					break;
        				}
        			}
        			
        			Object object;
        			object = udp.readObject(fromConnection);
        			
        			if(fromConnection != null) {
        				printDbg(fromConnection + " received UDP: " + (object == null ? "null" : object.getClass().getSimpleName()));
        				fromConnection.notifyReceived(object);
        				continue;
        			}
        			printDbg("Ignoring UDP from unregistered address: " + fromAddress);
        		} catch (CancelledKeyException ignored) {
					// Connection is closed.
				}
        	}
        }
	}

	/**
	 * Closes all open connections and the server port(s).
	 */
	public void close() {
		if(connections.size() > 0)
			print("Closing server connections...");
		for(Connection connection : connections)
            connection.close();
		connections = new HashSet<Connection>();
		connections.add(new Connection(null));
		
		if(serverChannel != null) {
			try {
				serverChannel.close();
				print("Server closed.");
			} catch(IOException e) {
				printDbg("Unable to close server.");
			}
			serverChannel = null;
		}
		
		if(udp != null) {
			udp.close();
			udp = null;
		}
		
		// Select one last time to complete closing the socket.
        synchronized (updateLock) {
        selector.wakeup();
		try {
			selector.selectNow();
		} catch (IOException ignored) {}
        }
	}
	
	/**
	 * Returns the current connections.
	 * @return Set of connections, should not be modified.
	 */
	public Set<Connection> getConnections() {
		return connections;
	}
	
	/**
	 * Send object to all clients over TCP except the one with the specified ID.
	 * @param connectionID Client ID to omit.
	 * @param object Object to send. Must be included in this server's SerializationScheme.
	 */
	public void sendToAllExceptTCP(int connectionID, Object object) {
		//TODO: Implement.
	}
	
	/**
	 * Send object to all clients over TCP.
	 * @param object Object to send. Must be included in this server's SerializationScheme.
	 */
	public void sendToAllTCP(Object object) {
		//TODO: Implement.
	}
	
	/**
	 * Send object to specified client over TCP.
	 * @param connectionID Client ID to send to.
	 * @param object Object to send. Must be included in this server's SerializationScheme.
	 */
	public void sendToTCP(int connectionID, Object object) {
		//TODO: Implement.
	}
	
	/**
	 * Continually updates this server until stop() is called.
	 */
	@Override
	public void run() {
		running = true;
		while(running) {
			try {
				update(500);
			} catch(IOException e) {
				printErr("Error updating server connections: " + e.getMessage());
				close();
			}
		}
	}
	
	/**
	 * Starts a new thread that calls run().
	 */
	public void start() {
		new Thread(this, "Server").start();
	}
	
	/**
	 * Closes the server and causes run() to return.
	 */
	public void stop() {
		running = false;
		close();
	}
}
