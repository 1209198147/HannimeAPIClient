package com.shikou.parser;

import com.shikou.model.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML页面解析器
 * 负责从Hanime1网站返回的HTML中解析出结构化数据
 */
public class HtmlParser {

    private HtmlParser() {}

    // ======================== 首页解析 ========================

    /**
     * 从首页HTML解析出HomePage对象
     */
    public static HomePage parseHomePage(Document doc) {
        HomePage homePage = new HomePage();

        // 解析轮播Banner
        homePage.setBanner(parseBanner(doc));

        // 解析各分类影片列表
        parseSection(homePage, doc);

        return homePage;
    }

    /**
     * 解析轮播Banner
     *
     * 对应HTML结构:
     *   图片:     div.nav-bottom-padding > div.hidden-xs.hidden-sm > div:nth-child(3) > img  (src, alt)
     *   标题:     #home-banner-wrapper > h1
     *   标签列表: #home-banner-wrapper > div.hidden-sm > span  (多个标签)
     *   作者/观看/时间: #home-banner-wrapper > h4  (格式: "SeejayDJ • 32.5万次觀看 • 3週前")
     *   videoUrl/videoCode: script中 window.open('https://hanimeone.me/watch?v=405087', '_blank')
     */
    private static Banner parseBanner(Document doc) {
        Element bannerEl = doc.selectFirst("#home-banner-wrapper");
        if (bannerEl == null) {
            return null;
        }

        Banner banner = new Banner();

        // 标题: #home-banner-wrapper > h1
        Element titleEl = bannerEl.selectFirst("h1");
        if (titleEl != null) {
            banner.setTitle(titleEl.text().trim());
        }

        // 标签列表: #home-banner-wrapper > div.hidden-sm 中的 span 元素
        Element tagDiv = bannerEl.selectFirst("div.hidden-sm");
        if (tagDiv != null) {
            List<String> tags = new ArrayList<>();
            Elements tagSpans = tagDiv.select("span");
            for (Element span : tagSpans) {
                String tagText = span.text().trim();
                if (!tagText.isEmpty()) {
                    tags.add(tagText);
                }
            }
            banner.setTags(tags);
        }

        // 作者/观看次数/上传时间: #home-banner-wrapper > h4
        // 格式: "SeejayDJ • 32.5万次觀看 • 3週前"
        Element metaEl = bannerEl.selectFirst("h4");
        if (metaEl != null) {
            String metaText = metaEl.text().trim();
            String[] parts = metaText.split("\s*•\s*");
            if (parts.length >= 1) {
                banner.setUploader(parts[0].trim());
            }
            if (parts.length >= 2) {
                banner.setViews(parts[1].trim());
            }
            if (parts.length >= 3) {
                banner.setUploadTime(parts[2].trim());
            }
        }

        // 图片: banner区域的背景图片
        Element imgEl = doc.selectFirst("div.nav-bottom-padding div.hidden-xs.hidden-sm img");
        if (imgEl != null) {
            String src = imgEl.attr("abs:src");
            if (src == null || src.isEmpty()) {
                src = imgEl.attr("src");
            }
            banner.setPicUrl(src);
        }

        // videoUrl 和 videoCode: 从 script 中提取 window.open('...') 
        Elements scripts = doc.select("script");
        for (Element script : scripts) {
            String data = script.data();
            if (data.contains("stripchat-popunder") && data.contains("window.open")) {
                Pattern pattern = Pattern.compile("window\\.open\\('([^']+)'");
                Matcher matcher = pattern.matcher(data);
                if (matcher.find()) {
                    String videoUrl = matcher.group(1);
                    banner.setVideoUrl(videoUrl);
                    banner.setVideoCode(extractVideoCode(videoUrl));
                }
                break;
            }
        }

        return banner;
    }

