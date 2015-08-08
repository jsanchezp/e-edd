package es.ucm.fdi.edd.core.erlang;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import es.ucm.fdi.edd.core.exception.EDDException;

/**
 * EDD server.
 */
public class ErlangServer implements Runnable, AutoCloseable {

	/** The working directory (Must contain the 'edd_jserver.beam' file). */
	private static final String WORKING_DIRECTORY = "D:/workspace/git/edd/ebin";
	/** The 'edd_jserver.beam' filename. */
	private static final String JSERVER_FILE = "edd_jserver.beam";

	public static final String NODE = "edderlang@localhost";

	private static final String CMD = "cmd";
	/** Carries out the command specified by string and then terminates. */
	private static final String C = "/C";
	/** Carries out the command specified by string but remains. */
	// private static final String K = "/K";
	private static final String START = "start";

	private boolean consoleVisible = false;

	private volatile int exitCode;
	
	private final CountDownLatch startSignal;
	private final CountDownLatch doneSignal;

	/**
	 * @param doneSignal
	 */
	public ErlangServer(CountDownLatch startSignal, CountDownLatch doneSignal) {
		this.startSignal = startSignal;
		this.doneSignal = doneSignal;

		shutdownHook();
		exitCode = -1;
	}

	private void shutdownHook() {
		handleShutdown();
	}

	private void handleShutdown() {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				System.out.println("In shutdown hook...");
			}
		}, "Shutdown-thread"));
	}

	/**
	 * Executes the 'edd_jserver' module in background mode.
	 * 
	 * @throws EDDException
	 */
	private int executeEddJServerInBackground() throws EDDException {
		Process process = null;
		try {
			StringBuffer sbCommand = new StringBuffer();
			sbCommand.append("erl -sname ");
			sbCommand.append(NODE);
			sbCommand.append(" -setcookie ");
			sbCommand.append(Erlang2Java.COOKIE);
			sbCommand.append(" -run edd_jserver start");
			// sbCommand.append(" -noshell -s erlang halt");

			String command = sbCommand.toString();
			String[] commands;
			if (consoleVisible) {
				commands = new String[] { CMD, C, START, command };
				// commands = new String[] {CMD, K, START, command};
			} else {
				commands = new String[] { CMD, C, command };
				// commands = new String[] {CMD, K, command};
			}

			ProcessBuilder processBuilder = new ProcessBuilder(commands);
			// processBuilder.redirectErrorStream(true);
			// processBuilder.redirectError(Redirect.INHERIT);
			// processBuilder.redirectOutput(Redirect.INHERIT);
			processBuilder.directory(new File(WORKING_DIRECTORY));
			File workDir = processBuilder.directory();
			boolean check = new File(workDir, JSERVER_FILE).exists();
			if (check) {
				System.out.println("Working directory: " + workDir);
			} else {
				throw new EDDException(
						"The working directory must contain the 'edd_jserver.beam' file");
			}

			// System.out.println("Configure parameters");
			// Map<String, String> env = new HashMap<String, String>();
			// env.put("name", "erl command");
			// env.put("echoCount", "1");
			// processBuilder.environment().putAll(env);
			//
			// System.out.println("Redirect output and error to file");
			// File outputFile = new File("ServerLog.txt");
			// File errorFile = new File("ServerErrLog.txt");
			// processBuilder.redirectOutput(outputFile);
			// processBuilder.redirectError(errorFile);

			process = processBuilder.inheritIO().start();

			System.out.printf("Output of running %s is:\n", Arrays.toString(commands));
			System.out.println("Echo process input:\n");
			IOThreadHandler inputHandler = new IOThreadHandler(process.getInputStream(), new CountDownLatch(1));
			inputHandler.start();
			System.out.println("Echo process error:\n");
			IOThreadHandler errorHandler = new IOThreadHandler(process.getErrorStream(), new CountDownLatch(1));
			errorHandler.start();

			// System.out.println("Echo process input:\n" +
			// input(procss.getInputStream()));
			// System.out.println("Echo process output:\n");
			// output(process.getOutputStream(), new String[0]);
			// if (false) {
			// String[] lines = new String[] {"halt()."};
			// output(process.getOutputStream(), lines);
			// }
			// System.out.println("Echo process error:\n" +
			// input(process.getErrorStream()));

			System.err.println("\t--> " + Thread.currentThread().getName() + " is Up");
			startSignal.countDown();
			doneSignal.countDown(); // reduce count of CountDownLatch by 1

//			if(!process.waitFor(1, TimeUnit.MINUTES)) {
//			    //timeout - kill the process.
//				try {
//					process.destroy();
//					process.destroyForcibly();
//					process = null;
//					return -1;
//				} catch (Exception e) {
//					e.printStackTrace();
//				} 
//			}
			
			// Wait to get exit value
			exitCode = process.waitFor();
			System.out.println("Echo command executed, any errors? " + (exitCode == 0 ? "No" : "Yes"));
			System.err.println("INPUT HANDLER: " + inputHandler.getOutput());
			System.err.println("OUTPUT HANDLER: " + errorHandler.getOutput());
		} catch (InterruptedException e) {
			System.out.println("The process has been interrupted");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Can't load erlang module");
			e.printStackTrace();
		} finally {
			try {
				process.destroy();
				process.destroyForcibly();
				process = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return exitCode;
	}

	/**
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	protected String input(InputStream inputStream) throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line);
				stringBuilder.append(System.getProperty("line.separator"));
				System.out.println(line);
			}
		} finally {
			bufferedReader.close();
		}

		return stringBuilder.toString();
	}

	/**
	 * @param outputStream
	 * @param lines
	 * @throws IOException
	 */
	protected void output(OutputStream outputStream, String[] lines)
			throws IOException {
		BufferedWriter bufferedWriter = null;
		try {
			bufferedWriter = new BufferedWriter(new OutputStreamWriter(
					outputStream));
			for (String line : lines) {
				bufferedWriter.write(line);
				System.out.println(line);
			}
		} finally {
			bufferedWriter.close();
		}
	}

	@Override
	public void run() {
		try {
			exitCode = executeEddJServerInBackground();
		} catch (EDDException e) {
			System.out.println("Can't load erlang module.\n" + e.getMessage());
			e.printStackTrace();
		}
		System.err.println("\t--> Server closed!");
	}


	public int getExitCode() {
		return exitCode;
	}

	public void stopServer() throws Exception {
		close();
	}

	@Override
	public void close() throws Exception {
		TaskList tasks = new TaskList();
		String erlPid = tasks.getPidByComandLine();
		if (erlPid != null) {
			Process process = Runtime.getRuntime().exec("taskkill /F /PID " + erlPid);
			int exitCode = process.waitFor();
			System.out.println("Echo kill server, any errors? " + (exitCode == 0 ? "No" : "Yes"));
		} else {
			System.err.println("El nodo '" + NODE + "' no esta en ejecuci�n");
		}
		
	}

	protected static void printFile(File file) {
		System.out.println("*********************************");
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		try {
			fileReader = new FileReader(file);
			bufferedReader = new BufferedReader(fileReader);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				System.out.println(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bufferedReader.close();
				fileReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("*********************************");
	}
}