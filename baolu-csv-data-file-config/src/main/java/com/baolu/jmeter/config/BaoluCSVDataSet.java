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

    public BaoluCSVDataSet(){

    }

    public void iterationStart(LoopIterationEvent loopIterationEvent) {
        FileServer server = FileServer.getFileServer();
        final JMeterContext context = getThreadContext();
        String delim = getDelimiter();
        if ("\\t".equals(delim)) { // $NON-NLS-1$
            delim = "\t";// Make it easier to enter a Tab // $NON-NLS-1$
        } else if (delim.isEmpty()){
            log.debug("Empty delimiter, will use ','");
            delim=",";
        }

        if (vars == null) {
            String fileName = getFilename().trim();
            String mode = getShareMode();
            int modeInt = BaoluCSVDataSetBeanInfo.getShareModeAsInt(mode);
            final String names = getVariableNames();
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
                server.reserveFile(fileName, getFileEncoding(), alias, ignoreFirstLine);
                vars = JOrphanUtils.split(names, ","); // $NON-NLS-1$
            }
            trimVarNames(vars);
        }

        // TODO: fetch this once as per vars above?
        JMeterVariables threadVars = context.getVariables();
        String[] lineValues = {};
        try {
            if (getQuotedData()) {
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
        //开始操作 此处为调试代码
        log.info("===================================[{}]",isAllocateData());
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
}
