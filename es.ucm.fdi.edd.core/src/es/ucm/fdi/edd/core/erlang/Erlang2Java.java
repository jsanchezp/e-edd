package es.ucm.fdi.edd.core.erlang;

import java.io.IOException;

import com.ericsson.otp.erlang.OtpNode;

/**
 * Erlang to Java communication. 
 */
public class Erlang2Java {

	private static final String NODE = "eddjava@localhost";
	private static final String COOKIE = "erlide";
	private static final String THREAD_NAME = "erlServer";
	
	private OtpNode node;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int params = args.length;
		switch (params) {
			case 0:
				System.out.println("The argument list can not be empty.");
				break;
				
			case 2:
				String buggyCall = args[0];
				String location = args[1];
				Erlang2Java main = new Erlang2Java();
				main.initialize(buggyCall, location);
				break;

			default:
				System.out.println("You must provide two argument: buggy call and its location");
				break;
		}
	}
	
	/**
	 * Initialize the communication.
	 * 
	 * @param buggyCall
	 * @param location 
	 */
	public void initialize(String buggyCall, String location) {
		try {
			node = new OtpNode(NODE);
			node.setCookie(COOKIE);
			Thread erlServer = new Thread(new ErlangServer(buggyCall, location, node), THREAD_NAME);
			erlServer.start();
		} catch (IOException e) {
			System.out.println("Can't load erlang module");
			System.out.println("No se puede iniciar nodo. Has arrancado epmd?");
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the node.
	 * 
	 * @return node
	 */
	public OtpNode getNode() {
		return node;
	}
}