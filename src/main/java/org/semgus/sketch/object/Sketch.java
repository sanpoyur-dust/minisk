package org.semgus.sketch.object;

import java.util.List;
import java.util.stream.Collectors;

public record Sketch(List<SketchDef> defs) {
  @Override
  public String toString() {
    return defs.stream()
        .map(SketchDef::toString)
        .collect(Collectors.joining("\n"));
  }
}
