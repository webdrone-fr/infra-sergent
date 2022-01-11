package net.manaty.sergent;

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
    
    @Inject
    AgentService service;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String sergent(@QueryParam("command") @DefaultValue("list") String command,@HeaderParam(value="X-delay-in-sec") @DefaultValue("10") long timeoutSec) {
        String result="{\"commands\":[\"list\",\"dockerpull\",\"gitpull\"]}";
        service.setWorkingPathName(System.getenv("SERGENT_COMMAND_PATH"));
        service.setTimeoutMillis(timeoutSec*1000);
        switch (command) {
            case "dockerpull" :
                service.setCommand("./dockerpull.sh");
                service.execute(null);
                if(service.getError()==null){
                    result = "{\"output\":\""+service.getOutput()+"\"}";
                } else {
                    result = "{\"error\":\""+service.getError()+"\"}";
                }
                break;
            case "gitpull" :
                service.setCommand("./gitpull.sh");
                service.execute(null);
                if(service.getError()==null){
                    result = "{\"output\":\""+service.getOutput()+"\"}";
                } else {
                    result = "{\"error\":\""+service.getError()+"\"}";
                }
                break;
        }
        // example of command "deploy-kc-theme https://username:password@mydomain/meveo/git/mytheme"
        /*if(command.startsWith("deploy-kc-theme")){
            String kcThemeRepoUrl = command.substring("deploy-kc-theme".length()+1);
            service.setCommand("./deploy-kc-theme.sh -giteRepo "+kcThemeRepoUrl);
        }*/
        return result;
    }
}
