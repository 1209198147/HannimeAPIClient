# HanimeAPIClient

一个用于 [Hanime1](https://hanime1.me/) 的 Java HTTP 客户端库，通过解析网站 HTML 提供结构化的数据访问和视频下载能力。

## 功能特性

- **首页浏览** — 获取轮播 Banner、分区视频列表
- **视频搜索** — 支持关键词、分类、标签、排序、时长、分页等高级筛选
- **影片详情** — 解析标题、简介、标签、作者信息、多分辨率视频源
- **视频下载** — 支持多画质选择、断点续传和下载进度回调
- **评论获取** — 获取影片评论列表和评论回复
- **用户信息** — 获取用户主页、上传视频、播放列表、个人资料
- **预览页面** — 按月份获取新片预览列表

## 快速开始

### 基本用法

```java
import com.shikou.client.HanimeApiClient;
import com.shikou.model.entities.*;
import com.shikou.model.entities.pages.*;
import com.shikou.model.entities.results.VideosResult;

public class Demo {
    public static void main(String[] args) throws Exception {
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
        VideosResult result = client.search(params);
        result.getVideos().forEach(v ->
                System.out.println(v.getTitle() + " | " + v.getViews())
        );

        // 获取影片详情
        HanimeVideo video = client.getVideoDetail("403047");
        System.out.println("标题: " + video.getTitle());
        System.out.println("作者: " + video.getArtist().getName());
        System.out.println("画质: " + video.getVideoUrls().keySet());

        // 获取评论
        List<Comment> comments = client.getVideoComments("403047");
        comments.forEach(c -> System.out.println(c.getUser() + ": " + c.getContent()));

        // 获取评论回复
        List<Comment> replies = client.getReplies("12345");

        // 用户相关
        Profile profile = client.getProfile("userId");
        UserUploadedPage uploaded = client.getUserUploadedPage("userId");
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

## API 参考

### 首页与搜索

| 方法 | 说明 |
|------|------|
| `getHomePage()` | 获取首页数据（Banner、分区视频） |
| `search(String query)` | 关键词搜索 |
| `search(SearchParams params)` | 高级搜索（分类、标签、排序、分页等） |
| `getSearchPage(SearchParams params)` | 获取搜索页面完整数据 |
| `getGenreList()` | 获取可用分类列表 |
| `getTagsMap()` | 获取可用标签映射 |
| `getSortTypeList()` | 获取排序方式列表 |

### 影片详情与下载

| 方法 | 说明 |
|------|------|
| `getVideoDetail(String videoCode)` | 获取影片详细信息 |
| `getWatchPage(String videoCode)` | 获取观看页面完整数据 |
| `getDownloadInfo(String videoCode)` | 获取下载信息（画质、下载链接） |
| `download(code, file)` | 默认最高画质下载 |
| `download(code, quality, file)` | 指定画质下载 |
| `download(code, file, listener)` | 带进度回调下载 |

### 评论

| 方法 | 说明 |
|------|------|
| `getComments(String type, String code)` | 按类型获取评论列表 |
| `getVideoComments(String videoCode)` | 获取影片评论 |
| `getReplies(String commentId)` | 获取评论回复 |

### 用户

| 方法 | 说明 |
|------|------|
| `getProfile(String userId)` | 获取用户个人资料 |
| `getUserPage(String userId)` | 获取用户主页 |
| `getUserUploadedPage(CommonParam)` | 获取用户上传视频分页 |
| `getUserPlaylistsPage(CommonParam)` | 获取用户播放列表分页 |
| `getUploadVideos(CommonParam)` | 获取用户上传视频详情 |
| `getPlaylists(CommonParam)` | 获取用户播放列表详情 |
| `getPlaylist(CommonParam)` | 获取指定播放列表内容 |

## 构建

```bash
mvn compile    # 编译
mvn test       # 运行测试
mvn package    # 打包
```
