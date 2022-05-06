package org.semgus.sketch.object;

public interface SketchStmt {
  record ConstStmt(String keyword, SketchExpr expr) implements SketchStmt {
    @Override
    public String toString() {
      return (keyword.isEmpty() ? "" : keyword + " ") + expr.toString() + ";";
    }
  }

  record SeqStmt(SketchStmt lhs, SketchStmt rhs) implements SketchStmt {
    @Override
    public String toString() {
      return lhs.toString() + " " + rhs.toString();
    }
  }

  record CondStmt(SketchExpr i, SketchStmt t, SketchStmt e) implements SketchStmt {
    @Override
    public String toString() {
      return "if (" + i.toString() + ") { " + t.toString() + " } else {" + e.toString() + " }";
    }
  }
}
