package org.semgus.sketch;

import java.io.*;

public class SemgusParserRunner {
  private static Process parserProcess = null;

  public static Reader run(String input) {
    String osName = System.getProperty("os.name");
    String parserPath = null;
    if (osName.startsWith("Windows")) {
      parserPath = "/semgus-parser-win.exe";
    } else if (osName.startsWith("Mac OS")) {
      parserPath = "/semgus-parser-osx";
    } else {
      parserPath = "/semgus-parser-linux";
    }

    try {
      String uri = new File(SemgusParserRunner.class.getResource(parserPath).toURI()).toString();
      parserProcess =
          new ProcessBuilder(
          uri, "--format", "json", "--mode", "batch", input)
          .start();
      return new BufferedReader(new InputStreamReader(parserProcess.getInputStream()));
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public static void terminate() {
    parserProcess.destroy();
  }
}
