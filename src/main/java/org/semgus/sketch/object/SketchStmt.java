package org.semgus.sketch.object;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A sketch statement.
 */
public interface SketchStmt {
  /**

  /**
   * An atomic statement.
   *
   * @param prefix The statement prefix
   * @param expr The expression in the statement
   */
  record AtomStmt(String prefix, SketchExpr expr) implements SketchStmt {
    @Override
    public String toString() {
      return (prefix.isEmpty() ? "" : prefix + " ") + expr.toString() + ";";
    }
  }

  /**
   * A sequential statement.
   *
   * @param stmts The list of statements
   * @param delim The delimiter
   */
  record SeqStmt(List<SketchStmt> stmts, String delim) implements SketchStmt {
    @Override
    public String toString() {
      return stmts.stream()
          .map(SketchStmt::toString)
          .collect(Collectors.joining(delim));
    }
  }

  /**
   * A conditional statement.
   *
   * @param i The if expression
   * @param t The then expression
   * @param e The else expression
   */
  record CondStmt(SketchExpr i, SketchStmt t, SketchStmt e) implements SketchStmt {
    @Override
    public String toString() {
      return "if (" + i.toString() + ") { " + t.toString() + " } else { " + e.toString() + " }";
    }
  }

  /**
   * A variable definition statement.
   *
   * @param decl The variable declaration in the definition
   * @param init The initial value expression
   */
  record VarDefStmt(SketchDecl decl, SketchExpr init) implements SketchStmt {
    @Override
    public String toString() {
      return decl.toString() + " = " + init.toString() + ";";
    }
  }

  /**
   * A function definition statement.
   *
   * @param decl The function declaration in the definition
   * @param args The list of arguments
   * @param body The function body
   */
  record FuncDefStmt(SketchDecl decl, List<SketchDecl> args, SketchStmt body) implements SketchStmt {
    @Override
    public String toString() {
      String content = args.stream()
          .map(SketchDecl::toString)
          .collect(Collectors.joining(", "));
      return decl.toString() + "(" + content + ") { " + body.toString() + " }";
    }
  }

  /**
   * A struct definition statement.
   *
   * @param type The struct type
   * @param decls The list of field declarations
   */
  record StructDefStmt(SketchType type, List<SketchDecl> decls) implements SketchStmt {
    @Override
    public String toString() {
      String content = decls.stream()
          .map(SketchDecl::toString)
          .collect(Collectors.joining("; "));
      return "struct " + type.typeName() + "_t" + " { " + content + "; }";
    }
  }
}
