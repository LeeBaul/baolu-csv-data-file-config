package com.baolu.jmeter.services;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import com.baolu.jmeter.utils.BaoluUtils;
import org.apache.commons.collections.ArrayStack;
import org.apache.jmeter.gui.JMeterFileFilter;
import org.apache.jmeter.save.CSVSaveService;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.util.JOrphanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2020/8/13 14:30
 **/
public class FileServer {

    private static final Logger log = LoggerFactory.getLogger(FileServer.class);

    /**
     * The default base used for resolving relative files, i.e.<br/>
     * {@code System.getProperty("user.dir")}
     */
    private static final String DEFAULT_BASE = System.getProperty("user.dir");// $NON-NLS-1$

    /** Default base prefix: {@value} */
    private static final String BASE_PREFIX_DEFAULT = "~/"; // $NON-NLS-1$

    private static final String BASE_PREFIX =
            JMeterUtils.getPropDefault("jmeter.save.saveservice.base_prefix", // $NON-NLS-1$
                    BASE_PREFIX_DEFAULT);

    private File base;

    private final Map<String, FileServer.FileEntry> files = new HashMap<>();

    private final Map<String,Integer> newThreadNo = new ConcurrentHashMap<>();

    private static int globalCounter = 1;

    private static final FileServer server = new FileServer();

    // volatile needed to ensure safe publication
    private volatile String scriptName;

    // Cannot be instantiated
    private FileServer() {
        base = new File(DEFAULT_BASE);
        log.info("Default base='{}'", DEFAULT_BASE);
    }

    /**
     * @return the singleton instance of the server.
     */
    public static FileServer getFileServer() {
        return server;
    }

    /**
     * Resets the current base to DEFAULT_BASE.
     */
    public synchronized void resetBase() {
        checkForOpenFiles();
        base = new File(DEFAULT_BASE);
        log.info("Reset base to '{}'", base);
    }

    /**
     * Sets the current base directory for relative file names from the provided path.
     * If the path does not refer to an existing directory, then its parent is used.
     * Normally the provided path is a file, so using the parent directory is appropriate.
     *
     * @param basedir the path to set, or {@code null} if the GUI is being cleared
     * @throws IllegalStateException if files are still open
     */
    public synchronized void setBasedir(String basedir) {
        checkForOpenFiles(); // TODO should this be called if basedir == null?
        if (basedir != null) {
            File newBase = new File(basedir);
            if (!newBase.isDirectory()) {
                newBase = newBase.getParentFile();
            }
            base = newBase;
            log.info("Set new base='{}'", base);
        }
    }

    /**
     * Sets the current base directory for relative file names from the provided script file.
     * The parameter is assumed to be the path to a JMX file, so the base directory is derived
     * from its parent.
     *
     * @param scriptPath the path of the script file; must be not be {@code null}
     * @throws IllegalStateException if files are still open
     * @throws IllegalArgumentException if scriptPath parameter is null
     */
    public synchronized void setBaseForScript(File scriptPath) {
        if (scriptPath == null){
            throw new IllegalArgumentException("scriptPath must not be null");
        }
        setScriptName(scriptPath.getName());
        // getParentFile() may not work on relative paths
        setBase(scriptPath.getAbsoluteFile().getParentFile());
    }

    /**
     * Sets the current base directory for relative file names.
     *
     * @param jmxBase the path of the script file base directory, cannot be null
     * @throws IllegalStateException if files are still open
     * @throws IllegalArgumentException if {@code basepath} is null
     */
    public synchronized void setBase(File jmxBase) {
        if (jmxBase == null) {
            throw new IllegalArgumentException("jmxBase must not be null");
        }
        checkForOpenFiles();
        base = jmxBase;
        log.info("Set new base='{}'", base);
    }

    /**
     * Check if there are entries in use.
     * <p>
     * Caller must ensure that access to the files map is single-threaded as
     * there is a window between checking the files Map and clearing it.
     *
     * @throws IllegalStateException if there are any entries still in use
     */
    private void checkForOpenFiles() throws IllegalStateException {
        if (filesOpen()) { // checks for entries in use
            throw new IllegalStateException("Files are still open, cannot change base directory");
        }
        files.clear(); // tidy up any unused entries
    }

    public synchronized String getBaseDir() {
        return base.getAbsolutePath();
    }

    public static String getDefaultBase(){
        return DEFAULT_BASE;
    }

