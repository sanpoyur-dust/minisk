package org.semgus.sketch.object;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A sketch.
 *
 * @param stmts The list of sketch statements.
 */
public record Sketch(List<SketchStmt> stmts) {
  @Override
  public String toString() {
    return stmts.stream()
        .map(SketchStmt::toString)
        .collect(Collectors.joining("\n"));
  }
}
