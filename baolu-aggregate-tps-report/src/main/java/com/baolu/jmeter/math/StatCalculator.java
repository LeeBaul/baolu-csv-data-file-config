package com.baolu.jmeter.math;

import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.jmeter.util.JMeterUtils;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2021-2-22 10:34
 **/
public abstract class StatCalculator<T extends Number & Comparable<? super T>> {
    // key is the type to collect (usually long), value = count of entries
    private final Map<T, MutableLong> valuesMap = new TreeMap<>();
    // We use a TreeMap because we need the entries to be sorted

    // Running values, updated for each sample
    private double sum = 0;

    private double sumOfSquares = 0;

    private double mean = 0;

    private double deviation = 0;

    private long count = 0;

    private long errorCount = 0;

    private T min;

    private T max;

    private long bytes = 0;

    private long sentBytes = 0;

    private final T ZERO;

    private final T MAX_VALUE; // e.g. Long.MAX_VALUE

    private final T MIN_VALUE; // e.g. Long.MIN_VALUE

    /**
     * This constructor is used to set up particular values for the generic class instance.
     *
     * @param zero - value to return for Median and PercentPoint if there are no values
     * @param min - value to return for minimum if there are no values
     * @param max - value to return for maximum if there are no values
     */
    public StatCalculator(final T zero, final T min, final T max) {
        super();
        ZERO = zero;
        MAX_VALUE = max;
        MIN_VALUE = min;
        this.min = MAX_VALUE;
        this.max = MIN_VALUE;
    }

    public void clear() {
        valuesMap.clear();
        sum = 0;
        sumOfSquares = 0;
        mean = 0;
        deviation = 0;
        count = 0;
        bytes = 0;
        sentBytes = 0;
        max = MIN_VALUE;
        min = MAX_VALUE;
    }

    /**
     * Add to received bytes
     * @param newValue number of newly received bytes
     */
    public void addBytes(long newValue) {
        bytes += newValue;
    }

    /**
     * Add to sent bytes
     * @param newValue number of newly sent bytes
     */
    public void addSentBytes(long newValue) {
        sentBytes += newValue;
    }

    public void addAll(StatCalculator<T> calc) {
        for(Map.Entry<T, MutableLong> ent : calc.valuesMap.entrySet()) {
            addEachValue(ent.getKey(), ent.getValue().longValue());
        }
    }

    public T getMedian() {
        return getPercentPoint(0.5);
    }

    public long getTotalBytes() {
        return bytes;
    }

    public long getTotalSentBytes() {
        return sentBytes;
    }

    /**
     * Get the value which %percent% of the values are less than. This works
     * just like median (where median represents the 50% point). A typical
     * desire is to see the 90% point - the value that 90% of the data points
     * are below, the remaining 10% are above.
     *
     * @param percent
     *            number representing the wished percent (between <code>0</code>
     *            and <code>1.0</code>)
     * @return number of values less than the percentage
     */
    public T getPercentPoint(float percent) {
        return getPercentPoint((double) percent);
    }

    /**
     * Get the value which %percent% of the values are less than. This works
     * just like median (where median represents the 50% point). A typical
     * desire is to see the 90% point - the value that 90% of the data points
     * are below, the remaining 10% are above.
     *
     * @param percent
     *            number representing the wished percent (between <code>0</code>
     *            and <code>1.0</code>)
     * @return the value which %percent% of the values are less than
     */
    public T getPercentPoint(double percent) {
        if (count <= 0) {
            return ZERO;
        }
        if (percent >= 1.0) {
            return getMax();
        }

        // use Math.round () instead of simple (long) to provide correct value rounding
        long target = Math.round(count * percent);
        try {
            for (Map.Entry<T, MutableLong> val : valuesMap.entrySet()) {
                target -= val.getValue().longValue();
                if (target <= 0){
                    return val.getKey();
                }
            }
        } catch (ConcurrentModificationException ignored) {
            // ignored. May happen occasionally, but no harm done if so.
        }
        return ZERO; // TODO should this be getMin()?
    }

