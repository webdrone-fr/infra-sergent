package net.manaty.sergent;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/sergent")
public class AgentResource {
    private static final List<String> COMMANDS = List
            .of("list", "dockerpull", "gitpull", "deploy-kc-theme");

    @Inject
    AgentService service;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String sergent(
            @QueryParam("command") @DefaultValue("list") String command,
            @QueryParam("param") List<String> paramList,
            @HeaderParam(value = "X-delay-in-sec") @DefaultValue("10") long timeoutSec) {
        String result = null;
        service.setWorkingPathName(System.getenv("SERGENT_COMMAND_PATH"));
        service.setTimeoutMillis(timeoutSec * 1000);

        switch (command) {
            case "dockerpull":
                result = execute("./dockerpull.sh");
                break;
            case "gitpull":
                result = execute("./gitpull.sh");
                break;
            case "deploy-kc-theme":
                // sample request
                // https://username:password@mydomain/meveo/api/rest/admin/files/downloadFile?file=git/keycloak-themes/themeName.zip
                // must be url-encoded e.g.
                // https%3A%2F%2Fusername%3Apassword%40mydomain%2Fmeveo%2Fapi%2Frest%2Fadmin%2Ffiles%2FdownloadFile%3Ffile%3Dgit%2Fkeycloak-themes%2FthemeName.zip

                String encodedUrl = paramList.get(0);
                try {
                    String themeUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.name());
                    result = execute("./deploy-kc-theme.sh -theme " + themeUrl);
                } catch (Exception e) {
                    result = String.format("{\"error\":\"%s\"}", "Error decoding: " + encodedUrl);
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
}
