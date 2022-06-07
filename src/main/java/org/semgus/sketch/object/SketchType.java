package org.semgus.sketch.object;

/**
 * A Sketch type.
 *
 * @param prefix The type prefix
 * @param typeName The type name
 */
public record SketchType(String prefix, String typeName) {
  @Override
  public String toString() {
    return (prefix.isEmpty() ? "" : prefix + " ") + switch (typeName) {
      case "Int" -> "int";
      case "Bool" -> "bit";
      case "void" -> "void";
      default -> "|" + typeName + "_t|";
    };
  }
}
