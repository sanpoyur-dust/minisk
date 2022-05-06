package org.semgus.sketch.object;

public record SketchTyped(String type, String name) {
  @Override
  public String toString() {
    return type + " " + name;
  }
}
