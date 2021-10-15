package org.opennms.plugins.zabbix;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("zabbix")
public interface WebhookHandler {

    @GET
    @Path("/ping")
    Response ping();

    @POST
    @Path("/hook")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    Response handleWebhook(String body);
}
