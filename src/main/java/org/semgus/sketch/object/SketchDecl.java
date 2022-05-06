package org.semgus.sketch.object;

public record SketchDecl(String type, String name) {
  @Override
  public String toString() {
    return type + " " + name;
  }
}
