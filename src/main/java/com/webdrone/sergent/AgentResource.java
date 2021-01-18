package com.webdrone.sergent;

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
        String result="{\"commands\":[\"list\",\"redeploy\",\"test\"]}";
        service.setWorkingPathName(System.getenv("SERGENT_COMMAND_PATH"));
        service.setTimeoutMillis(timeoutSec*1000);
        switch (command) {
            case "redeploy" :
                service.setCommand("./dockerpull.sh");
                service.execute(null);
                if(service.getError()==null){
                    result = "{\"output\":\""+service.getOutput()+"\"}";
                } else {
                    result = "{\"error\":\""+service.getError()+"\"}";
                }
                break;
            case "test" :
                service.setCommand("./test.sh");
                service.execute(null);
                if(service.getError()==null){
                    result = "{\"output\":\""+service.getOutput()+"\"}";
                } else {
                    result = "{\"error\":\""+service.getError()+"\"}";
                }
                break;
        }
        return result;
    }
}
