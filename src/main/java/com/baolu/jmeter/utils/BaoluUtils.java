package com.baolu.jmeter.utils;


import java.io.*;
import java.util.stream.Collectors;

/**
 * <p/>
 * 工具类
 * @author libaolu
 * @version 1.0
 * @dateTime 2020/8/17 14:01
 **/
public class BaoluUtils {

    public BaoluUtils(){

    }

    /**
     *
     * @param JMeterThreadName Thread Group 1-1
     * @return The thread index of the current single thread group
     */
    @Deprecated
    public static int getThreadIndex(String JMeterThreadName){
        String index = JMeterThreadName.substring(JMeterThreadName.lastIndexOf('-') + 1);
        return Integer.parseInt(index);
    }

    /**
     *
     * @param JMeterThreadName Thread Group 1-1
     * @return The thread index of the current multiple thread groups 1-1、1-n、2-1、2-n
     */
    public static String getMultipleTgIndex(String JMeterThreadName){
        return JMeterThreadName.substring(JMeterThreadName.lastIndexOf(' ') + 1);
    }

    public static String getResourceFileAsText(String name) {
        try {
            String lineEnd = System.getProperty("line.separator");
            InputStream is = BaoluUtils.class.getClassLoader().getResourceAsStream(name);
            if (is != null)
                try(Reader in = new InputStreamReader(is);
                    BufferedReader fileReader = new BufferedReader(in)) {
                    return fileReader.lines()
                            .collect(Collectors.joining(lineEnd, "", ""));
                }
            return "";
        } catch (IOException e) {
            return "";
        }
    }

}
