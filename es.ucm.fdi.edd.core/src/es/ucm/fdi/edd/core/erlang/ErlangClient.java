package es.ucm.fdi.edd.core.erlang;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

import es.ucm.fdi.edd.core.erlang.model.DebugTree;
import es.ucm.fdi.edd.core.erlang.model.EddModel;
import es.ucm.fdi.edd.core.erlang.model.EddState;
import es.ucm.fdi.edd.core.erlang.model.ZoomDebugTree;

/**
 * Para probar la comunicaci�n erlang<->java, debes ejecutar la aplicaci�n java y luego abrir una consola erlang para el paso de mensajes.  
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
				System.out.println("<-- RECEIVED message: " + message);
				if (message instanceof OtpErlangTuple) {
					OtpErlangTuple tuple = (OtpErlangTuple) message;
					int arity = tuple.arity();
					if (tuple.elementAt(0) instanceof OtpErlangAtom) {
						OtpErlangAtom command = (OtpErlangAtom) tuple.elementAt(0);
						switch (arity) {
							case 2:
								processMessage(command, tuple.elementAt(1));
								break;
							case 3:
								processMessage(command, tuple.elementAt(1), tuple.elementAt(2));
								break;
							case 4:
								processMessage(command, tuple.elementAt(1), tuple.elementAt(2), tuple.elementAt(3));
								break;
								
							default:
								break;
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
		int size = args.length;
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
				if (size == 2) {
					processQuestion(command, args[0], args[1]);
				}
				else if (size == 3) {
					processZoomQuestion(command, args[0], args[1], args[2]);
				}
				break;
			
			case "buggy_node":
				processBuggyNode(command, args[0], args[1]);
				break;
				
			case "aborted":
				processAborted(command);
				break;
				
			case "error":
				processError(command, args[0]);
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
				OtpErlangTuple vertextesTuple = (OtpErlangTuple) dbgTreeTuple.elementAt(0);
				if (vertextesTuple.arity() == 2) {
					OtpErlangList vertexesList = (OtpErlangList) vertextesTuple.elementAt(1);
					// Diferenciar el tipo de depuraci�n por el primer elemento de la lista 
					OtpErlangObject firstElement = vertexesList.elementAt(0);
					if (firstElement instanceof OtpErlangTuple) {
						OtpErlangTuple vertexTuple = (OtpErlangTuple) firstElement;
						if (vertexTuple.arity() == 4) {
							// Normal debugging
							eddModel.setDebugTree(new DebugTree(dbgTreeTuple));
						}
						else if (vertexTuple.arity() == 2) {
							// Zoom debugging
							eddModel.setZoomDebugTree(new ZoomDebugTree(dbgTreeTuple));
						}
						else {
							System.out.println("The 'vertex' arity is malformmed...");
						}
					}
				}
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
				eddModel.setCurrentZoomQuestionIndex((int) qIndex.longValue());
				eddModel.setState(new EddState(stateTuple));
				System.out.println("\tCurrent Question:" + qIndex.longValue());
				// Avanzar en la b�squeda del buggy_node...
				//sendAnswer("n");
			}
			else {
				System.out.println("The tuple is malformmed...");
			}
		}
		else {
			System.out.println("The 'question' tuple is malformmed...");
		}
	}
	
	/**
	 * @param command
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 */
	private void processZoomQuestion(OtpErlangAtom command, OtpErlangObject arg1, OtpErlangObject arg2, OtpErlangObject arg3) {
		if (arg1 instanceof OtpErlangLong && arg2 instanceof OtpErlangList && arg3 instanceof OtpErlangTuple) {
			OtpErlangLong qIndex = (OtpErlangLong) arg1;
			OtpErlangList zoomAnswers = (OtpErlangList) arg2;
			
			LinkedList<String> answersList = new LinkedList<String>();
			for (OtpErlangObject otpErlangObject : zoomAnswers) {
				if (otpErlangObject instanceof OtpErlangAtom) {
					OtpErlangAtom atom = (OtpErlangAtom) otpErlangObject;
					answersList.add(atom.atomValue());
				}
				else {
					System.out.println("The 'answer' is malformmed, it will be ignored...");	
				}
			}
			eddModel.setAnswerList(answersList);
			
			OtpErlangTuple stateTuple = (OtpErlangTuple) arg3;
			if (stateTuple.arity() == 4) {
				//FIXME Clear the buggy_node
				eddModel.setBuggyNodeIndex(-1);
				eddModel.setBuggyErrorCall(null);
				
				eddModel.setCurrentQuestionIndex((int) qIndex.longValue());
				eddModel.setCurrentZoomQuestionIndex((int) qIndex.longValue());
				eddModel.setState(new EddState(stateTuple));
				System.out.println("\tCurrent Zoom Question:" + qIndex.longValue());
			}
			else {
				System.out.println("The tuple is malformmed...");
			}
		}
		else {
			System.out.println("The 'question' tuple is malformmed...");
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
	private void processBuggyNode(OtpErlangAtom command, OtpErlangObject arg1, OtpErlangObject arg2) {
		if (arg1 instanceof OtpErlangLong && arg2 instanceof OtpErlangString) {
			OtpErlangLong buggyNode = (OtpErlangLong) arg1;
			OtpErlangString buggyCall = (OtpErlangString) arg2;
			int buggyNodeIndex = (int) buggyNode.longValue();
			String buggyErrorCall = buggyCall.stringValue();
			System.out.println(String.format("Buggy node: %s Error call: %s", buggyNodeIndex, buggyErrorCall));
			eddModel.setBuggyNodeIndex(buggyNodeIndex);
			eddModel.setBuggyErrorCall(buggyErrorCall);
		}
		else {
			System.out.println("The 'buggy_node' tuple is malformmed...");
		}
	}
	
	/**
	 * @param buggyErrorCall
	 */
	private void sendZoomDebug(String buggyErrorCall) {
		OtpErlangObject[]  reply = new OtpErlangObject[4];
		reply[0] = new OtpErlangAtom("zoom_dbg");
		reply[1] = new OtpErlangString(buggyErrorCall);
		reply[2] = new OtpErlangString(location);
		reply[3] = new OtpErlangAtom("none");
		sendMessage(pidServer, reply);
	}
	
	/**
	 * Finish the communication with the server.
	 * 
	 * @throws OtpException
	 */
	private void processAborted(OtpErlangAtom command) throws OtpException {
		System.out.println("Aborted...");
	}
	
	/**
	 * @throws OtpException
	 */
	private void processError(OtpErlangAtom command, OtpErlangObject arg1) throws OtpException {
		if (arg1 instanceof OtpErlangTuple) {
			OtpErlangTuple errorTuple = (OtpErlangTuple)arg1;
			if (errorTuple.arity() == 2) {
				OtpErlangAtom errorMsg1 = (OtpErlangAtom) errorTuple.elementAt(0);
				OtpErlangObject errorObj = errorTuple.elementAt(1);
				if (errorObj instanceof OtpErlangAtom) {
					OtpErlangAtom errorMsg2 = (OtpErlangAtom) errorObj;
					System.out.println(String.format("Error node: %s, %s", errorMsg1.atomValue(), errorMsg2.atomValue()));
				} else if (errorObj instanceof OtpErlangTuple) {
					OtpErlangTuple errorMsgTuple = (OtpErlangTuple) errorObj;
					OtpErlangObject[] elements = errorMsgTuple.elements();
					System.out.println(String.format("Error node: %s, %s", errorMsg1.atomValue(), Arrays.toString(elements)));
					
				} else {
					System.out.println(String.format("Error node: %s, %s", errorMsg1.atomValue(), errorObj.toString()));
				}
				
				// FIXME �Deber�a cerrar el cliente...?
				try {
					close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else {
				System.out.println("Unknown 'error' arity...");
			}
		}
		else {
			System.out.println("The 'error' tuple is malformmed...");
		}
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
		System.out.println("--> SEND message [" + pidSender + "]: " + message);
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
	
	public void startZoomDebug(String buggyErrorCall) {
		sendZoomDebug(buggyErrorCall);
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