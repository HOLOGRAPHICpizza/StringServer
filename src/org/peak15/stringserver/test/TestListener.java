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
		System.out.println("Recieved " + string + " on " + connection + ".");
		StringServerTest.recieved = string.toString();
	}

}
