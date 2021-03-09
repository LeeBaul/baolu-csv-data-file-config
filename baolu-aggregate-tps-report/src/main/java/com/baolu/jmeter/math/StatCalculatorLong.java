package com.baolu.jmeter.math;

/**
 * <p/>
 * StatCalculator for Long values
 * @author libaolu
 * @version 1.0
 * @dateTime 2021-2-22 10:33
 **/
public class StatCalculatorLong extends StatCalculator<Long> {

    public StatCalculatorLong() {
        super(0L, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    /**
     * Add a single value (normally elapsed time)
     *
     * @param val the value to add, which should correspond with a single sample
     */
    public void addValue(long val){
        super.addValue(val);
    }

    /**
     * Update the calculator with the value for an aggregated sample.
     *
     * @param val the aggregate value, normally the elapsed time
     * @param sampleCount the number of error samples contributing to the aggregate value
     */
    public void addValue(long val, int sampleCount,int errorCount, boolean calc_tps){
        super.addValue(val, (long)sampleCount,errorCount,calc_tps);
    }

    @Override
    protected Long divide(Long val, int n) {
        return val / (long)n;
    }

    @Override
    protected Long divide(Long val, long n) {
        return val / n;
    }
}
