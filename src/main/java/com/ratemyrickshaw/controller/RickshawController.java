package com.ratemyrickshaw.controller;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.ratemyrickshaw.lambda.RickshawAnalysisHandler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;


@Path("/analyze")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@RequiredArgsConstructor
public class RickshawController {

    private final RickshawAnalysisHandler lambda;

    @POST
    public Response analyze(String body) {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setBody(body);

        APIGatewayProxyResponseEvent result = lambda.handleRequest(event, null);

        return Response.status(result.getStatusCode())
                .entity(result.getBody())
                .build();
    }
}
