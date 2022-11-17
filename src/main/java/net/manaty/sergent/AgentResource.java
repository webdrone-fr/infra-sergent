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

import com.fasterxml.jackson.core.base.ParserMinimalBase;
import com.google.gson.Gson;
import com.google.gson.JsonObject;


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
            "deploy",
            "restart-docker",
            "docker-status",
            "sergent-update");

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
            @HeaderParam(value = "X-delay-in-sec") @DefaultValue("300") long timeoutSec) {
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
            case "docker-status":
                try {
                    result = executeMult("docker", "ps", "--format", "table {{.Status}}\t{{.Names}}");
                } catch (Exception e) {
                    result = String.format("{\"error\":\"%s\"}",
                            "Error executing docker-status");
                    LOG.error("Failed to execute docker-status: ", e);
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
     * @param timeoutSec Delay before timeout, default is 300 sec
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String sergentPost(
            @QueryParam("command") String command,
            String params,
            @HeaderParam(value = "X-delay-in-sec") @DefaultValue("300") long timeoutSec) {
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
                        JsonObject paramJson = new Gson().fromJson(params, JsonObject.class);
                        LOG.debug("All params: " + paramJson);
                        String paramStackName = paramJson.get("stackName").getAsString();
                        String paramServiceName = paramJson.get("serviceName").getAsString();
                        String paramServiceWebContext = paramJson.get("serviceWebContext").getAsString();
                        paramJson.remove("stackName");
                        paramJson.remove("serviceName");
                        paramJson.remove("serviceWebContext");
                        String paramDocker = paramJson.toString();
                        LOG.debug("param docker: " + paramDocker);
                        String usernameOutput = executeMult("./getenvvalue.sh","MEVEO_ADMIN_USERNAME");
                        String passOutput = executeMult("./getenvvalue.sh","MEVEO_ADMIN_PASSWORD");
                        String outputStr= "output\":\"";
                        String username = usernameOutput.substring(usernameOutput.indexOf(outputStr)+outputStr.length(),usernameOutput.indexOf("\"}")).stripTrailing();
                        String password = passOutput.substring(passOutput.indexOf(outputStr)+outputStr.length(),passOutput.indexOf("\"}")).stripTrailing();
                        if (paramStackName.isEmpty()) {
                            String stackNameOutput = executeMult("./getenvvalue.sh","STACK_NAME");
                            String stackName = stackNameOutput.substring(stackNameOutput.indexOf(outputStr)+outputStr.length(),stackNameOutput.indexOf("\"}")).stripTrailing();
                            LOG.debug("Stack Name : " + stackName);
                            if (paramServiceName.isEmpty() || paramServiceWebContext.isEmpty()) {
                                LOG.debug("Default way");
                                result = executeMult("docker", "exec", "-t", stackName+"-meveo", "curl", "-X", "POST", "-H", "Content-Type: application/json", "--user", username+":"+password, "--max-time", String.valueOf(timeoutSec) ,"--data", paramDocker, "localhost:8080/meveo/api/rest/module/initDefault");
                            } else {
                                LOG.debug("stackName empty => " + paramStackName + " -- But service and webcontext => " + paramServiceName + " " + paramServiceWebContext);
                                result = executeMult("docker", "exec", "-t", stackName+"-"+paramServiceName, "curl", "-X", "POST", "-H", "Content-Type: application/json", "--user", username+":"+password, "--max-time", String.valueOf(timeoutSec) ,"--data", paramDocker, "localhost:8080/"+paramServiceWebContext+"/api/rest/module/initDefault");
                            }
                            LOG.debug("Result: "+ result);
                        } else {
                            LOG.debug("Stack Name : " + paramStackName);
                            if (paramServiceName.isEmpty() || paramServiceWebContext.isEmpty()) {
                                LOG.debug("stackName not empty => " + paramStackName);
                                result = executeMult("docker", "exec", "-t", paramStackName+"-meveo", "curl", "-X", "POST", "-H", "Content-Type: application/json", "--user", username+":"+password, "--max-time", String.valueOf(timeoutSec) ,"--data", paramDocker, "localhost:8080/meveo/api/rest/module/initDefault");
                            } else {
                                LOG.debug("stackName not empty => " + paramStackName + " -- And service and webcontext => " + paramServiceName + " " + paramServiceWebContext);
                                result = executeMult("docker", "exec", "-t", paramStackName+"-"+paramServiceName, "curl", "-X", "POST", "-H", "Content-Type: application/json", "--user", username+":"+password, "--max-time", String.valueOf(timeoutSec) ,"--data", paramDocker, "localhost:8080/"+paramServiceWebContext+"/api/rest/module/initDefault");
                            }
                            LOG.debug("Result: "+ result);
                        }
                    } catch (Exception e) {
                        result = String.format("{\"error\":\"%s\"}",
                                "Error updating modules: " + params);
                        LOG.error("Failed to update modules: " + params, e);
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
                    service.chmod(commandPath, "/setup-docker.sh");
                    result = execute("./setup-docker.sh", params);
                } catch (Exception e) {
                    result = String.format("{\"error\":\"%s\"}",
                            "Error executing setup-docker for setup docker" + params);
                    LOG.error("Failed to execute setup-docker for setup docker: " + params, e);
                }
                break;
            case "setup-git":
                try {
                    service.setupGit(params);
                } catch (Exception e) {
                    result = String.format("{\"error\":\"%s\"}",
                            "Error executing setup-git with params" + params);
                    LOG.error("Failed to execute setup-git with params: " + params, e);
                }
                break;
            case "restart-docker":
                try {
                    JsonObject paramJson = new Gson().fromJson(params, JsonObject.class);
                    if (paramJson.get("container").getAsString().isEmpty()) {
                        result = executeMult("docker-compose", "down") + "\n";
                        result += executeMult("docker-compose", "up", "-d");
                    } else {
                        result = executeMult("docker", "restart", paramJson.get("container").getAsString());
                    }
                } catch (Exception e) {
                    result = String.format("{\"error\":\"%s\"}",
                            "Error executing restart-docker with params" + params);
                    LOG.error("Failed to execute restart-docker with params: " + params, e);
                }
                break;
            case "manage-sergent":
                try {
                    result = execute("./sergent.sh", params);
                } catch (Exception e) {
                    result = String.format("{\"error\":\"%s\"}",
                            "Error executing manage-sergent with param: " + params);
                    LOG.error("Failed to execute manage-sergent with param: " + params, e);
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
