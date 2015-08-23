package es.ucm.fdi.edd.core.erlang;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
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
	
	private ErlangClient erlangClient;
	private ErlangServer erlangServer;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int params = args.length;
		switch (params) {
			case 0: {
				System.out.println("The argument list can not be empty.");
				break;
			}
				
			case 2: {
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
					
					for (int i = 0; i < 2; i++) {
						System.out.println("=============== START [" + i + "]===============\n");
						TimeUnit.SECONDS.sleep(5);
						main = null;
						main = new Erlang2Java();
						main.initialize(buggyCall, location);
						TimeUnit.SECONDS.sleep(5);
						main.stopServer();	
						System.out.println("=============== END ===============\n");
					}
				} catch (Exception e) {
					e.printStackTrace();
				} 
				
				break;
			}

			default: {
				System.out.println("You must provide two argument: a buggy call and the location of the erlang source file to debug.");
				break;
			}
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
			OtpNode node = new OtpNode(NODE);
			node.setCookie(COOKIE);
			
			final CountDownLatch startSignal = new CountDownLatch(1);
			final CountDownLatch doneSignal = new CountDownLatch(2);
			
			erlangClient = new ErlangClient(startSignal, doneSignal, buggyCall, location, node);
			Thread erlClient = new Thread(erlangClient, THREAD_CLIENT_NAME);
			
			erlangServer = new ErlangServer(startSignal, doneSignal);
			Thread erlServer = new Thread(erlangServer, THREAD_SERVER_NAME);
			
			erlClient.start();
			erlServer.start();
					
			// main thread is waiting on CountDownLatch to finish
			doneSignal.await(); 
			System.err.println("\t--> All services are up, Application is starting now");
			
//			erlClient.join();
//			erlServer.join();
				
		} catch (IOException e) {
			System.out.println("No se puede iniciar nodo. Has arrancado epmd?");
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Stops the server.
	 * 
	 * @throws Exception
	 */
	public void stopServer() throws Exception {
		erlangClient.stopClient();
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
		erlangClient.setAnswer(reply, new CountDownLatch(1));
	}
	
	public void startZoomDebug(String buggyErrorCall) {
		erlangClient.startZoomDebug(buggyErrorCall);
	}
}