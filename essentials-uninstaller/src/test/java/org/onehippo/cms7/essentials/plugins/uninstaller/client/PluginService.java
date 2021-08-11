package org.onehippo.cms7.essentials.plugins.uninstaller.client;


import org.onehippo.cms7.essentials.sdk.api.model.rest.PluginDescriptor;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("plugins")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public interface PluginService {

    @GET
    @Path("/")
    public List<PluginDescriptor> getPlugins();
}