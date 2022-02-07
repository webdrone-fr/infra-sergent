package net.manaty.sergent;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

@Path("/sergent")
public class AgentResource {
    private static final Logger LOG = Logger.getLogger(AgentResource.class);
    private static final List<String> COMMANDS = List.of(
            "list",
            "dockerpull",
            "gitpull",
            "deploy-kc-theme");

    @Inject
    AgentService service;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String sergent(
            @QueryParam("command") @DefaultValue("list") String command,
            @QueryParam("params") String params,
            @HeaderParam(value = "X-delay-in-sec") @DefaultValue("10") long timeoutSec) {
        String result = null;
        service.setWorkingPathName(System.getenv("SERGENT_COMMAND_PATH"));
        service.setTimeoutMillis(timeoutSec * 1000);
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
                    String scriptCommand = new StringBuilder("./deploy-kc-theme.sh")
                            .append(parseParameters(params))
                            .toString();
                    LOG.debug("scriptCommand: " + scriptCommand);
                    result = execute(scriptCommand);
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

    private String parseParameters(String params) throws Exception {
        Map<String, String> parameterMap =
                new ObjectMapper().readValue(params, new TypeReference<Map<String, String>>() {});
        LOG.debug("parameterMap: " + parameterMap);
        String parameters = parameterMap.entrySet()
                .stream()
                .reduce(
                        new StringBuilder(),
                        (parameter, entry) -> parameter.append(" -")
                                .append(entry.getKey())
                                .append("=")
                                .append(entry.getValue()),
                        (previousParameter, nextParameter) -> previousParameter
                                .append(nextParameter))
                .toString();
        LOG.debug("parameters: " + parameters);
        return parameters;
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
}
