package net.manaty.sergent;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;

import org.buildobjects.process.ProcBuilder;
import org.buildobjects.process.ProcResult;
import org.buildobjects.process.TimeoutException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    /**
     * set output for log
     * @param procResult process result of the execution
     */
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

    /**
     * Execute the ProcBuilder with params or not
     * @param builder ProcBuilder to execte
     */
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

    /**
     * Global clear of the app
     */
    private void clear() {
        this.proc = null;
        this.output = null;
        this.error = null;
        this.exitValue = 0;
        this.executionTime = 0;
    }

    /**
     * Setup git : installation, initialize the repo
     * @param params json body with organization name, repo name, token
     */
    public void setupGit(String params) {
        // Check if file doesn't exist in /opt/webdrone/common
        File checkOptWebdrone = new File("/opt/webdrone/common");
        if (checkOptWebdrone.isDirectory()) {
            // if yes
            setCommand(".//opt/webdrone/common/setup-git.sh");
            execute(params);
        } else {
            // if no
            String pathWorking = File.separator + "tmp" + File.separator;
            String token = "";
            try{
                Map<String, String> parameterMap = new ObjectMapper().readValue(params, new TypeReference<Map<String, String>>() {});
                token = parameterMap.get("gitinit-token");
            } catch (Exception ex) {
                LOG.error("Error when parsing parameters (gitinit-token): ", ex);
            }
            // Install curl
            List<String> installCurl = Arrays.asList(
                "apt", "install", "curl", "-y"
            );

            try {
                ProcessBuilder processBuilder = new ProcessBuilder(installCurl);
                processBuilder.directory(new File("/"));
                Process process = processBuilder.start();
                process.waitFor();
                Log.info("Process exit value => " + process.exitValue());
            } catch (IOException ex) {
                LOG.error("IOexecption when start the process: ", ex);
            } catch (InterruptedException ex) {
                LOG.error("Thread interrupted: ", ex);
            }

            List<String> CopySetupGit = Arrays.asList(
                "curl", "--silent", "--show-error", "--fail", "-H", "Authorization: token " + token, "-H", "Accept: application/vnd.github.v3.raw", "-O", "-L", "https://api.github.com/repos/webdrone-infra/infra-common/contents/setup-git.sh"
            );
            List<String> CopyDeployGithubKey = Arrays.asList(
                "curl", "--silent", "--show-error", "--fail", "-H", "Authorization: token " + token, "-H", "Accept: application/vnd.github.v3.raw", "-O", "-L", "https://api.github.com/repos/webdrone-infra/infra-common/contents/deploy-github-key.sh"
            );

            curlCopyFileFromGit(CopySetupGit, "setup-git.sh", pathWorking);
            LOG.info("Copied setup-git.sh");
            curlCopyFileFromGit(CopyDeployGithubKey, "deploy-github-key.sh", pathWorking);
            LOG.info("Copied deploy-github-key.sh");
            setWorkingPathName("/");
            setCommand(".//tmp/setup-git.sh");
            execute(params);

            if (getError() == null) {
                LOG.info(String.format("{\"output\":\"%s\"}", getOutput()));
            } else {
                LOG.error(String.format("{\"error\":\"%s\",\"output\":\"%s\"}", getError(), getOutput()));
            }

            // log execute result
            LOG.info("Executed setup-git.sh with params");

            File setupGit = new File("/tmp/setup-git.sh");
            File deployGithubKey = new File("/tmp/deploy-github-key.sh");

            if (setupGit.delete()) {
                LOG.info("Removed setup-git");
            } else {
                LOG.info("setup-git NOT removed");
            }
            if (deployGithubKey.delete()) {
                LOG.info("Removed deploy-github-key");
            } else {
                LOG.info("deploy-github-key NOT removed");
            }
        }
    }

    /**
     * Copy file from command (curl, ...)
     * @param command command to copy file
     * @param fileName how the file is called
     * @param path where the file is created
     */
    private void curlCopyFileFromGit(List<String> command, String fileName, String path) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File(path));
            Process process = processBuilder.start();
            process.waitFor();
            Log.info("Process exit value => " + process.exitValue());
            // log error from process builder
            if (process.exitValue() != 0) {
                LOG.error("Command: " + command.toString());
                InputStream errorStream = process.getErrorStream();
                String text = new BufferedReader(
                    new InputStreamReader(errorStream, StandardCharsets.UTF_8))
                      .lines()
                      .collect(Collectors.joining("\n"));
                LOG.error(text);
            }
            process.destroy();
            chmod(path, fileName);
        } catch (IOException ex) {
            LOG.error("Error when copy file from curl: ", ex);
        } catch (InterruptedException ex) {
            LOG.error("Interrupted during curl: ", ex);
        }
    }

    /**
     * chmod +x (execute) on a file
     * @param path of the file
     * @param fileName name
     */
    public void chmod(String path, String fileName) {
        try {
            LOG.info("File path for chmod => " + path + fileName);
            String command = "/bin/sudo chmod +x " + path + fileName;
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
            Process process = processBuilder.start();
            process.waitFor();
            Log.info("Process exit value => " + process.exitValue());
            // log error from process builder
            if (process.exitValue() != 0) {
                LOG.error("Command: " + command.split(" "));
                InputStream errorStream = process.getErrorStream();
                String text = new BufferedReader(
                    new InputStreamReader(errorStream, StandardCharsets.UTF_8))
                      .lines()
                      .collect(Collectors.joining("\n"));
                LOG.error(text);
            }
            process.destroy();
        } catch (IOException ex) {
            LOG.error("Error when chmod file: ", ex);
        } catch (InterruptedException ex) {
            LOG.error("Interrupted during chmod: ", ex);
        }
    }
}
