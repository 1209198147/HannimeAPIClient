package com.shikou.config;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

/**
 * Hanime客户端全局配置
 */
@Data
@Builder
public class HanimeConfig {

    /** 默认Base URL */
    public static final String DEFAULT_BASE_URL = "https://hanime1.me/";

    /** 默认Base URL HK */
    public static final String DEFAULT_BASE_URL_HK = "https://hanimeone.me/";

    /** 默认User-Agent */
    public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /** 默认连接超时（秒） */
    public static final int DEFAULT_CONNECT_TIMEOUT = 60;

    /** 默认读取超时（秒） */
    public static final int DEFAULT_READ_TIMEOUT = 60;

    /** 默认写入超时（秒） */
    public static final int DEFAULT_WRITE_TIMEOUT = 60;

    /** 下载连接超时（秒） */
    public static final int DOWNLOAD_CONNECT_TIMEOUT = 120;

    /** 下载读取超时（秒） */
    public static final int DOWNLOAD_READ_TIMEOUT = 120;

    /** 下载写入超时（秒） */
    public static final int DOWNLOAD_WRITE_TIMEOUT = 120;

    /** 下载缓冲区大小（字节） */
    public static final int DOWNLOAD_BUFFER_SIZE = 8192;

    /** 下载分段大小（10MB） */
    public static final int DOWNLOAD_CHUNK_SIZE = 10 * 1024 * 1024;

    /** 自定义Base URL */
    @Builder.Default
    private final String baseUrl = DEFAULT_BASE_URL;

    /** 自定义User-Agent */
    @Builder.Default
    private final String userAgent = DEFAULT_USER_AGENT;

    /** 连接超时（秒） */
    @Builder.Default
    private final int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

    /** 读取超时（秒） */
    @Builder.Default
    private final int readTimeout = DEFAULT_READ_TIMEOUT;

    /** 写入超时（秒） */
    @Builder.Default
    private final int writeTimeout = DEFAULT_WRITE_TIMEOUT;

    /**
     * 创建默认配置
     */
    public static HanimeConfig defaultConfig() {
        return HanimeConfig.builder().build();
    }

    /**
     * 创建默认配置（香港）
     */
    public static HanimeConfig defaultConfigHK() {
        return HanimeConfig.builder().baseUrl(DEFAULT_BASE_URL_HK).build();
    }

    /**
     * 用户语言偏好
     * zht: 繁体中文
     * zhs: 简体中文
     */
    @Builder.Default
    private final String userLang = "zhs";
}
