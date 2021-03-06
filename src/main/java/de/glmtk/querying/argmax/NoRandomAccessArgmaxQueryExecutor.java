package de.glmtk.querying.argmax;

import static com.google.common.collect.Iterators.peekingIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.google.common.collect.PeekingIterator;

import de.glmtk.cache.CompletionTrieCache;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.counts.Discounts;
import de.glmtk.exceptions.SwitchCaseNotImplementedException;
import de.glmtk.querying.estimator.weightedsum.WeightedSumEstimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumFunction;
import de.glmtk.querying.estimator.weightedsum.WeightedSumFunction.Summand;
import de.glmtk.util.StringUtils;
import de.glmtk.util.completiontrie.CompletionTrie;
import de.glmtk.util.completiontrie.CompletionTrieEntry;


public class NoRandomAccessArgmaxQueryExecutor implements ArgmaxQueryExecutor {
    public enum ProbabilityDislay {
        EXACT,

        AVERAGE,

        LOWER_BOUND,

        UPPER_BOUND
    }

    private WeightedSumEstimator estimator;
    private CompletionTrieCache cache;
    private Collection<String> vocab;
    private ProbabilityDislay probabilityDislay;
    private int numSortedAccesses;
    private WeightedSumFunction weightedSumFunction;
    private NGram[] histories;
    private long[] lastCounts;

    private class ArgmaxObject {
        public String sequence;
        public double[] alphas;
        public boolean done;

        public double upperBoundCache;
        public int upperBoundCalc = -1;

        public double lowerBoundCache;
        public int lowerBoundCalc = -1;

        public double upperBound() {
            if (upperBoundCalc == numSortedAccesses) {
                return upperBoundCache;
            }

            double args[] = new double[alphas.length];
            for (int i = 0; i != alphas.length; ++i) {
                if (alphas[i] == 0) {
                    args[i] =
                        calcAlpha(histories[i].concat(sequence), lastCounts[i]);
                } else {
                    args[i] = alphas[i];
                }
            }
            upperBoundCache = calcProbability(weightedSumFunction, args);
            upperBoundCalc = numSortedAccesses;
            return upperBoundCache;
        }

        public double lowerBound() {
            if (lowerBoundCalc == numSortedAccesses) {
                return lowerBoundCache;
            }
            lowerBoundCache = calcProbability(weightedSumFunction, alphas);
            lowerBoundCalc = numSortedAccesses;
            return lowerBoundCache;
        }

        @Override
        public int hashCode() {
            return sequence.hashCode();
        }
    }

    public static final Comparator<ArgmaxObject> ARGMAX_OBJECT_COMPARATOR =
        new Comparator<ArgmaxObject>() {
            @Override
            public int compare(ArgmaxObject lhs,
                               ArgmaxObject rhs) {
                int cmp = -Double.compare(lhs.lowerBound(), rhs.lowerBound());
                if (cmp != 0) {
                    return cmp;
                }

                return -Double.compare(rhs.upperBound(), rhs.upperBound());
            }
        };

    public NoRandomAccessArgmaxQueryExecutor(WeightedSumEstimator estimator,
                                             CompletionTrieCache cache,
                                             Collection<String> vocab) {
        this.estimator = estimator;
        this.cache = cache;
        this.vocab = vocab;
        probabilityDislay = ProbabilityDislay.AVERAGE;
    }

    public NoRandomAccessArgmaxQueryExecutor(WeightedSumEstimator estimator,
                                             CompletionTrieCache cache) {
        this(estimator, cache, null);
    }

    public ProbabilityDislay getProbbabilityDislay() {
        return probabilityDislay;
    }

    public void setProbbabilityDislay(ProbabilityDislay probbabilityDislay) {
        probabilityDislay = probbabilityDislay;
    }

    @Override
    public List<ArgmaxResult> queryArgmax(String history,
                                          int numResults) {
        return queryArgmax(history, "", numResults);
    }

