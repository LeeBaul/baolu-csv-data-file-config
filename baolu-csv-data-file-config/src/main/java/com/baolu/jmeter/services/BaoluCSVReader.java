package com.baolu.jmeter.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2020/8/18 14:01
 **/
public class BaoluCSVReader {

    private static final Logger log = LoggerFactory.getLogger(BaoluCSVReader.class);

    private final BaoluCSVFilePerThread baoluCSVFilePerThread;

    public BaoluCSVReader (BaoluCSVFilePerThread bcfp) {
        baoluCSVFilePerThread = bcfp;
    }

    public String[] getCsvDataPerThreads(int JMeterThreadNum){
        return baoluCSVFilePerThread.getCsvDataPerThreads(JMeterThreadNum);
    }
}
