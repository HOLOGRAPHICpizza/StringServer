package org.peak15.stringserver.test;

import org.peak15.stringserver.Connection;
import org.peak15.stringserver.Listener;

public class TestListener implements Listener {

	@Override
	public void connected(Connection connection) {
		// :D
	}

	@Override
	public void disconnected(Connection connection) {
		// :D
	}

	@Override
	public void received(Connection connection, String string) {
		if(string.startsWith("ALL"))
			StringServerTest.server.sendToAll(string);
		else if(string.startsWith("EXCEPT"))
			StringServerTest.server.sendToAllExcept(connection.id, string);
		else if(string.startsWith("ONE"))
			StringServerTest.recieved1 = string;
		else if(string.startsWith("TWO")) {
			StringServerTest.recieved2 = string;
			StringServerTest.notHaveTwo = false;
		}
		else if(string.startsWith("THREE")) {
			StringServerTest.recieved3 = string;
			StringServerTest.notDone = false;
		}
	}

}
