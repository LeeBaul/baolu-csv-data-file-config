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
        int index = JMeterThreadName.lastIndexOf(" ");
        if (index > 0){
            String tmpStr = JMeterThreadName.substring(index + 1);
            String args[] = tmpStr.split("-");
            index = Integer.valueOf(args[1]);
        }
        return index;
    }

    public static void main(String[] args) {
        int index = BaoluUtils.getThreadIndex("bzm - Arrivals Thread Group 1-1");
        System.out.println(index);
    }
}
