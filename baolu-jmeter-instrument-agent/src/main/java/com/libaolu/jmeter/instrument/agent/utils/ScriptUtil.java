package com.libaolu.jmeter.instrument.agent.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2021/9/28 9:17
 **/
public class ScriptUtil {

    public static void writeStrTofile(String str) {
        FileWriter fstream = null;
        try {
            fstream = new FileWriter("C://str.txt",true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedWriter out =new BufferedWriter(fstream);
        try {
            out.write(str+"\n");
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

    public static String getEnhancedMethodBody(){
        StringBuffer buffer = new StringBuffer();
        buffer.append("for (Object o : new ArrayList<>(tree.list())) {");
            buffer.append("if (o instanceof TestElement) {");
                buffer.append("TestElement item = (TestElement) o;");
                buffer.append("if (item.isEnabled()) {");
                    buffer.append("if (item instanceof ReplaceableController) {");
                        buffer.append("ReplaceableController rc = ensureReplaceableControllerIsLoaded(item);");
                        buffer.append("HashTree subTree = tree.getTree(item);");
                        buffer.append("if (subTree != null) {");
                            buffer.append("HashTree replacementTree = rc.getReplacementSubTree();");
                            buffer.append("if (replacementTree != null) {");
                                buffer.append("pConvertSubTree(replacementTree);");
                                buffer.append("tree.replaceKey(item, rc);");
                                buffer.append("tree.set(rc, replacementTree);");
                            buffer.append("}");
                        buffer.append("}");
                    buffer.append("} else { ");
                        buffer.append("pConvertSubTree(tree.getTree(item)); ");
                    buffer.append("}");
                buffer.append("} else {");
                    buffer.append(" tree.remove(item);");
                buffer.append("}");
            buffer.append("} else {");
                buffer.append("JMeterTreeNode item = (JMeterTreeNode) o;");
                buffer.append("if (item.isEnabled()) {");
                    buffer.append("if (item.getUserObject() instanceof ReplaceableController) {");
                        buffer.append("TestElement controllerAsItem = item.getTestElement();");
                        buffer.append("ReplaceableController rc = ensureReplaceableControllerIsLoaded(controllerAsItem);");
                        buffer.append("HashTree subTree  =  tree.getTree(item);");
                        buffer.append("if (subTree  !=  null) {");
                            buffer.append("HashTree replacementTree = rc.getReplacementSubTree();");
                            buffer.append("if (replacementTree != null) {");
                                buffer.append("pConvertSubTree(replacementTree);");
                                buffer.append("tree.replaceKey(item, rc);");
                                buffer.append("tree.set(rc, replacementTree);");
                            buffer.append("}");
                        buffer.append("}");
                    buffer.append("} else {");
                        buffer.append("pConvertSubTree(tree.getTree(item));");
                        buffer.append("TestElement testElement = item.getTestElement();");
                        buffer.append("tree.replaceKey(item, testElement);");
                    buffer.append("}");
                buffer.append("} else {");
                    buffer.append(" tree.remove(item);");
                buffer.append("}");
            buffer.append("}");
        buffer.append("}");
        return buffer.toString();
    }

}
