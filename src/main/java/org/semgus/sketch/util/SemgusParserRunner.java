package org.semgus.sketch.util;

import java.io.*;
import java.util.Objects;
import java.util.stream.Collectors;

public class SemgusParserRunner {
  public static String run(String input) {
    String osName = System.getProperty("os.name");
    String parserPath;
    if (osName.startsWith("Windows")) {
      parserPath = "/semgus-parser-win.exe";
    } else if (osName.startsWith("Mac")) {
      parserPath = "/semgus-parser-osx";
    } else {
      parserPath = "/semgus-parser-linux";
    }

    try {
      String uri = new File(
          Objects.requireNonNull(SemgusParserRunner.class.getResource(parserPath)).toURI())
          .toString();

      if (osName.startsWith("Mac")) {
        new ProcessBuilder("chmod", "u+x", uri).start();
      }

      Process parserProcess =
          new ProcessBuilder(
          uri, "--format", "json", "--mode", "batch", input)
          .start();

      BufferedReader reader = new BufferedReader(new InputStreamReader(parserProcess.getInputStream()));
      String json = reader.lines().collect(Collectors.joining());
      parserProcess.destroy();

      return json;
    } catch (Exception e) {
      e.printStackTrace();
    }

    return "";
  }
}
