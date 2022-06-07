package org.semgus.sketch.core;

import org.semgus.java.object.AnnotatedVar;
import org.semgus.java.object.TypedVar;
import org.semgus.java.problem.SemanticRule;
import org.semgus.java.problem.SemgusNonTerminal;
import org.semgus.sketch.object.SketchDecl;
import org.semgus.sketch.object.SketchType;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a non-terminal in the Sketch translator.
 */
public record NonTerminal(
    String name,
    String relName,
    String varName,
    List<SketchDecl> inputs,
    List<SketchDecl> outputs,
    List<String> vars,
    List<String> attrs) {
  private static NonTerminal makeNonTerminal(SemgusNonTerminal semgusNonTerminal) {
    String name = semgusNonTerminal.name();

    // Get the first semantic rule of a non-terminal.
    SemanticRule rule0 = SketchTranslator.getSemRule0(semgusNonTerminal);
    String relName = rule0.head().name();

    String varName = "";
    List<SketchDecl> inputs = new ArrayList<>();
    List<SketchDecl> outputs = new ArrayList<>();
    List<String> vars = new ArrayList<>();
    List<String> attrs = new ArrayList<>();
    for (TypedVar typedVar : rule0.head().arguments()) {
      vars.add(typedVar.name());
      // Get the corresponding annotated variable for each typed variable.
      AnnotatedVar annotatedVar = rule0.variables().get(typedVar.name());
      if (annotatedVar.attributes().containsKey("input")) {
        // Add input variables.
        inputs.add(new SketchDecl(new SketchType("", typedVar.type().name()), typedVar.name()));
        attrs.add("input");
      } else if (annotatedVar.attributes().containsKey("output")) {
        // Add output variables.
        outputs.add(new SketchDecl(new SketchType("", typedVar.type().name()), typedVar.name()));
        attrs.add("output");
      } else {
        varName = typedVar.name();
        attrs.add("");
      }
    }
    inputs.add(new SketchDecl(new SketchType("", "Int"), "bnd"));

    return new NonTerminal(name, relName, varName, inputs, outputs, vars, attrs);
  }

  public NonTerminal(SemgusNonTerminal semgusNonTerminal) {
    this(makeNonTerminal(semgusNonTerminal));
  }

  public NonTerminal(NonTerminal nonTerminal) {
    this(
        nonTerminal.name,
        nonTerminal.relName,
        nonTerminal.varName,
        nonTerminal.inputs,
        nonTerminal.outputs,
        nonTerminal.vars,
        nonTerminal.attrs);
  }
}
