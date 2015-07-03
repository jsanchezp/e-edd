package es.ucm.fdi.edd.core.erlang;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangLong;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpErlangString;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.ericsson.otp.erlang.OtpException;
import com.ericsson.otp.erlang.OtpMbox;
import com.ericsson.otp.erlang.OtpNode;

/**
 * Para probar la comunicación erlang<->java, debes ejecutar la aplicación java y luego abrir una consola erlang para el paso de mensajes.  
 * 
 * Ejemplo:
 * 		cmd> erl -sname console -setcookie erlide
 * 
 * (console@localhost)1> {java, cliente@localhost} ! {buggy_call, self()}.
 */
public class ErlangServer implements Runnable {
	
	private static final String MAILBOX = "edd";
	
	private String buggyCall;
	private String location;
	private OtpMbox mailbox;
	
	/**
	 * 
	 * @param buggyCall
	 * @param location
	 * @param node
	 */
	public ErlangServer(String buggyCall, String location, OtpNode node) {
		this.buggyCall = buggyCall;
		this.location = location;
		this.mailbox = node.createMbox(MAILBOX);
	}

	@Override
	public void run() {
		System.out.println("Erlang EDD server running...");
		while (true) {
			try {
				OtpErlangObject message = mailbox.receive();
				if (message instanceof OtpErlangTuple) {
					OtpErlangTuple tuple = (OtpErlangTuple) message;
					if (tuple.arity() == 2 && tuple.elementAt(0) instanceof OtpErlangAtom && tuple.elementAt(1) instanceof OtpErlangPid) {
						OtpErlangAtom command = (OtpErlangAtom) tuple.elementAt(0);
						OtpErlangPid from = (OtpErlangPid) tuple.elementAt(1);
						processMessage(command, from);
					}
					
					if (tuple.arity() == 2 && tuple.elementAt(0) instanceof OtpErlangAtom && tuple.elementAt(1) instanceof OtpErlangString) {
						OtpErlangAtom command = (OtpErlangAtom) tuple.elementAt(0);
						OtpErlangString dbg_tree = (OtpErlangString) tuple.elementAt(1);
						processMessage(command, dbg_tree);
					}
				}
			} catch (OtpException e) {
				System.out.println("Se ha producido un error: "	+ e.getMessage());			
				System.out.println("El proceso 'ErlangServer' ha finalizado");
				e.printStackTrace();
				mailbox.close();
			}
		}
	}

	/**
	 * Process received message.
	 * 
	 * @param from
	 * @param command
	 * 
	 * @throws OtpException
	 */
	private void processMessage(OtpErlangAtom command, OtpErlangObject obj) throws OtpException {
		String cmd = command.atomValue();
		System.out.println("Command received: [" + obj + "]" + cmd);
		switch(cmd) {
			case "ready":
				break;
			
			case "buggy_call":
				processBuggyCall((OtpErlangPid)obj, command);
				break;
			
			case "question":
//				processQuestion((OtpErlangPid)obj, command);
				break;
			
			case "buggy_node":
//				processBuggyNode((OtpErlangString)obj, command);
				break;
			
			case "aborted":
				processAborted();
				break;
				
			default:
//				errorResponse(pidSender, response);
				System.err.println("An unknown error has occurred trying to execute the command: " + cmd);
		}
	}
	
	/**
	 * Send reply with buggy call and its location.
	 * 
	 * @param command
	 * @param from
	 */
	private void processBuggyCall(OtpErlangPid from, OtpErlangAtom command) {
		OtpErlangObject[] reply = new OtpErlangObject[3];
		reply[0] = command;
		reply[1] = new OtpErlangString(buggyCall);
		reply[2] = new OtpErlangString(location);
		OtpErlangTuple tuple = new OtpErlangTuple(reply);
		mailbox.send(from, tuple);
	}
	
	/**
	 * @param from
	 * @param command
	 * @param dbg_tree
	 */
	private void processBuggyNode(OtpErlangPid from, OtpErlangLong buggyNode) {
		System.out.println("Buggy node: " + buggyNode.longValue());
	}
	
	/**
	 * @param from
	 * @param question
	 * @param state
	 */
	private void processQuestion(OtpErlangPid from, OtpErlangLong question, OtpErlangTuple state) {
		System.out.println("Question: " + question.longValue());
		System.out.println("State: " + state.toString() );
		
		OtpErlangObject[] reply = new OtpErlangObject[2];
		reply[0] = new OtpErlangAtom("answer");
		reply[1] = new OtpErlangAtom("n");
		OtpErlangTuple tuple = new OtpErlangTuple(reply);
		mailbox.send(from, tuple);
	}
	
	/**
	 * Finish the communication.
	 * 
	 * @throws OtpException
	 */
	private void processAborted() throws OtpException {
		System.out.println("Aborted.");
		throw new EDDException("Aborted...");
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

/**
 * EDD Exception.
 */
class EDDException extends OtpException {
	
	private static final long serialVersionUID = 1L;

	public EDDException() {
		super();
	}

	public EDDException(String message) {
		super(message);
	}

	public EDDException(Throwable cause) {
//		super(cause);
	}

	public EDDException(String message, Throwable cause) {
//		super(message, cause);
	}

	public EDDException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
//		super(message, cause, enableSuppression, writableStackTrace);
	}
}