package de.typology.smoothing;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

public class PrintSmootherPropabilitiesByLength extends AbcCorpusTest {

    @Test
    @Ignore
    public void printMaximumLikelihoodSmoother() throws IOException {
        Smoother smoother = newMaximumLikelihoodSmoother();
        printPropabilitiesByLength(smoother, 5);
    }

    @Test
    @Ignore
    public void printDiscountSmoother() throws IOException {
        Smoother smoother = newDiscountSmoother(1.);
        printPropabilitiesByLength(smoother, 5);
    }

    @Test
    @Ignore
    public void printPropabilityCond2Smoother() throws IOException {
        Smoother smoother = newPropabilityCond2Smoother(1.);
        printPropabilitiesByLength(smoother, 5);
    }

    @Test
    public void printInterpolatedKneserNeySmoother() throws IOException {
        Smoother smoother = newInterpolatedKneserNeySmoother();
        printPropabilitiesByLength(smoother, 5);
    }

    private void printPropabilitiesByLength(Smoother smoother, int length) {
        Map<Integer, Map<String, Double>> propabilitiesByLength =
                new LinkedHashMap<Integer, Map<String, Double>>();
        for (int i = 1; i != length + 1; ++i) {
            propabilitiesByLength
                    .put(i, calcSequencePropabilities(smoother, i));
        }

        System.out.println(smoother.getClass().getSimpleName());
        for (Map<String, Double> propabilities : propabilitiesByLength.values()) {
            System.out.println("---");
            printPropabilities(propabilities);
        }
        System.out.println("===\n");
    }

    private Map<String, Double> calcSequencePropabilities(
            Smoother smooter,
            int length) {
        Map<String, Double> propabilities = new LinkedHashMap<String, Double>();

        for (int i = 0; i != ((int) Math.pow(3, length)); ++i) {
            String sequence = getAbcSequence(i, length);
            propabilities.put(sequence, smooter.propability(sequence));
        }

        return propabilities;
    }

    private void printPropabilities(Map<String, Double> propabilities) {
        double sum = 0;
        for (Map.Entry<String, Double> sequencePropability : propabilities
                .entrySet()) {
            String sequence = sequencePropability.getKey();
            double propability = sequencePropability.getValue();

            sum += propability;

            System.out.println(sequence + " -> " + propability);
        }
        System.out.println("sum = " + sum);
    }

}