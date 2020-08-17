package com.baolu.jmeter.services;

import org.apache.jorphan.util.JOrphanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2020/8/17 16:42
 **/
public class CsvFileReadPerThreads {

    private static final Logger log = LoggerFactory.getLogger(CsvFileReadPerThreads.class);
    /**
     * 开始位置
     */
    private int startPos;
    /**
     * 结束位置
     */
    private int endPos;
    /**
     * 当前位置
     */
    private int curPos;
    /**
     * 分隔符号
     */
    private final String delim;
    /**
     * csv文件编码
     */
    private final String csvfileEncoding;
    /**
     * csv文件名
     */
    private final String csvFileName;
    /**
     * csv文件数据
     */
    private final List<List<String>> csvFiledata;
    /**
     * 当前线程读取位置
     */
    private Map<Integer,Integer> curThreadPos;


    public CsvFileReadPerThreads(String alias ,String encoding,String delimiter) throws IOException {
        csvFileName = alias;
        csvfileEncoding = encoding;
        delim = delimiter;
        csvFiledata = new ArrayList<>();
        LoadCsvFile();
    }

    private void LoadCsvFile() throws IOException {
        synchronized (this){
            Charset charset = Charset.defaultCharset();
            if (csvfileEncoding != null && csvfileEncoding.length() != 0){
                charset = Charset.forName(csvfileEncoding);
            }
            try {
                BufferedReader br = Files.newBufferedReader(FileServer.getFileServer().getResolvedFile(csvFileName).toPath(),charset);
                try{
                    for (String line = br.readLine();line!=null && line.length() > 0;line = br.readLine()){
                        csvFiledata.add(Arrays.asList(JOrphanUtils.split(line, delim, false)));
                    }
                    log.info("LoadCsvFile() 方法结束[{}]",csvFiledata.toString());
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    if (br != null){
                        br.close();
                    }
                }
            } catch (IOException e) {
                csvFiledata.clear();
                throw e;
            }
        }
    }
}
