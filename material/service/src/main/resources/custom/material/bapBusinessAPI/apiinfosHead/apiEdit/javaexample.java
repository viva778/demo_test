//java调用示例
public String sendPostNormal(String url, Map<String, String> params) {
    byte[] responseBody = null;
    String responseStr = null;
    HttpClient client = new HttpClient();
    PostMethod post = new PostMethod(url);

    NameValuePair[] formParams = new NameValuePair[params.keySet().size()];
    int i = 0;
    for (String key: params.keySet()) {
        if (params.get(key) != null) {
            formParams[i++] = new NameValuePair(key, 
                params.get(key).toString());
        } else {
            formParams[i++] = new NameValuePair(key, null);
        }
    }
    try {
        post.getParams().setParameter(HttpMethodParams
            .HTTP_CONTENT_CHARSET, "utf-8");
        post.setRequestBody(formParams);
        post.releaseConnection();
        client.executeMethod(post);
        responseBody = post.getResponseBody();
        responseStr = new String(responseBody, "UTF-8");
    } catch (Exception e) {
        e.printStackTrace();
    }
    return responseStr;
}
