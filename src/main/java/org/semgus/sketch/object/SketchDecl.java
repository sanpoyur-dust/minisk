package org.semgus.sketch.object;

public record SketchDecl(SketchType type, String name) {
  @Override
  public String toString() {
    return type + " " + name;
  }
}
