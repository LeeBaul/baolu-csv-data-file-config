package com.libaolu.jmeter.instrument.agent;

import javassist.*;
import org.apache.jmeter.JMeter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2021/9/26 16:48
 **/
public class JMeterAgent {
    static String targetClass0 = "org.apache.jmeter.NewDriver";
    static String targetClass1 = "org.apache.jmeter.JMeter";
    /**
     * jvm 参数形式启动，运行此方法
     * @param agentArgs
     * @param inst
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer((loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
            byte[] result = null;
//            save(className);
            if (className != null && className.replace("/", ".").equals(targetClass1)) {
                ClassPool pool = new ClassPool();
                pool.insertClassPath(new LoaderClassPath(loader));
                try {
                    CtClass ctClass = pool.get(targetClass1);
                    CtMethod ctMethod = ctClass.getDeclaredMethod("start");//main 、 start 、pConvertSubTree
                    ctMethod.insertAfter("System.out.println(\"my name is libaolu\");");
                    result = ctClass.toBytecode();
                } catch (NotFoundException | CannotCompileException | IOException e) {
                    e.printStackTrace();
                }
            }
            return result;
        });
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

    public static void save(String classname) {
        FileWriter fstream = null;
        try {
            fstream = new FileWriter("C://classname.txt",true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedWriter out =new BufferedWriter(fstream);
        try {
            out.write(classname+"\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
            fstream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
