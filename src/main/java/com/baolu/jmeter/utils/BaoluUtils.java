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

    public static int getThreadIndex(String JMeterThreadName){
        String index = JMeterThreadName.substring(JMeterThreadName.lastIndexOf('-') + 1);
        return Integer.parseInt(index);
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
