package es.ucm.fdi.edd.core.erlang;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.ericsson.otp.erlang.OtpNode;

import es.ucm.fdi.edd.core.erlang.model.EddModel;

/**
 * Erlang to Java communication. 
 */
public class Erlang2Java {

	public static final String NODE = "eddjava@localhost";
	public static final String COOKIE = "erlide";
	
	private static final String THREAD_CLIENT_NAME = "EDD-Client";
	private static final String THREAD_SERVER_NAME = "EDD-Server";
	
	/** The Erlang/OTP node. */
	private OtpNode node;
	
	private ErlangClient erlangClient;
	private ErlangServer erlangServer;
	
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
				
				try {
					TimeUnit.SECONDS.sleep(5);
					String buggyCall2 = "merge:mergesort([b,a], fun merge:comp/2)";
					String location2 = "D:/workspace/runtime-tests/EDDSample/src/";
					main.restartDebugger(buggyCall2, location2);
					
					TimeUnit.SECONDS.sleep(5);
					main.stopServer();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				break;

			default:
				System.out.println("You must provide two argument: a buggy call and the location of the erlang source file to debug.");
				break;
		}
	}
	
	public Erlang2Java() {
		//
	}
	
	/**
	 * Initialize the communication.
	 * 
	 * @param buggyCall
	 * 			a buggy call to debug.
	 * @param location
	 * 			the location of the erlang source file to debug.
	 */
	public void initialize(String buggyCall, String location) {
		try {
			node = new OtpNode(NODE);
			node.setCookie(COOKIE);
			erlangClient = new ErlangClient(buggyCall, location, node);
			Thread erlClient = new Thread(erlangClient, THREAD_CLIENT_NAME);
			erlClient.start();
			erlangServer = new ErlangServer();
			Thread erlServer = new Thread(erlangServer, THREAD_SERVER_NAME);
			erlServer.start();
		} catch (IOException e) {
			System.out.println("No se puede iniciar nodo. Has arrancado epmd?");
			e.printStackTrace();
		} 
	}
	
	/**
	 * Returns the Erlang/OTP node.
	 * 
	 * @return node the java node.
	 */
	public OtpNode getNode() {
		return node;
	}	
	
	/**
	 * Stops the server.
	 * 
	 * @throws Exception
	 */
	public void stopServer() throws Exception {
		erlangClient.stopClient();
		TimeUnit.SECONDS.sleep(1);
		erlangServer.stopServer();
	}
	
	/**
	 * Starts the communication.
	 * 
	 * @param buggyCall
	 * 			a buggy call to debug.
	 * @param location
	 * 			the location of the erlang source file to debug.
	 */
	public void restartDebugger(String buggyCall, String location) {
		erlangClient.setBuggyCall(buggyCall);
		erlangClient.setLocation(location);
		erlangClient.restart();
	}
	
	public EddModel getEddModel() {
		return erlangClient.getEddModel();
	}
	
	public void sendAnswer(String reply) {
		erlangClient.setAnswer(reply);
	}
}