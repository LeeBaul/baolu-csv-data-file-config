package com.libaolu.jmeter.instrument.agent;

import javassist.*;
import org.apache.jmeter.JMeter;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2021/9/26 16:48
 **/
public class JMeterAgent {
    static String targetClass = "org.apache.jmeter.NewDriver";
    /**
     * jvm 参数形式启动，运行此方法
     * @param agentArgs
     * @param inst
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("==================premain====================");
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                byte[] result = null;
                if (className != null && className.replace("/", ".").equals(targetClass)) {
                    ClassPool pool = new ClassPool();
                    pool.insertClassPath(new LoaderClassPath(loader));
                    try {
                        CtClass ctClass = pool.get(targetClass);
                        CtMethod ctMethod = ctClass.getDeclaredMethod("main");
                        ctMethod.insertAfter("System.out.println(\"my name is libaolu\");");
                        result = ctClass.toBytecode();
                    } catch (NotFoundException | CannotCompileException | IOException e) {
                        e.printStackTrace();
                    }
                }
                return result;
            }
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

    public static void main(String[] args) {
        System.out.println(2222);
    }

}