    /**
     * Calculates the relative path from DEFAULT_BASE to the current base,
     * which must be the same as or a child of the default.
     *
     * @return the relative path, or {@code "."} if the path cannot be determined
     */
    public synchronized File getBaseDirRelative() {
        // Must first convert to absolute path names to ensure parents are available
        File parent = new File(DEFAULT_BASE).getAbsoluteFile();
        File f = base.getAbsoluteFile();
        ArrayStack l = new ArrayStack();
        while (f != null) {
            if (f.equals(parent)){
                if (l.isEmpty()){
                    break;
                }
                File rel = new File((String) l.pop());
                while(!l.isEmpty()) {
                    rel = new File(rel, (String) l.pop());
                }
                return rel;
            }
            l.push(f.getName());
            f = f.getParentFile();
        }
        return new File(".");
    }

    /**
     * Creates an association between a filename and a File inputOutputObject,
     * and stores it for later use - unless it is already stored.
     *
     * @param filename - relative (to base) or absolute file name (must not be null)
     */
    public void reserveFile(String filename) {
        reserveFile(filename,null);
    }

    /**
     * Creates an association between a filename and a File inputOutputObject,
     * and stores it for later use - unless it is already stored.
     *
     * @param filename - relative (to base) or absolute file name (must not be null)
     * @param charsetName - the character set encoding to use for the file (may be null)
     */
    public void reserveFile(String filename, String charsetName) {
        reserveFile(filename, charsetName, filename, false);
    }

    /**
     * Creates an association between a filename and a File inputOutputObject,
     * and stores it for later use - unless it is already stored.
     *
     * @param filename - relative (to base) or absolute file name (must not be null)
     * @param charsetName - the character set encoding to use for the file (may be null)
     * @param alias - the name to be used to access the object (must not be null)
     */
    public void reserveFile(String filename, String charsetName, String alias) {
        reserveFile(filename, charsetName, alias, false);
    }

    /**
     * Creates an association between a filename and a File inputOutputObject,
     * and stores it for later use - unless it is already stored.
     *
     * @param filename - relative (to base) or absolute file name (must not be null or empty)
     * @param charsetName - the character set encoding to use for the file (may be null)
     * @param alias - the name to be used to access the object (must not be null)
     * @param hasHeader true if the file has a header line describing the contents
     * @return the header line; may be null
     * @throws IllegalArgumentException if header could not be read or filename is null or empty
     */
    public synchronized String reserveFile(String filename, String charsetName, String alias, boolean hasHeader) {
        if (filename == null || filename.isEmpty()){
            throw new IllegalArgumentException("Filename must not be null or empty");
        }
        if (alias == null){
            throw new IllegalArgumentException("Alias must not be null");
        }
        FileServer.FileEntry fileEntry = files.get(alias);
        if (fileEntry == null) {
            fileEntry = new FileServer.FileEntry(resolveFileFromPath(filename), null, charsetName);
            if (filename.equals(alias)){
                log.info("Stored: {}", filename);
            } else {
                log.info("Stored: {} Alias: {}", filename, alias);
            }
            files.put(alias, fileEntry);
            if (hasHeader) {
                try {
                    fileEntry.headerLine = readLine(alias, false);
                    if (fileEntry.headerLine == null) {
                        fileEntry.exception = new EOFException("File is empty: " + fileEntry.file);
                    }
                } catch (IOException | IllegalArgumentException e) {
                    fileEntry.exception = e;
                }
            }
        }
        if (hasHeader && fileEntry.headerLine == null) {
            throw new IllegalArgumentException("Could not read file header line for file " + filename,
                    fileEntry.exception);
        }
        return fileEntry.headerLine;
    }

    /**
     * Resolves file name into {@link File} instance.
     * When filename is not absolute and not found from current working dir,
     * it tries to find it under current base directory
     * @param filename original file name
     * @return {@link File} instance
     */
    private File resolveFileFromPath(String filename) {
        File f = new File(filename);
        if (f.isAbsolute() || f.exists()) {
            return f;
        } else {
            return new File(base, filename);
        }
    }

    /**
     * Get the next line of the named file, recycle by default.
     *
     * @param filename the filename or alias that was used to reserve the file
     * @return String containing the next line in the file
     * @throws IOException when reading of the file fails, or the file was not reserved properly
     */
    public String readLine(String filename) throws IOException {
        return readLine(filename, true);
    }

