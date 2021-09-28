package com.libaolu.jmeter.instrument.agent;

import com.libaolu.jmeter.instrument.agent.utils.ClassEnhanced;
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
    private static final String TARGET_CLASS0 = "org.apache.jmeter.NewDriver";
    private static final String TARGET_CLASS1 = "org.apache.jmeter.JMeter";
    private static final String[] METHOD_NAME_TAGS = new String[3];
    static {
        METHOD_NAME_TAGS[0]  = "pConvertSubTree"; //$NON-NLS-1$
        METHOD_NAME_TAGS[1]  = "start"; //$NON-NLS-1$
    }
    /**
     * jvm 参数形式启动，运行此方法
     * @param agentArgs
     * @param inst
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer((loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
            byte[] result = null;
//            ScriptUtil.writeStrTofile(className);
            if (className != null && className.replace("/", ".").equals(TARGET_CLASS1)) {
                ClassPool pool = new ClassPool();
                pool.insertClassPath(new LoaderClassPath(loader));
                try {
                    CtClass ctClass = pool.get(TARGET_CLASS1);
                    ClassEnhanced.enhancedMethod(ctClass,METHOD_NAME_TAGS[0]);
//                    CtMethod ctMethod = ctClass.getDeclaredMethod("pConvertSubTree");//main 、 start 、pConvertSubTree
//                    ctMethod.insertAt(1173,"if (\"javaReq_0001\".equals(item.getName())){ " +
//                            " item.setEnabled(false); }");
//                    CtMethod enhancedMethod = CtNewMethod.copy(ctMethod, "pConvertSubTree", ctClass, null);
                    result = ctClass.toBytecode();
                } catch (NotFoundException | CannotCompileException | IOException e) {
                    e.printStackTrace();
                }
            }
            return result;
        });
    }

}