    private static void parseSection(HomePage homePage, Document doc) {
        List<HomePageSection> sections = homePage.getSections();
        if (sections == null) {
            sections = new ArrayList<>();
            homePage.setSections(sections);
        }

        Elements sectionElements = doc.select("#home-rows-wrapper");
        Elements children = sectionElements.select("> a, > div");

        for (int i = 0; i < children.size() - 1;) {
            Element a = children.get(i);     // 当前 a
            Element div = children.get(i+1); // 紧随的 div
            if (!a.tagName().equals("a")){
                i++;
                continue;
            }
            if (div.tagName().equals("div")) {
                String title = Optional.ofNullable(a.selectFirst("h3")).map(Element::ownText).orElse("未知部分" + (i/2 + 1));
                HomePageSection homePageSection = HomePageSection.builder()
                        .title(title.trim())
                        .videoInfoList(parseHanimeListByDiv(div))
                        .build();
                sections.add(homePageSection);
            }
            i += 2;
        }
    }

    public static DownloadInfo parseDownloadPage(Document doc) {
        DownloadInfo downloadInfo = new DownloadInfo();
            
        // 解析标题
        Element titleEl = doc.selectFirst("#content-div > div.row.no-gutter.video-show-width.download-panel > div.col-md-12 > div > div > h3");
        if (titleEl != null) {
            downloadInfo.setTitle(titleEl.text().trim());
        }
            
        // 解析上传时间/观看次数
        Element metaEl = doc.selectFirst("#content-div > div.row.no-gutter.video-show-width.download-panel > div.col-md-12 > div > div > div:nth-child(1) > p");
        if (metaEl != null) {
            String metaText = metaEl.text().trim();
            String[] parts = metaText.split("\\s*\\|\\s*");
            if (parts.length >= 1) {
                downloadInfo.setUpdateTime(parts[0].trim());
            }
            if (parts.length >= 2) {
                downloadInfo.setViews(parts[1].trim());
            }
        }
            
        // 解析封面图
        Element coverEl = doc.selectFirst("#content-div > div.row.no-gutter.video-show-width.download-panel > div.col-md-12 > div > div > img");
        if (coverEl != null) {
            String src = coverEl.attr("abs:src");
            if (src == null || src.isEmpty()) {
                src = coverEl.attr("src");
            }
            downloadInfo.setCoverImg(src);
        }
            
        // 解析下载项列表
        List<DownloadItem> downloadItems = new ArrayList<>();
        Element tableEl = doc.selectFirst("#content-div > div.row.no-gutter.video-show-width.download-panel > div.col-md-12 > div > div > table");
        if (tableEl != null) {
            Elements rows = tableEl.select("tbody > tr");
            for (int i = 1; i < rows.size(); i++) { // 跳过表头行
                Element row = rows.get(i);
                DownloadItem item = new DownloadItem();
                    
                // 解析分辨率
                Element resolutionEl = row.selectFirst("td:nth-child(2)");
                if (resolutionEl != null) {
                    String resolutionText = resolutionEl.text().trim();
                    // 使用正则表达式提取括号内的分辨率，如 "全高清畫質 (1080p)" -> "1080p"
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\((\\d+p)\\)");
                    java.util.regex.Matcher matcher = pattern.matcher(resolutionText);
                    if (matcher.find()) {
                        String resolution = matcher.group(1).toUpperCase();
                        item.setResolution(resolution);
                    } else {
                        // 如果没有找到括号格式，尝试其他方式
                        if (resolutionText.contains("1080")) {
                            item.setResolution("1080P");
                        } else if (resolutionText.contains("720")) {
                            item.setResolution("720P");
                        } else if (resolutionText.contains("480")) {
                            item.setResolution("480P");
                        } else if (resolutionText.contains("360")) {
                            item.setResolution("360P");
                        } else if (resolutionText.contains("240")) {
                            item.setResolution("240P");
                        } else {
                            item.setResolution("Unknown");
                        }
                    }
                }
                    
                // 解析文件类型
                Element typeEl = row.selectFirst("td:nth-child(3)");
                if (typeEl != null) {
                    item.setItemType(typeEl.text().trim());
                }
                    
                // 解析下载链接
                Element linkEl = row.selectFirst("td:nth-child(5) a");
                if (linkEl != null) {
                    String href = linkEl.attr("data-url");
                    if (href == null || href.isEmpty()) {
                        href = linkEl.attr("abs:href");
                    }
                    if (href == null || href.isEmpty()) {
                        href = linkEl.attr("href");
                    }
                    item.setDownloadUrl(href);
                }
                    
                downloadItems.add(item);
            }
        }
        downloadInfo.setDownloadItems(downloadItems);
            
        return downloadInfo;
    }

