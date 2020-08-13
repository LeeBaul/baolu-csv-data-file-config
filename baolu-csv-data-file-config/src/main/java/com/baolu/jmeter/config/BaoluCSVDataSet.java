package com.baolu.jmeter.config;

import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.TestStateListener;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @date 2020/7/23 22:34
 **/
public class BaoluCSVDataSet extends ConfigTestElement implements TestBean,LoopIterationListener, TestStateListener {

    public void iterationStart(LoopIterationEvent loopIterationEvent) {

    }

    public void testStarted() {

    }

    public void testStarted(String s) {

    }

    public void testEnded() {

    }

    public void testEnded(String s) {

    }
}
