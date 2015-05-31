package es.ucm.fdi.edd.core.erlang;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpErlangRangeException;
import com.ericsson.otp.erlang.OtpErlangString;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.ericsson.otp.erlang.OtpException;
import com.ericsson.otp.erlang.OtpMbox;
import com.ericsson.otp.erlang.OtpNode;

/**
 * Ejemplo:
 * 		cmd> erl -sname console -setcookie erlide
 * 		cmd> erl -sname edderlang -setcookie erlide -run edd_jserver start -noshell -s erlang halt
 * 
 * (console@localhost)1> {edd, eddjava@localhost} ! {self(), {move_worm, "Moley", 10}}.
 */
public class ErlangServer implements Runnable {
	
	private static final String MAILBOX = "edd";
	
	private OtpMbox mailbox; 
	
	/**
	 * @param name
	 * @param node
	 */
	public ErlangServer(String name, OtpNode node) {
		this.mailbox = node.createMbox(MAILBOX);
	}

	@Override
	public void run() {
		System.out.println("Erlang EDD server running...");
		while (true) {
			try {
				OtpErlangObject mensaje = mailbox.receive();
				if (mensaje instanceof OtpErlangTuple) {
					OtpErlangTuple tuple = (OtpErlangTuple) mensaje;
					if (tuple.arity() == 2 && 
						tuple.elementAt(0) instanceof OtpErlangPid && 
						tuple.elementAt(1) instanceof OtpErlangTuple) {
						OtpErlangPid pidSender = (OtpErlangPid) tuple.elementAt(0);
						OtpErlangTuple response = (OtpErlangTuple) tuple.elementAt(1);
						processMessage(pidSender, response);
					}
				}
			} catch (OtpException e) {
				System.out.println("Se ha producido un error: "	+ e.getMessage());			
				System.out.println("El proceso cliente ha finalizado");
				mailbox.close();
			}
		}
	}

	/**
	 * @param pidSender
	 * @param response
	 * @throws OtpErlangRangeException
	 */
	private void processMessage(OtpErlangPid pidSender, OtpErlangTuple response) throws OtpErlangRangeException {
		if (response.arity() > 1 && response.elementAt(0) instanceof OtpErlangAtom) {
			OtpErlangAtom operation = (OtpErlangAtom) response.elementAt(0);
			switch(operation.atomValue()) {
				case "buggy_call":
					processBuggyCall(pidSender, response);
					break;
				default:
					errorResponse(pidSender, response);
			}
		} else {
			errorResponse(pidSender, response);
		}
	}
	
	/**
	 * @param pidSender
	 * @param response
	 */
	private void processBuggyCall(OtpErlangPid pidSender, OtpErlangTuple response) {
		if (response.arity() == 2) {
			String a = ((OtpErlangString) response.elementAt(1)).stringValue();
			String b = ((OtpErlangString) response.elementAt(2)).stringValue();
			System.out.println(pidSender + ": " + a + ", " + b);
			
			// Construimos la respuesta y la enviamos
			OtpErlangObject[] reply = new OtpErlangObject[2];
			reply[0] = new OtpErlangString(a);
			reply[1] = new OtpErlangString(b);
			OtpErlangTuple myTuple = new OtpErlangTuple(reply);
			sendMessage(pidSender, myTuple);
		} else {
			errorResponse(pidSender, response);
		}
	}
	
	/**
	 * @param pidSender
	 * @param msg
	 */
	private void errorResponse(OtpErlangPid pidSender, OtpErlangTuple msg) {
		OtpErlangAtom errorAtom = new OtpErlangAtom("error");
		OtpErlangTuple response = new OtpErlangTuple(new OtpErlangObject[] {errorAtom, msg});
		sendMessage(pidSender, response);
	}

	/**
	 * @param pidSender
	 * @param msg
	 */
	private void sendMessage(OtpErlangPid pidSender, OtpErlangObject msg) {
		OtpErlangTuple tuple = new OtpErlangTuple(new OtpErlangObject[] {mailbox.self(), msg});
		mailbox.send(pidSender, tuple);
	}
}