package org.peak15.stringserver;

public interface Listener {
	/**
     * Called when the remote end has been connected. This will be invoked before any objects
     * are received by received(). This will be invoked on the same thread as Server.update().
     * This method should not block for long periods as other network activity will not be processed
     * until it returns.
     */
    public void connected(Connection connection);

    /**
     * Called when the remote end is no longer connected.
     * There is no guarantee as to what thread will invoke this method.
     */
    public void disconnected(Connection connection);

    /**
     * Called when an object has been received from the remote end of the connection.
     * This will be invoked on the same thread as Server.update().
     * This method should not block for long periods as other network
     * activity will not be processed until it returns.
     */
    public void received(Connection connection, Object object);

}
