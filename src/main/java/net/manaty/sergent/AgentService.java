package net.manaty.sergent;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;

import javax.enterprise.context.RequestScoped;

import org.buildobjects.process.ProcBuilder;
import org.buildobjects.process.ProcResult;
import org.buildobjects.process.TimeoutException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.FieldInfoList;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import io.quarkus.logging.Log;

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

    // public void saveFileToTmp(String fileName, String content) {
    //     LOG.info("fileName => " + fileName);
    //     LOG.info("content => " + content);
    //     try {
    //     // reading the files with buffered reader
    //     InputStream instr = new ByteArrayInputStream(content.getBytes());
    //     InputStreamReader strrd = new InputStreamReader(instr, "UTF-8"); 
    //     BufferedReader rr = new BufferedReader(strrd); 

    //     // reate file in /tmp/ and quill
    //     String fileUrlServ = "/tmp/" + fileName;
    //     File shScriptFile = new File(fileUrlServ);
    //     FileWriter quill = new FileWriter(shScriptFile);

    //     // read each line of the file
    //     String line;
    //     while ((line = rr.readLine()) != null) {
    //         quill.write(line);
    //     }
    //     quill.close();
    //     } catch (UnsupportedEncodingException ex) {
    //         LOG.error(ex);
    //     } catch (IOException ex) {
    //         LOG.error(ex);
    //     }
    // }

    // public void readExecDeleteFile(String meveoParam, String relativePath, String... fileName) {
    //     try (ScanResult scanResult = new ClassGraph().acceptPathsNonRecursive("").scan()) {
    //         scanResult.getResourcesWithExtension("sh")
    //             .forEachByteArrayThrowingIOException((Resource res, byte[] content) -> {
    //                 saveFileToTmp(res.getPath(), new String(content, StandardCharsets.UTF_8));
    //             });
    //     } catch(IOException ex) {
    //         LOG.error(ex);
    //     }

    //     for (String file : fileName) {
    //         String fileUrl = relativePath + file;
    //         try {
    //             InputStream instr = getClass().getClassLoader().getResourceAsStream(fileUrl); 
    
    //             // reading the files with buffered reader  
    //             InputStreamReader strrd = new InputStreamReader(instr, "UTF-8"); 
    //             BufferedReader rr = new BufferedReader(strrd); 
    
    //             // reate file in /tmp/ and quill
    //             String fileUrlServ = "/tmp/" + file;
    //             File shScriptFile = new File(fileUrlServ);
    //             FileWriter quill = new FileWriter(shScriptFile);
    
    //             // read each line of the file
    //             String line;
    //             while ((line = rr.readLine()) != null) {
    //                 quill.write(line);
    //             }
    //             quill.close();
                
    //             // Do chmod on script
    //             Path filePath = Paths.get(fileUrlServ);
    //             Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("rwxr--r--"));

    //             // Execute file
    //             switch (file) {
    //                 case "setup-git.sh":
    //                     setCommand("./" + fileUrlServ);
    //                     execute(meveoParam);
    //                     LOG.info("! Execution de setup-git.sh !");
    //                     break;
    //                 default:
    //                     LOG.info("! Execution de rien du tout !");
    //             }
                
    //         } catch (IOException ex) {
    //             LOG.error("Failed to interact with IOFile: " + meveoParam, ex);
    //         }
    //     }

    //     for (String fileDelete : fileName) {
    //         String fileUrlServ = "/tmp/" + fileDelete;
    //         File shScriptFile = new File(fileUrlServ);
    //         shScriptFile.delete();
    //     }
    // }

    public void setupGit(String params) {
        String pathWorking = File.separator + "tmp" + File.separator;
        String token = "";
        try{
            Map<String, String> parameterMap = new ObjectMapper().readValue(params, new TypeReference<Map<String, String>>() {});
            token = parameterMap.get("gitinit-token");
        } catch (Exception ex) {
            LOG.error("Error when parsing parameters: ", ex);
        }
        String installCurl = "sudo apt install curl -y";
        String CopySetupGit = installCurl + " && curl -H 'Authorization: token " + token + "' -H 'Accept: application/vnd.github.v3.raw' -O -L https://api.github.com/repos/ArthurGrenier/infra-common/contents/setup-git.sh";
        String CopyDeployGithubKey = installCurl + " && curl -H 'Authorization: token " + token + "' -H 'Accept: application/vnd.github.v3.raw' -O -L https://api.github.com/repos/ArthurGrenier/infra-common/contents/deploy-github-key.sh";    

        // Check if file doesn't exist in /opt/webdrone/common
        File checkOptWebdrone = new File("/opt/webdrone/common");
        if (checkOptWebdrone.isDirectory()) {
            // if yes
            setCommand(".//opt/webdrone/common/setup-git.sh");
            execute(params);
        } else {
            // if no
            curlCopyFileFromGit(CopySetupGit, "setup-git.sh", pathWorking);
            LOG.info("Copied setup-git.sh");
            curlCopyFileFromGit(CopyDeployGithubKey, "deploy-github-key.sh", pathWorking);
            LOG.info("Copied deploy-github-key.sh");

            setCommand(".//tmp/setup-git.sh");
            execute(params);
            LOG.info("Executed setup-git.sh with params");

            File setupGit = new File("/tmp/setup-git.sh");
            File deployGithubKey = new File("/tmp/deploy-github-key.sh");

            setupGit.delete();
            deployGithubKey.delete();
            LOG.info("Removed setup-git and deploy-github-key");
        }
    }

    private void curlCopyFileFromGit(String command, String fileName, String path) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
            processBuilder.directory(new File(path));
            Process process = processBuilder.start();
            process.waitFor();
            Log.info("Process exit value => " + process.exitValue());
            process.destroy();
            // TODO chmod on the script
            chmod(path, fileName);
            processBuilder.command("");
        } catch (IOException ex) {
            LOG.error("Error when copy file from curl: ", ex);
        } catch (InterruptedException ex) {
            LOG.error("Interrupted during curl: ", ex);
        }
    }

    private void chmod(String path, String fileName) {
        try {
            String command = "sudo -i && chmod +x " + path + fileName + " && su debian";
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
            processBuilder.directory(new File(path));
            Process process = processBuilder.start();
            process.waitFor();
            Log.info("Process exit value => " + process.exitValue());
            process.destroy();
        } catch (IOException ex) {
            LOG.error("Error when chmod file: ", ex);
        } catch (InterruptedException ex) {
            LOG.error("Interrupted during chmod: ", ex);
        }
    }
}
