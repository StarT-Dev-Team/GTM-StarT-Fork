package com.gregtechceu.gtceu.common.valueprovider.distribution;

import com.gregtechceu.gtceu.common.valueprovider.WeightedInt;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Factory methods for {@link IntDistribution}s, exposed to KubeJS as {@code Distributions} */
public final class Distributions {

    private Distributions() {}

    /** A bell curve centered in the middle of the range, with a spread of a sixth of the range */
    public static final DistributionFamily GAUSS = new DistributionFamily() {

        @Override
        public WeightedInt build(int min, int max) {
            return gaussOf((min + max) / 2.0, defaultSd(min, max)).build(min, max);
        }

        @Override
        public IntDistribution withAverage(double average) {
            return new IntDistribution() {

                @Override
                public WeightedInt build(int min, int max) {
                    checkRange(min, max);
                    checkAverage(average, min, max);

                    return gaussOf(average, defaultSd(min, max)).build(min, max);
                }
            };
        }
    };

    /** A binomial distribution with a chance of 0.5, where n is max - min */
    public static final DistributionFamily BINOMIAL = new DistributionFamily() {

        @Override
        public WeightedInt build(int min, int max) {
            return binomial(0.5).build(min, max);
        }

        @Override
        public IntDistribution withAverage(double average) {
            return new IntDistribution() {

                @Override
                public WeightedInt build(int min, int max) {
                    checkRange(min, max);
                    checkAverage(average, min, max);

                    return binomial((average - min) / (max - min)).build(min, max);
                }
            };
        }
    };

    /** A geometric distribution with a chance of 0.5 */
    public static final DistributionFamily GEOMETRIC = new DistributionFamily() {

        @Override
        public WeightedInt build(int min, int max) {
            return geometric(0.5).build(min, max);
        }

        @Override
        public IntDistribution withAverage(double average) {
            return new IntDistribution() {

                @Override
                public WeightedInt build(int min, int max) {
                    checkRange(min, max);
                    checkAverage(average, min, max);

                    if (average >= (min + max) / 2.0) throw new IllegalArgumentException("a geometric distribution can only reach averages below " +
                                "the middle of the range (" + (min + max) / 2.0 + "), got " + average);

                    int n = max - min;
                    double lo = 1e-9;
                    double hi = 1 - 1e-9;

                    for (int i = 0; i < 100; i++) {
                        double mid = (lo + hi) / 2;

                        if (geometricMean(n, mid) < average - min) lo = mid;
                        else hi = mid;
                    }

                    return geometric((lo + hi) / 2).build(min, max);
                }
            };
        }
    };

    public static IntDistribution gauss(double mean, double sd) {
        return gaussOf(mean, sd);
    }

    public static IntDistribution binomial(double p) {
        checkProbability("binomial", p);
        
        return new IntDistribution() {

            @Override
            public WeightedInt build(int min, int max) {
                checkRange(min, max);

                int n = max - min;
                double[] logw = new double[n + 1];
                double lp = Math.log(p);
                double lq = Math.log1p(-p);

                logw[0] = n * lq;

                double maxLog = logw[0];

                for (int k = 0; k < n; k++) {
                    logw[k + 1] = logw[k] + Math.log((double) (n - k) / (k + 1)) + lp - lq;
                    maxLog = Math.max(maxLog, logw[k + 1]);
                }

                double[] w = new double[n + 1];

                for (int k = 0; k <= n; k++) {
                    w[k] = Math.exp(logw[k] - maxLog);
                }

                return WeightedInt.of(min, w);
            }
        };
    }

    public static IntDistribution geometric(double p) {
        checkProbability("geometric", p);

        return new IntDistribution() {

            @Override
            public WeightedInt build(int min, int max) {
                checkRange(min, max);

                double[] w = new double[max - min + 1];

                for (int k = 0; k < w.length; k++) {
                    w[k] = Math.pow(p, k);
                }

                return WeightedInt.of(min, w);
            }
        };
    }

    /** The curve that assumes the least (an exponential tilt) while having the given average */
    public static IntDistribution targetAverage(double average) {
        return new IntDistribution() {

            @Override
            public WeightedInt build(int min, int max) {
                checkRange(min, max);
                checkAverage(average, min, max);

                int n = max - min;
                double target = (average - min) / n;
                double lo = -1e6;
                double hi = 1e6;

                for (int i = 0; i < 200; i++) {
                    double mid = (lo + hi) / 2;

                    if (tiltedMean(n, mid) < target) lo = mid;
                    else hi = mid;
                }

                return WeightedInt.of(min, tiltWeights(n, (lo + hi) / 2));
            }
        };
    }

    public static IntDistribution targetAverage(double average, DistributionFamily family) {
        return family.withAverage(average);
    }

    public static IntDistribution fromFunction(WeightFunction function) {
        Objects.requireNonNull(function, "weight function must not be null");

        return new IntDistribution() {

            @Override
            public WeightedInt build(int min, int max) {
                checkRange(min, max);

                double[] w = new double[max - min + 1];

                for (int i = 0; i < w.length; i++) {
                    w[i] = function.weight(min + i);
                }

                return WeightedInt.of(min, w);
            }
        };
    }

