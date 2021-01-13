package com.baolu.jmeter.config;

import com.baolu.jmeter.services.FileServer;
import com.baolu.jmeter.utils.BaoluUtils;
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
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.baolu.jmeter.utils.BaoluUtils.getResourceFileAsText;

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
     * 当前位置
     */
    private static final ThreadLocal<Integer>  curParaPos =  new ThreadLocal<>();

    /**
     * 线程数据
     */
    private final Map<Integer,String[]> curThreadsCsvFileData = new ConcurrentHashMap<>();

    /**
     * 固定结尾
     */
    private static final String [] PARAMS_EOF = {"<EOF>"};

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
                lineValues = itemParaSetVars(delim);
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
        /**
         * Allocate data for each thread and reached End of per thread of str array stop thread
         */
        if (isAllocateData() && getStopThread()) {
            if (curParaPos.get() < 0){
                throw new JMeterStopThreadException("End of file:"+ getFilename()+" detected for CSV DataSet:"
                        +getName()+" configured with stopThread:"+ getStopThread()+", recycle:" + getRecycle());
            }
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
        String pluginsShow = JMeterUtils.getProperty("baolu-jmeter-plugins");
        if (StringUtils.isEmpty(pluginsShow)){
            log.info(System.getProperty("line.separator")+""+getResourceFileAsText("banner/banner.txt"));
            JMeterUtils.setProperty("baolu-jmeter-plugins","show");
        }
    }

    public void testStarted(String s) {
        testStarted();
    }

    public void testEnded() {
        curThreadsCsvFileData.clear();
    }

    public void testEnded(String s) {
        testEnded();
    }

    /**
     * create by libaolu
     * @param delim 分隔符
     * @return
     */
    public String[] itemParaSetVars(String delim){
        if ((curParaPos.get() == null)){
            curParaPos.set(1);
        }
        int threadNum = BaoluUtils.getThreadIndex(Thread.currentThread().getName());
        if (getStopThread()){
            String[] paraArrOld = curThreadsCsvFileData.get(threadNum);
            /**
              * For per thread of str array connect PARAMS_EOF array only when
              * reached End of per thread of str array stop thread.Prevent the
              * problem of reading per thread of str array values the last one
              * param is missing.
            */
            if (!"<EOF>".equals(paraArrOld[paraArrOld.length-1])){
                String[] paraArrNew = new String[paraArrOld.length + 1];
                System.arraycopy(paraArrOld, 0, paraArrNew, 0, paraArrOld.length);
                System.arraycopy(PARAMS_EOF, 0, paraArrNew, paraArrOld.length, PARAMS_EOF.length);
                curThreadsCsvFileData.put(threadNum,paraArrNew);
            }
        }
        String[] paraArr = curThreadsCsvFileData.get(threadNum);
        String[] params = JOrphanUtils.split(paraArr[(curParaPos.get() - 1)], delim, false);
        if (curParaPos.get() == paraArr.length) {
            if (getStopThread()){
                /**
                 * So set curParaPos value is -1 do that each thread reads its own block
                 * data,it needs to stop current thread.
                 */
                curParaPos.set(-1);
            }else {
                curParaPos.set(1);
            }
            if (log.isDebugEnabled()){
                log.debug("current offset is [{}],max offset is [{}]",curParaPos,paraArr.length);
            }
        }else {
            curParaPos.set(curParaPos.get()+1);
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
    public void getThreadsBlockData(FileServer server,boolean recycle, boolean ignoreFirstLine){
        JMeterContext context = getThreadContext();
        String fileName = getFilename();
        int threadNum = BaoluUtils.getThreadIndex(Thread.currentThread().getName());
        int totalLines = server.getCsvFileRows(fileName, ignoreFirstLine);
        int blockSize;

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

        int startLine = server.getstartLine(context, blockSize);
        if (curThreadsCsvFileData.get(threadNum) == null) {//判断当前线程分配的文件是否已经缓存
            String[] blockPara = server.readLineBlock(fileName, recycle, ignoreFirstLine, startLine, blockSize);
            curThreadsCsvFileData.put(threadNum,blockPara);
            if (log.isDebugEnabled()){
                log.debug("current threadNum [{}],blockPara is [{}]",Thread.currentThread().getName(),blockPara);
            }
        }

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
