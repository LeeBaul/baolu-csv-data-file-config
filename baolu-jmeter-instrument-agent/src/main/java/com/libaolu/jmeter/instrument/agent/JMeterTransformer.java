package com.libaolu.jmeter.instrument.agent;

import javassist.*;
import org.apache.jmeter.JMeter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2021/9/26 17:03
 **/
public class JMeterTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            ClassPool cp = ClassPool.getDefault();
//            cp.insertClassPath(new ClassClassPath(JMeter.class));
//            cp.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
            cp.insertClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
            System.out.println("##1##");
            CtClass clazz = cp.get("org.apache.jmeter.JMeter");
            System.out.println("##2##");
//            CtClass clazz = cp.get(JMeter.class.getName());
            CtMethod ctMethod = clazz.getDeclaredMethod("pConvertSubTree");
            System.out.println("获取方法名称："+ ctMethod.getName());
            ctMethod.insertBefore("System.out.println(\" 动态插入的打印语句 \");");
//            ctMethod.insertAfter("System.out.println($_);");
            return clazz.toBytecode();
//            Class c = clazz.toClass();
//            JMeter jMeter = (JMeter) c.newInstance();
//            jMeter.pConvertSubTree();

        }catch (Exception e){
            e.printStackTrace();
        }
        return classfileBuffer;
    }
}
