# HanimeAPIClient

一个用于 [Hanime1](https://hanime1.me/) 的 Java HTTP 客户端库，通过解析网站 HTML 提供结构化的数据访问和视频下载能力。

## 功能特性

- **首页浏览** — 获取轮播 Banner、分区视频列表
- **视频搜索** — 支持关键词、分类、标签、排序、时长、分页等高级筛选
- **影片详情** — 解析标题、简介、标签、作者信息、多分辨率视频源
- **视频下载** — 支持多画质选择、断点续传和下载进度回调


### 基本用法

```java
import com.shikou.client.*;
import com.shikou.model.entities.*;
import com.shikou.model.entities.pages.HomePage;

public class Demo {
    public static void main(String[] args) throws Exception {
        // 创建客户端
        HanimeApiClient client = new HanimeApiClient();

        // 获取首页
        HomePage home = client.getHomePage();
        home.getSections().forEach(section -> {
            System.out.println("分区: " + section.getTitle());
            section.getVideoInfoList().forEach(v ->
                    System.out.println("  - " + v.getTitle())
            );
        });

        // 搜索影片
        SearchParams params = SearchParams.builder()
                .query("关键词")
                .genre("3D")
                .sort("uploaded_at")
                .page(1)
                .build();
        client.search(params).forEach(v ->
                System.out.println(v.getTitle() + " | " + v.getViews())
        );

        // 获取影片详情
        HanimeVideo video = client.getVideoDetail("403047");
        System.out.println("标题: " + video.getTitle());
        System.out.println("作者: " + video.getArtist().getName());
        System.out.println("画质: " + video.getVideoUrls().keySet());
    }
}
```

### 视频下载

```java
HanimeApiClient client = new HanimeApiClient();

// 默认最高画质下载
client.download("403047", new File("video.mp4"));

// 指定画质下载
client.download("403047", "1080P", new File("video.mp4"));

// 带进度回调的下载
client.download("403047", new File("video.mp4"), (downloaded, total) -> {
    double percent = downloaded * 100.0 / total;
    System.out.printf("\r下载进度: %.1f%%", percent);
});

// 使用 DownloadArgs 下载
DownloadArgs args = DownloadArgs.builder()
        .videoCode("403047")
        .quality("720P")
        .videoName("我的视频")
        .suffix("mp4")
        .downloadDir("/path/to/output")
        .build();
client.download(args, (downloaded, total) -> {
    System.out.printf("进度: %d / %d%n", downloaded, total);
});
```

### 自定义配置

```java
// 使用 Builder 自定义配置
HanimeConfig config = HanimeConfig.builder()
        .baseUrl("https://hanimeone.me/") 
        .connectTimeout(30)
        .readTimeout(60)
        .userLang("zht")                    // 繁体中文
        .build();

HanimeApiClient client = HanimeApiClient.builder()
        .config(config)
        .build();
```

## 构建

```bash
# 编译
mvn compile

# 运行测试
mvn test

# 打包
mvn package
```
