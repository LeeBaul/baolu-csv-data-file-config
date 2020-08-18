package com.baolu.jmeter.utils;

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
        return Integer.valueOf(index);
    }

    public static void main(String[] args) {
        int index = BaoluUtils.getThreadIndex("bzm - Arrivals Thread Group 1-1");
        System.out.println(index);
    }
}
