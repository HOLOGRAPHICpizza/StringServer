package org.peak15.stringserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * NIO server wrapper for dealing exclusively in simple strings, i.e. telnet, HTTP, etc...
 * Based on the excellent KryoNet library. (http://code.google.com/p/kryonet/)
 * Released under the New BSD License.
 * 
 * To use StringServer, instantiate this class with a Listener, call start(), then call bind().
 * 
 * @author Michael Craft <mcraft@peak15.org>
 * @author Nathan Sweet <misc@n4te.com>
 */
public class StringServer implements Runnable {
	private boolean running = false;
	private Selector selector;
	private Object updateLock = new Object();
	private ServerSocketChannel serverChannel;
	private Set<Connection> connections = new HashSet<Connection>();
	private int nextConnectionID = 1;
	private Listener listener;
	
	public static boolean debug = false;
	
	private void acceptOperation(SocketChannel socketChannel) {
		Connection connection = new Connection(listener);
		connection.server = this;
		try {
			SelectionKey selectionKey = connection.tcp.accept(selector, socketChannel);
			selectionKey.attach(connection);
			
			int id = nextConnectionID++;
			if(nextConnectionID == -1) nextConnectionID = 1;
			connection.id = id;
			connection.setConnected(true);
			
			connections.add(connection);
			
			connection.notifyConnected();
		} catch(IOException e) {
			connection.close();
			printDbg("Unable to accept connection: " + e);
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
	 * Opens a server on the specified port.
	 * @param port Port to listen on.
	 * @throws IOException if the port could not be bound to.
	 */
	public void bind(int port) throws IOException {
		bind(new InetSocketAddress(port));
	}
	
	/**
	 * Opens a server on the specified InetSocketAddesses.
	 * @param socket Socket to listen on.
	 * @throws IOException if the socket could not be bound to.
	 */
	public void bind(InetSocketAddress socket) throws IOException {
		close();
		synchronized(updateLock) {
			selector.wakeup();
			try {
				serverChannel = selector.provider().openServerSocketChannel();
				serverChannel.socket().bind(socket);
				serverChannel.configureBlocking(false);
				serverChannel.register(selector, SelectionKey.OP_ACCEPT);
				printDbg("Accepting connections on socket: " + socket);
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
        				// Must be a read or write operation.
        				if((ops & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
        					try {
        						// Gobble up all the strings immediately available.
        						while(true) {
        							String string = fromConnection.tcp.readString(fromConnection);
        							if(string == null || string.equals("")) break;
        							printDbg(fromConnection + " received string.");
        							fromConnection.notifyReceived(string);
        						}
        					} catch(IOException e) {
        						printDbg(fromConnection + " warning: " + e.getMessage());
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
	 * Send string to all clients except the one with the specified ID.
	 * @param connectionID Client ID to omit.
	 * @param string String to send.
	 */
	public void sendToAllExcept(int connectionID, String string) {
		for(Connection c : connections) {
			if(c.id != connectionID)
				c.send(string);
		}
	}
	
	/**
	 * Send string to all clients.
	 * @param string String to send.
	 */
	public void sendToAll(String string) {
		for(Connection c : connections) {
			c.send(string);
		}
	}
	
	/**
	 * Send string to specified client.
	 * @param connectionID Client ID to send to.
	 * @param string String to send.
	 */
	public void sendTo(int connectionID, String string) {
		for(Connection c : connections) {
			if(c.id == connectionID) {
				c.send(string);
				break;
			}
		}
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
