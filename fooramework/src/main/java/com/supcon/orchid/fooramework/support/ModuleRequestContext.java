package com.supcon.orchid.fooramework.support;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Map;

@Data
@Builder
public class ModuleRequestContext {
    private boolean log;

    private String moduleName;

    private HttpMethod method;

    private String path;

    private Map<String,String> urlParams;

    private HttpHeaders httpHeaders;

    private Object body;

    private Integer connectRequestTimeout;

    private Integer connectTimeout;

    private Integer readTimeout;

    private boolean gateway;

    public static HttpHeaders getDefaultHeader(){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
