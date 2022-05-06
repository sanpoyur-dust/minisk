package org.semgus.sketch.core;

import org.semgus.java.event.SemgusSpecEvent;
import org.semgus.java.event.SpecEvent;
import org.semgus.java.object.RelationApp;
import org.semgus.java.object.SmtTerm;
import org.semgus.java.object.TypedVar;
import org.semgus.java.problem.SemanticRule;
import org.semgus.java.problem.SemgusNonTerminal;
import org.semgus.java.problem.SemgusProblem;
import org.semgus.java.problem.SemgusProduction;
import org.semgus.sketch.object.*;

import java.util.*;
import java.util.stream.Collectors;

public class SketchTranslator {
  SemgusProblem problem;
  Map<String, List<List<String>>> semNameToAttrLists;

  public SketchTranslator(SemgusProblem problem, List<SpecEvent> events) {
    this.problem = problem;
    this.semNameToAttrLists = new HashMap<>();

    for (SpecEvent event : events) {
      if (event instanceof SemgusSpecEvent.HornClauseEvent hce) {
        String semName = hce.head().name();
        if (semNameToAttrLists.containsKey(semName)) {
          continue;
        }
        semNameToAttrLists.put(semName, new ArrayList<>());
        List<List<String>> attrLists = semNameToAttrLists.get(semName);

        // the argument order matters
        for (TypedVar arg : hce.head().arguments()) {
          attrLists.add(new ArrayList<>(hce.variables().get(arg.name()).attributes().keySet()));
        }
      }
    }
  }

  private String getTypeName(String input) {
    return switch (input) {
      case "Int" -> "int";
      case "Bool" -> "bit";
      default -> input;
    };
  }

