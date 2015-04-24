package io.lp0onfire.smtnes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class STP implements AutoCloseable {

  private static String pathToSTP = null;
  
  private void findSTP() throws FileNotFoundException {
    Map<String, String> env = System.getenv();
    if (env.containsKey("PATH")) {
      String[] paths = env.get("PATH").split(":");
      for (String path : paths) {
        File stp = new File(path + "/stp");
        if (stp.exists() && !stp.isDirectory() && stp.canExecute()) {
          pathToSTP = path + "/stp";
          break;
        }
      }
    } else {
      File stp = new File("./stp");
      if (stp.exists() && !stp.isDirectory() && stp.canExecute()) {
        pathToSTP = "./stp";
      }
    }
    if (pathToSTP == null) {
      throw new FileNotFoundException("cannot find STP executable");
    }
  }
  
  public STP() throws FileNotFoundException {
    if (pathToSTP == null) {
      findSTP();
    }
  }

  private Process stpProcess = null;
  private BufferedWriter writer;
  private BufferedReader reader;
  
  public void open() throws IOException {
    List<String> command = new LinkedList<>();
    command.add(pathToSTP);
    command.add("--SMTLIB2");
    ProcessBuilder builder = new ProcessBuilder(command);
    builder.redirectErrorStream(true);
    stpProcess = builder.start();
    
    OutputStream os = stpProcess.getOutputStream();
    OutputStreamWriter osw = new OutputStreamWriter(os);
    writer = new BufferedWriter(osw);
    
    InputStream is = stpProcess.getInputStream();
    InputStreamReader isr = new InputStreamReader(is);
    reader = new BufferedReader(isr);
  }
  
  public void write(String data) throws IOException {
    if (stpProcess == null) throw new IllegalStateException("STP session has not been opened");
    writer.write(data);
    writer.newLine();
  }
  
  public boolean checkSat() throws IOException {
    write("(check-sat)");
    write("(exit)");
    writer.flush();
    writer.close();
    String result = reader.readLine();
    if (result.equals("sat")) {
      return true;
    } else if(result.equals("unsat")) {
      return false;
    } else {
      throw new RuntimeException("STP encountered an error: " + result);
    }
  }
  
  @Override
  public void close() {
    if (stpProcess != null) {
      stpProcess.destroyForcibly();
    }
  }
  
}
