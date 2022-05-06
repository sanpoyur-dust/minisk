package org.semgus.sketch.object;

import java.util.List;
import java.util.stream.Collectors;

public record Sketch(List<SketchStmt> defs) {
  @Override
  public String toString() {
    return defs.stream()
        .map(SketchStmt::toString)
        .collect(Collectors.joining("\n"));
  }
}
