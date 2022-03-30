package com.baolu.jmeter.services;

import org.apache.jorphan.util.JMeterStopThreadException;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2020/8/17 16:42
 **/
public class BaoluCSVFileReader {

    private static final Logger log = LoggerFactory.getLogger(BaoluCSVFileReader.class);
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
    private Map<Integer,Integer> curThreadsPos;
    /**
     * 每个线程分得数据块大小
     */
    private int blockSizePerThread;
    /**
     * 循环读取
     */
    private boolean recycle = true;
    /**
     * 忽略首行
     */
    private boolean ignoreFirstLine = false;
    /**
     *
     */
    private boolean  stopThread;



    public BaoluCSVFileReader(String alias ,String encoding,String delimiter,int blockSize,
                                 boolean loopReadCsvData,boolean readfirstLine,boolean stopThreadReadCsvFile) throws IOException {
        startPos = 1;
        curPos = 1;
        csvFileName = alias;
        csvfileEncoding = encoding;
        delim = delimiter;
        csvFiledata = new ArrayList<>();
        curThreadsPos = new ConcurrentHashMap<>();
        LoadCsvFile();
        blockSizePerThread = blockSize;
        recycle = loopReadCsvData;
        ignoreFirstLine = readfirstLine;
        stopThread = stopThreadReadCsvFile;
        if (ignoreFirstLine){
            ++startPos;
        }
        endPos = csvFiledata.size();
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

    public String[] getCsvDataPerThreads(int threadNum){

        synchronized (this){
            String[] lineValues;
            int threadStartPos = startPos * blockSizePerThread;
            int threadEndPos = threadStartPos + blockSizePerThread;
            if (log.isDebugEnabled()){
                log.debug("threadStartPos[{}],threadEndPos[{}]",threadStartPos,threadEndPos);
            }
            if (threadStartPos < endPos && threadEndPos <= endPos){
                if (!curThreadsPos.containsKey(threadNum)){
                    curThreadsPos.put(threadNum,threadStartPos);//保存每个线程起始读取位置
                }

                int threadCurReadPos = curThreadsPos.get(threadNum);//获取线程读取位置

                if (threadCurReadPos >= threadEndPos){
                    if (stopThread){
                        throw new JMeterStopThreadException("End of file:"+ getCsvFileName()+" configured with stopThread:"+ isStopThread()+", recycle:" + isRecycle());
                    }else {
                        log.error("threadCurReadPos > threadEndPos check block size for each threads please!");
                        return null;
                    }
                }else {
                   List<String> dataLine = csvFiledata.get(threadCurReadPos);
                   lineValues = dataLine.toArray(new String[dataLine.size()]);
                   nextPos(threadNum,threadStartPos,threadEndPos);
                   return lineValues;
                }
            }else {
                log.error(" threadStartPos > endPos &&  threadEndPos > endPos  check block size for each threads please! ");
                return null;
            }
        }
    }

    private void nextPos(int threadNum, int threadStartPos, int threadEndPos) {
        int curThreadPos = curThreadsPos.get(threadNum);
        ++curThreadPos;
        if (curThreadPos >= threadEndPos && !isStopThread()){
            if (isRecycle()){
                curThreadPos = threadStartPos;
            }else{
                --curThreadPos;
            }
        }
        curThreadsPos.put(threadNum,curThreadPos);
    }

    public String getCsvFileName() {
        return csvFileName;
    }

    public int getCsvFileLines() {
        return csvFiledata.size();
    }

    public boolean isRecycle() {
        return recycle;
    }

    public boolean isStopThread() {
        return stopThread;
    }
}