    /**
     * Get the next line of the named file, first line is name to false
     *
     * @param filename the filename or alias that was used to reserve the file
     * @param recycle - should file be restarted at EOF?
     * @return String containing the next line in the file (null if EOF reached and not recycle)
     * @throws IOException when reading of the file fails, or the file was not reserved properly
     */
    public String readLine(String filename, boolean recycle) throws IOException {
        return readLine(filename, recycle, false);
    }
    /**
     * Get the next line of the named file
     *
     * @param filename the filename or alias that was used to reserve the file
     * @param recycle - should file be restarted at EOF?
     * @param ignoreFirstLine - Ignore first line
     * @return String containing the next line in the file (null if EOF reached and not recycle)
     * @throws IOException when reading of the file fails, or the file was not reserved properly
     */
    public synchronized String readLine(String filename, boolean recycle,
                                        boolean ignoreFirstLine) throws IOException {
        FileServer.FileEntry fileEntry = files.get(filename);
        if (fileEntry != null) {
            if (fileEntry.inputOutputObject == null) {
                fileEntry.inputOutputObject = createBufferedReader(fileEntry);
            } else if (!(fileEntry.inputOutputObject instanceof Reader)) {
                throw new IOException("File " + filename + " already in use");
            }
            BufferedReader reader = (BufferedReader) fileEntry.inputOutputObject;
            String line = reader.readLine();
            if (line == null && recycle) {
                reader.close();
                reader = createBufferedReader(fileEntry);
                fileEntry.inputOutputObject = reader;
                if (ignoreFirstLine) {
                    // read first line and forget
                    reader.readLine();//NOSONAR
                }
                line = reader.readLine();
            }
            log.debug("Read:{}", line);
            return line;
        }
        throw new IOException("File never reserved: "+filename);
    }

    /**
     *
     * @param alias the file name or alias
     * @param recycle whether the file should be re-started on EOF
     * @param ignoreFirstLine whether the file contains a file header which will be ignored
     * @param delim the delimiter to use for parsing
     * @return the parsed line, will be empty if the file is at EOF
     * @throws IOException when reading of the aliased file fails, or the file was not reserved properly
     */
    public synchronized String[] getParsedLine(String alias, boolean recycle, boolean ignoreFirstLine, char delim) throws IOException {
        BufferedReader reader = getReader(alias, recycle, ignoreFirstLine);
        return CSVSaveService.csvReadFile(reader, delim);
    }

    /**
     * Return BufferedReader handling close if EOF reached and recycle is true
     * and ignoring first line if ignoreFirstLine is true
     *
     * @param alias           String alias
     * @param recycle         Recycle at eof
     * @param ignoreFirstLine Ignore first line
     * @return {@link BufferedReader}
     */
    private BufferedReader getReader(String alias, boolean recycle, boolean ignoreFirstLine) throws IOException {
        FileServer.FileEntry fileEntry = files.get(alias);
        if (fileEntry != null) {
            BufferedReader reader;
            if (fileEntry.inputOutputObject == null) {
                reader = createBufferedReader(fileEntry);
                fileEntry.inputOutputObject = reader;
                if (ignoreFirstLine) {
                    // read first line and forget
                    reader.readLine(); //NOSONAR
                }
            } else if (!(fileEntry.inputOutputObject instanceof Reader)) {
                throw new IOException("File " + alias + " already in use");
            } else {
                reader = (BufferedReader) fileEntry.inputOutputObject;
                if (recycle) { // need to check if we are at EOF already
                    reader.mark(1);
                    int peek = reader.read();
                    if (peek == -1) { // already at EOF
                        reader.close();
                        reader = createBufferedReader(fileEntry);
                        fileEntry.inputOutputObject = reader;
                        if (ignoreFirstLine) {
                            // read first line and forget
                            reader.readLine(); //NOSONAR
                        }
                    } else { // OK, we still have some data, restore it
                        reader.reset();
                    }
                }
            }
            return reader;
        } else {
            throw new IOException("File never reserved: "+alias);
        }
    }

