import org.junit.jupiter.api.Test;
import org.semgus.java.problem.ProblemGenerator;
import org.semgus.java.problem.SemgusProblem;
import org.semgus.sketch.core.SketchTranslator;
import org.semgus.sketch.object.Sketch;
import org.semgus.sketch.util.SemgusParserRunner;

import java.io.File;

public class DemoTest {
  @Test
  void demo() {
    String benchmarkPath = "/benchmarks/max2-exp.sl";

    try {
      String uri = new File(getClass().getResource(benchmarkPath).toURI()).toString();
      String json = SemgusParserRunner.run(uri);

      SemgusProblem problem = ProblemGenerator.parse(json);
//      System.out.println(problem.dump());

      SketchTranslator translator = new SketchTranslator(problem);
      Sketch sketch = translator.translate();
      System.out.println(sketch.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
