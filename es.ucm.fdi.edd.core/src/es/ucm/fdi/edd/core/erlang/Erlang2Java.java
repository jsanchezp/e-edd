package es.ucm.fdi.edd.core.erlang;

import java.io.IOException;

import com.ericsson.otp.erlang.OtpNode;

public class Erlang2Java {

	private static final String NODE = "eddjava@localhost";
	private static final String COOKIE = "erlide";
	
	private static final String THREAD_NAME = "erlServer";

	public static void main(String[] args) {			
		Erlang2Java main = new Erlang2Java();
		main.initialize();
	}
	
	public void initialize() {
		OtpNode node;
		try {
			node = new OtpNode(NODE);
			node.setCookie(COOKIE);
			Thread erlServer = new Thread(new ErlangServer(THREAD_NAME, node), THREAD_NAME);
			erlServer.start();
		} catch (IOException e) {
			System.out.println("No se puede iniciar nodo. Has arrancado epmd?");
			e.printStackTrace();
		}
	}
}