    private BufferedReader createBufferedReader(FileServer.FileEntry fileEntry) throws IOException {
        if (!fileEntry.file.canRead() || !fileEntry.file.isFile()) {
            throw new IllegalArgumentException("File "+ fileEntry.file.getName()+ " must exist and be readable");
        }
        FileInputStream fis = new FileInputStream(fileEntry.file);
        InputStreamReader isr = null;
        // If file encoding is specified, read using that encoding, otherwise use default platform encoding
        String charsetName = fileEntry.charSetEncoding;
        if(!JOrphanUtils.isBlank(charsetName)) {
            isr = new InputStreamReader(fis, charsetName);
        } else {
            isr = new InputStreamReader(fis);
        }
        return new BufferedReader(isr);
    }

    public synchronized void write(String filename, String value) throws IOException {
        FileServer.FileEntry fileEntry = files.get(filename);
        if (fileEntry != null) {
            if (fileEntry.inputOutputObject == null) {
                fileEntry.inputOutputObject = createBufferedWriter(fileEntry);
            } else if (!(fileEntry.inputOutputObject instanceof Writer)) {
                throw new IOException("File " + filename + " already in use");
            }
            BufferedWriter writer = (BufferedWriter) fileEntry.inputOutputObject;
            log.debug("Write:{}", value);
            writer.write(value);
        } else {
            throw new IOException("File never reserved: "+filename);
        }
    }

    private BufferedWriter createBufferedWriter(FileServer.FileEntry fileEntry) throws IOException {
        FileOutputStream fos = new FileOutputStream(fileEntry.file);
        OutputStreamWriter osw;
        // If file encoding is specified, write using that encoding, otherwise use default platform encoding
        String charsetName = fileEntry.charSetEncoding;
        if(!JOrphanUtils.isBlank(charsetName)) {
            osw = new OutputStreamWriter(fos, charsetName);
        } else {
            osw = new OutputStreamWriter(fos);
        }
        return new BufferedWriter(osw);
    }

    public synchronized void closeFiles() throws IOException {
        for (Map.Entry<String, FileServer.FileEntry> me : files.entrySet()) {
            closeFile(me.getKey(),me.getValue() );
        }
        files.clear();
    }

    /**
     * @param name the name or alias of the file to be closed
     * @throws IOException when closing of the aliased file fails
     */
    public synchronized void closeFile(String name) throws IOException {
        FileServer.FileEntry fileEntry = files.get(name);
        closeFile(name, fileEntry);
    }

    private void closeFile(String name, FileServer.FileEntry fileEntry) throws IOException {
        if (fileEntry != null && fileEntry.inputOutputObject != null) {
            log.info("Close: {}", name);
            fileEntry.inputOutputObject.close();
            fileEntry.inputOutputObject = null;
        }
    }

    boolean filesOpen() { // package access for test code only
        return files.values().stream()
                .anyMatch(fileEntry -> fileEntry.inputOutputObject != null);
    }

    /**
     * Method will get a random file in a base directory
     * <p>
     * TODO hey, not sure this method belongs here.
     * FileServer is for thread safe File access relative to current test's base directory.
     *
     * @param basedir    name of the directory in which the files can be found
     * @param extensions array of allowed extensions, if <code>null</code> is given,
     *                   any file be allowed
     * @return a random File from the <code>basedir</code> that matches one of
     * the extensions
     */
    public File getRandomFile(String basedir, String[] extensions) {
        File input = null;
        if (basedir != null) {
            File src = new File(basedir);
            File[] lfiles = src.listFiles(new JMeterFileFilter(extensions));
            if (lfiles != null) {
                // lfiles cannot be null as it has been checked before
                int count = lfiles.length;
                input = lfiles[ThreadLocalRandom.current().nextInt(count)];
            }
        }
        return input;
    }

    /**
     * Get {@link File} instance for provided file path,
     * resolve file location relative to base dir or script dir when needed
     *
     * @param path original path to file, maybe relative
     * @return {@link File} instance
     */
    public File getResolvedFile(String path) {
        reserveFile(path);
        return files.get(path).file;
    }

    private static class FileEntry{
        private String headerLine;
        private Throwable exception;
        private final File file;
        private Closeable inputOutputObject;
        private final String charSetEncoding;

        FileEntry(File f, Closeable o, String e) {
            file = f;
            inputOutputObject = o;
            charSetEncoding = e;
        }
    }

    /**
     * Resolve a file name that may be relative to the base directory. If the
     * name begins with the value of the JMeter property
     * "jmeter.save.saveservice.base_prefix" - default "~/" - then the name is
     * assumed to be relative to the basename.
     *
     * @param relativeName
     *            filename that should be checked for
     *            <code>jmeter.save.saveservice.base_prefix</code>
     * @return the updated filename
     */
    public static String resolveBaseRelativeName(String relativeName) {
        if (relativeName.startsWith(BASE_PREFIX)){
            String newName = relativeName.substring(BASE_PREFIX.length());
            return new File(getFileServer().getBaseDir(),newName).getAbsolutePath();
        }
        return relativeName;
    }

