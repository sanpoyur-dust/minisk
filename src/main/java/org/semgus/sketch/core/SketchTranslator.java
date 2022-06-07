package org.semgus.sketch.core;

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
  // the problem to translate from
  private final SemgusProblem problem;
  // relation names -> non-terminals
  private final Map<String, NonTerminal> nonTerminals;
  private Sketch sketch;

  /**
   * Constructor.
   *
   * @param problem A SemGuS problem
   */
  public SketchTranslator(SemgusProblem problem) {
    this.problem = problem;
    this.nonTerminals = new HashMap<>();
    for (SemgusNonTerminal semgusNonTerminal : problem.nonTerminals().values()) {
      NonTerminal nonTerminal = new NonTerminal(semgusNonTerminal);
      this.nonTerminals.put(nonTerminal.relName(), nonTerminal);
    }
  }

  /**
   * Gets a generator function definition statement for a non-terminal.
   * <p>
   * It consists of:
   * (a). A function declaration: generator [the output type] [the non-terminal name],
   * (b). An argument list containing the input variable declarations,
   * (c). A function body based on the non-terminal productions.
   *
   * @param semgusNonTerminal The SemGuS non-terminal
   * @return a generator function definition statement for a non-terminal
   */
  private SketchStmt.FuncDefStmt getNonTerminalDefStmt(SemgusNonTerminal semgusNonTerminal) {
    SemanticRule rule0 = SketchTranslator.getSemRule0(semgusNonTerminal);
    NonTerminal nonTerminal = nonTerminals.get(rule0.head().name());

    // Also build a struct declaration.
    SketchType structType = new SketchType("", nonTerminal.name());
    sketch.stmts().add(new SketchStmt.StructDefStmt(structType, nonTerminal.outputs()));

    // (a). the function declaration
    SketchType resType = new SketchType("generator", nonTerminal.name());
    SketchDecl resDecl = new SketchDecl(resType, nonTerminal.name());

    // (b). the argument list
    List<SketchDecl> resArgs = nonTerminal.inputs();

    // (c). the function body
    SketchStmt.SeqStmt resBody = new SketchStmt.SeqStmt(new ArrayList<>(), " ");

    // the result
    SketchStmt.FuncDefStmt res = new SketchStmt.FuncDefStmt(resDecl, resArgs, resBody);

    // Update (c).
    resBody.stmts().add(new SketchStmt.AtomStmt("assert", new SketchExpr.AtomExpr("(bnd > 0)")));

    List<SketchExpr> subexprs = new ArrayList<>();
    resBody.stmts().add(new SketchStmt.AtomStmt("return", new SketchExpr.RegExpr(subexprs)));
    int ruleCnt = 0;
    for (SemgusProduction production : semgusNonTerminal.productions().values()) {
      for (SemanticRule rule : production.semanticRules()) {
        // Build a generator function for each semantic rule

        // (a). the function declaration
        String ruleDefName = nonTerminal.name() + "_rule" + (ruleCnt++);
        SketchDecl ruleDefDecl = new SketchDecl(resType, ruleDefName);

        // (c). the function body
        SketchStmt.SeqStmt ruleDefBody = new SketchStmt.SeqStmt(new ArrayList<>(), " ");

        // semantic rule generator function definition
        SketchStmt.FuncDefStmt ruleDef = new SketchStmt.FuncDefStmt(
            ruleDefDecl,
            resArgs,
            ruleDefBody
        );
        sketch.stmts().add(ruleDef);

        for (RelationApp bodyRelation : rule.bodyRelations()) {
          // Call the corresponding generator function for each CHC body relation
          NonTerminal childNonTerminal = nonTerminals.get(bodyRelation.name());

          String varDefName = "";
          for (int i = 0; i < bodyRelation.arguments().size(); i++) {
            TypedVar typedVar = bodyRelation.arguments().get(i);
            if (childNonTerminal.attrs().get(i).isEmpty()) {
              varDefName = typedVar.name();
            }
          }
          List<SketchExpr> varDefCallArgs = new ArrayList<>();
          SketchStmt.VarDefStmt varDefStmt = new SketchStmt.VarDefStmt(
              new SketchDecl(new SketchType("", childNonTerminal.name()), varDefName),
              new SketchExpr.FuncExpr(childNonTerminal.name(), varDefCallArgs)
          );
          ruleDefBody.stmts().add(varDefStmt);

          for (int i = 0; i < bodyRelation.arguments().size(); i++) {
            TypedVar typedVar = bodyRelation.arguments().get(i);
            if (childNonTerminal.attrs().get(i).equals("input")) {
              varDefCallArgs.add(new SketchExpr.AtomExpr(typedVar.name()));
            } else if (childNonTerminal.attrs().get(i).equals("output")) {
              ruleDefBody.stmts().add(new SketchStmt.VarDefStmt(
                  new SketchDecl(new SketchType("", typedVar.type().name()), typedVar.name()),
                  new SketchExpr.AtomExpr(varDefName + "." + childNonTerminal.vars().get(i))
              ));
            }
          }
          varDefCallArgs.add(new SketchExpr.AtomExpr("bnd - 1"));
        }

        // Build the return statement in the rule generator function.
        SketchStmt.VarDefStmt retVarDefStmt = new SketchStmt.VarDefStmt(
            new SketchDecl(structType, nonTerminal.varName()),
            new SketchExpr.FuncExpr(
                structType.toString(),
                nonTerminal.outputs().stream()
                    .filter(sketchDecl -> !sketchDecl.name().equals("bnd"))
                    .map(sketchDecl -> new SketchExpr.AssignExpr(
                        new SketchExpr.AtomExpr(sketchDecl.name()),
                        new SketchExpr.AtomExpr("??")))
                    .collect(Collectors.toList())
            )
        );
        ruleDefBody.stmts().add(retVarDefStmt);
        for (SketchDecl output : nonTerminal.outputs()) {
          ruleDefBody.stmts().add(new SketchStmt.VarDefStmt(
              output,
              new SketchExpr.AtomExpr(nonTerminal.varName() + "." + output.name())
          ));
        }

        // constraint assertion
        SmtTerm constraint = rule.constraint();
        SketchExpr e = eval(constraint);
        ruleDefBody.stmts().add(new SketchStmt.AtomStmt("assert", e));

        ruleDefBody.stmts().add(new SketchStmt.AtomStmt("return", new SketchExpr.AtomExpr(nonTerminal.varName())));

        // Add the call as a part of the return statement in the non-terminal generator function.
        List<SketchExpr> subexprArgs = resArgs.stream()
            .map(sketchDecl -> new SketchExpr.AtomExpr(sketchDecl.name()))
            .collect(Collectors.toList());
        subexprs.add(new SketchExpr.FuncExpr(
            ruleDefName, subexprArgs
        ));
      }
    }
    return res;
  }


  private SketchStmt.FuncDefStmt getHarnessDefStmt() {
    SketchDecl resDecl = new SketchDecl(new SketchType("harness", "void"), "sketch");
    List<SketchDecl> resArgs = new ArrayList<>();
    SketchStmt.SeqStmt resBody = new SketchStmt.SeqStmt(new ArrayList<>(), " ");
    SketchStmt.FuncDefStmt res = new SketchStmt.FuncDefStmt(resDecl, resArgs, resBody);



    return res;
  }

