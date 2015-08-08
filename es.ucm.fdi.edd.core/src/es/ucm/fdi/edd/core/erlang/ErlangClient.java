package es.ucm.fdi.edd.core.erlang;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangLong;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpErlangString;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.ericsson.otp.erlang.OtpException;
import com.ericsson.otp.erlang.OtpMbox;
import com.ericsson.otp.erlang.OtpNode;

import es.ucm.fdi.edd.core.erlang.model.DebugTree;
import es.ucm.fdi.edd.core.erlang.model.EddModel;
import es.ucm.fdi.edd.core.erlang.model.EddState;

/**
 * Para probar la comunicación erlang<->java, debes ejecutar la aplicación java y luego abrir una consola erlang para el paso de mensajes.  
 * 
 * Ejemplo:
 * 		cmd> erl -sname console -setcookie erlide
 * 
 * (console@localhost)1> {edd, eddjava@localhost} ! {buggy_call, self()}.
 * (console@localhost)2> {edd, eddjava@localhost} ! {question, 16, none}.
 */
public class ErlangClient implements Runnable, AutoCloseable {
	
	/** The Erlang/OTP mailbox. */
	private static final String MAILBOX = "edd";
	
	private String buggyCall;
	private String location;
	private OtpNode node;
	private OtpMbox mailbox;
	
	private OtpErlangPid pidServer;
	/** state: none | {vertices, correct, notCorrect, unknown} */
	private OtpErlangObject erlState;
	
	private EddModel eddModel;
	private boolean firstTime;
	
	private volatile boolean stop = false;
	
	private final CountDownLatch startSignal;
	private final CountDownLatch doneSignal;
	
	/**
	 * Starts the erlang server client. 
	 * 
	 * @param doneSignal 
	 * 			done signal
	 * @param buggyCall
	 * 			a buggy call to debug.
	 * @param location
	 * 			the location of the erlang source file to debug.
	 * @param node
	 * 			the Erlang/OTP node.
	 * 
	 */
	public ErlangClient(CountDownLatch startSignal, CountDownLatch doneSignal, String buggyCall, String location, OtpNode node) {
		this.startSignal = startSignal;
		this.doneSignal = doneSignal;
		this.buggyCall = buggyCall;
		this.location = location;
		this.node = node;
		this.mailbox = node.createMbox(MAILBOX);
		
		eddModel = new EddModel();
		firstTime = true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		System.out.println("Erlang EDD server running...");
		while (!stop) {
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
				TimeUnit.SECONDS.sleep(0);
			} catch (OtpException e) {
				System.out.println("Se ha producido un error: "	+ e.getMessage());			
				System.out.println("El proceso 'ErlangServer' ha finalizado");
				e.printStackTrace();
				mailbox.close();
			} catch (InterruptedException e) {
				System.out.println("Interrupted, closing...");
				mailbox.close();
		    }
		}
		node.close();
		mailbox.close();
		System.err.println("\t--> Client closed!");
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
			case "ready": {
				proccessReady(command, args[0]);
	    		System.err.println("\t--> " + Thread.currentThread().getName() + " is Up");
	    		doneSignal.countDown(); //reduce count of CountDownLatch by 1
				break;
			}
				
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
			pidServer = (OtpErlangPid) otpErlangObject;
			sendBuggyCall();
		}
		else {
			System.out.println("The 'ready' tuple is malformmed...");
		}
	}

	/**
	 * 
	 */
	private void sendBuggyCall() {
		// Send reply with buggy call and its location
		OtpErlangObject[] reply = new OtpErlangObject[4];
		reply[0] = new OtpErlangAtom("buggy_call");
		reply[1] = new OtpErlangString(buggyCall);
		reply[2] = new OtpErlangString(location);
		if (firstTime) {
			erlState = new OtpErlangAtom("none"); 
			firstTime = false;
		}
		reply[3] = erlState;

		try {
			startSignal.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		sendMessage(pidServer, reply);		
		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> + se envía el buggy_call");
	}
	
	/**
	 * {command, json}
	 * {dbg_tree, "{"vertices..."}"}
	 * 
	 * @param command
	 * @param otpErlangObject
	 */
	private void processDebugTree(OtpErlangAtom command, OtpErlangObject otpErlangObject) {
		if (otpErlangObject instanceof OtpErlangTuple) {
			OtpErlangTuple dbgTreeTuple = (OtpErlangTuple) otpErlangObject;
			if (dbgTreeTuple.arity() == 2) {
				eddModel.setDebugTree(new DebugTree(dbgTreeTuple));
			}
			else {
				System.out.println("The 'dbg_tree' tuple is malformmed...");
			}			
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
			OtpErlangTuple stateTuple = (OtpErlangTuple) arg2;
			if (stateTuple.arity() == 4) {
				eddModel.setCurrentQuestionIndex((int) qIndex.longValue());
				eddModel.setState(new EddState(stateTuple));
				
				// FIXME Descomentada sirve para avanzar en la búsqueda del buggy_node...
//				 sendAnswer("n");
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
		if (eddModel.getBuggyNodeIndex() == null || eddModel.getBuggyNodeIndex() == -1) {
			sendMessage(pidServer, reply);
		} else {
			System.out.println("Discard: " + response);
		}
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
			int buggyNodeIndex = (int) buggyNode.longValue();
			System.out.println("Buggy node: " + buggyNodeIndex);
			eddModel.setBuggyNodeIndex(buggyNodeIndex);
			
			try {
				close();
			} catch (Exception e) {
				e.printStackTrace();
			}
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
		System.out.println("Aborted...");
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
//		OtpErlangAtom errorAtom = new OtpErlangAtom("error");
//		OtpErlangObject[] response = new OtpErlangObject[] {errorAtom, message};
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
	
	public String getBuggyCall() {
		return buggyCall;
	}

	public void setBuggyCall(String buggyCall) {
		this.buggyCall = buggyCall;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
	
	public void restart() {
		firstTime = true;
		sendBuggyCall();
	}

	public EddModel getEddModel() {
		return eddModel;
	}
	
	public void setAnswer(String reply, CountDownLatch countDownLatch) {
		sendAnswer(reply);		
		countDownLatch.countDown(); //reduce count of CountDownLatch by 1
	}

	public void stopClient() throws Exception {
		sendAnswer("a");
		close();
	}
	
	@Override
	public void close() throws Exception {
		stop = true;
	}
}