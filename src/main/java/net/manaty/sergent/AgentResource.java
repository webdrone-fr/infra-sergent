package net.manaty.sergent;

import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.jboss.logging.Logger;


@Path("/sergent")
public class AgentResource {
    private static final Logger LOG = Logger.getLogger(AgentResource.class);
    private static final List<String> COMMANDS = List.of(
            "list",
            "dockerpull",
            "gitpull",
            "install",
            "setup-docker",
            "setup-git",
            "deploy-kc-theme",
            "update-modules",
            "deploy");

    @Inject
    AgentService service;

    /**
     * GET Request to Sergent Service
     * Executes a specific script depending on command passed
     * @param command Query Parameter => command to execute which executes a specific script
     * @param params Query Parameter => Input of type Container/ File/ Realm/ fullUrl/ theme for deploy-kc-theme 
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String sergent(
            @QueryParam("command") @DefaultValue("list") String command,
            @QueryParam("params") String params,
            @HeaderParam(value = "X-delay-in-sec") @DefaultValue("10") long timeoutSec) {
        String result = null;
        String commandPath = System.getenv("SERGENT_COMMAND_PATH");
        service.setWorkingPathName(commandPath);
        service.setTimeoutMillis(timeoutSec * 1000);
        LOG.debug("commandPath: " + commandPath);
        LOG.debug("command: " + command);

        switch (command) {
            case "dockerpull":
                result = execute("./dockerpull.sh");
                break;
            case "gitpull":
                result = execute("./gitpull.sh");
                break;
            case "install":
                result = execute("./install.sh");
                break;
            case "deploy-kc-theme":
                try {
                    // LOG.debug("params: " + params);
                    result = execute("./deploy-kc-theme.sh", params);
                } catch (Exception e) {
                    result = String.format("{\"error\":\"%s\"}",
                            "Error deploying theme: " + params);
                    // LOG.error("Failed to deploy theme: " + params, e);
                }
                break;
            case "deploy":
                try {
                    // LOG.debug("params: " + params);
                    result = execute("./deploy.sh", params);
                } catch (Exception e) {
                    result = String.format("{\"error\":\"%s\"}",
                            "Error executing deploy cmd: " + params);
                    // LOG.error("Failed to execute deploy cmd: " + params, e);
                }
                break;
            default:
                result = String.format("{\"commands\":[%s]}", String.join(",", COMMANDS.stream()
                        .map(cmd -> String.format("\"%s\"", cmd)).toArray(String[]::new)));
        }
        return result;
    }

    /**
     * POST Request to Sergent Service
     * @param command Command to execute 
     * @param params String of parameters to pass with command
     * @param timeoutSec Delay before timeout, default is 10 sec
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String sergentPost(
            @QueryParam("command") String command,
            String params,
            @HeaderParam(value = "X-delay-in-sec") @DefaultValue("10") long timeoutSec) {
        String result = null;
        String commandPath = System.getenv("SERGENT_COMMAND_PATH");
        service.setWorkingPathName(commandPath);
        service.setTimeoutMillis(timeoutSec * 1000);
        LOG.debug("commandPath: " + commandPath);
        LOG.debug("command: " + command);
        switch (command) {
            case "update-modules":
                String resultGitpull = execute("./gitpull.sh");
                LOG.debug("GitPull Result: "+resultGitpull);
                if (resultGitpull.contains("output")){
                    try {
                        // LOG.debug("params: " + params);
                        String stackNameOutput = executeMult("./getenvvalue.sh","STACK_NAME");
                        String usernameOutput = executeMult("./getenvvalue.sh","MEVEO_ADMIN_USERNAME");
                        String passOutput = executeMult("./getenvvalue.sh","MEVEO_ADMIN_PASSWORD");
                        String outputStr= "output\":\"";
                        String stackName = stackNameOutput.substring(stackNameOutput.indexOf(outputStr)+outputStr.length(),stackNameOutput.indexOf("\"}")).stripTrailing();
                        String username = usernameOutput.substring(usernameOutput.indexOf(outputStr)+outputStr.length(),usernameOutput.indexOf("\"}")).stripTrailing();
                        String password = passOutput.substring(passOutput.indexOf(outputStr)+outputStr.length(),passOutput.indexOf("\"}")).stripTrailing();
                        LOG.debug("Stack Name : " + stackName);
                        result = executeMult("docker", "exec", "-t", stackName+"-meveo", "curl", "-X", "POST", "-H", "Content-Type: application/json", "--user", username+":"+password, "--max-time", String.valueOf(timeoutSec) ,"--data", params, "localhost:8080/meveo/api/rest/module/initDefault");
                        LOG.debug("Result: "+ result);
                    } catch (Exception e) {
                        result = String.format("{\"error\":\"%s\"}",
                                "Error updating modules: " + params);
                        // LOG.error("Failed to update modules: " + params, e);
                    }
                } else {
                    result = String.format("{\"error\":\"%s\"}",
                            "Error executing gitpull for update-modules" + params);
                    LOG.error("Failed to execute gitpull for update-modules" + resultGitpull);
                }
                break;
            case "setup-docker":
                // LOG.debug("params: " + params);
                try {
                    result = executeMult("./setup-docker.sh", params);
                } catch (Exception e) {
                    result = String.format("{\"error\":\"%s\"}",
                            "Error executing setup-docker for setup docker" + params);
                    // LOG.error("Failed to execute setup-docker for setup docker: " + params, e);
                }
                break;
            case "setup-git":
                // LOG.debug("params: " + params);
                try {
                    result = execute("./setup-git.sh", params);
                } catch (Exception e) {
                    result = String.format("{\"error\":\"%s\"}",
                            "Error executing gitpull for update-modules" + params);
                    // LOG.error("Failed to execute gitpull for update-modules: " + params, e);
                }
                break;
            default:
                result = String.format("{\"commands\":[%s]}", String.join(",", COMMANDS.stream()
                        .map(cmd -> String.format("\"%s\"", cmd)).toArray(String[]::new)));
        }
        return result;
    }

    /**
     * Execute command with no additional params
     * @param command Command to execute
     * @return
     */
    private String execute(String command) {
        String result = null;
        service.setCommand(command);
        service.execute((String[])null);
        if (service.getError() == null) {
            result = String.format("{\"output\":\"%s\"}", service.getOutput());
        } else {
            result = String.format("{\"error\":\"%s\",\"output\":\"%s\"}", service.getError(), service.getOutput());
        }
        return result;
    }

    /**
     * Execute command with params as options of command
     * @param command Command to execute
     * @param params Key-Value Options for specified command
     * @return
     */
    private String execute(String command, String params) {
        String result = null;
        service.setCommand(command);
        service.execute(params);
        if (service.getError() == null) {
            result = String.format("{\"output\":\"%s\"}", service.getOutput());
        } else {
            result = String.format("{\"error\":\"%s\",\"output\":\"%s\"}", service.getError(), service.getOutput());
        }
        return result;
    }

    /**
     * Execute command with multiple parameters
     * @param command Command to execute
     * @param params Additional Options to add to command
     * @return
     */
    private String executeMult(String command, String...params) {
        String result = null;
        service.setCommand(command);
        service.execute(params);
        if (service.getError() == null) {
            result = String.format("{\"output\":\"%s\"}", service.getOutput());
        } else {
            result = String.format("{\"error\":\"%s\",\"output\":\"%s\"}", service.getError(), service.getOutput());
         }
        return result;
    }
}
