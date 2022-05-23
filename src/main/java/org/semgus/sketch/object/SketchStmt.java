package org.semgus.sketch.object;

import java.util.List;

import static java.util.stream.Collectors.joining;

public interface SketchStmt {
  static public SketchStmt fromList(List<SketchStmt> list, String sep) {
    if (list.size() == 0)
      throw new IllegalStateException("empty list to sketch stmt");
    else if (list.size() == 1)
      return list.get(0);
    else {
      SketchStmt res = new SeqStmt(list.get(0), sep, list.get(1));
      for (int i = 2; i < list.size(); i++)
        res = new SeqStmt(res, sep, list.get(i));
      return res;
    }
  }
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
      return "if (" + i.toString() + ") { " + t.toString() + " } else { " + e.toString() + " }";
    }
  }

  record VarDefStmt(SketchDecl decl, SketchExpr expr) implements SketchStmt {
    @Override
    public String toString() {
      return decl.toString() + " = " + expr.toString() + ";";
    }
  }

  record AssignStmt(SketchExpr var, SketchExpr expr) implements SketchStmt {
    @Override
    public String toString() {
      return var.toString() + " = " + expr.toString() + ";";
    }
  }

  record FuncDefStmt(String prefix, SketchDecl decl, List<SketchDecl> args, SketchStmt stmt) implements SketchStmt {
    @Override
    public String toString() {
      String content = args.stream()
          .map(SketchDecl::toString)
          .collect(joining(", "));
      return prefix + " " + decl.toString() + "(" + content + ") { " + stmt.toString() + " }";
    }
  }

  record declStmt(SketchDecl decl) implements SketchStmt {
    @Override
    public String toString() {
      return decl.toString() + ";";
    }
  }

  record StructDefStmt(SketchType.StructType struct) implements SketchStmt {
    @Override
    public String toString() {
      return "struct { "
          + struct.getFields().stream().map(SketchDecl::toString).collect(joining("; "))
          + "}; ";
    }
  }
}
