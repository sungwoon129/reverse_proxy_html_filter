package com.drminside.html_filter.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

@RestController
public class ProxyController {

    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> filter(HttpServletRequest request, @RequestBody(required = false) String body) throws IOException, URISyntaxException {
        String googleUrl = "http://www.google.com" + request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
        CloseableHttpClient httpClient = HttpClients.custom().build();
        HttpUriRequest proxyRequest;

        switch (request.getMethod()) {
            case "POST":
                HttpPost postRequest = new HttpPost(new URI(googleUrl));
                if (body != null) {
                    postRequest.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
                }
                proxyRequest = postRequest;
                break;
            case "PUT":
                HttpPut putRequest = new HttpPut(new URI(googleUrl));
                if (body != null) {
                    putRequest.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
                }
                proxyRequest = putRequest;
                break;
            case "DELETE":
                proxyRequest = new HttpDelete(new URI(googleUrl));
                break;
            default:
                proxyRequest = new HttpGet(new URI(googleUrl));
        }

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!headerName.equalsIgnoreCase("host") && !headerName.equalsIgnoreCase("accept-encoding") ) {
                proxyRequest.setHeader(headerName, request.getHeader(headerName));
            }
        }

        HttpResponse proxyResponse = httpClient.execute(proxyRequest);
        HttpEntity entity = proxyResponse.getEntity();
        HttpHeaders headers = new HttpHeaders();

        for (Header header : proxyResponse.getAllHeaders()) {
            headers.add(header.getName(), header.getValue());
        }

        byte[] responseBody = null;
        if(entity != null) {
            responseBody = EntityUtils.toByteArray(entity);
        }

        String contentType = headers.getContentType() != null ? headers.getContentType().toString() : "";

        if (contentType.startsWith("image/") || contentType.equals("application/octet-stream")) {
            return handleBinaryResponse(responseBody, headers, proxyResponse.getStatusLine().getStatusCode());
        } else {
            return handleHtmlResponse(responseBody, headers, proxyResponse.getStatusLine().getStatusCode());
        }
    }

    private ResponseEntity<byte[]> handleBinaryResponse(byte[] responseBody, HttpHeaders headers, int statusCode) {
        return new ResponseEntity<>(responseBody, headers, HttpStatus.valueOf(statusCode));
    }

    private ResponseEntity<String> handleHtmlResponse(byte[] responseBody, HttpHeaders headers, int statusCode) {
        String responseBodyStr = responseBody == null ? null : new String(responseBody, StandardCharsets.UTF_8);

        if(responseBodyStr != null) {
            responseBodyStr = responseBodyStr.replaceAll("<img class=\"jfN4p\"[^>]*>",
                    "<img id=\"dimg_YZ9pZqjaCIvJ1e8PqdP9wAM_29\" " +
                            "src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAC0AAAA4CAMAAABe34GAAAAAZlBMVEUAAAD////u7u6Dg4OTk5NkZGSsrKzg4OD19fWLi4vZ2dkVFRUyMjL6+vppaWnm5uZXV1d5eXnS0tIqKipeXl47Ozu2trafn5/BwcEiIiIaGhrLy8uZmZlSUlKlpaVvb28LCwtFRUUINVvnAAABgUlEQVRIiZXW63qCMAwG4EDlaDkqiEPB3f9NjtJGixz6Lf/o3ofFpG0g7zB60dysRzqy3Z2ITqAOSEUI6UzOmGpINxovwK5ODK4QXRhMEaLzrVfv6Zg2st7VrcEFpPVvPH/hPT2nnayWd7T4KYd4vWzrLO3rr/+dtkPYRWsdP0ynRcpLXcCVqdKl5tapkOGURGSvEAXxRxdnWsb38xQp63T9t42otfYhrFql9AXDctYnDAdz3rEbqrjqmmCvvpt6bxRrI3R7KMLzmHQI6d7oCtK8q0oEP1lLRAvWL0TzKUKs9W6o3Dnrxm2JLqyvUCr+v+ptBgQNkDbtoR7TuoYEbm+9U8iDmkl85hM31NEqXaOaAp/Q5quQk37AeiD88qGpft5nIrkin++qwg3niPStOUI4MHcs1k+f72/kmrh9ZsPTiaU1Sdy5FPbccfW/XUwp7/hUiOVMW176cqxG+3jzl4Q1Xbkwl1BP4Szkrf/+bLNnsZ80r19hf0fVSSnLk/9+/gNKNAxPI/SsfAAAAABJRU5ErkJggg==\"" +
                            " class=\"YQ4gaf zr758c wA1Bge\" height=\"56\" width=\"45\" alt=\"\" data-csiid=\"13\" data-atf=\"1\">");
        }

        return new ResponseEntity<>(responseBodyStr, headers, HttpStatus.valueOf(statusCode));
    }
}
