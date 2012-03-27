# StringServer
* * *

## Overview
StringServer is a Java library that provides a clean and simple API for building string-based TCP servers, such as for supporting dumb telnet clients, or serving content with simple protocols like HTTP.

[Javadoc](http://www.peak15.org/stringserver/)

StringServer is derived from the excellent [KryoNet](http://code.google.com/p/kryonet/) library, and is released under the New BSD License. See the LICENSE file for details.

## Example
First, create a class implementing the Listener interface, specifying what the server should do when a client connects, disconnects, and sends data:
```java
import org.peak15.stringserver.Connection;
import org.peak15.stringserver.Listener;

public class ExampleListener implements Listener {
	@Override
	public void connected(Connection connection) {
		MainClass.server.sendToAll(connection.toString() + " has connected.");
	}

	@Override
	public void disconnected(Connection connection) {
		MainClass.server.sendToAll(connection.toString() + " has disconnected.");
	}

	@Override
	public void received(Connection connection, String string) {
		MainClass.server.sendToAllExcept(connection.id, connection.toString() + " says: " + string);
		MainClass.server.sendTo(connection.id, "Your message has been broadcasted.");
	}
}
```

Now, create a server object using your listener, start it, and bind it to a port.
```java
import org.peak15.stringserver.StringServer;

public class MainClass {
	public static StringServer server;
	
	public static void main(String[] args) {
		server = new StringServer(new ExampleListener());
		server.start();
		server.bind(1337);
	}
}
```

And that's it!
