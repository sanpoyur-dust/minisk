package org.semgus.sketch.object;

import java.util.List;
import java.util.stream.Collectors;

public interface SketchStmt {
  record ConstStmt(String keyword, SketchExpr expr) implements SketchStmt {
    @Override
    public String toString() {
      return (keyword.isEmpty() ? "" : keyword + " ") + expr.toString() + ";";
    }
  }

  record SeqStmt(SketchStmt lhs, String sep, SketchStmt rhs) implements SketchStmt {
    @Override
    public String toString() {
      return lhs.toString() + sep + rhs.toString();
    }
  }

  record CondStmt(SketchExpr i, SketchStmt t, SketchStmt e) implements SketchStmt {
    @Override
    public String toString() {
      return "if (" + i.toString() + ") { " + t.toString() + " } else {" + e.toString() + " }";
    }
  }

  record varDefStmt(SketchTyped typed, SketchExpr expr) implements SketchStmt {
    @Override
    public String toString() {
      return typed.toString() + " = " + expr.toString() + ";";
    }
  }

  record funcDefStmt(SketchTyped typed, List<SketchTyped> args, SketchStmt stmt) implements SketchStmt {
    @Override
    public String toString() {
      String content = args.stream()
          .map(SketchTyped::toString)
          .collect(Collectors.joining(", "));
      return typed.toString() + "(" + content + ") { " + stmt.toString() + " }";
    }
  }
}