package com.libaolu.jmeter.instrument.agent;

import org.apache.jmeter.JMeter;

import java.lang.instrument.Instrumentation;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2021/9/26 16:48
 **/
public class JMeterAgent {
    /**
     * jvm 参数形式启动，运行此方法
     * @param agentArgs
     * @param inst
     */
    public static void premain(String agentArgs, Instrumentation inst){
        System.out.println("premain");
        inst.addTransformer(new JMeterTransformer(), true);
        try {
            //重定义类并载入新的字节码
            inst.retransformClasses(JMeter.class);
            System.out.println("JMeterAgent Load Done.");
        } catch (Exception e) {
            System.out.println("JMeterAgent load failed!");
        }
    }

    /**
     * 动态 attach 方式启动，运行此方法
     * @param agentArgs
     * @param inst
     */
    public static void agentmain(String agentArgs, Instrumentation inst){
        System.out.println("agentmain");
        inst.addTransformer(new JMeterTransformer(), true);
        try {
            //重定义类并载入新的字节码
            inst.retransformClasses(JMeter.class);
            System.out.println("JMeterAgent Load Done.");
        } catch (Exception e) {
            System.out.println("JMeterAgent load failed!");
        }
    }

}
