package com.shikou.client;

import com.shikou.config.HanimeConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeUnit;

/**
 * 视频下载器 - 支持断点续传和进度回调
 */
public class VideoDownloader {

    private final OkHttpClient client;
    private final HanimeConfig config;

    public VideoDownloader(OkHttpClient client, HanimeConfig config) {
        this.config = config;
        this.client = client.newBuilder()
                .connectTimeout(HanimeConfig.DOWNLOAD_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(HanimeConfig.DOWNLOAD_READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(HanimeConfig.DOWNLOAD_WRITE_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 进度监听器
     */
    public interface ProgressListener {
        void onProgress(long downloaded, long total);
    }

    /**
     * 下载文件（无进度回调）
     */
    public void download(String url, File outputFile) throws IOException {
        download(url, outputFile, null);
    }

    /**
     * 下载文件（带进度回调）
     */
    public void download(String url, File outputFile, ProgressListener listener) throws IOException {
        long contentLength = getContentLength(url);
        if (contentLength <= 0) {
            simpleDownload(url, outputFile, listener);
            return;
        }

        long downloadedBytes = 0;
        if (outputFile.exists()) {
            downloadedBytes = outputFile.length();
            if (downloadedBytes >= contentLength) {
                if (listener != null) {
                    listener.onProgress(contentLength, contentLength);
                }
                return;
            }
        }

        File parentFile = outputFile.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }

        RandomAccessFile randomAccessFile = new RandomAccessFile(outputFile, "rw");
        randomAccessFile.setLength(contentLength);
        randomAccessFile.seek(downloadedBytes);

        try {
            while (downloadedBytes < contentLength) {
                long rangeStart = downloadedBytes;
                long rangeEnd = Math.min(downloadedBytes + HanimeConfig.DOWNLOAD_CHUNK_SIZE - 1, contentLength - 1);

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", config.getUserAgent())
                        .addHeader("Referer", config.getBaseUrl())
                        .addHeader("Range", "bytes=" + rangeStart + "-" + rangeEnd)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.body() == null) {
                        throw new IOException("响应体为空");
                    }

                    try (InputStream inputStream = response.body().byteStream()) {
                        byte[] buffer = new byte[HanimeConfig.DOWNLOAD_BUFFER_SIZE];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            randomAccessFile.write(buffer, 0, bytesRead);
                            downloadedBytes += bytesRead;
                            if (listener != null) {
                                listener.onProgress(downloadedBytes, contentLength);
                            }
                        }
                    }
                }
            }
        } finally {
            randomAccessFile.close();
        }
    }

    private void simpleDownload(String url, File outputFile, ProgressListener listener) throws IOException {
        File parentFile = outputFile.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("Referer", config.getBaseUrl())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("下载失败: " + response.code());
            }

            long contentLength = response.body().contentLength();
            long downloadedBytes = 0;

            try (InputStream inputStream = response.body().byteStream();
                 FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[HanimeConfig.DOWNLOAD_BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    downloadedBytes += bytesRead;
                    if (listener != null && contentLength > 0) {
                        listener.onProgress(downloadedBytes, contentLength);
                    }
                }
            }
        }
    }

    private long getContentLength(String url) throws IOException {
        Request headRequest = new Request.Builder()
                .url(url)
                .head()
                .addHeader("User-Agent", config.getUserAgent())
                .addHeader("Referer", config.getBaseUrl())
                .build();

        try (Response headResponse = client.newCall(headRequest).execute()) {
            String contentLengthStr = headResponse.header("Content-Length");
            if (contentLengthStr != null) {
                return Long.parseLong(contentLengthStr);
            }
            String contentRange = headResponse.header("Content-Range");
            if (contentRange != null) {
                String[] parts = contentRange.split("/");
                if (parts.length == 2) {
                    return Long.parseLong(parts[1]);
                }
            }
            return -1;
        }
    }
}
