package es.ucm.fdi.edd.core.erlang.jinterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

public class ErlangServer extends Thread {
	
	private boolean visible = false;
	private String path;

	public ErlangServer(String path) {
		this.path = path;
	}
    
	public void run() {
        try {
			//Process cmdProc = Runtime.getRuntime().exec("./load_erlang_server.sh");
//        	String command = "erl -sname edderlang@localhost -run edd_jserver start -noshell -s erlang halt";
        	
        	String command = "erl -sname edderlang@localhost -run edd_jserver start";
        	
        	String[] commands;
        	if (visible) {
        		commands = new String[] {"cmd", "/C", "start", command};
//        		commands = new String[] {"cmd", "/K", "start", command};
        	}
			else { 
				commands = new String[] {"cmd", "/C", command};
//				commands = new String[] {"cmd", "/K", command};
			}
			
        	ProcessBuilder probuilder = new ProcessBuilder(commands);
        	File file = new File(path);
        	System.out.println(file.getAbsolutePath());
    		probuilder.directory(new File("D:/workspace/runtime-tests/EDDSample/ebin"));
    		Process process = probuilder.start();

    		// Read out dir output
    		InputStream is = process.getInputStream();
    		InputStreamReader isr = new InputStreamReader(is);
    		BufferedReader br = new BufferedReader(isr);
    		String line;
    		System.out.printf("Output of running %s is:\n",	Arrays.toString(commands));
    		while ((line = br.readLine()) != null) {
    			System.out.println(line);
    		}

    		// Wait to get exit value
    		try {
    			int exitValue = process.waitFor();
    			System.out.println("\n\nExit Value is " + exitValue);
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    		}
        	
//        	Runtime runtime = Runtime.getRuntime();
//			Process cmdProc = runtime.exec(command);
//
//            BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(cmdProc.getInputStream()));
//            while ((line = stdoutReader.readLine()) != null) {
//               System.out.println(line);
//            }
//
//            BufferedReader stderrReader = new BufferedReader(new InputStreamReader(cmdProc.getErrorStream()));
//            while ((line = stderrReader.readLine()) != null) {
//               System.out.println(line);
//            }
//
//            int retValue = cmdProc.exitValue();
//            if(retValue != 0)
//                new RuntimeException(new Exception("Can't load erlang module"));
            
        } catch(Exception e) {
            System.out.println("Can't load erlang module");
            e.printStackTrace();
        }
    }
}