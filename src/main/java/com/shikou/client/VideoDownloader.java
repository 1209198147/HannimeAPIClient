package com.shikou.client;

import com.shikou.config.HanimeConfig;
import com.shikou.exception.*;
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
    public void download(String url, File outputFile) throws HanimeApiException, HanimeNetworkException, IOException {
        download(url, outputFile, null);
    }

    /**
     * 下载文件（支持断点续传，边写边增长）
     * @param url         下载URL
     * @param outputFile  本地输出文件
     * @param listener    进度监听器（可为null）
     * @throws HanimeApiException 业务异常
     * @throws HanimeNetworkException 网络异常
     * @throws IOException     网络或IO异常
     */
    public void download(String url, File outputFile, ProgressListener listener)
            throws HanimeApiException, HanimeNetworkException, IOException {

        // 1. 获取服务器文件总大小
        long contentLength = getContentLength(url);
        if (contentLength <= 0) {
            // 服务器未返回Content-Length，降级为简单下载（不支持断点续传）
            simpleDownload(url, outputFile, listener);
            return;
        }

        // 2. 检查本地已下载字节数（断点续传）
        long downloadedBytes = 0;
        boolean fileExists = outputFile.exists();

        if (fileExists) {
            downloadedBytes = outputFile.length();
            if (downloadedBytes >= contentLength) {
                // 文件已完整，直接回调100%后返回
                if (listener != null) {
                    listener.onProgress(contentLength, contentLength);
                }
                return;
            }
            // 处理服务器文件变小的情况（例如资源被替换为更小的文件）
            if (downloadedBytes > contentLength) {
                // 删除旧文件，从头下载
                outputFile.delete();
                downloadedBytes = 0;
                fileExists = false;
            }
        }

        // 3. 创建父目录
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 4. 构建 Range 请求（从断点处开始）
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Range", "bytes=" + downloadedBytes + "-")
                .build();

        // 5. 执行请求并处理响应
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Download failed, response code: " + response.code());
            }

            int statusCode = response.code();
            if (statusCode == 200) {
                // 服务器不支持 Range 请求（忽略了我发的 Range 头），降级为完全覆盖下载
                // 注意：需要关闭可能已存在的文件，重新从头下载
                if (fileExists) {
                    outputFile.delete();
                }
                simpleDownload(url, outputFile, listener);
                return;
            } else if (statusCode != 206) {
                throw new IOException("Unsupported response code for range request: " + statusCode);
            }

            // 6. 206 Partial Content - 开始写入数据
            try (RandomAccessFile raf = new RandomAccessFile(outputFile, "rw");
                 InputStream is = response.body().byteStream()) {

                // 移动到断点位置（如果文件不存在则位置为0）
                raf.seek(downloadedBytes);
                // 不需要 setLength，文件会在写入时自动增长

                byte[] buffer = new byte[HanimeConfig.DOWNLOAD_BUFFER_SIZE];
                int bytesRead;
                long lastCallbackBytes = downloadedBytes;
                final long PROGRESS_CALLBACK_THRESHOLD = 1024 * 1024; // 每1MB回调一次

                while ((bytesRead = is.read(buffer)) != -1) {
                    raf.write(buffer, 0, bytesRead);
                    downloadedBytes += bytesRead;

                    // 进度回调（限流：每1MB或文件最后一块）
                    if (listener != null) {
                        if ((downloadedBytes - lastCallbackBytes) >= PROGRESS_CALLBACK_THRESHOLD
                                || downloadedBytes == contentLength) {
                            listener.onProgress(downloadedBytes, contentLength);
                            lastCallbackBytes = downloadedBytes;
                        }
                    }
                }

                // 确保最终100%回调（如果最后一块不足阈值）
                if (listener != null && downloadedBytes != lastCallbackBytes) {
                    listener.onProgress(downloadedBytes, contentLength);
                }
            }
        }
    }

    private void simpleDownload(String url, File outputFile, ProgressListener listener) throws HanimeApiException, HanimeNetworkException, IOException {
        File parentFile = outputFile.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new HanimeApiException("视频不存在或链接失效，可以尝试通过videoCode下载");
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
