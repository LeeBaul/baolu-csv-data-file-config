package com.libaolu.network.sampler;

import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestStateListener;

/**
 * @author libaolu
 * @date 2021/8/4 15:59
 */
public class CheckNetworkSampler extends AbstractSampler implements TestStateListener {

    @Override
    public SampleResult sample(Entry entry) {
        return null;
    }

    @Override
    public void testStarted() {

    }

    @Override
    public void testStarted(String s) {

    }

    @Override
    public void testEnded() {

    }

    @Override
    public void testEnded(String s) {

    }
}