    /**
     * 从指定分区解析影片列表
     */
    private static List<VideoInfo> parseHanimeListByDiv(Element div) {
        if (div == null) {
            return Collections.emptyList();
        }
        return parseVideoCards(div);
    }

    // ======================== 影片详情解析 ========================

    /**
     * 从影片详情页HTML解析出HanimeVideo对象
     */
    public static HanimeVideo parseVideoDetail(Document doc) {
        HanimeVideo video = new HanimeVideo();

        // 标题 - 优先中文标题
        Element titleEl = doc.selectFirst("h3#shareBtn-title, h1.title, .video-title, h3.title, h3.video-details-wrapper");
        if (titleEl != null) {
            video.setTitle(titleEl.text().trim());
        }

        // 中文标题
        Element cnTitleEl = doc.selectFirst(".video-cn-title, h4.title");
        if (cnTitleEl != null) {
            video.setChineseTitle(cnTitleEl.text().trim());
        } else if (video.getTitle() != null) {
            // 如果没有单独的中文标题字段，标题本身可能就是中文标题
            video.setChineseTitle(video.getTitle());
        }

        // 封面URL
        Element coverEl = doc.selectFirst("video#player");
        if (coverEl != null) {
            video.setCoverUrl(coverEl.attr("poster"));
        }
        if (video.getCoverUrl() == null || video.getCoverUrl().isEmpty()) {
            Element coverImg = doc.selectFirst(".video-cover img, .cover img");
            if (coverImg != null) {
                video.setCoverUrl(coverImg.absUrl("src"));
            }
        }

        // 简介
        Element introEl = doc.selectFirst("#player-div-wrapper .video-caption-text, .caption-expand, .video-introduction, .description, p.text-light");
        if (introEl != null) {
            video.setIntroduction(introEl.text().trim());
        }

        // 观看次数 + 上传时间（同一元素 .hidden-xs 内，如 "观看次数：1.6万次  2026-05-02"）
        Element metaEl = doc.selectFirst("#player-div-wrapper > div:nth-child(13) > div > div.hidden-xs");
        if (metaEl != null) {
            String metaText = metaEl.text().trim();
            String[] split = metaText.split(" ");
            if (split.length > 1){
                // 提取观看次数
                String[] viewsStr = split[0].split("：");
                video.setViews(viewsStr.length > 1 ? viewsStr[1] : viewsStr[0]);

                // 提取上传时间
                String uploadTime = split[1];
                video.setUploadTime(uploadTime);
            }
        }

        // 视频URL列表（多分辨率）
        video.setVideoUrls(parseVideoUrls(doc));

        // 标签
        video.setTags(parseTags(doc));

        // 收藏数
        Element favEl = doc.selectFirst(".fav-count, .like-count");
        if (favEl != null) {
            try {
                video.setFavTimes(Integer.parseInt(favEl.text().trim()));
            } catch (NumberFormatException ignored) {}
        }

        // 是否已收藏
        Element favBtn = doc.selectFirst(".fav-button.active, .like-button.active");
        video.setFav(favBtn != null);

        // 作者信息
        video.setArtist(parseArtist(doc));

        // 播放列表
        video.setPlaylist(parsePlaylist(doc));

        // 相关影片
        video.setRelatedHanimes(parseRelatedHanimes(doc));

        return video;
    }

