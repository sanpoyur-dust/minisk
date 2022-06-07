package org.semgus.sketch.object;

/**
 * A sketch declaration.
 *
 * @param type The declaration type
 * @param name The declaration name
 */
public record SketchDecl(SketchType type, String name) {
  @Override
  public String toString() {
    return type.toString() + " " + name;
  }
}
