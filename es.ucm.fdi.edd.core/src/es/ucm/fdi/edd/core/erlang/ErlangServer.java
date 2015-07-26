package es.ucm.fdi.edd.core.erlang;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.util.Arrays;

import jna.Kernel32;
import jna.W32API;

import com.sun.jna.Pointer;

import es.ucm.fdi.edd.core.exception.EDDException;

/**
 * EDD server.
 * 
 * @see http://examples.javacodegeeks.com/core-java/lang/processbuilder/java-lang-processbuilder-example/
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
//	private static final String K = "/K";
	private static final String START = "start";
	
	private boolean consoleVisible = false;
	
	private volatile int exitCode = -1;
	
	/**
	 * 
	 */
	public ErlangServer() {
		shutdownHook();
	}

	private void shutdownHook() {
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
    		process = processBuilder.inheritIO().start();
	        getWindowsPid(process);
    		
    		System.out.printf("Output of running %s is:\n",	Arrays.toString(commands));
	        System.out.println("Echo process input:\n" + input(process.getInputStream()));
	        System.out.println("Echo process output:\n");
	        output(process.getOutputStream(), new String[0]);
    		System.out.println("Echo process error:\n" + input(process.getErrorStream()));

    		// Wait to get exit value
    		int exitValue = process.waitFor();
    		System.out.println("\nExit Value is " + exitValue);
    		return exitValue;
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
        
        return -1;
	}
	
	/**
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	private String input(InputStream inputStream) throws IOException {
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
	private void output(OutputStream outputStream, String[] lines) throws IOException {
		BufferedWriter bufferedWriter = null;
		try {
			bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
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
		System.out.println(">>>>> Server closed!");
	}
		
	protected void killProcessX(Process process) throws Exception {
		if (process != null) {
			Integer pid;
	        Runtime rt = Runtime.getRuntime();
			if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
				pid = getWindowsPid(process);
				rt.exec("taskkill /F /PID " +  pid);
			}
			else {
				pid = getUnixPid(process);
				rt.exec("kill -9 " + pid);
			}
			
			process.destroy();
			process.destroyForcibly();
			process = null;
		}
	}
	
	/**
	 * Determine the pid on windows plattforms.
	 * 
	 * @return the pid.
	 */
	private Integer getWindowsPid(Process process) {
		Class<?> processImpl = process.getClass();
		String processClassName = processImpl.getName();
		if (processClassName.equals("java.lang.Win32Process") || 
			processClassName.equals("java.lang.ProcessImpl")) {
			try {
				Field field = processImpl.getDeclaredField("handle");
				field.setAccessible(true);
				long peer = field.getLong(process);

				Kernel32 kernel = Kernel32.INSTANCE;
				W32API.HANDLE handle = new W32API.HANDLE();
				handle.setPointer(Pointer.createConstant(peer));
				Integer pid = kernel.GetProcessId(handle);
				System.err.println("Windows --> Process handle: " + peer + " \tpid: " + pid);
				getPid();
				return pid;
			} catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		
		return -1;
	}
	
	/**
	 * Gets the PID on unix/linux systems
	 * 
	 * @return the pid.
	 */
	private Integer getUnixPid(Process process) {
		if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
			try {
				Class<?> processImpl = process.getClass();
				Field f = processImpl.getDeclaredField("pid");
				f.setAccessible(true);
				Integer pid = f.getInt(process);
				return pid;
			} catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		
		return -1;
	}
	
	private void getPid() {
		try {
			RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
			
			String jvmName = runtimeMXBean.getName();
			System.out.println("JVM Name = " + jvmName);
			
			long pid = Long.valueOf(jvmName.split("@")[0]);
			System.out.println("JVM PID  = " + pid);
			
			ThreadMXBean bean = ManagementFactory.getThreadMXBean();
			int peakThreadCount = bean.getPeakThreadCount();
			System.out.println("Peak Thread Count = " + peakThreadCount);
			
//			Field jvmField = runtimeMXBean.getClass().getDeclaredField("jvm");
//			jvmField.setAccessible(true);
//			VMManagement vmManagement = (VMManagement) jvmField.get(runtimeMXBean);
//			Method getProcessIdMethod = vmManagement.getClass().getDeclaredMethod("getProcessId");
//			getProcessIdMethod.setAccessible(true);
//			Integer processId = (Integer) getProcessIdMethod.invoke(vmManagement);
//			System.out.println("################    ProcessId = " + processId);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
			Runtime.getRuntime().exec("taskkill /F /PID " +  erlPid);
		}
	}
}