    /**
     * 解析视频URL（多分辨率）
     */
    public static Map<String, VideoQuality> parseVideoUrls(Document doc) {
        Map<String, VideoQuality> videoUrls = new LinkedHashMap<>();

        // 方式1: 从 video 标签的 source 子标签解析
        Element videoElement = doc.selectFirst("video#player");
        if (videoElement != null) {
            Elements sources = videoElement.select("source");
            for (Element source : sources) {
                String resolution = source.attr("size") + "P";
                String url = source.absUrl("src");
                if (url.isEmpty()) {
                    url = source.attr("src");
                }
                String mimeType = source.attr("type");
                String suffix = getFileSuffix(mimeType);
                if (!url.isEmpty()) {
                    videoUrls.put(resolution, new VideoQuality(resolution, url, suffix));
                }
            }
        }

        // 方式2: 从 script 标签解析 (备用)
        if (videoUrls.isEmpty()) {
            Element playerDiv = doc.selectFirst("div#player-div-wrapper");
            if (playerDiv != null) {
                Elements scripts = playerDiv.select("script");
                for (Element script : scripts) {
                    String data = script.data();
                    if (data.isEmpty()) continue;

                    Pattern pattern = Pattern.compile("const source = '(.+)'");
                    Matcher matcher = pattern.matcher(data);
                    if (matcher.find()) {
                        String url = matcher.group(1);
                        videoUrls.put("Unknown", new VideoQuality("Unknown", url, "mp4"));
                        break;
                    }
                }
            }
        }

        return videoUrls;
    }

