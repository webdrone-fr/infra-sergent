package net.manaty.sergent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;

import org.buildobjects.process.ProcBuilder;
import org.buildobjects.process.ProcResult;
import org.buildobjects.process.TimeoutException;

import org.jboss.logging.Logger;

@RequestScoped
public class AgentService {

    private static final Logger LOG = Logger.getLogger(AgentService.class);
    
    private long timeoutMillis;
    private String command;
    private List<String> args = new ArrayList<>();
    String[] argsTemplate = new String[]{};
    private String proc;
    private String output;
    private String error;
    private int exitValue;
    private long executionTime;
    private File workingPath;

    public void setWorkingPathName(String workingPathName){
        if(workingPathName==null){
            this.workingPath = null;
        } else {
            this.workingPath = new File(workingPathName);
        } 
    }

    public void setTimeoutMillis(long timeoutMillis){
        this.timeoutMillis = timeoutMillis;
    }

    public void setCommand(String command){
        this.command = command;
    }

    public String getProc(){
        return proc;
    }

    public String getOutput(){
        return output;
    }

    public String getError(){
        return error;
    }

    public int getExitValue(){
        return exitValue;
    }

    public long getExecutionTime(){
        return executionTime;
    }

    private void setOutput(ProcResult procResult){
        this.proc=procResult.getProcString();
        this.output=procResult.getOutputString();
        this.error=procResult.getErrorString();
        this.exitValue=procResult.getExitValue();
        this.executionTime = procResult.getExecutionTime();
    }

    public void execute(Map<String,Object> params){
        ProcBuilder builder = new ProcBuilder(command);
        LOG.debug("workingPath: " + workingPath);
        if(workingPath!=null){
            builder.withWorkingDirectory(workingPath);
        }
        if(args.size()>0){
            builder.withArgs(args.toArray(argsTemplate));
        } 
        if(timeoutMillis>0){
            builder.withTimeoutMillis(timeoutMillis);
        }
        ProcResult procResult=null;
        try {
            procResult = builder.run();
            setOutput(procResult);
        } catch (TimeoutException e) {
            if(procResult!=null){
                setOutput(procResult);
            } else {
                this.error = "timeout";
            }
        } catch (Exception e) {
            e.printStackTrace();
            if(procResult!=null){
                setOutput(procResult);
            } else {
                this.error = e.getMessage();
            }
        }
    }

}
