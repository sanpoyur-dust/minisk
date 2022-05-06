package org.semgus.sketch.object;

import java.util.List;
import java.util.stream.Collectors;

public record SketchDef(SketchTyped typed, List<SketchTyped> args, SketchStmt stmt) {
  @Override
  public String toString() {
    String content = args.stream()
        .map(Object::toString)
        .collect(Collectors.joining(", "));
    return typed.toString() + "(" + content + ") { " + stmt.toString() + " }";
  }
}
