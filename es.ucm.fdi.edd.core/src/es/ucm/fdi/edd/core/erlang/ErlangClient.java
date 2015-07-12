package es.ucm.fdi.edd.core.erlang;

import java.util.Arrays;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangLong;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpErlangString;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.ericsson.otp.erlang.OtpException;
import com.ericsson.otp.erlang.OtpMbox;
import com.ericsson.otp.erlang.OtpNode;

import es.ucm.fdi.edd.core.exception.EddOTPException;

/**
 * Para probar la comunicación erlang<->java, debes ejecutar la aplicación java y luego abrir una consola erlang para el paso de mensajes.  
 * 
 * Ejemplo:
 * 		cmd> erl -sname console -setcookie erlide
 * 
 * (console@localhost)1> {edd, eddjava@localhost} ! {buggy_call, self()}.
 */
public class ErlangClient implements Runnable {
	
	/** The Erlang/OTP mailbox. */
	private static final String MAILBOX = "edd";
	
	private String buggyCall;
	private String location;
	private OtpMbox mailbox;
	private OtpErlangPid pidServer;

	private String debugTree;
	private Integer questionIndex;
	private boolean isBuggyNode;
	private Integer buggyNodeIndex;
	
	/**
	 * Starts the erlang server client. 
	 * 
	 * @param buggyCall
	 * 			a buggy call to debug.
	 * @param location
	 * 			the location of the erlang source file to debug.
	 * @param node
	 * 			the Erlang/OTP node.
	 */
	public ErlangClient(String buggyCall, String location, OtpNode node) {
		this.buggyCall = buggyCall;
		this.location = location;
		this.mailbox = node.createMbox(MAILBOX);
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		System.out.println("Erlang EDD server running...");
		while (true) {
			try {
				OtpErlangObject message = mailbox.receive();
				System.out.println("<<<<< Received message: " + message);
				if (message instanceof OtpErlangTuple) {
					OtpErlangTuple tuple = (OtpErlangTuple) message;
					int arity = tuple.arity();
					if (tuple.elementAt(0) instanceof OtpErlangAtom) {
						OtpErlangAtom command = (OtpErlangAtom) tuple.elementAt(0);
						if (arity == 2) {
							processMessage(command, tuple.elementAt(1));
						}
						if (arity == 3) {
							processMessage(command, tuple.elementAt(1), tuple.elementAt(2));
						}
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
	 * Process any received message.
	 * 
	 * @param command
	 * 			The command to execute.
	 * @param args
	 * 			The arguments list.
	 * 
	 * @throws OtpException
	 */
	private void processMessage(OtpErlangAtom command, OtpErlangObject... args) throws OtpException {
		String key = command.atomValue();
//		int size = args.length;
		switch(key) {
			case "ready":
				proccessReady(command, args[0]);
				break;
				
			case "dbg_tree":
				processDebugTree(command, args[0]);
				break;
			
			case "question":
				processQuestion(command, args[0], args[1]);
				break;
			
			case "buggy_node":
				processBuggyNode(command, args[0]);
				break;
			
			case "aborted":
				processAborted();
				break;
				
			default:
			OtpErlangTuple response = new OtpErlangTuple(command);
			errorResponse(mailbox.self(), response);
		}
	}
	
	/**
	 * {command, pid}
	 * {ready, <edderlang@localhost>}
	 * 
	 * @param command
	 * @param otpErlangObject
	 */
	private void proccessReady(OtpErlangAtom command, OtpErlangObject otpErlangObject) {
		if (otpErlangObject instanceof OtpErlangPid) {
			OtpErlangPid from = (OtpErlangPid) otpErlangObject;
			isBuggyNode = false;
			buggyNodeIndex = null;
			pidServer =  from;
			sendBuggyCall(from);
		}
		else {
			System.out.println("The 'ready' tuple is malformmed...");
		}
	}

	/**
	 * @param from
	 */
	private void sendBuggyCall(OtpErlangPid from) {
		// Send reply with buggy call and its location
		OtpErlangObject[] reply = new OtpErlangObject[3];
		reply[0] = new OtpErlangAtom("buggy_call");
		reply[1] = new OtpErlangString(buggyCall);
		reply[2] = new OtpErlangString(location);
		sendMessage(from, reply);
	}
	
	/**
	 * {command, json}
	 * {dbg_tree, "{"vertices..."}"}
	 * 
	 * @param command
	 * @param otpErlangObject
	 */
	private void processDebugTree(OtpErlangAtom command, OtpErlangObject otpErlangObject) {
		if (otpErlangObject instanceof OtpErlangString) {
			OtpErlangString dbgTree = (OtpErlangString) otpErlangObject;
			debugTree = dbgTree.stringValue();
		}
		else {
			System.out.println("The 'dbg_tree' tuple is malformmed...");
		}
	}
	
	/**
	 * {command, questionIndex, {tuple}}
	 * {question,53,{...}}
	 * 
	 * @param from
	 * @param question
	 * @param state
	 */
	private void processQuestion(OtpErlangAtom command, OtpErlangObject arg1, OtpErlangObject arg2) {
		if (arg1 instanceof OtpErlangLong && arg2 instanceof OtpErlangTuple) {
			OtpErlangLong qIndex = (OtpErlangLong) arg1;
			OtpErlangTuple tuple = (OtpErlangTuple) arg2;
			if (tuple.arity() == 4) {
				OtpErlangString t1 = (OtpErlangString) tuple.elementAt(0);
				OtpErlangList t2 = (OtpErlangList) tuple.elementAt(1);
				for (OtpErlangObject otpErlangObject : t2) {
					System.out.println(otpErlangObject);
				}
				OtpErlangString t3 = (OtpErlangString) tuple.elementAt(2);
				OtpErlangList t4 = (OtpErlangList) tuple.elementAt(3);
				for (OtpErlangObject otpErlangObject : t4) {
					System.out.println(otpErlangObject.toString());
				}
				
//				try {
//					OtpOutputStream oos = new OtpOutputStream(t1);
//					byte[] bytes = oos.toByteArray();
//					String str1 = new String(bytes, StandardCharsets.UTF_8);
//					InputStream inputStream = new ByteArrayInputStream(bytes); 
////					String str2 = IOUtils.toString(inputStream);
//					String str2 = inputStream.toString();
//					
//					System.out.println("OOS: " + bytes + " :: " + str1 + " :1: " + str2);
//				}
//				catch (Exception e) {
//					e.printStackTrace();
//				}
				
				System.out.println("1: " + t1.stringValue());
				System.out.println("2: " + Arrays.toString(t2.elements()));
				System.out.println("3: " + t3.stringValue());
				System.out.println("4: " + Arrays.toString(t4.elements()));
				
				questionIndex = (int) qIndex.longValue();
//				sendAnswer("n");
			}
			else {
				System.out.println("The tuple is malformmed...");
			}
		}
		else {
			System.out.println("The 'dbg_tree' tuple is malformmed...");
		}
	}

	/**
	 * Send reply with answer
	 * 
	 * @param response
	 * 			the response
	 */
	private void sendAnswer(String response) {
		OtpErlangObject[]  reply = new OtpErlangObject[2];
		reply[0] = new OtpErlangAtom("answer");
		reply[1] = new OtpErlangAtom(response);
//		sendMessage(mailbox.self(), reply);
		sendMessage(pidServer, reply);
	}
	
	/**
	 * {command, number}
	 * {buggy_node, 16}
	 * 
	 * @param command
	 * @param otpErlangObject
	 */
	private void processBuggyNode(OtpErlangAtom command, OtpErlangObject otpErlangObject) {
		if (otpErlangObject instanceof OtpErlangLong) {
			OtpErlangLong buggyNode = (OtpErlangLong) otpErlangObject;
			isBuggyNode = true;
			buggyNodeIndex =  (int) buggyNode.longValue();
			System.out.println("Buggy node: " + buggyNodeIndex);
		}
		else {
			System.out.println("The 'buggy_node' tuple is malformmed...");
		}
	}	

	/**
	 * Finish the communication with the server.
	 * 
	 * @throws OtpException
	 */
	private void processAborted() throws OtpException {
		throw new EddOTPException("Aborted...");
	}
	
	/**
	 * Sends a error message given the source node.
	 * 
	 * Sends:
	 * 		<code>{error, message}</code>
	 * 
	 * @param pidSender
	 * 			The source node.
	 * @param message
	 * 			The error message.
	 */
	private void errorResponse(OtpErlangPid pidSender, OtpErlangTuple message) {
		System.err.println("An unknown error has occurred trying to execute the command: " + message.toString());
		OtpErlangAtom errorAtom = new OtpErlangAtom("error");
		OtpErlangObject[] response = new OtpErlangObject[] {errorAtom, message};
//		sendMessage(pidSender, response);
	}

	/**
	 * Sends a message given the source node.
	 * 
	 * @param pidSender
	 * 			The source node.
	 * @param message
	 * 			The message.
	 */
	private void sendMessage(OtpErlangPid pidSender, OtpErlangObject[] message) {
		System.out.println(">>>>>>>>>> Send message [" + pidSender + "]: " + message);
		OtpErlangTuple tuple = new OtpErlangTuple(message);
		mailbox.send(pidSender, tuple);
	}
	
	//--------------------------------------------------------------------------------------------------------------------------
	
	public String getDebugTree() {
		return debugTree;
	}
	
	public Integer getQuestionIndex() {
		return questionIndex;
	}
	
	public void setAnswer(String reply) {
		sendAnswer(reply);		
	}
	
	public boolean isBuggyNode() {
		return isBuggyNode;
	}
	
	public Integer getBuggyNodeIndex() {
		return buggyNodeIndex;
	}
}