    /**
     * @return JMX Script name
     * @since 2.6
     */
    public String getScriptName() {
        return scriptName;
    }

    /**
     * @param scriptName Script name
     * @since 2.6
     */
    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * create by libaolu
     * @param fileName parameter file
     * @param recycle recycle
     * @param firstLineIsNames ignore first line flag
     * @param startNumber start number
     * @param blockSize block size for each threads
     * @return
     */
    public synchronized String[] readLineBlock(String fileName, boolean recycle, boolean firstLineIsNames, int startNumber, int blockSize ,String charsetName){
        File file = resolveFileFromPath(fileName);
        String[] paramArr = null;
        InputStreamReader isr = null;
        // If file encoding is specified, read using that encoding, otherwise use default platform encoding
        try {
            FileInputStream fis = new FileInputStream(file);
            if(!JOrphanUtils.isBlank(charsetName)) {
                isr = new InputStreamReader(fis, charsetName);
            } else {
                isr = new InputStreamReader(fis);
            }
            LineNumberReader reader = new LineNumberReader(isr);
            String s = null;
            if (firstLineIsNames)
                s = reader.readLine();
            if (blockSize >= 1) {
                paramArr = new String[blockSize];
                if (blockSize < 1) {
                    log.error("blockSize can not be less than 1");
                }
                int k = blockSize + startNumber;
                int i = startNumber + 1;
                int p = 0; int j = 0;
                while ((s = reader.readLine()) != null){
                    ++p;
                    if ((p >= i) && (p <= k)){
                        paramArr[j] = s;
                        ++j;
                    }
                    if (p == k) break;
                }
                reader.close();
                isr.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return paramArr;
    }

    /**
     * create by libaolu
     * @param context JMeterContext
     * @param blockSize block size for each threads
     * @return start line for each threads
     */
    @Deprecated
    public int getStartLine(JMeterContext context, int blockSize){
        int curThreadNo = context.getThread().getThreadNum();
        return (curThreadNo * blockSize);
    }

    /**
     * create by libaolu
     * Reset thread number,there can only be one thread number 0
     * key the original thread number and value the new thread number
     * @param blockSize block size for each threads
     * @return start line for each threads
     */
    public int getStartLine(int blockSize){
        String multipleTgIndex = BaoluUtils.getMultipleTgIndex(Thread.currentThread().getName());
        synchronized (this){
            if (newThreadNo.get(multipleTgIndex) == null){//First judge whether the original thread number has been modified.
                if(newThreadNo.size() == 0){
                    newThreadNo.put(multipleTgIndex,0);//Multithreading does not need to care which thread arrives first.
                }else {
                    newThreadNo.put(multipleTgIndex,globalCounter);
                    globalCounter +=1;
                }
            }
            return newThreadNo.get(multipleTgIndex) * blockSize;
        }
    }

    /**
     * create by libaolu
     * @param context JMeterContext
     * @param fileLines parameter file total line
     * @return block size for each threads
     */
    public int getBlockSize(JMeterContext context, int fileLines){
        int blockSize = 0;
        int threadTotal = JMeterContextService.getTotalThreads();
        if (fileLines > 0) {
            blockSize = fileLines / threadTotal;
            if (blockSize < 1) {
                log.error("params size < threads count; params size {},threads count {}",fileLines,threadTotal);
            }
        }
        return blockSize;
    }

    /**
     * create by libaolu
     * @param fileName parameter file
     * @param ignoreFirstLine ignore first line flag
     * @return parameter file total line
     */
    public int getCsvFileRows(String fileName, boolean ignoreFirstLine){
        int rows = 0;
        File file = resolveFileFromPath(fileName);
        try {
            FileReader in = new FileReader(file);
            LineNumberReader reader = new LineNumberReader(in);
            String line = null;
            if (ignoreFirstLine)
                line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                ++rows;
            }
            reader.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rows;
    }

    public void setGlobalCounter(int globalCounter) {
        this.globalCounter = globalCounter;
    }

    public Map<String, Integer> getNewThreadNo() {
        return newThreadNo;
    }
}
