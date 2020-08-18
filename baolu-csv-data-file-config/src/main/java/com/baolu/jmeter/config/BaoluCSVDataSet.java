package com.baolu.jmeter.config;

import com.baolu.jmeter.services.FileServer;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.save.CSVSaveService;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.util.JMeterStopThreadException;
import org.apache.jorphan.util.JOrphanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @date 2020/7/23 22:34
 **/
public class BaoluCSVDataSet extends ConfigTestElement implements TestBean,LoopIterationListener,TestStateListener {

    private static final Logger log = LoggerFactory.getLogger(BaoluCSVDataSet.class);

    private static final String EOFVALUE = // value to return at EOF
            JMeterUtils.getPropDefault("csvdataset.eofstring", "<EOF>"); //$NON-NLS-1$ //$NON-NLS-2$

    private transient String filename;

    private transient String fileEncoding;

    private transient String variableNames;

    private transient String delimiter;

    private transient boolean quoted;

    private transient boolean recycle = true;

    private transient boolean stopThread;

    private transient String[] vars;

    private transient String alias;

    private transient String shareMode;

    private boolean firstLineIsNames = false;

    private boolean ignoreFirstLine = false;

    /**
     * 是否为每个线程分配数据
     */
    private boolean allocateData = false;
    /**
     * 自动分配
     */
    private boolean automaticallyAllocate = true;

    /**
     * 自定义每个线程分得的数据块大小
     */
    private int blockSize = 0;

    /**
     * 数据块
     */
    private String[] blockPara = null;

    /**
     * 当前位置
     */
    private int curParaPos = 1;

    public BaoluCSVDataSet(){

    }

    public void iterationStart(LoopIterationEvent loopIterationEvent) {
        FileServer server = FileServer.getFileServer();
        JMeterContext context = getThreadContext();
        String delim = getDelimiter();
        if ("\\t".equals(delim)) { // $NON-NLS-1$
            delim = "\t";// Make it easier to enter a Tab // $NON-NLS-1$
        } else if (delim.isEmpty()){
            log.debug("Empty delimiter, will use ','");
            delim=",";
        }
        if (isAllocateData()){
            getThreadsBlockData(server,getRecycle(),isIgnoreFirstLine());
        }
        if (vars == null) {
            String fileName = getFilename().trim();
            String mode = getShareMode();
            int modeInt = BaoluCSVDataSetBeanInfo.getShareModeAsInt(mode);
            String names = getVariableNames();
            String header;
            switch(modeInt){
                case BaoluCSVDataSetBeanInfo.SHARE_ALL:
                    alias = fileName;
                    break;
                case BaoluCSVDataSetBeanInfo.SHARE_GROUP:
                    alias = fileName+"@"+System.identityHashCode(context.getThreadGroup());
                    break;
                case BaoluCSVDataSetBeanInfo.SHARE_THREAD:
                    alias = fileName+"@"+System.identityHashCode(context.getThread());
                    break;
                default:
                    alias = fileName+"@"+mode; // user-specified key
                    break;
            }

            if (StringUtils.isEmpty(names)) {
                header = server.reserveFile(fileName, getFileEncoding(), alias, true);
                try {
                    vars = CSVSaveService.csvSplitString(header, delim.charAt(0));
                    firstLineIsNames = true;
                } catch (IOException e) {
                    throw new IllegalArgumentException("Could not split CSV header line from file:" + fileName,e);
                }
            } else {
                if (!isAllocateData()){
                    server.reserveFile(fileName, getFileEncoding(), alias, ignoreFirstLine);
                }
                vars = JOrphanUtils.split(names, ","); // $NON-NLS-1$
            }
            trimVarNames(vars);
        }

        // TODO: fetch this once as per vars above?
        JMeterVariables threadVars = context.getVariables();
        String[] lineValues = {};
        try {
            if (isAllocateData()){
                lineValues = itemParaSetVars(blockPara, delim);
            }else if (getQuotedData()) {
                lineValues = server.getParsedLine(alias, recycle,
                        firstLineIsNames || ignoreFirstLine, delim.charAt(0));
            } else {
                String line = server.readLine(alias, recycle,
                        firstLineIsNames || ignoreFirstLine);
                lineValues = JOrphanUtils.split(line, delim, false);
            }

            for (int a = 0; a < vars.length && a < lineValues.length; a++) {
                threadVars.put(vars[a], lineValues[a]);
            }
        } catch (IOException e) { // treat the same as EOF
            log.error(e.toString());
        }

        if (lineValues.length == 0) {// i.e. EOF
            if (getStopThread()) {
                throw new JMeterStopThreadException("End of file:"+ getFilename()+" detected for CSV DataSet:"
                        +getName()+" configured with stopThread:"+ getStopThread()+", recycle:" + getRecycle());
            }
            for (String var :vars) {
                threadVars.put(var, EOFVALUE);
            }
        }
    }

    public void testStarted() {

    }

    public void testStarted(String s) {
        testStarted();
    }

