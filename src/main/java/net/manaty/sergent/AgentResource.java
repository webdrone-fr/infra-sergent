package net.manaty.sergent;

import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
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
            case "update-modules":
                String resultGitpull = execute(".gitpull.sh");
                if (resultGitpull.contains("output")){
                    try {
                        LOG.debug("params: " + params);
                        String stackName = System.getenv("STACK_NAME");
                        // this should be async => needs new execute method
                        // callback to be done by meveo target
                        // need to add wait time for meveo to confirm meveo is up
                        // could have initial response as acknowledgement of reception of command
                        result = execute("docker exec -it "+ stackName + "-meveo curl -X POST localhost:8080/meveo/api/rest/module/initDefault -d params=" + params);
                    } catch (Exception e) {
                        result = String.format("{\"error\":\"%s\"}",
                                "Error updating modules: " + params);
                        LOG.error("Failed to update modules: " + params, e);
                    }
                } else {
                    result = String.format("{\"error\":\"%s\"}",
                            "Error executing gitpull for update-modules" + params);
                    LOG.error("Failed to execute gitpull for update-modules");
                }
                break;
            default:
                result = String.format("{\"commands\":[%s]}", String.join(",", COMMANDS.stream()
                        .map(cmd -> String.format("\"%s\"", cmd)).toArray(String[]::new)));
        }
        return result;
    }

    private String execute(String command) {
        String result = null;
        service.setCommand(command);
        service.execute(null);
        if (service.getError() == null) {
            result = String.format("{\"output\":\"%s\"}", service.getOutput());
        } else {
            result = String.format("{\"error\":\"%s\"}", service.getError());
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
            result = String.format("{\"error\":\"%s\"}", service.getError());
        }
        return result;
    }
}