  private SketchExpr eval(SmtTerm term, Map<String, SketchExpr> dict) {
    // TODO: support quantifiers
    if (term instanceof SmtTerm.Application app) {
      return switch (app.name().name()) {
        case "=" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term(), dict),
            "=",
            eval(app.arguments().get(1).term(), dict));
        case "+" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term(), dict),
            "+",
            eval(app.arguments().get(1).term(), dict));
        case "-" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term(), dict),
            "-",
            eval(app.arguments().get(1).term(), dict));
        case "*" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term(), dict),
            "*",
            eval(app.arguments().get(1).term(), dict));
        case "/" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term(), dict),
            "/",
            eval(app.arguments().get(1).term(), dict));
        case ">" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term(), dict),
            ">",
            eval(app.arguments().get(1).term(), dict));
        case ">=" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term(), dict),
            ">=",
            eval(app.arguments().get(1).term(), dict));
        case "<" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term(), dict),
            "<",
            eval(app.arguments().get(1).term(), dict));
        case "<=" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term(), dict),
            "<=",
            eval(app.arguments().get(1).term(), dict));
        case "==" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term(), dict),
            "==",
            eval(app.arguments().get(1).term(), dict));
        case "!=" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term(), dict),
            "!=",
            eval(app.arguments().get(1).term(), dict));
        case "and" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term(), dict),
            "&&",
            eval(app.arguments().get(1).term(), dict));
        case "or" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term(), dict),
            "||",
            eval(app.arguments().get(1).term(), dict));
        case "not" -> new SketchExpr.ConstExpr(
            "!" + eval(app.arguments().get(0).term(), dict));
        case "ite" -> new SketchExpr.FuncExpr(
            "__ite",
            app.arguments().stream().map(t -> eval(t.term(), dict)).collect(Collectors.toList())
        );
        case "true" -> new SketchExpr.ConstExpr("true");
        case "false" -> new SketchExpr.ConstExpr("false");
        default -> new SketchExpr.ConstExpr(app.name().name());
      };
    }

    if (term instanceof SmtTerm.Variable x) {
      return dict.getOrDefault(x.name(), new SketchExpr.ConstExpr(x.name()));
    }

    if (term instanceof SmtTerm.CNumber n) {
      return new SketchExpr.ConstExpr(String.valueOf(n.value()));
    }

    if (term instanceof SmtTerm.CString s) {
      return new SketchExpr.ConstExpr(s.value());
    }

    // TODO: support bit vectors

    return new SketchExpr.ConstExpr("");
  }

  private SketchStmt getSemDefStmt(SemgusNonTerminal nonTerminal) {
    // there must be at least one rule, right?
    SemanticRule rule0 = nonTerminal.productions().values().iterator().next().semanticRules().get(0);
    String semName = rule0.head().name();
    List<TypedVar> semArgs = rule0.head().arguments();

    // TODO: multiple output variables
    String outputName = "";
    String outputTypeName = "";
    List<SketchDecl> args = new ArrayList<>();

    List<List<String>> attrLists = semNameToAttrLists.get(semName);
    int ind = 0;
    for (TypedVar semArg : semArgs) {
      // TODO: support bit vectors
      String typeName = getTypeName(semArg.type().name());
      List<String> attrList = attrLists.get(ind);
      if (outputName.isEmpty() && attrList.contains("output")) {
        outputName = semArg.name();
        outputTypeName = typeName;
      } else if (attrList.contains("input")) {
        args.add(new SketchDecl(typeName, semArg.name()));
      }
      ++ind;
    }
    args.add(new SketchDecl("int", "bnd"));

    List<SketchExpr> exprs = new ArrayList<>();
    for (SemgusProduction production : nonTerminal.productions().values()) {
      // TODO: why are there multiple rules?
      SemanticRule rule = production.semanticRules().get(0);

      Map<String, SketchExpr> callExprs = new HashMap<>();
      for (RelationApp rel : rule.bodyRelations()) {
        String callKey = "";
        String callName = "";
        List<SketchExpr> callExprArgs = new ArrayList<>();
        int callInd = 0;
        for (TypedVar relArg : rel.arguments()) {
          String typeName = getTypeName(relArg.type().name());
          List<String> attrList = semNameToAttrLists.get(rel.name()).get(callInd);
          if (callKey.isEmpty() && attrList.contains("output")) {
            callKey = relArg.name();
          } else if (attrList.contains("input")) {
            // TODO: why not eval here?
            callExprArgs.add(new SketchExpr.ConstExpr(relArg.name()));
          } else if (attrList.isEmpty()) {
            callName = typeName;
          }
          ++callInd;
        }
        callExprArgs.add(new SketchExpr.ConstExpr("bnd - 1"));
        callExprs.put(callKey, new SketchExpr.FuncExpr(
            callName, callExprArgs
        ));
      }

      SmtTerm constraint = rule.constraint();
      SketchExpr e = eval(constraint, callExprs);
      if (e instanceof SketchExpr.BinaryExpr be && be.binop().equals("=")) {
        if (be.lhs().toString().equals(outputName)) {
          exprs.add(be.rhs());
        } else if (be.rhs().toString().equals(outputName)) {
          exprs.add(be.lhs());
        }
      } else {
        throw new IllegalStateException("Cannot handle complicated nonterminal constraint.");
      }
    }

    SketchExpr regex =
        new SketchExpr.RegExpr(exprs);

    SketchStmt bndCheck =
        new SketchStmt.ConstStmt("assert", new SketchExpr.ConstExpr("bnd > 0"));
    SketchStmt ret =
        new SketchStmt.ConstStmt("return", regex);

    SketchStmt body =
        new SketchStmt.SeqStmt(bndCheck, " ", ret);

    return new SketchStmt.funcDefStmt(
        new SketchDecl("generator " + outputTypeName, nonTerminal.name()),
        args,
        body);
  }

  private SketchStmt getHarnessDefStmt() {
    // TODO: quantifiers
    List<SketchDecl> args = new ArrayList<>();

    // TODO: harness body
    SketchStmt body = new SketchStmt.ConstStmt("assert", new SketchExpr.ConstExpr("true"));

    for (SmtTerm constraint : problem.constraints()) {
      if (constraint instanceof SmtTerm.Application app) {
        List<List<String>> attrLists = semNameToAttrLists.get(app.name().name());
        int ind = 0;
        String callName = "";
        List<SketchExpr> callArgs = new ArrayList<>();
        SketchExpr outputExpr = null;
        for (SmtTerm.Application.TypedTerm appArg : app.arguments()) {
          // TODO: support bit vectors
          String typeName = getTypeName(appArg.type().name());
          List<String> attrList = attrLists.get(ind);
          if (outputExpr == null && attrList.contains("output")) {
            outputExpr = eval(appArg.term(), new HashMap<>());
          } else if (attrList.contains("input")) {
            callArgs.add(eval(appArg.term(), new HashMap<>()));
          } else if (attrList.isEmpty()) {
            callName = eval(appArg.term(), new HashMap<>()).toString();
          }
          ++ind;
        }
        body = new SketchStmt.SeqStmt(
            body,
            " ",
            new SketchStmt.ConstStmt(
                "assert",
                new SketchExpr.BinaryExpr(
                    new SketchExpr.FuncExpr(callName, callArgs),
                    "==",
                    outputExpr
                ))
        );
      }
    }

    return new SketchStmt.funcDefStmt(
        new SketchDecl("harness void", "__sketch"),
        args,
        body);
  }

  public Sketch translate() {
    List<SketchStmt> defs = new ArrayList<>();

    for (SemgusNonTerminal nonTerminal : problem.nonTerminals().values()) {
      SketchStmt semDefStmt = getSemDefStmt(nonTerminal);
      defs.add(semDefStmt);

      // target function
      if (semDefStmt instanceof SketchStmt.funcDefStmt s && nonTerminal.equals(problem.targetNonTerminal())) {
        List<SketchDecl> targetArgs = new ArrayList<>(s.args());
        List<SketchExpr> callArgs =
            s.args().stream().map(decl -> new SketchExpr.ConstExpr(decl.name())).collect(Collectors.toList());
        targetArgs.remove(targetArgs.size() - 1);
        defs.add(
            new SketchStmt.funcDefStmt(
                new SketchDecl(
                    s.decl().type().replaceAll("generator ", ""),
                    problem.targetName()),
                targetArgs,
                new SketchStmt.SeqStmt(
                    new SketchStmt.varDefStmt(
                        new SketchDecl("int", "bnd"),
                        new SketchExpr.ConstExpr("3")
                    ),
                    " ",
                    new SketchStmt.ConstStmt(
                        "return",
                        new SketchExpr.FuncExpr(problem.targetNonTerminal().name(), callArgs)))
            ));
      }
    }

    defs.add(
        new SketchStmt.funcDefStmt(
            new SketchDecl("int", "__ite"),
            Arrays.asList(
                new SketchDecl("bit", "i"),
                new SketchDecl("int", "t"),
                new SketchDecl("int", "e")),
            new SketchStmt.CondStmt(
                new SketchExpr.ConstExpr("i"),
                new SketchStmt.ConstStmt("return", new SketchExpr.ConstExpr("t")),
                new SketchStmt.ConstStmt("return", new SketchExpr.ConstExpr("e")))
        ));

    defs.add(getHarnessDefStmt());
    return new Sketch(defs);
  }
}
