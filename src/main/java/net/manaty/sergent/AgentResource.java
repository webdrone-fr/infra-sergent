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
            "deploy-kc-theme",
            "update-modules");

    @Inject
    AgentService service;

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
            case "deploy-kc-theme":
                try {
                    LOG.debug("params: " + params);
                    result = execute("./deploy-kc-theme.sh", params);
                } catch (Exception e) {
                    result = String.format("{\"error\":\"%s\"}",
                            "Error deploying theme: " + params);
                    LOG.error("Failed to deploy theme: " + params, e);
                }
                break;
            default:
                result = String.format("{\"commands\":[%s]}", String.join(",", COMMANDS.stream()
                        .map(cmd -> String.format("\"%s\"", cmd)).toArray(String[]::new)));
        }
        return result;
    }

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
                        LOG.debug("params: " + params);
                        String stackNameOutput = execute("./getstackname.sh");
                        String usernameOutput = execute("./getusername.sh");
                        String passOutput = execute("./getpass.sh");
                        String outputStr= "output\":\"";
                        String stackName = stackNameOutput.substring(stackNameOutput.indexOf(outputStr)+outputStr.length(),stackNameOutput.indexOf("\"}")).stripTrailing();
                        String username = usernameOutput.substring(usernameOutput.indexOf(outputStr)+outputStr.length(),usernameOutput.indexOf("\"}")).stripTrailing();
                        String password = passOutput.substring(passOutput.indexOf(outputStr)+outputStr.length(),passOutput.indexOf("\"}")).stripTrailing();
                        LOG.debug("Stack Name : " + stackName);
                        params = params.replaceAll("\\s", "");
                        result = execute("docker", ("exec -t "+ stackName +"-meveo curl -X POST 'http://localhost:8080/meveo/api/rest/module/initDefault' -u " + username + ":" + password + " -H 'Content-Type:application/json' --max-time "+ timeoutSec +" -d '" + params + "'").split("\\s+"));
                        LOG.debug("Result: "+ result);
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
        }
        return result;
    }

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

    private String execute(String command, String[] params) {
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