//    for (SmtTerm constraint : problem.constraints()) {
//      if (constraint instanceof SmtTerm.Application app) {
//        List<List<String>> attrLists = semNameToAttrLists.get(app.name().name());
//        int ind = 0;
//        String callName = "";
//        List<SketchExpr> callArgs = new ArrayList<>();
//        SketchExpr outputExpr = null;
//        for (SmtTerm.Application.TypedTerm appArg : app.arguments()) {
//          String typeName = getTypeName(appArg.type().name());
//          List<String> attrList = attrLists.get(ind);
//          if (outputExpr == null && attrList.contains("output")) {
//            outputExpr = eval(appArg.term(), new HashMap<>());
//          } else if (attrList.contains("input")) {
//            callArgs.add(eval(appArg.term(), new HashMap<>()));
//          } else if (attrList.isEmpty()) {
//            callName = eval(appArg.term(), new HashMap<>()).toString();
//          }
//          ++ind;
//        }
//        body = new SketchStmt.SeqStmt(
//            body,
//            " ",
//            new SketchStmt.AtomStmt(
//                "assert",
//                new SketchExpr.BinaryExpr(
//                    new SketchExpr.FuncExpr(callName, callArgs),
//                    "==",
//                    outputExpr
//                ))
//        );
//      }
//    }
//
//    return new SketchStmt.FuncDefStmt(
//        new SketchDecl("harness void", "__sketch"),
//        args,
//        body);

  public Sketch translate() {
    sketch = new Sketch(new ArrayList<>());

    for (SemgusNonTerminal semgusNonTerminal : problem.nonTerminals().values()) {
      // Build a generator function definition for each non-terminal
      sketch.stmts().add(getNonTerminalDefStmt(semgusNonTerminal));

      // Target function
//      if (semDefStmt instanceof SketchStmt.FuncDefStmt s && nonTerminal.equals(problem.targetNonTerminal())) {
//        List<SketchDecl> targetArgs = new ArrayList<>(s.args());
//        List<SketchExpr> callArgs =
//            s.args().stream().map(decl -> new SketchExpr.AtomExpr(decl.name())).collect(Collectors.toList());
//        targetArgs.remove(targetArgs.size() - 1);
//        defs.add(
//            new SketchStmt.FuncDefStmt(
//                new SketchDecl(
//                    s.decl().type().replaceAll("generator ", ""),
//                    problem.targetName()),
//                targetArgs,
////                new SketchStmt.SeqStmt(
////                    new SketchStmt.VarDefStmt(
////                        new SketchDecl("int", "bnd"),
////                        new SketchExpr.AtomExpr("3")
////                    ),
////                    " ",
////                    new SketchStmt.AtomStmt(
////                        "return",
////                        new SketchExpr.FuncExpr(problem.targetNonTerminal().name(), callArgs)))
//            ));
//      }
    }

    sketch.stmts().add(getHarnessDefStmt());
    return sketch;
  }

  /**
   * Gets the first semantic rule of a non-terminal.
   *
   * @param nonTerminal The non-terminal
   * @return the first semantic rule
   */
  public static SemanticRule getSemRule0(SemgusNonTerminal nonTerminal) {
    return nonTerminal.productions().values().iterator().next().semanticRules().get(0);
  }

  private static SketchExpr eval(SmtTerm term) {
    // TODO: support quantifiers
    if (term instanceof SmtTerm.Application app) {
      return switch (app.name().name()) {
        case "+" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term()),
            "+",
            eval(app.arguments().get(1).term()));
        case "-" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term()),
            "-",
            eval(app.arguments().get(1).term()));
        case "*" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term()),
            "*",
            eval(app.arguments().get(1).term()));
        case "/" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term()),
            "/",
            eval(app.arguments().get(1).term()));
        case ">" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term()),
            ">",
            eval(app.arguments().get(1).term()));
        case ">=" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term()),
            ">=",
            eval(app.arguments().get(1).term()));
        case "<" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term()),
            "<",
            eval(app.arguments().get(1).term()));
        case "<=" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term()),
            "<=",
            eval(app.arguments().get(1).term()));
        case "=" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term()),
            "==",
            eval(app.arguments().get(1).term()));
        case "!=" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term()),
            "!=",
            eval(app.arguments().get(1).term()));
        case "and" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term()),
            "&&",
            eval(app.arguments().get(1).term()));
        case "or" -> new SketchExpr.BinaryExpr(
            eval(app.arguments().get(0).term()),
            "||",
            eval(app.arguments().get(1).term()));
        case "not" -> new SketchExpr.UnaryExpr(
            "!",
            eval(app.arguments().get(0).term()));
        case "ite" -> new SketchExpr.CondExpr(
            eval(app.arguments().get(0).term()),
            eval(app.arguments().get(1).term()),
            eval(app.arguments().get(2).term()));
        case "true" -> new SketchExpr.AtomExpr("true");
        case "false" -> new SketchExpr.AtomExpr("false");
        default -> new SketchExpr.AtomExpr(app.name().name());
      };
    }

    if (term instanceof SmtTerm.Variable x) {
      return new SketchExpr.AtomExpr(x.name());
    }

    if (term instanceof SmtTerm.CNumber n) {
      return new SketchExpr.AtomExpr(String.valueOf(n.value()));
    }

    if (term instanceof SmtTerm.CString s) {
      return new SketchExpr.AtomExpr(s.value());
    }

    return new SketchExpr.AtomExpr("");
  }
}
