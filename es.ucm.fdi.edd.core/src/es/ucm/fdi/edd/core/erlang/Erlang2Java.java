package es.ucm.fdi.edd.core.erlang;

import java.io.IOException;

import com.ericsson.otp.erlang.OtpNode;

public class Erlang2Java {

	private static final String NODE = "cliente@localhost";
	private static final String COOKIE = "erlide";

	public static void main(String[] args) {			
		Erlang2Java main = new Erlang2Java();
		main.initialize();
	}
	
	public void initialize() {
		OtpNode node;
		try {
			node = new OtpNode(NODE);
			node.setCookie(COOKIE);
			Thread cliente = new Thread(new Cliente("Cliente", node), "Cliente");
//			Thread t1 = new Thread(new Mole("Moley", node), "Moley");
			cliente.start();
//			t1.start();
		} catch (IOException e) {
			System.out.println("No se puede iniciar nodo. Has arrancado epmd?");
			System.out.println(e.getMessage());
		}
	}
}
