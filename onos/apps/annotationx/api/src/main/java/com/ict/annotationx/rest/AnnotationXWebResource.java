/*
 * Copyright 2019-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ict.annotationx.rest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ict.annotationx.intf.AnnotationXService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.onlab.util.Tools.readTreeFromStream;

@Path("annotationx")
public class AnnotationXWebResource extends AbstractWebResource {

    //FIX ME not found Multi status error code 207 in jaxrs Response Status.
    private static final int  MULTI_STATUS_RESPONE = 207;

    private AnnotationXService annotationExtendService = get(AnnotationXService.class);

    /**
     * Hello world.
     * REST API:
     * http://localhost:8181/onos/v1/annotationx/annotationx/hello
     *
     * @return Hello world
     */
    @GET
    @Path("hello")
    @Produces(MediaType.APPLICATION_JSON)
    public Response hello() {
        ObjectNode root = mapper().createObjectNode();
        root.put("Hello", "HK");
        return ok(root.toString()).build();
    }

    @POST
    @Path("devices")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response uploadDevices(InputStream request) throws IOException {
        ObjectNode root = readTreeFromStream(mapper(), request);
        // annotationExtendService process objectNode
        List<String> errorMsgs = annotationExtendService.updateDeviceAnnotations(root);
        if (!errorMsgs.isEmpty()) {
            return Response.status(MULTI_STATUS_RESPONE).entity(produceErrorJson(errorMsgs)).build();
        }
        return Response.ok().build();
    }

    @POST
    @Path("port")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response uploadPorts(InputStream request) throws IOException {
        ObjectNode root = readTreeFromStream(mapper(), request);

        return Response.ok().build();
    }

    private ObjectNode produceErrorJson(List<String> errorMsgs) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode().put("code", 207).putPOJO("message", errorMsgs);
        return result;
    }


}
