package es.ucm.fdi.edd.core.erlang;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangLong;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpErlangRangeException;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.ericsson.otp.erlang.OtpException;
import com.ericsson.otp.erlang.OtpMbox;
import com.ericsson.otp.erlang.OtpNode;

/**
 * Ejemplo:
 * 		cmd> erl -sname console -setcookie erlide
 * 
 * (console@localhost)1> {java, cliente@localhost} ! {self(), {move_worm, "Moley", 10}}.
 */
public class Cliente implements Runnable {
	
	private static final String MAILBOX = "java";
	
	private OtpMbox mailbox; 
	
	public Cliente(String name, OtpNode node) {
		this.mailbox = node.createMbox(MAILBOX);
	}

	@Override
	public void run() {
		System.out.println("Proceso cliente iniciado.");
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

	private void processMessage(OtpErlangPid pidSender, OtpErlangTuple response) throws OtpErlangRangeException {
		if (response.arity() > 1 && response.elementAt(0) instanceof OtpErlangAtom) {
			OtpErlangAtom operation = (OtpErlangAtom) response.elementAt(0);
			switch(operation.atomValue()) {
				case "move_worm":
					processMoveWorm(pidSender, response);
					break;
				case "move_snake":
					break;
				case "move_mole":
					break;
				default:
					errorResponse(pidSender, response);
			}
		} else {
			errorResponse(pidSender, response);
		}
	}

	//{PidWorm, {move_worm, WormName, To}}
	private void processMoveWorm(OtpErlangPid pidSender, OtpErlangTuple response) throws OtpErlangRangeException {
		if (response.arity() == 3) {
			OtpErlangAtom wormName = ((OtpErlangAtom) response.elementAt(1));
			Long to = ((OtpErlangLong) response.elementAt(2)).longValue();
			System.out.println(pidSender + ": " + wormName + ", " + to);
			switch (wormName.atomValue()) {
				case "jim":
					break;
				case "swarley":
					break;
				default:
					break;
			}
		} else {
			errorResponse(pidSender, response);
		}
	}
	
	private void errorResponse(OtpErlangPid pidSender, OtpErlangTuple msg) {
		OtpErlangAtom errorAtom = new OtpErlangAtom("error");
		OtpErlangTuple response = new OtpErlangTuple(new OtpErlangObject[] {
				errorAtom, msg });
		sendMessage(pidSender, response);
	}

	private void sendMessage(OtpErlangPid pidSender, OtpErlangObject msg) {
		OtpErlangTuple tuple = new OtpErlangTuple(new OtpErlangObject[] {
				mailbox.self(), msg });
		mailbox.send(pidSender, tuple);
	}
}