    @Override
    public List<ArgmaxResult> queryArgmax(String history,
                                          String prefix,
                                          int numResults) {
        numSortedAccesses = 0;

        if (numResults == 0) {
            return new ArrayList<>();
        }
        if (numResults < 0) {
            throw new IllegalArgumentException("numResults must be positive.");
        }

        NGram hist = new NGram(StringUtils.split(history, ' '));
        weightedSumFunction = estimator.calcWeightedSumFunction(hist);

        int size = weightedSumFunction.size();
        if (size == 0) {
            // TODO: what to do here?
            return new ArrayList<>();
        }

        Pattern[] patterns = weightedSumFunction.getPatterns();
        histories = weightedSumFunction.getHistories();
        CompletionTrie[] tries = new CompletionTrie[size];
        @SuppressWarnings("unchecked")
        Iterator<CompletionTrieEntry>[] iters = new Iterator[size];
        lastCounts = new long[size];

        for (int i = 0; i != size; ++i) {
            Pattern pattern = patterns[i];
            String hi = histories[i].toString();
            String h = hi.isEmpty() ? prefix : hi + " " + prefix;

            CompletionTrie trie = cache.getCountCompletionTrie(pattern);
            PeekingIterator<CompletionTrieEntry> iter =
                peekingIterator(trie.getCompletions(h));

            tries[i] = trie;
            iters[i] = iter;
            if (iter.hasNext()) {
                lastCounts[i] = iter.peek().getScore();
            } else {
                lastCounts[i] = 0;
            }
        }

        Map<String, ArgmaxObject> objects = new HashMap<>();
        PriorityQueue<ArgmaxObject> queue =
            new PriorityQueue<>(11, ARGMAX_OBJECT_COMPARATOR);
        List<ArgmaxResult> results = new ArrayList<>(numResults);

        List<Integer> ptrs = new ArrayList<>(size);
        for (int i = 0; i != size; ++i) {
            ptrs.add(i);
        }

        Iterator<Integer> ptrIter = ptrs.iterator();
        while (results.size() != numResults) {
            if (!ptrIter.hasNext()) {
                if (ptrs.isEmpty()) {
                    break;
                }
                ptrIter = ptrs.iterator();
            }
            int ptr = ptrIter.next();

            Iterator<CompletionTrieEntry> iter = iters[ptr];
            if (!iter.hasNext()) {
                lastCounts[ptr] = 0;
                ptrIter.remove();
                continue;
            }

            CompletionTrieEntry entry = iter.next();
            ++numSortedAccesses;
            lastCounts[ptr] = entry.getScore();

            String string = entry.getString();
            int lastSpacePos = string.lastIndexOf(' ');
            String sequence = string;
            if (lastSpacePos != -1) {
                sequence = string.substring(lastSpacePos + 1);
            }

            if (vocab != null && !vocab.contains(sequence)) {
                continue;
            }

            ArgmaxObject curObject = objects.get(sequence);
            if (curObject == null) {
                curObject = new ArgmaxObject();
                objects.put(sequence, curObject);
                curObject.sequence = sequence;
                curObject.done = false;
                curObject.alphas = new double[size];
                for (int i = 0; i != size; ++i) {
                    curObject.alphas[i] = 0;
                }
            } else if (!curObject.done) {
                queue.remove(curObject);
            }
            if (!curObject.done) {
                curObject.alphas[ptr] = calcAlpha(
                    histories[ptr].concat(sequence), entry.getScore());
                queue.add(curObject);
            }

            while (results.size() != numResults) {
                ArgmaxObject object = queue.remove();
                if (queue.isEmpty()
                    || object.lowerBound() < queue.peek().upperBound()) {
                    queue.add(object);
                    break;
                }

                results.add(
                    new ArgmaxResult(object.sequence, calcDisplayProbability(
                        weightedSumFunction, histories, object)));
                object.done = true;
            }
        }

        for (int i = results.size(); i < numResults; ++i) {
            if (queue.isEmpty()) {
                break;
            }
            ArgmaxObject object = queue.remove();
            if (object.upperBound() == 0.0) {
                break;
            }
            results
                .add(new ArgmaxResult(object.sequence, calcDisplayProbability(
                    weightedSumFunction, histories, object)));
        }

        return results;
    }

    private double
            calcDisplayProbability(WeightedSumFunction weightedSumFunction,
                                   NGram[] histories,
                                   ArgmaxObject object) {
        switch (probabilityDislay) {
            case EXACT:
                int size = histories.length;
                double args[] = new double[size];
                for (int i = 0; i != size; ++i) {
                    if (object.alphas[i] == 0) {
                        args[i] =
                            calcAlpha(histories[i].concat(object.sequence));
                    } else {
                        args[i] = object.alphas[i];
                    }
                }
                return calcProbability(weightedSumFunction, args);
            case AVERAGE:
                return (object.lowerBound() + object.upperBound()) / 2;
            case LOWER_BOUND:
                return object.lowerBound();
            case UPPER_BOUND:
                return object.upperBound();
            default:
                throw new SwitchCaseNotImplementedException();
        }
    }

    private double calcProbability(WeightedSumFunction weightedSumFunction,
                                   double[] args) {
        double prob = 0;

        int i = 0;
        for (Summand summand : weightedSumFunction) {
            prob += summand.getWeight() * args[i];
            ++i;
        }

        return prob;
    }

    /**
     * For when we want alpha of random access sequence.
     */
    private double calcAlpha(NGram sequence) {
        return calcAlpha(sequence, cache.getCount(sequence));
    }

    /**
     * For when we want alpha of sequence we have the count for.
     */
    private double calcAlpha(NGram sequence,
                             long count) {
        if (count == 0) {
            return 0;
        }

        long absSequenceCount = count;

        if (!sequence.getPattern().isAbsolute()) {
            sequence = sequence.remove(0).convertWskpToSkp();
            absSequenceCount = cache.getCount(sequence);
        }

        if (sequence.getPattern().numElems(PatternElem.CNT) == 1) {
            // If we are on last order don't discount.
            return count;
        }

        Discounts discounts = cache.getDiscounts(sequence.getPattern());
        double d = discounts.getForCount(absSequenceCount);

        return Math.max(count - d, 0.0);
    }

    public int getNumSortedAccesses() {
        return numSortedAccesses;
    }
}
