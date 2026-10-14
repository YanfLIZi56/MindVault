package com.yanglizi.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp静态工具类，不依赖Spring Bean，内置连接池，静态单例
 */
@Slf4j
public final class OkHttpUtil {

    // 静态全局唯一OkHttpClient，自带连接池，只初始化1次
    private static final OkHttpClient HTTP_CLIENT;
    private static final ObjectMapper OBJECT_MAPPER;
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json;charset=utf-8");

    static {
        HTTP_CLIENT = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                // 连接池：最大空闲64，空闲30s回收
                .connectionPool(new ConnectionPool(64, 30, TimeUnit.SECONDS))
                .followRedirects(false)
                .build();

        OBJECT_MAPPER = new ObjectMapper();
    }

    private OkHttpUtil() {
    }

    // --------------------------- GET 请求 ---------------------------

    /**
     * get 请求，返回字符串
     */
    public static String get(String url) throws IOException {
        return get(url, null);
    }

    /**
     * get 请求，带header，返回字符串
     */
    public static String get(String url, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url).get();
        fillHeader(builder, headers);
        Request request = builder.build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful()) {
                throw new IOException("Http请求失败,code:" + response.code() + ",url:" + url);
            }
            return body != null ? body.string() : null;
        }
    }

    /**
     * get 请求，自动反序列化为对象
     * @param url 地址
     * @param headers 请求头，可以null
     * @param clazz 返回对象class
     * @return T
     */
    public static <T> T get(String url, Map<String, String> headers, Class<T> clazz) throws IOException {
        String respStr = get(url, headers);
        if (respStr == null || respStr.isBlank()) {
            return null;
        }
        return OBJECT_MAPPER.readValue(respStr, clazz);
    }

    // --------------------------- POST JSON请求 ---------------------------

    /**
     * post json，返回字符串
     * @param url url
     * @param body 入参对象（会序列化为json）
     * @param headers header，允许null
     */
    public static String postJson(String url, Object body, Map<String, String> headers) throws IOException {
        String json = OBJECT_MAPPER.writeValueAsString(body);
        RequestBody requestBody = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request.Builder builder = new Request.Builder().url(url).post(requestBody);
        fillHeader(builder, headers);
        Request request = builder.build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            ResponseBody respBody = response.body();
            if (!response.isSuccessful()) {
                throw new IOException("Http POST失败,code:" + response.code() + ",url:" + url);
            }
            return respBody != null ? respBody.string() : null;
        }
    }

    /**
     * post json，自动解析返回为目标对象
     * @param url 地址
     * @param body 请求body对象
     * @param headers header map，可null
     * @param clazz 返回对象Class
     * @return T
     */
    public static <T> T postJson(String url, Object body, Map<String, String> headers, Class<T> clazz) throws IOException {
        String respStr = postJson(url, body, headers);
        if (respStr == null || respStr.isBlank()) {
            return null;
        }
        return OBJECT_MAPPER.readValue(respStr, clazz);
    }

    /**
     * 支持泛型集合返回，例如 List<User>
     * <pre>
     * List<User> list = OkHttpUtil.postJson(url,reqBody,null,new TypeReference<List<User>>(){});
     * </pre>
     */
    public static <T> T postJson(String url, Object body, Map<String, String> headers, TypeReference<T> typeReference) throws IOException {
        String respStr = postJson(url, body, headers);
        if (respStr == null || respStr.isBlank()) {
            return null;
        }
        return OBJECT_MAPPER.readValue(respStr, typeReference);
    }

    /**
     * 填充header
     */
    private static void fillHeader(Request.Builder builder, Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(builder::addHeader);
        }
    }

    // -------------------------- 安全不抛异常版本（业务常用） --------------------------

    /**
     * postJson 安全版，不抛异常，出错返回null，自行打日志
     */
    public static <T> T postJsonSafe(String url, Object body, Map<String, String> headers, Class<T> clazz) {
        try {
            return postJson(url, body, headers, clazz);
        } catch (Exception e) {
             log.error("okhttp post调用异常 url={}", url, e);
            return null;
        }
    }

    public static String postJsonSafe(String url, Object body, Map<String, String> headers) {
        try {
            return postJson(url, body, headers);
        } catch (Exception e) {
            // log.error("okhttp post调用异常 url={}", url, e);
            return null;
        }
    }
}
