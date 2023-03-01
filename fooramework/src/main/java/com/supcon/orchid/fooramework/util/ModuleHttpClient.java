package com.supcon.orchid.fooramework.util;

import com.supcon.orchid.fooramework.support.ModuleRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 跨模块接口调用
 */
@SuppressWarnings("unchecked")
public class ModuleHttpClient {

    private static LoadBalancerClient loadBalancerClient;

    private static LoadBalancerClient loadBalancerClient(){
        return loadBalancerClient!=null?loadBalancerClient:(loadBalancerClient= Springs.getBean(LoadBalancerClient.class));
    }

    private static final Logger log = LoggerFactory.getLogger(ModuleHttpClient.class);

    /**
     * 跨模块接口调用
     * @param request_context 请求上下文
     * @param type_reference 返回类型参照
     * @return 接口返回
     */
    public static <T> T exchange(ModuleRequestContext request_context, ParameterizedTypeReference<T> type_reference){
        return (T) _Exchange(request_context,type_reference);
    }


    /**
     * 跨模块接口调用
     * @param request_context 请求上下文
     * @param target_class 返回类型
     * @return 接口返回
     */
    public static <T> T exchange(ModuleRequestContext request_context, Class<T> target_class){
        return (T) _Exchange(request_context,target_class);
    }


    private static RestTemplate _GetRestTemplate(ModuleRequestContext request_context){
        HttpComponentsClientHttpRequestFactory httpRequestFactory = null;
        if(request_context.getConnectRequestTimeout()!=null){
            httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
            httpRequestFactory.setConnectionRequestTimeout(request_context.getConnectRequestTimeout());
        }
        if(request_context.getConnectTimeout()!=null){
            if(httpRequestFactory==null){
                httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
            }
            httpRequestFactory.setConnectTimeout(request_context.getConnectTimeout());
        }
        if(request_context.getReadTimeout()!=null){
            if(httpRequestFactory==null){
                httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
            }
            httpRequestFactory.setReadTimeout(request_context.getReadTimeout());
        }
        return httpRequestFactory!=null?
                new RestTemplate(httpRequestFactory):Springs.getBean(RestTemplate.class);
    }

    private static Object _Exchange(ModuleRequestContext context, Object whatever){
        HttpServletRequest request = Https.getRequest();
        String pathWithParams = context.getUrlParams()==null?
                context.getPath():context.getPath()+"?"+context.getUrlParams().entrySet().stream().map(entry->entry.getKey()+"="+entry.getValue()).collect(Collectors.joining("&"));
        String url;
        if(request!=null&&context.isGateway()){
            url = String.format("http://%s:%s",request.getServerName(),request.getServerPort())+"/msService"+pathWithParams;
        } else {
            ServiceInstance serviceInstance = loadBalancerClient().choose(context.getModuleName());
            url = String.format("http://%s:%s",serviceInstance.getHost(),serviceInstance.getPort())+pathWithParams;
        }
        HttpEntity<?> requestEntity = new HttpEntity<>(context.getBody(), Optional.ofNullable(context.getHttpHeaders()).orElseGet(()->{
            HttpHeaders httpHeaders = ModuleRequestContext.getDefaultHeader();
            if(request!=null){
                //从请求中拷贝头信息
                Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    String value = request.getHeader(name);
                    httpHeaders.set(name, value);
                }
            }
            return httpHeaders;
        }));
        if(context.isLog()){
            log.info("向{}服务{}接口发送请求：{}",new Object[]{
                    context.getModuleName(),url, Jacksons.writeValue(context.getBody())
            });
        }
        ResponseEntity<?> result;
        if(whatever instanceof ParameterizedTypeReference){
            result = _GetRestTemplate(context).exchange(url,context.getMethod(),requestEntity,((ParameterizedTypeReference<?>)whatever));
        } else if(whatever instanceof Class){
            result = _GetRestTemplate(context).exchange(url,context.getMethod(),requestEntity,((Class<?>)whatever));
        } else {
            throw new IllegalArgumentException("exchange type mismatch");
        }
        if(context.isLog()){
            log.info("{}服务返回：{}",new Object[]{
                    context.getModuleName(),Jacksons.writeValue(result)
            });
        }
        return result.getBody();
    }


}

