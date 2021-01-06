package com.libaolu.jmeter.functions;

import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.functions.AbstractFunction;
import org.apache.jmeter.functions.InvalidVariableException;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2019/12/4 18:33
 **/
public class SteppingShape extends AbstractFunction {
    private static final String FUNCTION_NAME = "__SteppingShape";
    private static final List<String> desc = new LinkedList<String>();
    private static final Logger log = LoggerFactory.getLogger(SteppingShape.class);

    private CompoundVariable[] values;

    private static final int MAX_PARAM_COUNT = 3;

    private static final int MIN_PARAM_COUNT = 1;

    private static final String SUFFIX = "vu";

    static {
        desc.add("请按升序输入整数梯度值,多个梯度值以英文逗号分割。");
    }

    public SteppingShape(){

    }

    @Override
    public String execute(SampleResult sampleResult, Sampler sampler) throws InvalidVariableException {
        int threads = JMeterContextService.getNumberOfThreads();
        String step = values[0].execute();
        List<Integer> list = new ArrayList<>();
        String[] arg;
        if ("".equals(step) || step == null){
            log.error("梯度值为空，请检查梯度值");
        }else  {
            int index = step.indexOf(",");
            if (index > 0){
                arg = step.split(",");
                for (int i=0; i<arg.length; i++){
                    list.add(i,Integer.valueOf(arg[i]));
                }
                if (list.contains(threads)){
                    return String.valueOf(threads)+SUFFIX;
                }
            }else {
                log.error("梯度值之间未以英文逗号分隔:{}",step);
            }
        }
        return "";
    }

    @Override
    public void setParameters(Collection<CompoundVariable> parameters) throws InvalidVariableException {
        checkParameterCount(parameters, MIN_PARAM_COUNT, MAX_PARAM_COUNT);
        values = parameters.toArray(new CompoundVariable[parameters.size()]);
    }

    @Override
    public String getReferenceKey() {
        return FUNCTION_NAME;
    }

    @Override
    public List<String> getArgumentDesc() {
        return desc;
    }
}
