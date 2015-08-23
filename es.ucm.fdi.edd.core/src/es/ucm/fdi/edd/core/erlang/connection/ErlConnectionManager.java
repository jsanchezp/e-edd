package es.ucm.fdi.edd.core.erlang.connection;

import java.util.Observable;

/**
 * The {@link ErlConnectionManager} will notify its subscribers upon connection/disconnection.
 */
public class ErlConnectionManager extends Observable {

	private boolean clientConnected;
	private boolean serverConnected;

	private static final ErlConnectionManager instance = new ErlConnectionManager();

	/**
	 * 
	 */
	private ErlConnectionManager() {
		// empty
	}

	/**
	 * @return
	 */
	public static ErlConnectionManager getInstance() {
		return instance;
	}

	/**
	 * @return
	 */
	public boolean isClientConnected() {
		return clientConnected;
	}
	
	/**
	 * @return
	 */
	public boolean isServerConnected() {
		return serverConnected;
	}

	/**
	 * 
	 */
	public void clientConnect() {
		clientConnected = true;
		setChanged();
		notifyObservers();
	}
	
	/**
	 * 
	 */
	public void serverConnect() {
		serverConnected = true;
		setChanged();
		notifyObservers();
	}


	/**
	 * 
	 */
	public void clientDisconnect() {
		clientConnected = false;
		setChanged();
		notifyObservers();
	}
	
	/**
	 * 
	 */
	public void serverDisconnect() {
		serverConnected = false;
		setChanged();
		notifyObservers();
	}
}