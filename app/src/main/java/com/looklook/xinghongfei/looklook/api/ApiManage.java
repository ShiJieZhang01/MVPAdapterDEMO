package com.looklook.xinghongfei.looklook.api;

import com.looklook.xinghongfei.looklook.MyApplication;
import com.looklook.xinghongfei.looklook.util.NetWorkUtil;

import java.io.File;
import java.io.IOException;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by xinghongfei on 16/8/12.
 */
public class ApiManage {

    //拦截器可以用来转换，重试，重写请求的机制
    //Application Interceptors  应用拦截器主要用于查看请求信息及返回信息，如链接地址、头信息、参数信息等
    //Network Interceptors  可以添加、删除或替换请求头信息，还可以改变的请求携带的实体
    //此拦截器的目的就是设置缓存
    private static final Interceptor REWRITE_CACHE_CONTROL_INTERCEPTOR = new Interceptor() {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Response originalResponse = chain.proceed(chain.request());
            if (NetWorkUtil.isNetWorkAvailable(MyApplication.getContext())) {
                int maxAge = 60; // 在线缓存在1分钟内可读取
                return originalResponse.newBuilder()
                        .removeHeader("Pragma")
                        .removeHeader("Cache-Control")
                        .header("Cache-Control", "public, max-age=" + maxAge)
                        .build();
            } else {
                int maxStale = 60 * 60 * 24 * 28; // 离线时缓存保存4周
                return originalResponse.newBuilder()
                        .removeHeader("Pragma")
                        .removeHeader("Cache-Control")
                        .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                        .build();
            }
        }
    };
    public static ApiManage apiManage;
    private static File httpCacheDirectory = new File(MyApplication.getContext().getCacheDir(), "zhihuCache");
    private static int cacheSize = 10 * 1024 * 1024; // 10 MiB
    private static Cache cache = new Cache(httpCacheDirectory, cacheSize);
    //设置缓存空间及大小，设置拦截器
    //缓存实现原理可以参考http://werb.github.io/2016/07/29/%E4%BD%BF%E7%94%A8Retrofit2+OkHttp3%E5%AE%9E%E7%8E%B0%E7%BC%93%E5%AD%98%E5%A4%84%E7%90%86/
    //以及http://www.tuicool.com/articles/u6ZvyeI
    private static OkHttpClient client = new OkHttpClient.Builder()
            .addNetworkInterceptor(REWRITE_CACHE_CONTROL_INTERCEPTOR)
            .addInterceptor(REWRITE_CACHE_CONTROL_INTERCEPTOR)
            .cache(cache)
            .build();
    public ZhihuApi zhihuApi;
    public TopNews topNews;
    public GankApi ganK;
    private Object zhihuMonitor = new Object();

    public static ApiManage getInstence() {
        if (apiManage == null) {
            synchronized (ApiManage.class) {
                if (apiManage == null) {
                    apiManage = new ApiManage();
                }
            }
        }
        return apiManage;
    }

    public ZhihuApi getZhihuApiService() {
        if (zhihuApi == null) {
            synchronized (zhihuMonitor) {
                if (zhihuApi == null) {
                    zhihuApi = new Retrofit.Builder()
                            .baseUrl("http://news-at.zhihu.com")
                            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build().create(ZhihuApi.class);
                }
            }
        }

        return zhihuApi;
    }

    public TopNews getTopNewsService() {
        if (topNews == null) {
            synchronized (zhihuMonitor) {
                if (topNews == null) {
                    topNews = new Retrofit.Builder()
                            .baseUrl("http://c.m.163.com")
                            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build().create(TopNews.class);
                }
            }
        }
        return topNews;
    }


    public GankApi getGankService(){
        if (ganK==null){
            synchronized (zhihuMonitor){
                if (ganK==null){
                    ganK=new Retrofit.Builder()
                            .baseUrl("http://gank.io")
                            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build().create(GankApi.class);
                }
            }
        }
        return ganK;
    }

}
