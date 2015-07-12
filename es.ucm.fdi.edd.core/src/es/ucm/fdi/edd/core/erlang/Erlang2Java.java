package es.ucm.fdi.edd.core.erlang;

import java.io.IOException;

import com.ericsson.otp.erlang.OtpNode;

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
				break;

			default:
				System.out.println("You must provide two argument: a buggy call and the location of the erlang source file to debug.");
				break;
		}
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
	
	public boolean isLoaded() {
		return erlangServer.isLoaded();
	}
	
	public String getOutput() {
		return erlangServer.getOutput();
	}

	public String getDebugTree() {
		return erlangClient.getDebugTree();
	}
	
	public Integer getQuestionIndex() {
		return erlangClient.getQuestionIndex();
	}
	
	public void setAnswer(String reply) {
		erlangClient.setAnswer(reply);
	}
	
	public boolean isBuggyNode() {
		return erlangClient.isBuggyNode();
	}
	
	public Integer getBuggyNodeIndex() {
		return erlangClient.getBuggyNodeIndex();
	}
}