package com.libaolu.jmeter.instrument.agent;

import com.libaolu.jmeter.instrument.agent.utils.ScriptUtil;
import javassist.*;
import org.apache.jmeter.JMeter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2021/9/26 16:48
 **/
public class AgentBootstrap {
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
//            ScriptUtil.writeStrTofile(className);
            if (className != null && className.replace("/", ".").equals(targetClass1)) {
                ClassPool pool = new ClassPool();
                pool.insertClassPath(new LoaderClassPath(loader));
                try {
                    CtClass ctClass = pool.get(targetClass1);
                    CtMethod ctMethod = ctClass.getDeclaredMethod("pConvertSubTree");//main 、 start 、pConvertSubTree
                    ctMethod.insertAt(1173,"if (\"javaReq_0001\".equals(item.getName())){ " +
                            " item.setEnabled(false); }");
                    CtMethod enhancedMethod = CtNewMethod.copy(ctMethod, "pConvertSubTree", ctClass, null);
                    result = ctClass.toBytecode();
                } catch (NotFoundException | CannotCompileException | IOException e) {
                    e.printStackTrace();
                }
            }
            return result;
        });
    }

}
