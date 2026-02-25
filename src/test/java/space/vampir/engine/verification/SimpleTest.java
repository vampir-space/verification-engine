package space.vampir.engine.verification;

import org.junit.jupiter.api.Test;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.mapconverter.map.Converter;
import tools.refinery.mapconverter.map.MapHandler;
import tools.refinery.mapconverter.scope.Circle;
import tools.refinery.mapconverter.scope.Point;
import tools.refinery.mapconverter.scope.Scope;
import tools.refinery.mapconverter.transform.BasicWithTypeRefinementStrategy;
import tools.refinery.mapconverter.transform.ComplexityStrategy;
import tools.refinery.mapconverter.transform.ModelSeedFragment;
import tools.refinery.mapconverter.transform.ModelSeedStrategy;
import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SimpleTest {

    @Test
    void test() {

        // once
        String filePath = "/CrossWalk_6.xodr";
        URL resource = SimpleTest.class.getResource(filePath);
        String fileString =  resource.getFile();
        File file = new File(fileString);

        MapHandler map = new MapHandler(file);
        ModelSeedStrategy outputStrategy = new ModelSeedStrategy();
        ComplexityStrategy<ModelSeedFragment> complexityStrategy = new BasicWithTypeRefinementStrategy<>(outputStrategy);
        Converter<ModelSeedFragment> converter = new Converter<>(map, complexityStrategy);

        // multiple times
        Circle circle = new Circle(new Point(0.0, 0.0), 10000.0);
        Scope<ModelSeedFragment> scope = converter.getScope(circle);
        scope.roadCutter(10.0);

        ModelSeedFragment refineryFragment = scope.translateMap();
        ModelSeed modelSeed = refineryFragment.buildSeed();

        try (Model model = outputStrategy.problemProvider.solve(modelSeed)){

            Symbol<TruthValue> signals=null;
            for(AnySymbol s: model.getStore().getSymbols()) {
                if(s.name()=="SignalObservation::signal") {
                    signals = (Symbol<TruthValue>) s;
                }
            }
            var cursor = model.getInterpretation(signals).getAll();
            while (cursor.move()) {
                Tuple t = cursor.getKey();
                int observation = t.get(0);
                int landmark = t.get(1);
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
