package org.semgus.sketch.object;

import java.util.List;

/**
 * Types in sketch
 */
public interface SketchType {
  int size();
  /**
   *  void
   */
  record VoidType() implements SketchType {
    @Override
    public String toString() {
      return "void";
    }

    /**
     * Get the amount of data in this type
     * @return 0, void don't have data
     */
    @Override public int size() {
      return 0;
    }
  }

  /**
   * int
   */
  record IntType() implements SketchType {
    @Override
    public String toString() {
      return "int";
    }

    /**
     * Get the amount of data in this type
     * @return 1
     */
    @Override public int size() {
      return 1;
    }
  }


  /**
   * bit
   */
  record BitType() implements SketchType {
    @Override
    public String toString() {
      return "bit";
    }

    /**
     * Get the amount of data in this type
     * @return 1
     */
    @Override public int size() {
      return 1;
    }
  }


  /**
   * A type defined by struct
   */
  record StructType(String name, List<SketchDecl> fields) implements SketchType {
    @Override
    public String toString() {
      return name;
    }

    public List<SketchDecl> getFields() {
      return fields;
    }

    public int size() {
      return fields.size();
    }
  }
}
