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

    public static void writeStrTofile(String classname) {
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