    public void testEnded() {
        //结束后操作
    }

    public void testEnded(String s) {
        testEnded();
    }

    /**
     * create by libaolu
     * @param paraArr 被分割数组
     * @param delim 分隔符
     * @return
     */
    public String[] itemParaSetVars(String[] paraArr, String delim){
        String[] params = JOrphanUtils.split(paraArr[(curParaPos - 1)], delim, false);
        if (curParaPos == paraArr.length) {
            curParaPos = 1;
            if (log.isDebugEnabled()){
                log.debug("current offset is [{}],max offset is [{}]",curParaPos,paraArr.length);
            }
        }else {
            curParaPos += 1;
        }
        if (log.isDebugEnabled()){
            log.debug("current threadNum [{}],next offset is [{}],current element array is [{}]",Thread.currentThread().getName(),curParaPos,params);
        }
        return params;
    }

    /**
     * create by libaolu
     * @param recycle 默认true
     * @param ignoreFirstLine 默认false
     * @return
     */
    public String[] getThreadsBlockData(FileServer server,boolean recycle, boolean ignoreFirstLine){
        JMeterContext context = getThreadContext();
        String fileName = getFilename();
        int totalLines = 0;
        int blockSize = 0;
        int startLine = 0;

        if (totalLines == 0){
            totalLines = server.getTotalLines(fileName, ignoreFirstLine);
        }

        if (isAutomaticallyAllocate()){
            blockSize = server.getBlockSize(context, totalLines);
        }else {
            blockSize = getBlockSize();
            if (blockSize <= 0){
                log.warn("BlockSize can not be less than zero.Automatically allocate block size by default");
                blockSize = server.getBlockSize(context, totalLines);
            }else if (blockSize > totalLines){
                log.warn("BlockSize is greater than csv file total lines,the blockSize does not take effect.Automatically allocate block size by default");
                blockSize = server.getBlockSize(context, totalLines);
            }

        }
        startLine = server.getstartLine(context, blockSize);
        blockPara = server.readLineBlock(fileName, recycle, ignoreFirstLine, startLine, blockSize);
        if (log.isDebugEnabled()){
            log.debug("current threadNum [{}],blockPara is [{}]",Thread.currentThread().getName(),blockPara);
        }
        return blockPara;
    }

    /**
     * trim content of array varNames
     * @param varsNames
     */
    private void trimVarNames(String[] varsNames) {
        for (int i = 0; i < varsNames.length; i++) {
            varsNames[i] = varsNames[i].trim();
        }
    }

    /**
     * @return Returns the filename.
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename
     *            The filename to set.
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * @return Returns the file encoding.
     */
    public String getFileEncoding() {
        return fileEncoding;
    }

    /**
     * @param fileEncoding
     *            The fileEncoding to set.
     */
    public void setFileEncoding(String fileEncoding) {
        this.fileEncoding = fileEncoding;
    }

    /**
     * @return Returns the variableNames.
     */
    public String getVariableNames() {
        return variableNames;
    }

    /**
     * @param variableNames
     *            The variableNames to set.
     */
    public void setVariableNames(String variableNames) {
        this.variableNames = variableNames;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public boolean getQuotedData() {
        return quoted;
    }

    public void setQuotedData(boolean quoted) {
        this.quoted = quoted;
    }

    public boolean getRecycle() {
        return recycle;
    }

    public void setRecycle(boolean recycle) {
        this.recycle = recycle;
    }

    public boolean getStopThread() {
        return stopThread;
    }

    public void setStopThread(boolean value) {
        this.stopThread = value;
    }

    public String getShareMode() {
        return shareMode;
    }

    public void setShareMode(String value) {
        this.shareMode = value;
    }

    /**
     * @return the ignoreFirstLine
     */
    public boolean isIgnoreFirstLine() {
        return ignoreFirstLine;
    }

    /**
     * @param ignoreFirstLine the ignoreFirstLine to set
     */
    public void setIgnoreFirstLine(boolean ignoreFirstLine) {
        this.ignoreFirstLine = ignoreFirstLine;
    }

    /**
     * @return the allocateData
     */
    public boolean isAllocateData() {
        return allocateData;
    }

    /**
     *
     * @param allocateData the allocateData to set
     */
    public void setAllocateData(boolean allocateData) {
        this.allocateData = allocateData;
    }

    /**
     * @return the automaticallyAllocate
     */
    public boolean isAutomaticallyAllocate() {
        return automaticallyAllocate;
    }

    /**
     *
     * @param automaticallyAllocate Automatically allocate block size
     */
    public void setAutomaticallyAllocate(boolean automaticallyAllocate) {
        this.automaticallyAllocate = automaticallyAllocate;
    }

    /**
     *
     * @param blockSize Set block size for each threads
     */
    public void setBlockSize(int blockSize){
        this.blockSize = blockSize;
    }

    /**
     *
     * @return blockSize
     */
    public int getBlockSize(){
        return blockSize;
    }
}
