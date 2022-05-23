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
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SketchTranslator {
  SemgusProblem problem;
  Map<String, List<List<String>>> semNameToAttrLists;
  Map<String, SketchType> semNameToOutputType;

  public SketchTranslator(SemgusProblem problem, List<SpecEvent> events) {
    this.problem = problem;
    this.semNameToAttrLists = new HashMap<>();
    this.semNameToOutputType = new HashMap<>();

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

    for (SemgusNonTerminal nonTerminal : problem.nonTerminals().values()) {
      // there must be at least one rule, right?
      SemanticRule rule0 = nonTerminal.productions().values().iterator().next().semanticRules().get(0);
      String semName = rule0.head().name();
      List<TypedVar> semArgs = rule0.head().arguments();

      List<List<String>> attrLists = semNameToAttrLists.get(semName);

      // TODO: multiple output variables
      List<SketchDecl> output = IntStream.range(0, semArgs.size())
          .filter(x -> attrLists.get(x).contains("output"))
          .mapToObj(semArgs::get)
          .map(arg -> new SketchDecl(getType(arg.type().name()), arg.name()))
          .collect(Collectors.toList());

      semNameToOutputType.put(semName, output.size() == 1 ?
          output.get(0).type() :
          new SketchType.StructType(semName + "0", output));
    }
  }

  private SketchType getType(String input) {
    return switch (input) {
      case "Int" -> new SketchType.IntType();
      case "Bool" -> new SketchType.BitType();
      default -> //new SketchType.SpecialType(input);
      throw new IllegalStateException("type " + input + " not distinguishable");
    };
  }

  private SketchExpr eval(SmtTerm term, Map<String, SketchExpr> dict) {
    // TODO: support quantifiers
    if (term instanceof SmtTerm.Application app) {
      return switch (app.name().name()) {
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
        case "=" -> new SketchExpr.BinaryExpr(
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
        case "ite" -> new SketchExpr.CondExpr(
            eval(app.arguments().get(0).term(), dict),
            eval(app.arguments().get(1).term(), dict),
            eval(app.arguments().get(2).term(), dict));
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

  List<SmtTerm> getEqualClauses(SmtTerm term) {
    if (term instanceof SmtTerm.Application app)
      return switch (app.name().name()) {
        case "=" -> List.of(term);
        case "and" ->
          Stream.of(getEqualClauses(app.arguments().get(0).term()),
                getEqualClauses(app.arguments().get(1).term()))
              .flatMap(Collection::stream)
              .collect(Collectors.toList());
        default -> throw new IllegalStateException(term + " cannot be splitted");
      };
    else
      throw new IllegalStateException(term + " cannot be splitted");
  }

  private SketchStmt getSemDefStmt(SemgusNonTerminal nonTerminal) {
    // there must be at least one rule, right?
    SemanticRule rule0 = nonTerminal.productions().values().iterator().next().semanticRules().get(0);
    String semName = rule0.head().name();
    String selfName = "";
    List<TypedVar> semArgs = rule0.head().arguments();

    // TODO: multiple output variables
    List<SketchDecl> output = new ArrayList<>();
    List<SketchDecl> args = new ArrayList<>();

    List<SketchStmt.FuncDefStmt> funcs = new ArrayList<>();

    List<List<String>> attrLists = semNameToAttrLists.get(semName);
    int ind = 0;
    for (TypedVar semArg : semArgs) {
      // TODO: support bit vectors
      List<String> attrList = attrLists.get(ind);
      if (attrList.contains("output")) {
        output.add(new SketchDecl(getType(semArg.type().name()), semArg.name()));
      } else if (attrList.contains("input")) {
        args.add(new SketchDecl(getType(semArg.type().name()), semArg.name()));
      } else {
        selfName = semArg.name();
      }
      ++ind;
    }
    SketchType outputType = output.size() == 1 ?
        output.get(0).type() :
        new SketchType.StructType(semName + "0", output);
    // TODO: split this out
    semNameToOutputType.put(semName, outputType);
    args.add(new SketchDecl(new SketchType.IntType(), "bnd"));

    List<SketchExpr> exprs = new ArrayList<>();
    boolean fl = false;
    for (SemgusProduction production : nonTerminal.productions().values()) {
      // TODO: why are there multiple rules?
      SemanticRule rule = production.semanticRules().get(0);

      Map<String, SketchExpr> callExprs = new HashMap<>();
      List<SketchStmt> bodyStmts = new ArrayList<>();
      for (RelationApp rel : rule.bodyRelations()) {
        List<String> callKeys = new ArrayList<>();
        List<SketchExpr> callExprArgs = new ArrayList<>();
        int callInd = 0;
        TypedVar selfVar = null;
        for (TypedVar relArg : rel.arguments()) {
          List<String> attrList = semNameToAttrLists.get(rel.name()).get(callInd);
          if (attrList.contains("output")) {
            callKeys.add(relArg.name());
          } else if (attrList.contains("input")) {
            // TODO: why not eval here?
            callExprArgs.add(new SketchExpr.ConstExpr(relArg.name()));
          } else if (attrList.isEmpty()) {
            selfVar = relArg;
          }
          ++callInd;
        }
        if (selfVar == null) {
          throw new RuntimeException(rel + " selfVar is null");
        }
        callExprArgs.add(new SketchExpr.ConstExpr("bnd - 1"));
        if (callKeys.size() == 1) { // TODO: change condition
          if (outputType.size() == 1)
            callExprs.put(callKeys.get(0), new SketchExpr.FuncExpr(selfVar.type().name(), callExprArgs));
          else {
            SketchType curOutputType = semNameToOutputType.get(rel.name());
            bodyStmts.add(new SketchStmt.VarDefStmt(
                new SketchDecl(curOutputType, selfVar.name()),
                new SketchExpr.FuncExpr(selfVar.type().name(), callExprArgs)
            ));
            for (int i = 0; i < callKeys.size(); i++) {
              callExprs.put(
                  callKeys.get(i),
                  new SketchExpr.ConstExpr(selfVar.name())
              );
            }
          }
        } else {
          fl = true;
          SketchType curOutputType = semNameToOutputType.get(rel.name());
          if (callKeys.size() != ((SketchType.StructType) curOutputType).getFields().size()) {
            throw new RuntimeException("callKeys size and outputType size mismatch");
          }
          bodyStmts.add(
              new SketchStmt.VarDefStmt(
                  new SketchDecl(outputType, selfVar.name()),
                  new SketchExpr.FuncExpr(selfVar.type().name(), callExprArgs)));
          for (int i = 0; i < callKeys.size(); i++) {
            callExprs.put(callKeys.get(i), new SketchExpr.FieldExpr(new SketchExpr.ConstExpr(selfVar.name()),
                ((SketchType.StructType) curOutputType).getFields().get(i).name()));
          }
        }
      }

      SmtTerm constraint = rule.constraint();
      List<SmtTerm> clauses = getEqualClauses(rule.constraint());
      List<SketchExpr> es = clauses.stream().map(x -> eval(x, callExprs)).toList();
      SketchExpr e = eval(constraint, callExprs);
      if (es.size() == 1 && !fl) {
        if (es.get(0) instanceof SketchExpr.BinaryExpr be && be.binop().equals("==")) {
          if (be.lhs().toString().equals(output.get(0).name())) {
            exprs.add(be.rhs());
          } else if (be.rhs().toString().equals(output.get(0).name())) {
            exprs.add(be.lhs());
          }
        } else {
          throw new IllegalStateException(e + " Not a well-formed semantics relation.");
        }
      } else {
        if (outputType instanceof SketchType.StructType) {
          bodyStmts.add(new SketchStmt.VarDefStmt(new SketchDecl(outputType, selfName),
              new SketchExpr.ConstExpr("new " + outputType + "()")));
          bodyStmts.addAll(es.stream().map(x -> {
            if (x instanceof SketchExpr.BinaryExpr xe && xe.binop().equals("==")) {
              return new SketchStmt.AssignStmt(xe.lhs(), xe.rhs());
            } else {
              throw new IllegalStateException("this assignment is illegal");
            }
          }).toList());
          bodyStmts.add(new SketchStmt.ConstStmt("return ", new SketchExpr.ConstExpr(selfName)));
        } else {
          if (es.get(0) instanceof SketchExpr.BinaryExpr xe && xe.binop().equals("=="))
            bodyStmts.add(new SketchStmt.ConstStmt("return ", xe.rhs()));
          else
            throw new IllegalStateException("this assignment is illegal");
        }
        funcs.add(new SketchStmt.FuncDefStmt(
            "generator",
            new SketchDecl(outputType, production.operator()),
            args.subList(0, args.size() - 1),
            SketchStmt.fromList(bodyStmts, " ")
        ));
        exprs.add(new SketchExpr.FuncExpr(
            production.operator(),
            args.stream().map(x -> (SketchExpr)new SketchExpr.ConstExpr(x.name())).toList()));
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

    funcs.add(new SketchStmt.FuncDefStmt(
        "generator",
        new SketchDecl(outputType, nonTerminal.name()),
        args,
        body));

    return SketchStmt.fromList(funcs.stream().map(x -> (SketchStmt)x).toList(), "\n");
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

    return new SketchStmt.FuncDefStmt(
        "harness",
        new SketchDecl(new SketchType.VoidType(), "__sketch"),
        args,
        body);
  }

  public Sketch translate() {
    List<SketchStmt> defs = new ArrayList<>();

    for (SemgusNonTerminal nonTerminal : problem.nonTerminals().values()) {
      SketchStmt semDefStmt = getSemDefStmt(nonTerminal);

      defs.add(semDefStmt);

      // target function
      if (semDefStmt instanceof SketchStmt.FuncDefStmt s && nonTerminal.equals(problem.targetNonTerminal())) {
        List<SketchDecl> targetArgs = new ArrayList<>(s.args());
        List<SketchExpr> callArgs =
            s.args().stream().map(decl -> new SketchExpr.ConstExpr(decl.name())).collect(Collectors.toList());
        // TODO: Check output one by one
        targetArgs.remove(targetArgs.size() - 1);
        defs.add(
            new SketchStmt.FuncDefStmt(
                "",
                new SketchDecl(
                    s.decl().type(),
                    problem.targetName()),
                targetArgs,
                new SketchStmt.SeqStmt(
                    new SketchStmt.VarDefStmt(
                        new SketchDecl(new SketchType.IntType(), "bnd"),
                        new SketchExpr.ConstExpr("3")
                    ),
                    " ",
                    new SketchStmt.ConstStmt(
                        "return",
                        new SketchExpr.FuncExpr(problem.targetNonTerminal().name(), callArgs)))
            ));
      }
    }

    defs.add(getHarnessDefStmt());
    return new Sketch(defs);
  }
}
