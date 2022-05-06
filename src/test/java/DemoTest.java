import org.junit.jupiter.api.Test;
import org.semgus.java.event.EventParser;
import org.semgus.java.event.SpecEvent;
import org.semgus.java.problem.ProblemGenerator;
import org.semgus.java.problem.SemgusProblem;
import org.semgus.sketch.util.SemgusParserRunner;
import org.semgus.sketch.object.Sketch;
import org.semgus.sketch.core.SketchTranslator;

import java.io.File;
import java.util.List;

public class DemoTest {
  @Test
  void demo() {
    String benchmarkPath = "/benchmarks/max2-exp.sl";
//    String benchmarkPath = "/benchmarks/sum-by-while.sl";

    try {
      String uri = new File(getClass().getResource(benchmarkPath).toURI()).toString();
      String json = SemgusParserRunner.run(uri);

      SemgusProblem problem = ProblemGenerator.parse(json);
//      System.out.println(problem.dump());

      List<SpecEvent> events = EventParser.parse(json);
//      System.out.println(events);

      SketchTranslator translator = new SketchTranslator(problem, events);
      Sketch sketch = translator.translate();
      System.out.println(sketch.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
