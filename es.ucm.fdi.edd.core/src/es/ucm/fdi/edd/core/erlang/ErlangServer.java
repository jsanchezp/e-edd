package es.ucm.fdi.edd.core.erlang;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import es.ucm.fdi.edd.core.exception.EDDException;

/**
 * EDD server.
 */
public class ErlangServer implements Runnable {
	
	/** The working directory (Must contain the 'edd_jserver.beam' file). */
	private static final String WORKING_DIRECTORY = "D:/workspace/git/edd/ebin";
	/** The 'edd_jserver.beam' filename. */
	private static final String JSERVER_FILE = "edd_jserver.beam";

	public static final String NODE = "edderlang@localhost";
	
	private static final String CMD = "cmd";
	/** Carries out the command specified by string and then terminates. */
	private static final String C = "/C";
	/** Carries out the command specified by string but remains. */
//	private static final String K = "/K";
	
	private static final String START = "start";
	

	private boolean consoleVisible = false;
	private boolean isLoaded;
	private StringBuffer sbOutput;
	
	private volatile boolean finished = false;
	
	/**
	 * 
	 */
	public ErlangServer() {
		//
	}
	
	/**
	 * Executes the 'edd_jserver' module in background mode.
	 *  
	 * @throws EDDException 
	 */
	private boolean executeEddJServerInBackground() throws EDDException {
		Process process = null;
        BufferedReader processInputReader = null;
        BufferedReader processErrorReader = null;
        
		try { 	
        	StringBuffer sbCommand = new StringBuffer();
        	sbCommand.append("erl -sname ");
        	sbCommand.append(NODE);
        	sbCommand.append(" -setcookie ");
        	sbCommand.append(Erlang2Java.COOKIE);
        	sbCommand.append(" -run edd_jserver start");
        	sbCommand.append(" -noshell -s erlang halt");
        	
        	String command = sbCommand.toString();   	
        	String[] commands;
        	if (consoleVisible) {
        		commands = new String[] {CMD, C, START, command};
//        		commands = new String[] {CMD, K, START, command};
        	}
			else { 
				commands = new String[] {CMD, C, command};
//				commands = new String[] {CMD, K, command};
			}
			
        	ProcessBuilder processBuilder = new ProcessBuilder(commands);
        	processBuilder.redirectErrorStream(true);
        	processBuilder.directory(new File(WORKING_DIRECTORY));
        	File workDir = processBuilder.directory();
        	boolean check = new File(workDir, JSERVER_FILE).exists();
        	if (check) {
        		System.out.println("Working directory: " + workDir);
        	}
        	else {
        		throw new EDDException("The working directory must contain the 'edd_jserver.beam' file");
        	}
    		process = processBuilder.start();

    		// Read out dir output
    		InputStream processInput = process.getInputStream();
    		processInputReader = new BufferedReader(new InputStreamReader(processInput));
    		sbOutput = new StringBuffer();
    		System.out.printf("Output of running %s is:\n",	Arrays.toString(commands));
    		String line;
    		while ((line = processInputReader.readLine()) != null) {
    			System.out.println(line);
    			sbOutput.append(line);
    		}
    		processInputReader.close();
    		
    		OutputStream processOutput = process.getOutputStream();
    		BufferedWriter processWriter = new BufferedWriter(new OutputStreamWriter(processOutput));
    		//...
    		//processWriter.write("Message... "); 
    		processWriter.close();
    		
    		InputStream processError = process.getErrorStream();
    		processErrorReader = new BufferedReader(new InputStreamReader(processError));
    		while ((line = processErrorReader.readLine()) != null) {
    			System.out.println(line);
    		}
    		processErrorReader.close();

    		// Wait to get exit value
    		int exitValue = process.waitFor();
    		System.out.println("\nExit Value is " + exitValue);
    		finished = true;
    		if (exitValue == 0) {
    			return true;
    		}
    		
        } catch (InterruptedException e) {
        	System.out.println("The process has been interrupted");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Can't load erlang module");
			e.printStackTrace();
		} finally {
			try {
				processInputReader.close();
				processErrorReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			process.destroy();
		}
        
        return false;
	}

	@Override
	public void run() {
		try {
			isLoaded = executeEddJServerInBackground();
			TimeUnit.SECONDS.sleep(1);
		} catch (EDDException e) {
			System.out.println("Can't load erlang module.\n" + e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isLoaded() {
		return isLoaded;
	}
	
	public boolean isFinished() {
		return finished;
	}
	
	public String getOutput() {
		return sbOutput.toString();
	}
}