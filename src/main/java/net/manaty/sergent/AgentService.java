package net.manaty.sergent;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;

import javax.enterprise.context.RequestScoped;

import org.buildobjects.process.ProcBuilder;
import org.buildobjects.process.ProcResult;
import org.buildobjects.process.TimeoutException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.logging.Logger;

@RequestScoped
public class AgentService {

    private static final Logger LOG = Logger.getLogger(AgentService.class);

    private long timeoutMillis;
    private String command;
    private String proc;
    private String output;
    private String error;
    private int exitValue;
    private long executionTime;
    private File workingPath;

    public void setWorkingPathName(String workingPathName) {
        if (workingPathName == null) {
            this.workingPath = null;
        } else {
            this.workingPath = new File(workingPathName);
        }
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getProc() {
        return proc;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }

    public int getExitValue() {
        return exitValue;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    private void setOutput(ProcResult procResult) {
        this.proc = procResult.getCommandLine();
        this.output = procResult.getOutputString();
        this.error = procResult.getErrorString();
        this.exitValue = procResult.getExitValue();
        this.executionTime = procResult.getExecutionTime();
    }

    /**
     *  Execute command with key-Value parameters
     * @param params Key-Value Parameters
     */
    public void execute(String params) {
        ProcBuilder builder = new ProcBuilder(command);
        clear();
        if (params != null && !params.isEmpty()) {
            this.error = null;
            try {
                Map<String, String> parameterMap = new ObjectMapper()
                        .readValue(params, new TypeReference<Map<String, String>>() {});
                // LOG.debug("parameterMap: " + parameterMap);
                parameterMap.entrySet().stream()
                        .forEach(entry -> {
                            builder.withArg("--" + entry.getKey());
                            builder.withArg(entry.getValue());
                        });
            } catch (Exception e) {
                // LOG.error("Failed to parse parameters: " + params, e);
                this.error = "Failed to parse parameters: " + params;
                return;
            }
        }
        doExecute(builder);
    }

    /**
     * Execute command with options
     * @param params Command options
     */
    public void execute(String[] params) {
        ProcBuilder builder = new ProcBuilder(command);
        clear();
        if (params != null && params.length>0) {
            this.error = null;
            try {
                LOG.debug("parameterArray: " + params);
                Arrays.asList(params).stream()
                        .forEach(entry -> {
                            builder.withArg(entry);
                        });
            } catch (Exception e) {
                // LOG.error("Failed to parse parameters: " + params, e);
                this.error = "Failed to parse parameters: " + params;
                return;
            }
        }

        doExecute(builder);
    }


    private void doExecute(ProcBuilder builder) {
        LOG.debug("workingPath: " + workingPath);
        if (workingPath != null) {
            builder.withWorkingDirectory(workingPath);
        }

        if (timeoutMillis > 0) {
            builder.withTimeoutMillis(timeoutMillis);
        }
        ProcResult procResult = null;
        try {
            LOG.debug("command: " + builder.getCommandLine());
            procResult = builder.run();
            setOutput(procResult);
        } catch (TimeoutException e) {
            if (procResult != null) {
                setOutput(procResult);
            } else {
                this.error = "timeout";
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
            if (procResult != null) {
                setOutput(procResult);
            } else {
                this.error = e.getMessage();
            }
        }
    }

    private void clear() {
        this.proc = null;
        this.output = null;
        this.error = null;
        this.exitValue = 0;
        this.executionTime = 0;
    }

    public void readExecDeleteFile(String relativePath, String fileName, String meveoParam) {
        String fileUrl = relativePath + fileName;
        try {
            InputStream instr = getClass().getClassLoader().getResourceAsStream(fileUrl); 

            // reading the files with buffered reader  
            InputStreamReader strrd = new InputStreamReader(instr, "UTF-8"); 
            BufferedReader rr = new BufferedReader(strrd); 

            // reate file in /tmp/ and quill
            String fileUrlServ = "/tmp/" + fileName;
            File shScriptFile = new File(fileUrlServ);
            FileWriter quill = new FileWriter(shScriptFile);

            // read each line of the file
            String line;
            while ((line = rr.readLine()) != null) {
                quill.write(line);
            }
            quill.close();
            
            // Do chmod on script
            Path filePath = Paths.get(fileUrlServ);
            Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("rwxr--r--"));

            // Execution
            setCommand("./" + fileUrlServ);
            execute(meveoParam); // Read params from meveo ?

            // Deletion of scripts
            shScriptFile.delete();

        } catch (IOException ex) {
            // TODO
        }
    }
}
