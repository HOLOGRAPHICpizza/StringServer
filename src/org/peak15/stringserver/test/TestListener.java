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
		else
			StringServerTest.server.sendToAllExcept(connection.id, string);
	}

}
