/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.samples.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;

import com.microsoft.azure.samples.controllers.CityService;
import com.microsoft.azure.samples.controllers.CountryService;
import com.microsoft.azure.samples.entities.City;
import com.microsoft.azure.samples.entities.Country;

@Path("/")
public class WorldServiceEndpoint {
    @Inject
    CountryService countrySvc;

    @Inject
    CityService citySvc;

    @Context
    HttpHeaders headers;
    @Context Request request;

    @GET
    @Path(value = "/area")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllContinents() {

        String fileDir = "/share1";

        StringBuilder fileContents = new StringBuilder();
        fileContents.append("Headers: ");
        fileContents.append(System.lineSeparator());
        fileContents.append(headers.getRequestHeaders());
        fileContents.append(System.lineSeparator());
        fileContents.append("Method: ");
        fileContents.append(System.lineSeparator());
        fileContents.append(request.getMethod());

        byte[] fileBytes = fileContents.toString().getBytes(StandardCharsets.UTF_8);
        InputStream fileData = new ByteArrayInputStream(fileBytes);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HHmmss");
        String formattedTime = LocalDateTime.now().format(formatter);

        StringBuilder fileName = new StringBuilder();
        fileName.append("HeadersMethodNewVersion");
        fileName.append(formattedTime);
        fileName.append(".txt");

        java.nio.file.Path filePath = java.nio.file.Path.of(fileDir, fileName.toString());

        try {
            Files.copy(fileData,filePath);
            fileData.close();
        } catch(IOException exception) {
            System.out.println(exception.getMessage());
        }
        List<String> continents = countrySvc.findAllContinents();
        return Response.ok(continents, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/area/{continent}")
    public Response getContinent(@PathParam("continent") String continent) {
        List<Country> country = countrySvc.findItemByContinent(continent);
        GenericEntity<List<Country>> genericEntity = new GenericEntity<List<Country>>(country) {
        };
        return Response.ok(genericEntity, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/countries/{countrycode}")
    public Response getCountry(@PathParam("countrycode") String countrycode) {
        List<City> city = citySvc.findOver1MillPopulation(countrycode);
        GenericEntity<List<City>> genericEntity = new GenericEntity<List<City>>(city) {
        };
        return Response.ok(genericEntity, MediaType.APPLICATION_JSON).build();
    }
}