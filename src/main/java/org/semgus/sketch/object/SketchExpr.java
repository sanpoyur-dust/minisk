package org.semgus.sketch.object;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a sketch expression.
 */
public interface SketchExpr {
  /**
   * An atomic expression.
   *
   * @param expr The expression
   */
  record AtomExpr(String expr) implements SketchExpr {
    @Override
    public String toString() {
      return expr;
    }
  }

  /**
   * A unary expression.
   *
   * @param op The binary operator
   * @param expr The expression
   */
  record UnaryExpr(String op, SketchExpr expr) implements SketchExpr {
    @Override
    public String toString() {
      return op + "(" + expr + ")";
    }
  }

  /**
   * A binary expression.
   *
   * @param lhs The left expression
   * @param op The binary operator
   * @param rhs The right expression
   */
  record BinaryExpr(SketchExpr lhs, String op, SketchExpr rhs) implements SketchExpr {
    @Override
    public String toString() {
      return "(" + lhs.toString() + " " + op + " " + rhs.toString() + ")";
    }
  }

  /**
   * An assign expression.
   *
   * @param lhs The left expression
   * @param rhs The right expression
   */
  record AssignExpr(SketchExpr lhs, SketchExpr rhs) implements SketchExpr {
    @Override
    public String toString() {
      return lhs + "=" + rhs;
    }
  }

  /**
   * A conditional expression.
   *
   * @param i The if expression
   * @param t The then expression
   * @param e The else expression
   */
  record CondExpr(SketchExpr i, SketchExpr t, SketchExpr e) implements SketchExpr {
    @Override
    public String toString() {
      return "(" + i.toString() + " ? " + t.toString() + " : " + e.toString() + ")";
    }
  }

  /**
   * A function expression.
   *
   * @param name The function name
   * @param args The list of function arguments
   */
  record FuncExpr(String name, List<SketchExpr> args) implements SketchExpr {
    @Override
    public String toString() {
      String content = args.stream()
          .map(SketchExpr::toString)
          .collect(Collectors.joining(", "));
      return name + "(" + content + ")";
    }
  }

  /**
   * A regular expression with only the | operator.
   *
   * @param subexprs The list of sub-expressions
   */
  record RegExpr(List<SketchExpr> subexprs) implements SketchExpr {
    @Override
    public String toString() {
      String content = subexprs.stream()
          .map(SketchExpr::toString)
          .collect(Collectors.joining(" | "));
      return "{| " + content + " |}";
    }
  }
}
