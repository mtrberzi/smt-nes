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

public class Z3 implements AutoCloseable {

  private static String pathToZ3 = null;
  
  private void findZ3() throws FileNotFoundException {
    Map<String, String> env = System.getenv();
    if (env.containsKey("PATH")) {
      String[] paths = env.get("PATH").split(":");
      for (String path : paths) {
        File stp = new File(path + "/z3");
        if (stp.exists() && !stp.isDirectory() && stp.canExecute()) {
          pathToZ3 = path + "/z3";
          break;
        }
      }
    } else {
      File stp = new File("./z3");
      if (stp.exists() && !stp.isDirectory() && stp.canExecute()) {
        pathToZ3 = "./z3";
      }
    }
    if (pathToZ3 == null) {
      throw new FileNotFoundException("cannot find Z3 executable");
    }
  }
  
  public Z3() throws FileNotFoundException {
    if (pathToZ3 == null) {
      findZ3();
    }
  }

  private Process z3Process = null;
  private BufferedWriter writer;
  private BufferedReader reader;
  
  public void open() throws IOException {
    List<String> command = new LinkedList<>();
    command.add(pathToZ3);
    command.add("-smt2");
    command.add("-in");
    ProcessBuilder builder = new ProcessBuilder(command);
    builder.redirectErrorStream(true);
    z3Process = builder.start();
    
    OutputStream os = z3Process.getOutputStream();
    OutputStreamWriter osw = new OutputStreamWriter(os);
    writer = new BufferedWriter(osw);
    
    InputStream is = z3Process.getInputStream();
    InputStreamReader isr = new InputStreamReader(is);
    reader = new BufferedReader(isr);
    
    write("(set-logic QF_ABV)");
  }
  
  public void write(String data) throws IOException {
    if (z3Process == null) throw new IllegalStateException("Z3 session has not been opened");
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
      throw new RuntimeException("Z3 encountered an error: " + result);
    }
  }
  
  @Override
  public void close() {
    if (z3Process != null) {
      z3Process.destroyForcibly();
    }
  }
  
}
