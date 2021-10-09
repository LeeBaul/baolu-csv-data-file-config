package com.libaolu.jmeter.instrument.agent.utils;

import javassist.*;

import java.io.IOException;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2021/9/28 22:31
 **/
public class ClassEnhanced {
    public static void enhancedMethod(CtClass ctClass,String methodName) throws NotFoundException, CannotCompileException {
        CtMethod ctMethod = ctClass.getDeclaredMethod("pConvertSubTree");//main 、 start 、pConvertSubTree
        String oriMethod = methodName + "$temp";
        ctMethod.setName(oriMethod);//设置源方法名
        CtMethod enhancedMethod = CtNewMethod.copy(ctMethod, methodName, ctClass, null);
        //对复制方法注入新方法体
        enhancedMethod.setBody(ScriptUtil.getEnhancedMethodBody());
        ctClass.addMethod(enhancedMethod);
    }
}
