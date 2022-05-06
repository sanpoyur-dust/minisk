package org.semgus.sketch.object;

import java.util.List;
import java.util.stream.Collectors;

public interface SketchExpr {
  record ConstExpr(String expr) implements SketchExpr {
    @Override
    public String toString() {
      return expr;
    }
  }

  record BinaryExpr(SketchExpr lhs, String binop, SketchExpr rhs) implements SketchExpr {
    @Override
    public String toString() {
      return lhs.toString() + " " + binop + " " + rhs.toString();
    }
  }

  record FuncExpr(String name, List<SketchExpr> args) implements SketchExpr {
    @Override
    public String toString() {
      String content = args.stream()
          .map(SketchExpr::toString)
          .collect(Collectors.joining(", "));
      return name + "(" + content + ")";
    }
  }

  record RegExpr(List<SketchExpr> exprs) implements SketchExpr {
    @Override
    public String toString() {
      String content = exprs.stream()
          .map(SketchExpr::toString)
          .collect(Collectors.joining(" | "));
      return "{| " + content + " |}";
    }
  }
}
