package com.libaolu.network.sampler;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import static org.apache.jmeter.util.JMeterUtils.getResourceFileAsText;

/**
 * @author libaolu
 * @date 2021/8/4 15:59
 */
public class CheckNetworkSampler extends AbstractSampler implements TestStateListener {

    private static final Logger log = LoggerFactory.getLogger(CheckNetworkSampler.class);
    public static final String HOST = "CheckNetworkSampler.host";
    public static final String PORT = "CheckNetworkSampler.port";
    public static final String CONNECT_TIME_OUT = "CheckNetworkSampler.timeout";
    @Override
    public SampleResult sample(Entry entry) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        boolean flag = telnet(getHost(),getPort(),getConnectTimeOut(),result);
        if (flag){
            result.setSuccessful(true);
            result.setResponseCodeOK();
        }else {
            result.setSuccessful(false);
            result.setResponseCode("504");
            result.setResponseMessage("Connection timed out");
        }
        return result;
    }

    public static boolean telnet(String host, String port, String timeout ,SampleResult result) {
        Socket socket = new Socket();
        boolean isConnected = false;
        try {
            if (StringUtils.isEmpty(timeout)){
                timeout = "5000";
            }
            socket.connect(new InetSocketAddress(host, Integer.parseInt(port)), Integer.parseInt(timeout));
            isConnected = socket.isConnected();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                socket.close();
                result.sampleEnd();
                if (isConnected){
                    result.setSampleLabel(host+" "+port +" established");
                    log.info("{} {} established",host,port);
                }else{
                    result.setSampleLabel(host+" "+port +"connect timeout");
                    log.error("{} {} connect timeout",host,port);
                }
            } catch (IOException e) {
               e.printStackTrace();
            }

        }

        return isConnected;
    }

        @Override
    public void testStarted() {
        String pluginsShow = JMeterUtils.getProperty("baolu-jmeter-plugins");
        if (StringUtils.isEmpty(pluginsShow)){
            log.info(System.getProperty("line.separator")+""+getResourceFileAsText("banner/banner.txt"));
            JMeterUtils.setProperty("baolu-jmeter-plugins","show");
        }
    }

    @Override
    public void testStarted(String s) {
        testStarted();
    }

    @Override
    public void testEnded() {

    }

    @Override
    public void testEnded(String s) {
        testEnded();
    }

    public void setHost(String ip) {
        this.setProperty(HOST, ip);
    }

    public String getHost(){
        return getPropertyAsString(HOST);
    }

    public void setPort(String port) {
        setProperty(PORT, port);
    }

    public String getPort(){
        return getPropertyAsString(PORT);
    }

    public void setConnectTimeOut(String timeOut) {
        setProperty(CONNECT_TIME_OUT, timeOut);
    }
    public String getConnectTimeOut() {
        return getPropertyAsString(CONNECT_TIME_OUT);
    }

}