    /**
     * 根据MIME类型获取文件后缀
     */
    public static String getFileSuffix(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return "mp4";
        }
        String lower = mimeType.toLowerCase();
        if (lower.contains("mp4")) return "mp4";
        if (lower.contains("mpeg")) return "mpeg";
        if (lower.contains("x-msvideo")) return "avi";
        if (lower.contains("3gpp2")) return "3g2";
        if (lower.contains("3gpp")) return "3gp";
        if (lower.contains("ogg")) return "ogv";
        if (lower.contains("mp2t")) return "ts";
        if (lower.contains("webm")) return "webm";
        return "mp4";
    }

    /**
     * 解析标签列表
     */
    private static List<String> parseTags(Document doc) {
        List<String> tags = new ArrayList<>();
        Elements tagEls = doc.select(".single-video-tag");
        List<String> ignoreTags = Arrays.asList("add", "remove");
        for (Element tagEl : tagEls) {
            String tagText = tagEl.text().trim();
            if (ignoreTags.contains(tagText.toLowerCase())) {
                continue;
            }
            // 去掉尾部数字括号
            tagText = tagText.replaceAll("\\s*\\(\\d+\\)$", "").trim();
            if (!tagText.isEmpty()) {
                tags.add(tagText);
            }
        }
        return tags;
    }

    /**
     * 解析作者信息
     *
     * 对应HTML结构:
     * <div class="video-details-wrapper desktop-inline-mobile-block">
     *   <img id="video-user-avatar" src="..." alt="Eroshi3D">
     *   <a id="video-artist-name" href="...search?query=Eroshi3D">Eroshi3D</a>
     *   <a href="...search?genre=2.5D">2.5D</a>
     *   <button class="video-subscribe-btn">订阅</button>
     * </div>
     */
    private static Artist parseArtist(Document doc) {
        Element artistEl = doc.selectFirst("#player-div-wrapper .video-details-wrapper");
        if (artistEl == null) {
            // 备选旧版选择器
            artistEl = doc.selectFirst(".artist-info, .channel-info, .uploader-info");
        }
        if (artistEl == null) {
            return null;
        }

        Artist artist = new Artist();

        // 作者名称: a#video-artist-name
        Element nameEl = artistEl.selectFirst("a#video-artist-name");
        if (nameEl == null) {
            nameEl = artistEl.selectFirst("a, .name");
        }
        if (nameEl != null) {
            artist.setName(nameEl.text().trim());
        }

        // 头像: 优先取第二个img（用户头像覆盖层），否则取第一个
        Elements avatarImgs = artistEl.select("img#video-user-avatar, img[alt]");
        if (avatarImgs.size() >= 2) {
            // 第二个img是实际用户头像
            String avatarSrc = avatarImgs.get(1).absUrl("src");
            if (avatarSrc == null || avatarSrc.isEmpty()) {
                avatarSrc = avatarImgs.get(1).attr("src");
            }
            artist.setAvatarUrl(avatarSrc);
        } else if (avatarImgs.size() == 1) {
            String avatarSrc = avatarImgs.get(0).absUrl("src");
            if (avatarSrc == null || avatarSrc.isEmpty()) {
                avatarSrc = avatarImgs.get(0).attr("src");
            }
            artist.setAvatarUrl(avatarSrc);
        }

        // 类型/Genre: a[href*=search?genre=]
        Element genreEl = artistEl.selectFirst("a[href*=search?genre=]");
        if (genreEl != null) {
            artist.setGenre(genreEl.text().trim());
        }

        // 是否已订阅: #video-subscribe-form-wrapper 内按钮状态
        Element subWrapper = artistEl.selectFirst("#video-subscribe-form-wrapper");
        if (subWrapper != null) {
            Element subBtn = subWrapper.selectFirst(".video-subscribe-btn.active");
            artist.setSubscribed(subBtn != null);
        }

        return artist;
    }

    /**
     * 解析相关影片
     */
    private static List<VideoInfo> parseRelatedHanimes(Document doc) {
        List<VideoInfo> related = new ArrayList<>();
        Element relatedSection = doc.selectFirst("#related-tabcontent > div.row.doujin-row");
        if (relatedSection != null) {
            related.addAll(parseVideoCards(relatedSection));
        }
        return related;
    }

    // ======================== 搜索结果解析 ========================

    /**
     * 从搜索结果页面HTML解析影片列表
     */
    public static List<VideoInfo> parseSearchResults(Document doc) {
        return parseVideoCards(doc);
    }

    // ======================== 预览页面解析 ========================

    /**
     * 从预览页面HTML解析PreviewPage
     */
    public static PreviewPage parsePreviewPage(Document doc) {
        PreviewPage page = new PreviewPage();

        // 头部图片
        Element headerImg = doc.selectFirst(".preview-header img, .banner img");
        if (headerImg != null) {
            page.setHeaderPicUrl(headerImg.absUrl("src"));
        }

        // 分页信息
        Element prevLink = doc.selectFirst("a.prev, a[rel=prev], a:has(.fa-chevron-left)");
        page.setHasPrevious(prevLink != null);

        Element nextLink = doc.selectFirst("a.next, a[rel=next], a:has(.fa-chevron-right)");
        page.setHasNext(nextLink != null);

        // 最新影片
        page.setLatestHanime(parseVideoCards(doc));

        // 预览信息列表
        page.setPreviewInfoList(parsePreviewInfoList(doc));

        return page;
    }

    /**
     * 解析预览信息列表
     */
    private static List<PreviewInfo> parsePreviewInfoList(Document doc) {
        List<PreviewInfo> list = new ArrayList<>();
        Elements items = doc.select(".preview-item, .preview-card");
        for (Element item : items) {
            PreviewInfo info = new PreviewInfo();

            Element titleEl = item.selectFirst("h3, .title");
            if (titleEl != null) {
                info.setTitle(titleEl.text().trim());
            }

            Element coverEl = item.selectFirst("img");
            if (coverEl != null) {
                info.setCoverUrl(coverEl.absUrl("src"));
            }

            Element introEl = item.selectFirst("p, .description");
            if (introEl != null) {
                info.setIntroduction(introEl.text().trim());
            }

            Element brandEl = item.selectFirst(".brand, .studio");
            if (brandEl != null) {
                info.setBrand(brandEl.text().trim());
            }

            Element dateEl = item.selectFirst(".date, time");
            if (dateEl != null) {
                info.setReleaseDate(dateEl.text().trim());
            }

            Element linkEl = item.selectFirst("a[href*=/watch]");
            if (linkEl != null) {
                info.setVideoCode(extractVideoCode(linkEl.attr("href")));
            }

            list.add(info);
        }
        return list;
    }

    // ======================== 播放列表解析 ========================

    /**
     * 从播放列表页面HTML解析PlaylistItem列表
     */
    public static List<PlaylistItem> parsePlaylistItems(Document doc) {
        List<PlaylistItem> playlists = new ArrayList<>();
        Elements items = doc.select(".playlist-item, .playlist-card");
        for (Element item : items) {
            PlaylistItem pl = new PlaylistItem();

            Element titleEl = item.selectFirst(".title, h3, h4");
            if (titleEl != null) {
                pl.setTitle(titleEl.text().trim());
            }

            Element codeEl = item.selectFirst("a[href*=/playlist], input[name=list_code]");
            if (codeEl != null) {
                String href = codeEl.attr("href");
                if (href.contains("list=")) {
                    pl.setListCode(extractQueryParam(href, "list"));
                } else {
                    pl.setListCode(codeEl.attr("value"));
                }
            }

            Element totalEl = item.selectFirst(".count, .total");
            if (totalEl != null) {
                try {
                    pl.setTotal(Integer.parseInt(totalEl.text().trim()));
                } catch (NumberFormatException ignored) {}
            }

            playlists.add(pl);
        }
        return playlists;
    }

    /**
     * 从影片详情页解析播放列表
     *
     * 对应HTML结构（每个 .card-mobile-panel.inner 为一个视频项）:
     * <div class="card-mobile-panel inner">
     *   <img src="...背景图...">           <!-- 第1个img: 背景 -->
     *   <img src="...缩略图..." alt="标题">  <!-- 第2个img: 缩略图+alt标题 -->
     *   <div class="card-mobile-duration card-playlist-small">03:02</div>
     *   <div>現正播放</div>  <!-- 可选，表示当前正在播放 -->
     *   <div class="card-mobile-title">Hotel with Makima</div>
     *   <a class="card-mobile-user" href="...search?query=MujitaX">MujitaX</a>
     *   <div class="card-playlist-large"><i>thumb_up</i> 100%</div>
     *   <div class="card-playlist-large">123次</div>
     * </div>
     */
    public static Playlist parsePlaylist(Document doc) {
        List<VideoInfo> videos = new ArrayList<>();

        Elements cards = doc.select("div.card-mobile-panel.inner");
        for (Element card : cards) {
            VideoInfo info = parseMobilePanelCard(card);
            if (info != null) {
                videos.add(info);
            }
        }

        if (videos.isEmpty()) {
            return null;
        }

        // 尝试获取播放列表代码
        String listCode = null;
        Element listCodeInput = doc.selectFirst("input[name=list_code]");
        if (listCodeInput != null) {
            listCode = listCodeInput.attr("value");
        }

        return Playlist.builder()
                .listCode(listCode)
                .total(videos.size())
                .videos(videos)
                .build();
    }

    /**
     * 解析单个 .card-mobile-panel.inner 卡片
     */
    private static VideoInfo parseMobilePanelCard(Element card) {
        VideoInfo info = new VideoInfo();

        // 封面图：取第2个img（缩略图层，带亮度遮罩）
        Elements imgs = card.select("img");
        if (imgs.size() >= 2) {
            Element thumbImg = imgs.get(1);
            String src = thumbImg.attr("abs:src");
            if (src == null || src.isEmpty()) {
                src = thumbImg.attr("src");
            }
            info.setCoverUrl(src);
            String alt = thumbImg.attr("alt");
            if (alt != null && !alt.isEmpty()) {
                info.setTitle(alt.trim());
            }
        } else if (imgs.size() == 1) {
            Element img = imgs.get(0);
            String src = img.attr("abs:src");
            if (src == null || src.isEmpty()) {
                src = img.attr("src");
            }
            info.setCoverUrl(src);
        }

        // videoCode：从链接中提取
        Element watchLink = card.selectFirst("a[href*=/watch]");
        if (watchLink != null) {
            String href = watchLink.attr("abs:href");
            if (href == null || href.isEmpty()) {
                href = watchLink.attr("href");
            }
            info.setVideoUrl(href);
            info.setVideoCode(extractVideoCode(href));
        }

        // 时长: .card-playlist-small
        Element durationEl = card.selectFirst("div.card-playlist-small");
        if (durationEl != null) {
            info.setDuration(durationEl.text().trim());
        }

        // 标题: .card-mobile-title（优先于img alt）
        Element titleEl = card.selectFirst("div.card-mobile-title");
        if (titleEl != null) {
            info.setTitle(titleEl.text().trim());
        }

        // 上传者: a.card-mobile-user
        Element uploaderEl = card.selectFirst("a.card-mobile-user");
        if (uploaderEl != null) {
            info.setUploader(uploaderEl.text().trim());
            String uploaderHref = uploaderEl.attr("abs:href");
            if (uploaderHref == null || uploaderHref.isEmpty()) {
                uploaderHref = uploaderEl.attr("href");
            }
            info.setUploaderUrl(uploaderHref);
        }

        // 统计信息: .card-playlist-large 列表
        Elements statEls = card.select("div.card-playlist-large");
        for (Element stat : statEls) {
            String text = stat.text().trim();
            if (text.contains("%")) {
                info.setLikeRate(text.replace("thumb_up", "").trim());
            } else if (!text.isEmpty() && (text.contains("次") || text.matches(".*\\d.*"))) {
                info.setViews(text);
            }
        }

        // 現正播放
        Element playingEl = card.selectFirst("div[style*=translate]");
        if (playingEl != null && playingEl.text().contains("現正播放")) {
            info.setPlaying(true);
        }

        return info;
    }

    // ======================== 工具方法 ========================

    /**
     * 从URL中提取影片代码
     * /watch?v=12345 -> 12345
     */
    public static String extractVideoCode(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        return extractQueryParam(url, "v");
    }

    /**
     * 从URL中提取指定查询参数
     */
    public static String extractQueryParam(String url, String param) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        // 处理相对URL
        if (url.startsWith("?")) {
            url = "/watch" + url;
        }
        Pattern pattern = Pattern.compile("[?&]" + Pattern.quote(param) + "=([^&#]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    // ======================== 影片卡片解析 ========================

    /**
     * 从页面中解析所有影片卡片元素为 VideoInfo 列表
     * 支持两种卡片结构:
     *   1. .horizontal-card（搜索结果、相关影片等）
     *   2. .video-card-inner（首页分区卡片）
     */
    public static List<VideoInfo> parseVideoCards(Element root) {
        List<VideoInfo> list = new ArrayList<>();

        // 先解析 .horizontal-card
        Elements horizontalCards = root.select("div.horizontal-card");
        for (Element card : horizontalCards) {
            VideoInfo info = parseSingleVideoCard(card);
            if (info != null && info.getVideoCode() != null) {
                list.add(info);
            }
        }

        // 再解析 .video-card-inner（首页分区用）
        Elements videoCardInners = root.select("div.video-card-inner");
        for (Element card : videoCardInners) {
            VideoInfo info = parseVideoCardInner(card);
            if (info != null && info.getVideoCode() != null) {
                list.add(info);
            }
        }

        return list;
    }

    /**
     * 解析单个 .horizontal-card 元素（搜索结果、相关影片等）
     *
     * 对应HTML结构:
     * <div class="horizontal-card">
     *   <a href="https://hanimeone.me/watch?v=403047" class="video-link">
     *     <div class="thumb-container">
     *       <img class="main-thumb" src="...">
     *       <div class="duration">11:00</div>
     *       <div class="stats-container">
     *         <div class="stat-item"><i class="material-icons">thumb_up</i> 99%</div>
     *         <div class="stat-item">40.9萬次</div>
     *       </div>
     *     </div>
     *     <div class="title">标题</div>
     *   </a>
     *   <div class="subtitle">
     *     <a href="...search?query=紺そめ（Consome）">紺そめ（Consome） • 3個月前</a>
     *   </div>
     * </div>
     */
    private static VideoInfo parseSingleVideoCard(Element card) {
        VideoInfo info = new VideoInfo();

        // 视频链接 → videoUrl / videoCode
        Element linkEl = card.selectFirst("a.video-link");
        if (linkEl == null) {
            linkEl = card.selectFirst("a[href*=/watch]");
        }
        if (linkEl != null) {
            String href = linkEl.attr("abs:href");
            if (href == null || href.isEmpty()) {
                href = linkEl.attr("href");
            }
            info.setVideoUrl(href);
            info.setVideoCode(extractVideoCode(href));
        }

        // 封面图
        Element imgEl = card.selectFirst("img.main-thumb");
        if (imgEl == null) {
            imgEl = card.selectFirst("img");
        }
        if (imgEl != null) {
            String src = imgEl.attr("abs:src");
            if (src == null || src.isEmpty()) {
                src = imgEl.attr("src");
            }
            info.setCoverUrl(src);
        }

        // 时长
        Element durationEl = card.selectFirst("div.duration");
        if (durationEl != null) {
            info.setDuration(durationEl.text().trim());
        }

        // 统计信息（stat-item 列表）
        Elements statItems = card.select("div.stat-item");
        for (Element stat : statItems) {
            String text = stat.text().trim();
            if (text.contains("%")) {
                info.setLikeRate(text.replace("thumb_up", "").trim());
            } else if (!text.isEmpty() && (text.contains("次") || text.matches(".*\\d.*"))) {
                info.setViews(text);
            }
        }

        // 标题
        Element titleEl = card.selectFirst("div.title");
        if (titleEl != null) {
            info.setTitle(titleEl.text().trim());
        }

        // 副标题（上传者 + 时间）
        Element subtitleEl = card.selectFirst("div.subtitle a");
        if (subtitleEl != null) {
            info.setUploaderUrl(subtitleEl.attr("abs:href"));
            String subtitleText = subtitleEl.text().trim();
            String[] parts = subtitleText.split("\\s*•\\s*");
            if (parts.length >= 1) {
                info.setUploader(parts[0].trim());
            }
            if (parts.length >= 2) {
                info.setUploadTime(parts[1].trim());
            }
        }

        return info;
    }

    /**
     * 解析单个 .video-card-inner 元素（首页分区卡片）
     *
     * 对应HTML结构:
     * <div class="video-card-inner">
     *   <a href="/watch?v=xxx">
     *     <img loading="lazy" src="https://vdownload.hembed.com/image/cover/434.jpg?secure=...">
     *     <div class="home-rows-videos-title">xxxxxx 2</div>
     *   </a>
     * </div>
     */
    private static VideoInfo parseVideoCardInner(Element card) {
        VideoInfo info = new VideoInfo();

        // 视频链接 → videoUrl / videoCode
        Element linkEl = card.selectFirst("a[href*=/watch]");
        if (linkEl == null) {
            // .video-card-inner 外层可能被 <a> 包裹
            linkEl = card.parent();
        }
        if (linkEl != null) {
            String href = linkEl.attr("abs:href");
            if (href == null || href.isEmpty()) {
                href = linkEl.attr("href");
            }
            info.setVideoUrl(href);
            info.setVideoCode(extractVideoCode(href));
        }

        // 封面图
        Element imgEl = card.selectFirst("img");
        if (imgEl != null) {
            String src = imgEl.attr("abs:src");
            if (src == null || src.isEmpty()) {
                src = imgEl.attr("src");
            }
            info.setCoverUrl(src);
            // alt 作为标题备选
            String alt = imgEl.attr("alt");
            if (alt != null && !alt.isEmpty()) {
                info.setTitle(alt.trim());
            }
        }

        // 标题（优先 .home-rows-videos-title）
        Element titleEl = card.selectFirst("div.home-rows-videos-title");
        if (titleEl != null) {
            info.setTitle(titleEl.text().trim());
        }

        return info;
    }
}