    /**
     * Returns the distribution of the values in the list.
     *
     * @return map containing either Integer or Long keys; entries are a Number array containing the key and the [Integer] count.
     * TODO - why is the key value also stored in the entry array? See Bug 53825
     */
    public Map<Number, Number[]> getDistribution() {
        Map<Number, Number[]> items = new HashMap<>();

        for (Map.Entry<T, MutableLong> entry : valuesMap.entrySet()) {
            Number[] dis = new Number[2];
            dis[0] = entry.getKey();
            dis[1] = entry.getValue();
            items.put(entry.getKey(), dis);
        }
        return items;
    }

    public double getMean() {
        return mean;
    }

    public double getStandardDeviation() {
        return deviation;
    }

    public T getMin() {
        return min;
    }

    public T getMax() {
        return max;
    }

    public long getCount() {
        return count;
    }

    public double getSum() {
        return sum;
    }

    protected abstract T divide(T val, int n);

    protected abstract T divide(T val, long n);

    /**
     * Update the calculator with the values for a set of samples.
     *
     * @param val the common value, normally the elapsed time
     * @param sampleCount the number of samples with the same value
     */
    void addEachValue(T val, long sampleCount) {
        if (val.intValue()!=0){
            count += sampleCount;
        }
        double currentVal = val.doubleValue();
        sum += currentVal * sampleCount;
        // For n same values in sum of square is equal to n*val^2
        sumOfSquares += currentVal * currentVal * sampleCount;
        updateValueCount(val, sampleCount);
        calculateDerivedValues(val,false);
    }

    /**
     * Update the calculator with the value for an aggregated sample.
     *
     * @param val the aggregate value, normally the elapsed time
     * @param sampleCount the number of samples contributing to the aggregate value
     * @param errorSampleCount the number of error samples contributing to the aggregate value
     * @param calc_tps calc tps flag
     */
    public void addValue(T val, long sampleCount, long errorSampleCount,boolean calc_tps) {
        count += sampleCount;
        errorCount += errorSampleCount;
        double currentVal = val.doubleValue();
        sum += currentVal;
        T actualValue = val;
        if (sampleCount > 1){
            // For n values in an aggregate sample the average value = (val/n)
            // So need to add n * (val/n) * (val/n) = val * val / n
            sumOfSquares += currentVal * currentVal / sampleCount;
            actualValue = divide(val, sampleCount);
        } else { // no need to divide by 1
            sumOfSquares += currentVal * currentVal;
        }
        updateValueCount(actualValue, sampleCount);
        calculateDerivedValues(actualValue,calc_tps);
    }

    private void calculateDerivedValues(T actualValue ,boolean calc_tps) {
//        String calc_tps = JMeterUtils.getProperty("baolu-aggregate-tps-report");
        mean = sum / count;
        if (calc_tps){
            mean = sum / (count-errorCount);
        }
        deviation = Math.sqrt((sumOfSquares / count) - (mean * mean));
        if (actualValue.compareTo(max) > 0){
            max = actualValue;
        }
        if (actualValue.compareTo(min) < 0){
            if (calc_tps){
                if (actualValue.intValue() != 0){
                    min = actualValue;
                }else {
                    if (min.equals(MAX_VALUE)){
                        min = actualValue;
                    }
                }
            }else {
                min = actualValue;
            }

        }
    }

    /**
     * Add a single value (normally elapsed time)
     *
     * @param val the value to add, which should correspond with a single sample
     * @see #addValue(Number, long,long,boolean)
     */
    public void addValue(T val) {
        addValue(val, 1L,0L,false);
    }

    private void updateValueCount(T actualValue, long sampleCount) {
        MutableLong count = valuesMap.get(actualValue);
        if (count != null) {
            count.add(sampleCount);
        } else {
            // insert new value
            valuesMap.put(actualValue, new MutableLong(sampleCount));
        }
    }
}