    /**
     * Creates a weight table from a map of amounts to weights.
     *
     * @param weights the weight of each amount. Keys and values can be numbers or numeric text, since JS object keys
     *                arrive as text. Missing amounts between the lowest and highest key get weight 0
     */
    public static WeightedInt fromMap(Map<?, ?> weights) {
        if (weights == null || weights.isEmpty()) throw new IllegalArgumentException("the weight map must not be empty");

        TreeMap<Integer, Double> sorted = new TreeMap<>();

        for (Map.Entry<?, ?> entry : weights.entrySet()) {
            sorted.put(toAmount(entry.getKey()), toWeight(entry.getValue()));
        }

        int min = sorted.firstKey();
        int max = sorted.lastKey();

        if (min < 0) throw new IllegalArgumentException("amounts must not be negative, got " + min);
        if ((long) max - min + 1 > WeightedInt.MAX_ENTRIES) throw new IllegalArgumentException("range " + min + ".." + max + " is too large, at most " +
                    WeightedInt.MAX_ENTRIES + " different amounts are supported");

        double[] w = new double[max - min + 1];

        sorted.forEach((amount, weight) -> w[amount - min] = weight);

        return WeightedInt.of(min, w);
    }

    private static double defaultSd(int min, int max) {
        return Math.max((max - min) / 6.0, 1e-6);
    }

    private static IntDistribution gaussOf(double mean, double sd) {
        if (Double.isNaN(mean) || Double.isInfinite(mean)) throw new IllegalArgumentException("gauss mean must be a finite number, got " + mean);
        if (!(sd > 0) || Double.isInfinite(sd)) throw new IllegalArgumentException("gauss spread must be above 0, got " + sd);

        return new IntDistribution() {

            @Override
            public WeightedInt build(int min, int max) {
                checkRange(min, max);

                double[] w = new double[max - min + 1];

                for (int i = 0; i < w.length; i++) {
                    int amount = min + i;
                    double lo = i == 0 ? Double.NEGATIVE_INFINITY : (amount - 0.5 - mean) / sd;
                    double hi = i == w.length - 1 ? Double.POSITIVE_INFINITY : (amount + 0.5 - mean) / sd;

                    w[i] = Math.max(normalCdf(hi) - normalCdf(lo), 0);
                }

                return WeightedInt.of(min, w);
            }
        };
    }

    private static void checkProbability(String name, double p) {
        if (!(p > 0 && p < 1)) throw new IllegalArgumentException(name + " p must be above 0 and below 1, got " + p);
    }

    private static double geometricMean(int n, double p) {
        double sum = 0;
        double weighted = 0;
        double term = 1;

        for (int k = 0; k <= n; k++) {
            sum += term;
            weighted += k * term;
            term *= p;
        }

        return weighted / sum;
    }

    private static double[] tiltWeights(int n, double lambda) {
        double[] w = new double[n + 1];
        double maxExponent = Math.max(0, lambda);

        for (int k = 0; k <= n; k++) {
            w[k] = Math.exp(lambda * k / n - maxExponent);
        }

        return w;
    }

    private static double tiltedMean(int n, double lambda) {
        double[] w = tiltWeights(n, lambda);
        double sum = 0;
        double weighted = 0;

        for (int k = 0; k <= n; k++) {
            sum += w[k];
            weighted += (double) k / n * w[k];
        }

        return weighted / sum;
    }

    private static double normalCdf(double z) {
        if (z == Double.POSITIVE_INFINITY) return 1;
        if (z == Double.NEGATIVE_INFINITY) return 0;

        return 0.5 * (1 + erf(z / Math.sqrt(2)));
    }

    /** Approximates erf with Abramowitz and Stegun 7.1.26, which has an absolute error below 1.5e-7 */
    private static double erf(double x) {
        double sign = Math.signum(x);
        double ax = Math.abs(x);
        double t = 1.0 / (1.0 + 0.3275911 * ax);
        double poly = ((((1.061405429 * t - 1.453152027) * t + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t;

        return sign * (1.0 - poly * Math.exp(-ax * ax));
    }

    private static int toAmount(Object key) {
        double value;

        if (key instanceof Number number) {
            value = number.doubleValue();
        } else if (key instanceof CharSequence text) {
            try {
                value = Double.parseDouble(text.toString().trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("weight map key '" + key + "' is not a number");
            }
        } else {
            throw new IllegalArgumentException("weight map key '" + key + "' is not a number");
        }
        if (value != Math.rint(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("weight map key '" + key + "' must be a whole number");
        }

        return (int) value;
    }

    private static double toWeight(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof CharSequence text) {
            try {
                return Double.parseDouble(text.toString().trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("weight '" + value + "' is not a number");
            }
        }

        throw new IllegalArgumentException("weight '" + value + "' is not a number");
    }
}
