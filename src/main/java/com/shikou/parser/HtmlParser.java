package com.shikou.parser;

import com.shikou.model.entities.*;
import com.shikou.model.entities.pages.*;
import com.shikou.model.entities.results.PlaylistsResult;
import com.shikou.model.entities.results.VideosResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
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
            banner.setPicUrl(getAbsUrl(imgEl, "src"));
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
                String moreLink = getAbsUrl(a, "href");
                HomePageSection homePageSection = HomePageSection.builder()
                        .title(title.trim())
                        .moreLink(moreLink)
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
            downloadInfo.setCoverImg(getAbsUrl(coverEl, "src"));
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
                Element qualityEl = row.selectFirst("td:nth-child(2)");
                if (qualityEl != null) {
                    String qualityText = qualityEl.text().trim();
                    // 使用正则表达式提取括号内的分辨率，如 "全高清畫質 (1080p)" -> "1080p"
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\((\\d+p)\\)");
                    java.util.regex.Matcher matcher = pattern.matcher(qualityText);
                    if (matcher.find()) {
                        String quality = matcher.group(1).toUpperCase();
                        item.setQuality(quality);
                    } else {
                        // 如果没有找到括号格式，尝试其他方式
                        if (qualityText.contains("1080")) {
                            item.setQuality("1080P");
                        } else if (qualityText.contains("720")) {
                            item.setQuality("720P");
                        } else if (qualityText.contains("480")) {
                            item.setQuality("480P");
                        } else if (qualityText.contains("360")) {
                            item.setQuality("360P");
                        } else if (qualityText.contains("240")) {
                            item.setQuality("240P");
                        } else {
                            item.setQuality("Unknown");
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
                    if (StringUtils.isEmpty(href)) {
                        href = linkEl.attr("abs:href");
                    }
                    if (StringUtils.isEmpty(href)) {
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
        if (StringUtils.isEmpty(video.getCoverUrl())) {
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
        Element metaEl = doc.selectFirst("#player-div-wrapper > div.video-details-wrapper.hidden-sm.hidden-md.hidden-lg.hidden-xl");
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

        // 作者信息
        video.setArtist(parseArtist(doc));

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
                String quality = source.attr("size") + "P";
                String url = source.absUrl("src");
                if (StringUtils.isEmpty(url)) {
                    url = source.attr("src");
                }
                String mimeType = source.attr("type");
                String suffix = getFileSuffix(mimeType);
                if (!url.isEmpty()) {
                    videoUrls.put(quality, new VideoQuality(quality, url, suffix));
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
        if (StringUtils.isEmpty(mimeType)) {
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
    private static Map<String, String> parseTags(Document doc) {
        Map<String, String> tags = new HashMap<>();
        Elements tagEls = doc.select(".single-video-tag");
        List<String> ignoreTags = Arrays.asList("add", "remove");
        for (Element tagEl : tagEls) {
            Element element = tagEl.selectFirst("a");
            String href = element.attr("href");
            String tagText = tagEl.text().trim();
            if (ignoreTags.contains(tagText.toLowerCase())) {
                continue;
            }
            // 去掉尾部数字括号
            tagText = tagText.replaceAll("\\s*\\(\\d+\\)$", "").trim();
            if (!StringUtils.isAnyBlank(tagText, href)) {
                tags.put(tagText, href);
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
        Element artistEl = doc.selectFirst("#player-div-wrapper > div.video-details-wrapper.desktop-inline-mobile-block > div:nth-child(1)");
        if (artistEl == null) {
            return null;
        }

        Artist artist = new Artist();

        // 作者id
        Element artistProfileEl = artistEl.selectFirst("a");
        if (artistProfileEl != null) {
            String profileUrl = artistProfileEl.attr("href");
            artist.setId(extractUserId(profileUrl));
            artist.setProfileUrl(profileUrl);
        }

        // 作者名称: a#video-artist-name
        Element nameEl = doc.selectFirst("#video-artist-name");
        if (nameEl != null) {
            artist.setName(nameEl.text().trim());
        }

        // 头像: 优先取第二个img（用户头像覆盖层），否则取第一个
        Elements avatarImgs = artistEl.select("a > div > img");
        if (avatarImgs.size() >= 2) {
            // 第二个img是实际用户头像
            artist.setAvatarUrl(getAbsUrl(avatarImgs.get(1), "src"));
        } else if (avatarImgs.size() == 1) {
            artist.setAvatarUrl(getAbsUrl(avatarImgs.get(0), "src"));
        }

        // 类型/Genre: a[href*=search?genre=]
        Element genreEl = artistEl.selectFirst("a[href*=search?genre=]");
        if (genreEl != null) {
            artist.setGenre(genreEl.text().trim());
        }

        // 是否已订阅: #video-subscribe-form-wrapper 内按钮状态
        Element subButtonElem = artistEl.selectFirst("#video-subscribe-form-wrapper > button");
        if (subButtonElem != null) {
            String buttonText = subButtonElem.text().trim();
            artist.setSubscribed("订阅".equals(buttonText));
        }
        return artist;
    }

    /**
     * 解析相关影片
     */
    private static List<VideoInfo> parseRelatedHanimes(Document doc) {
        List<VideoInfo> related = new ArrayList<>();
        Element relatedSection = doc.selectFirst("#related-tabcontent > div.row");
        if (relatedSection != null) {
            related.addAll(parseVideoCards(relatedSection));
        }
        return related;
    }

    // ======================== 搜索结果解析 ========================

    /**
     * 从搜索结果页面HTML解析影片列表
     */
    public static VideosResult parseSearchResults(Document doc) {
        List<VideoInfo> videoInfos = parseVideoCards(doc);
        VideosResult videosResult = new VideosResult(videoInfos);
        // 获取分页信息
        parsePagination(doc, videosResult);
        return videosResult;
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
                info.setCoverUrl(getAbsUrl(coverEl, "src"));
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
        List<PlaylistItem> list = new ArrayList<>();

        // 解析 .horizontal-card
        Elements horizontalCards = doc.select("div.horizontal-card");
        for (Element card : horizontalCards) {
            PlaylistItem item = parseSinglePlaylistCard(card);
            if (item != null && item.getListCode() != null) {
                list.add(item);
            }
        }

        return list;
    }

    private static PlaylistItem parseSinglePlaylistCard(Element card) {
        PlaylistItem item = new PlaylistItem();

        // 视频链接 → ListUrl / ListCode
        Element linkEl = card.selectFirst("a.video-link");
        if (linkEl == null) {
            linkEl = card.selectFirst("a[href*=/watch]");
        }
        if (linkEl != null) {
            String href = linkEl.attr("abs:href");
            if (StringUtils.isEmpty(href)) {
                href = linkEl.attr("href");
            }
            item.setListUrl(href);
            item.setListCode(extractListCode(href));
        }

        // 封面图
        Element imgEl = card.selectFirst("img.main-thumb");
        if (imgEl == null) {
            imgEl = card.selectFirst("img");
        }
        if (imgEl != null) {
            item.setCoverUrl(getAbsUrl(imgEl, "src"));
        }

        // 标题
        Element titleEl = card.selectFirst("div.title");
        if (titleEl != null) {
            item.setTitle(titleEl.text().trim());
        }

        // 副标题（上传者 + 时间）
        Element subtitleEl = card.selectFirst("div.subtitle a");
        if (subtitleEl != null) {
            item.setUploaderUrl(subtitleEl.attr("abs:href"));
            String subtitleText = subtitleEl.text().trim();
            String[] parts = subtitleText.split("\\s*•\\s*");
            if (parts.length >= 1) {
                item.setUploader(parts[0].trim());
            }
            if (parts.length >= 2) {
                item.setUploadTime(parts[1].trim());
            }
        }

        // 总数
        Element totalEl = card.selectFirst("div.stats-container > div");
        if (totalEl != null) {
            String totalText = totalEl.text().trim().replaceAll("[^0-9]", "");
            item.setTotal(Integer.parseInt(totalText));
        }

        return item;
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

        Element titleElem = doc.selectFirst("#video-playlist-wrapper > div.single-icon-wrapper.video-playlist-top > h4:nth-child(1)");
        String title = null;
        if (titleElem != null) {
            title = titleElem.text().trim();
        }

        Element playListElem = doc.selectFirst(".hover-video-playlist");
        if (playListElem != null) {
            Elements cards = playListElem.select("div.related-watch-wrap.multiple-link-wrapper");
            for (Element card : cards) {
                VideoInfo info = parsePlayListCard(card);
                if (info != null) {
                    videos.add(info);
                }
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
                .title(title)
                .total(videos.size())
                .videos(videos)
                .build();
    }

    /**
     * 解playlist card
     */
    private static VideoInfo parsePlayListCard(Element card) {
        if(card == null) {
            return null;
        }

        VideoInfo info = new VideoInfo();
        // videoCode：从链接中提取
        Element watchLink = card.selectFirst("a[href*=/watch]");
        if (watchLink != null) {
            String href = watchLink.attr("abs:href");
            if (StringUtils.isEmpty(href)) {
                href = watchLink.attr("href");
            }
            info.setVideoUrl(href);
            info.setVideoCode(extractVideoCode(href));
        }
        Element mobilePanelCardElem = card.selectFirst("div");
        parseMobilePanelCard(mobilePanelCardElem, info);
        return info;
    }


    /**
     * 解析单个 .card-mobile-panel.inner 卡片
     */
    private static void parseMobilePanelCard(Element mobilePanelCardElem, VideoInfo info) {

        if(mobilePanelCardElem == null || info == null) {
            return;
        }

        // 封面图：取第2个img（缩略图层，带亮度遮罩）
        Elements imgs = mobilePanelCardElem.select("img");
        if (imgs.size() >= 2) {
            Element thumbImg = imgs.get(1);
            info.setCoverUrl(getAbsUrl(thumbImg, "src"));
            String alt = thumbImg.attr("alt");
            if (StringUtils.isNotEmpty(alt)) {
                info.setTitle(alt.trim());
            }
        } else if (imgs.size() == 1) {
            info.setCoverUrl(getAbsUrl(imgs.get(0), "src"));
        }

        // 时长: .card-playlist-small
        Element durationEl = mobilePanelCardElem.selectFirst("div.card-playlist-small");
        if (durationEl != null) {
            info.setDuration(durationEl.text().trim());
        }

        // 标题: .card-mobile-title（优先于img alt）
        Element titleEl = mobilePanelCardElem.selectFirst("div.card-mobile-title");
        if (titleEl != null) {
            info.setTitle(titleEl.text().trim());
        }

        // 上传者: a.card-mobile-user
        Element uploaderEl = mobilePanelCardElem.selectFirst("a.card-mobile-user");
        if (uploaderEl != null) {
            info.setUploader(uploaderEl.text().trim());
            info.setUploaderUrl(getAbsUrl(uploaderEl, "href"));
        }

        // 统计信息: .card-playlist-large 列表
        Elements statEls = mobilePanelCardElem.select("div.card-playlist-large");
        for (Element stat : statEls) {
            String text = stat.text().trim();
            if (text.contains("%")) {
                info.setLikeRate(text.replace("thumb_up", "").trim());
            } else if (!text.isEmpty() && (text.contains("次") || text.matches(".*\\d.*"))) {
                info.setViews(text);
            }
        }

        // 現正播放
        Element playingEl = mobilePanelCardElem.selectFirst("div[style*=translate]");
        if (playingEl != null && playingEl.text().contains("現正播放")) {
            info.setPlaying(true);
        }
    }

    // ======================== 工具方法 ========================

    /**
     * 从URL中提取影片代码
     * /watch?v=12345 -> 12345
     */
    public static String extractVideoCode(String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        return extractQueryParam(url, "v");
    }

    /**
     * 从URL中提取列表代码
     * /playlist?list=691833 -> 691833
     */
    public static String extractListCode(String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        return extractQueryParam(url, "list");
    }

    /**
     * 从URL中提取用户id
     * https://hanimeone.me/user/1863268 -> 1863268
     */
    public static String extractUserId(String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        Pattern pattern = Pattern.compile("/user/(\\d+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 从URL中提取指定查询参数
     */
    public static String extractQueryParam(String url, String param) {
        if (StringUtils.isEmpty(url)) {
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

        // 再解析 .video-card-inner
        Elements videoCardInners = root.select("div.video-card-inner");
        for (Element card : videoCardInners) {
            VideoInfo info = parseVideoCardInner(card);
            if (info != null && info.getTitle() != null) {
                updateVideoItemType(info);
                list.add(info);
            }
        }

        return list;
    }

    private static void updateVideoItemType(VideoInfo videoInfo){
        int itemType = 0;
    
        boolean hasCoverUrl = StringUtils.isNotEmpty(videoInfo.getCoverUrl());
        boolean hasTitle = StringUtils.isNotEmpty(videoInfo.getTitle());
        boolean hasOtherFields = StringUtils.isNotEmpty(videoInfo.getVideoCode())
                || StringUtils.isNotEmpty(videoInfo.getVideoUrl())
                || StringUtils.isNotEmpty(videoInfo.getDuration())
                || StringUtils.isNotEmpty(videoInfo.getLikeRate())
                || StringUtils.isNotEmpty(videoInfo.getViews())
                || StringUtils.isNotEmpty(videoInfo.getUploader())
                || StringUtils.isNotEmpty(videoInfo.getUploaderUrl())
                || StringUtils.isNotEmpty(videoInfo.getUploadTime())
                || StringUtils.isNotEmpty(videoInfo.getGenre());
    
        if (hasCoverUrl && hasTitle && !hasOtherFields) {
            itemType = 1;
        }
    
        videoInfo.setItemType(itemType);
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
            if (StringUtils.isEmpty(href)) {
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
            info.setCoverUrl(getAbsUrl(imgEl, "src"));
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
            // .video-card-inner 外层可能被 <a> 和 <div> 包裹
            linkEl = card.parent();
            Element element = linkEl.selectFirst("a[href*=/watch]");
            if(element == null){
                linkEl = linkEl.parent();
            }else{
                linkEl = element;
            }
        }
        if (linkEl != null) {
            String href = getAbsUrl(linkEl, "href");
            info.setVideoUrl(href);
            String videoCode = extractVideoCode(href);
            if(StringUtils.isBlank(videoCode)){
                return null;
            }
            info.setVideoCode(videoCode);
        }

        // 封面图
        Element imgEl = card.selectFirst("img");
        if (imgEl != null) {
            info.setCoverUrl(getAbsUrl(imgEl, "src"));
            // alt 作为标题备选
            String alt = imgEl.attr("alt");
            if (StringUtils.isNotEmpty(alt)) {
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

    public static Map<String, String> parseGenreList(Document doc) {
        return parseGenreModalOptions(doc, "#genre-modal");
    }

    public static Map<String, Map<String, String>> parseTagsMap(Document doc) {
        Map<String, Map<String, String>> tagsMap = new HashMap<>();
        Element tagModalElem = doc.selectFirst("#tags > div > div > div.modal-body");
        if (tagModalElem != null) {
            Elements children = tagModalElem.children();
            String currentType = null;
            Map<String, String> tagMap = new HashMap<>();
            for (Element child : children) {
                if (child.is("div")) {
                    continue;
                }
                if (child.is("h5")) {
                    if(currentType != null) {
                        tagsMap.put(currentType, tagMap);
                    }
                    currentType = child.text().trim();
                    tagMap = new HashMap<>();
                }
                if (child.is("label")) {
                    String key = child.text().trim();
                    Element inputEl = child.selectFirst("input");
                    if (inputEl != null) {
                        String value = inputEl.attr("value");
                        tagMap.put(key, value);
                    }
                }
            }
            if(currentType != null && MapUtils.isNotEmpty(tagMap)) {
                tagsMap.put(currentType, tagMap);
            }
        }
        return tagsMap;
    }

    public static Map<String, String> parseSortTypeList(Document doc) {
        return parseSortModalOptions(doc, "#sort-modal");
    }

    public static SearchPage parseSearchPage(Document doc) {
        SearchPage searchPage = new SearchPage();
        Map<String, String> genres = parseGenreList(doc);
        searchPage.setGenres(genres);

        Map<String, Map<String, String>> tagsMap = parseTagsMap(doc);
        searchPage.setTagsMap(tagsMap);

        Map<String, String> sortMap = parseSortTypeList(doc);
        searchPage.setSortTypes(sortMap);

        List<VideoInfo> videoInfos = parseVideoCards(doc);
        searchPage.setVideos(videoInfos);

        parsePagination(doc, searchPage);

        return searchPage;
    }

    public static WatchPage parseWatchPage(Document doc) {
        WatchPage page = new WatchPage();

        // 标题 - 优先中文标题
        Element titleEl = doc.selectFirst("h3#shareBtn-title, h1.title, .video-title, h3.title, h3.video-details-wrapper");
        if (titleEl != null) {
            page.setTitle(titleEl.text().trim());
        }

        // 中文标题
        Element cnTitleEl = doc.selectFirst(".video-cn-title, h4.title");
        if (cnTitleEl != null) {
            page.setChineseTitle(cnTitleEl.text().trim());
        } else if (page.getTitle() != null) {
            // 如果没有单独的中文标题字段，标题本身可能就是中文标题
            page.setChineseTitle(page.getTitle());
        }

        // 封面URL
        Element coverEl = doc.selectFirst("video#player");
        if (coverEl != null) {
            page.setCoverUrl(coverEl.attr("poster"));
        }
        if (StringUtils.isEmpty(page.getCoverUrl())) {
            Element coverImg = doc.selectFirst(".video-cover img, .cover img");
            if (coverImg != null) {
                page.setCoverUrl(coverImg.absUrl("src"));
            }
        }

        // 简介
        Element introEl = doc.selectFirst("#player-div-wrapper .video-caption-text, .caption-expand, .video-introduction, .description, p.text-light");
        if (introEl != null) {
            page.setIntroduction(introEl.text().trim());
        }

        // 观看次数 + 上传时间（同一元素 .hidden-xs 内，如 "观看次数：1.6万次  2026-05-02"）
        Element metaEl = doc.selectFirst("#player-div-wrapper > div.video-details-wrapper.hidden-sm.hidden-md.hidden-lg.hidden-xl");
        if (metaEl != null) {
            String metaText = metaEl.text().trim();
            String[] split = metaText.split(" ");
            if (split.length > 1){
                // 提取观看次数
                String[] viewsStr = split[0].split("：");
                page.setViews(viewsStr.length > 1 ? viewsStr[1] : viewsStr[0]);

                // 提取上传时间
                String uploadTime = split[1];
                page.setUploadTime(uploadTime);
            }
        }

        // 视频URL列表（多分辨率）
        page.setVideoUrls(parseVideoUrls(doc));

        // 标签
        page.setTags(parseTags(doc));

        // 收藏数
        Element favEl = doc.selectFirst(".fav-count, .like-count");
        if (favEl != null) {
            try {
                page.setFavTimes(Integer.parseInt(favEl.text().trim()));
            } catch (NumberFormatException ignored) {}
        }

        // 是否已收藏
        Element favBtn = doc.selectFirst(".fav-button.active, .like-button.active");
        page.setFav(favBtn != null);

        // 作者信息
        page.setArtist(parseArtist(doc));

        // 播放列表
        page.setPlaylist(parsePlaylist(doc));

        // 相关影片
        page.setRelatedHanimes(parseRelatedHanimes(doc));

        return page;
    }

    // ======================== 评论解析 ========================

    /**
     * 从评论/回复 HTML 中解析评论列表
     * <p>解析 JSON 响应中的 comments 或 replies 字段（HTML片段）</p>
     *
     * @param html 评论 HTML 片段
     * @return 评论列表
     */
    public static List<Comment> parseComments(String html) {
        List<Comment> comments = new ArrayList<>();
        if (StringUtils.isEmpty(html)) {
            return comments;
        }

        Document doc = Jsoup.parseBodyFragment(html);

        // 每个评论以 div.report-btn-wrapper 为核心，从中提取 data-reportable-id
        Elements wrappers = doc.select("div.report-btn-wrapper");
        for (Element wrapper : wrappers) {
            Comment comment = parseSingleCommentBlock(wrapper);
            if (comment != null && comment.getId() != null) {
                comments.add(comment);
            }
        }

        return comments;
    }

    /**
     * 从单个 report-btn-wrapper 元素解析一条评论
     *
     * <p>评论 HTML 结构:</p>
     * <pre>
     * &lt;a&gt;&lt;img class="img-circle" src="..."&gt;&lt;/a&gt;          ← 头像（评论场景在 wrapper 前面）
     * &lt;div class="report-btn-wrapper"&gt;                       ← 当前元素
     *   &lt;div class="comment-index-text" style="font-size: 0.9em;"&gt;
     *     &lt;a&gt;用户名 &lt;span&gt;时间&lt;/span&gt;&lt;/a&gt;
     *   &lt;/div&gt;
     *   &lt;div class="comment-index-text" style="...font-size: 1em..."&gt;内容&lt;/div&gt;
     *   &lt;span data-reportable-id="ID"&gt;more_vert&lt;/span&gt;
     * &lt;/div&gt;
     * &lt;div id="comment-like-form-wrapper"&gt;                     ← 点赞区域（后一个兄弟元素）
     *   &lt;div&gt;&lt;span&gt;thumb_up&lt;/span&gt;&lt;span&gt;点赞数&lt;/span&gt;&lt;/div&gt;
     *   &lt;div&gt;&lt;span&gt;thumb_down&lt;/span&gt;&lt;/div&gt;
     * &lt;/div&gt;
     * </pre>
     *
     * <p>回复 HTML 结构（avatar 在 wrapper 内部）:</p>
     * <pre>
     * &lt;div class="report-btn-wrapper"&gt;
     *   &lt;a&gt;&lt;img class="img-circle" src="..."&gt;&lt;/a&gt;          ← 头像（回复场景在 wrapper 内部）
     *   &lt;div class="comment-index-text"&gt;...&lt;/div&gt;
     *   ...
     * &lt;/div&gt;
     * </pre>
     */
    private static Comment parseSingleCommentBlock(Element reportWrapper) {
        // 1. 提取评论ID（data-reportable-id）
        Element reportBtn = reportWrapper.selectFirst("[data-reportable-id]");
        if (reportBtn == null) {
            return null;
        }
        String id = reportBtn.attr("data-reportable-id");
        if (StringUtils.isEmpty(id)) {
            return null;
        }

        Comment comment = new Comment();
        comment.setId(id);

        // 2. 头像：优先在 wrapper 内部查找（回复场景），否则向前查找兄弟元素（评论场景）
        Element avatarLink = reportWrapper.selectFirst("a:has(img.img-circle)");
        if (avatarLink == null) {
            Element prev = reportWrapper.previousElementSibling();
            while (prev != null && !"a".equals(prev.tagName())) {
                prev = prev.previousElementSibling();
            }
            avatarLink = prev;
        }
        if (avatarLink != null) {
            Element img = avatarLink.selectFirst("img.img-circle");
            if (img != null) {
                comment.setAvatarUrl(getAbsUrl(img, "src"));
            }
        }

        // 3. 用户名和时间：第一个 comment-index-text > a
        Element nameTimeDiv = reportWrapper.selectFirst("div.comment-index-text");
        if (nameTimeDiv != null) {
            Element nameLink = nameTimeDiv.selectFirst("a");
            if (nameLink != null) {
                Element timeSpan = nameLink.selectFirst("span");
                if (timeSpan != null) {
                    comment.setTime(timeSpan.text().trim());
                    // ownText() 排除 span 内部的文本，只取 <a> 直接文本
                    comment.setUsername(nameLink.ownText().trim());
                } else {
                    comment.setUsername(nameLink.text().trim());
                }
            }
        }

        // 4. 内容：style 中包含 "font-size: 1em" 的 comment-index-text（评论区正文）
        Elements textDivs = reportWrapper.select("div.comment-index-text");
        for (Element div : textDivs) {
            String style = div.attr("style");
            if (style.contains("font-size: 1em") || style.contains("font-size:1em")) {
                comment.setText(div.text().trim());
                break;
            }
        }
        // 降级处理：取第二个 comment-index-text
        if (comment.getText() == null && textDivs.size() >= 2) {
            comment.setText(textDivs.get(1).text().trim());
        }

        // 5. 点赞数和踩数：在后续兄弟元素中查找
        //    评论区：report-btn-wrapper 的下一个兄弟是 comment-like-form-wrapper
        //    回复区：report-btn-wrapper 的下一个兄弟是包含点赞的普通 div
        Element sibling = reportWrapper.nextElementSibling();
        while (sibling != null) {
            String siblingId = sibling.id();
            boolean isLikeForm = (siblingId != null && siblingId.startsWith("comment-like-form-wrapper"))
                    || sibling.select("span.material-icons-outlined").size() > 0;
            if (isLikeForm) {
                // 点赞数：thumb_up 后面的 span
                Elements thumbSpans = sibling.select("span.material-icons-outlined");
                for (Element span : thumbSpans) {
                    String iconText = span.text().trim();
                    if ("thumb_up".equals(iconText)) {
                        Element parentDiv = span.parent();
                        if (parentDiv != null) {
                            Elements allSpans = parentDiv.select("span");
                            if (allSpans.size() >= 2) {
                                String likesStr = allSpans.get(1).text().trim();
                                // 过滤掉 "display:none" 的占位 0
                                String displayStyle = allSpans.get(1).attr("style");
                                if (!displayStyle.contains("display:none")) {
                                    try {
                                        comment.setLikes(Integer.parseInt(likesStr));
                                    } catch (NumberFormatException ignored) {
                                    }
                                }
                            }
                        }
                    }
                }
                // 6. 回复数：从 .load-replies-btn 中提取
                //    文本格式如 "查看 1 則回覆" 或 "查看 5 則回覆"
                Element repliesBtn = sibling.selectFirst(".load-replies-btn");
                if (repliesBtn != null) {
                    String repliesText = repliesBtn.text().trim();
                    java.util.regex.Pattern replyPattern = java.util.regex.Pattern.compile("(\\d+)\\s*則回覆");
                    java.util.regex.Matcher replyMatcher = replyPattern.matcher(repliesText);
                    if (replyMatcher.find()) {
                        try {
                            comment.setReplyCount(Integer.parseInt(replyMatcher.group(1)));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                break;
            }
            sibling = sibling.nextElementSibling();
        }

        return comment;
    }

    // ======================== 工具方法 ========================

    /**
     * 获取元素的绝对URL属性值，支持abs:前缀回退
     */
    private static String getAbsUrl(Element el, String attr) {
        String value = el.attr("abs:" + attr);
        if (StringUtils.isEmpty(value)) {
            value = el.attr(attr);
        }
        return StringUtils.isEmpty(value) ? null : value;
    }

    /**
     * 解析排序方式Modal弹窗中的选项列表
     */
    private static Map<String, String> parseSortModalOptions(Document doc, String modalSelector) {
        return parseModalOptions(doc, modalSelector, ".hentai-sort-options-wrapper");
    }

    /**
     * 解析排序方式Modal弹窗中的选项列表
     */
    private static Map<String, String> parseGenreModalOptions(Document doc, String modalSelector) {
        return parseModalOptions(doc, modalSelector, ".genre-option");
    }

    /**
     * 解析Modal弹窗中的选项列表（如类型、排序方式）
     */
    private static Map<String, String> parseModalOptions(Document doc, String modalSelector, String cssQuery) {
        Map<String, String> options = new HashMap<>();
        Element modalElem = doc.selectFirst(modalSelector + " > div > div > div.modal-body");
        if (modalElem != null) {
            Elements elems = modalElem.select(cssQuery);
            for (Element elem : elems) {
                if (elem != null) {
                    String key = elem.text().trim();
                    String value = elem.attr("data-value").trim();
                    if (StringUtils.isAnyBlank(key, value)) {
                        continue;
                    }
                    options.put(key, value);
                }
            }
        }
        return options;
    }

    public static UserPage parseUserPage(Document doc) {
        UserPage page = new UserPage();

        // 解析用户信息
        Profile profile = parseProfile(doc);
        page.setProfile(profile);

        // 解析视频列表
        List<VideoInfo> videoList = parseVideoList(doc);
        page.setVideoList(videoList);

        // 解析播放列表
        List<PlaylistItem> playlist = parsePlaylistItems(doc);
        page.setPlaylists(playlist);
        return page;
    }

    public static List<VideoInfo> parseVideoList(Document doc) {
        Element videoListElem = doc.selectFirst("#home-rows-wrapper > div.tab-content-container > div > div:nth-child(2)");
        if (videoListElem != null) {
            return parseVideoCards(videoListElem);
        }
        return Collections.emptyList();
    }

    public static Profile parseProfile(Document doc) {
        Element profileElem = doc.selectFirst("#playlist-headings-wrapper > div.profile-main-container");
        if (profileElem != null) {
            Profile profile = new Profile();
            // id
            Element idElem = profileElem.selectFirst("div.profile-content-right > div.profile-sub-stats > div.profile-sub-stats-id");
            if (idElem != null){
                String idText = idElem.text().trim().replaceAll("[^0-9]", "");
                if (!idText.isEmpty()) {
                    profile.setId(idText);
                }
            }
            // 昵称
            Element displayNameElem = profileElem.selectFirst("div.profile-content-right > h1");
            if (displayNameElem != null) {
                profile.setName(displayNameElem.text().trim());
            }

            // 头像
            Element avatarElem = profileElem.selectFirst("div.profile-avatar-wrapper > a > img");
            if (avatarElem != null) {
                profile.setAvatarUrl(avatarElem.attr("abs:src"));
            }

            // 订阅者数 & 视频数
            Element subscriberAndVideoElem = profileElem.selectFirst("div.profile-content-right > div.profile-sub-stats > div.profile-sub-stats-new-line");
            if (subscriberAndVideoElem != null) {
                String text = subscriberAndVideoElem.text().trim();
                String[] parts = text.split(" • ");
                if (parts.length == 2) {
                    String subscriberText = parts[0].replaceAll("[^0-9]", "");
                    String videoText = parts[1].replaceAll("[^0-9]", "");
                    if (!subscriberText.isEmpty()) {
                        profile.setSubscriberCount(Integer.parseInt(subscriberText));
                    }
                    if (!videoText.isEmpty()) {
                        profile.setVideoCount(Integer.parseInt(videoText));
                    }
                }
            }
            return profile;
        }
        return null;
    }

    private static Map<String, String> parseUserPageSort(Element doc) {
        Element sortGroupElem = doc.selectFirst("div.filter-button-group");
        if (sortGroupElem != null){
            Elements children = sortGroupElem.children();
            if(CollectionUtils.isNotEmpty(children)){
                Map<String, String> sortMap = new HashMap<>();
                for (Element child : children) {
                    String href = getAbsUrl(child, "href");
                    String type = extractUserPageSortType(href);
                    String text = child.text().trim();
                    sortMap.put(text, type);
                }
                return sortMap;
            }
        }
        return null;
    }

    private static String extractUserPageSortType(String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        return extractQueryParam(url, "sort");
    }

    public static UserUploadedPage parseUserUploadedPage(Document doc) {
        UserUploadedPage page = new UserUploadedPage();

        // 解析用户信息
        Profile profile = parseProfile(doc);
        page.setProfile(profile);

        // 解析排序
        Map<String, String> sort = parseUserPageSort(doc);
        page.setSort(sort);

        // 解析视频列表
        List<VideoInfo> videoList = parseVideoList(doc);
        page.setVideoList(videoList);

        // 获取分页信息
        parsePagination(doc, page);

        return page;
    }

    public static UserPlaylistsPage parseUserPlaylistsPage(Document doc) {
        UserPlaylistsPage page = new UserPlaylistsPage();

        // 解析用户信息
        Profile profile = parseProfile(doc);
        page.setProfile(profile);

        // 解析排序
        Map<String, String> sort = parseUserPageSort(doc);
        page.setSort(sort);

        // 解析播放列表
        List<PlaylistItem> playlist = parsePlaylistItems(doc);
        page.setPlaylists(playlist);

        // 获取分页信息
        parsePagination(doc, page);

        return page;
    }

    public static Playlist parsePlaylistPage(Document doc) {
        Element playlistSidebarElem = doc.selectFirst("body > div > div:nth-child(3) > div.nav-bottom-padding.playlist-page-container.page-container-padding > div > div.playlist-sidebar > div");
        if (playlistSidebarElem != null) {
            Playlist playlist = new Playlist();

            Element coverElem = playlistSidebarElem.selectFirst("div.playlist-cover-wrapper");
            if (coverElem != null) {
                Element imgElem = coverElem.selectFirst("img");
                if (imgElem != null) {
                    playlist.setCoverUrl(imgElem.attr("abs:src"));
                }

                Element listUrlElem = coverElem.selectFirst("a");
                if (listUrlElem != null) {
                    String href = listUrlElem.attr("href");
                    String listCode = extractListCode(href);
                    playlist.setListCode(listCode);
                    playlist.setListUrl(href);
                }
            }

            // 标题
            Element titleElem = playlistSidebarElem.selectFirst("div.playlist-details > h1");
            if (titleElem != null) {
                playlist.setTitle(titleElem.text().trim());
            }

            // 上传者
            Element uploaderInfoElem = playlistSidebarElem.selectFirst("div.playlist-author-info");
            if (uploaderInfoElem != null) {
                // 上传者名称 和 url
                Element uploaderNameElem = uploaderInfoElem.selectFirst("a");
                if (uploaderNameElem != null) {
                    playlist.setUploader(uploaderNameElem.text().trim());
                    String href = getAbsUrl(uploaderNameElem, "href");
                    playlist.setUploaderUrl(href);
                }
                // 上传者头像
                Element uploaderAvatarElem = uploaderInfoElem.selectFirst("img.author-avatar");
                if (uploaderAvatarElem != null) {
                    playlist.setUploaderAvatarUrl(uploaderAvatarElem.attr("abs:src"));
                }
            }


            // 视频数
            Element videoCountElem = playlistSidebarElem.selectFirst("#sidebar-video-count");
            if (videoCountElem != null) {
                playlist.setTotal(Integer.parseInt(videoCountElem.text().trim()));
            }

            // 观看数
            Element viewsElem = playlistSidebarElem.selectFirst("div.playlist-details > div.playlist-meta > p.playlist-stats");
            if (viewsElem != null) {
                String fullText = viewsElem.text().trim();
                // 格式: "播放清單 • 138 部影片 • 觀看次數：0 次", 提取 "0 次"
                Matcher m = Pattern.compile("觀看次數：([\\d,]+ 次)").matcher(fullText);
                if (m.find()) {
                    playlist.setViews(m.group(1));
                }
            }

            // 描述
            Element descriptionElem = playlistSidebarElem.selectFirst("div.playlist-details > div.playlist-meta > p.playlist-description");
            if (descriptionElem != null) {
                playlist.setDescription(descriptionElem.text().trim());
            }

            Element videoListElem = doc.selectFirst("body > div > div:nth-child(3) > div.nav-bottom-padding.playlist-page-container.page-container-padding > div > div.playlist-video-list");
            if (videoListElem != null) {
                // 排序类型
                Map<String, String> sortMap = parseUserPageSort(videoListElem);
                playlist.setSort(sortMap);

                // 影片
                List<VideoInfo> videos = parsePlaylistVideoList(videoListElem);
                playlist.setVideos(videos);
            }

            // 获取分页信息
            parsePagination(doc, playlist);

            return playlist;
        }
        return null;
    }

    /**
     * 从播放列表视频区域解析视频信息列表
     */
    private static List<VideoInfo> parsePlaylistVideoList(Element videoListElem) {
        List<VideoInfo> list = new ArrayList<>();

        Elements cards = videoListElem.select(".playlist-video-card.video-item-container");
        for (Element card : cards) {
            VideoInfo info = new VideoInfo();

            // 缩略图区域
            Element thumbContainer = card.selectFirst(".video-thumb-container .thumb-container > a[href]");
            if (thumbContainer != null) {
                // 封面图
                Element imgEl = thumbContainer.selectFirst("img.main-thumb");
                if (imgEl != null) {
                    info.setCoverUrl(getAbsUrl(imgEl, "src"));
                }

                // 时长
                Element durationEl = thumbContainer.selectFirst("div.duration");
                if (durationEl != null) {
                    info.setDuration(durationEl.text().trim());
                }

                // 统计信息（好评率 + 观看次数）
                Elements statItems = thumbContainer.select("div.stat-item");
                for (Element stat : statItems) {
                    String text = stat.text().trim();
                    if (text.contains("%")) {
                        info.setLikeRate(text.replace("thumb_up", "").trim());
                    } else if (!text.isEmpty() && (text.contains("次") || text.matches(".*\\d.*"))) {
                        info.setViews(text);
                    }
                }
            }

            // 视频信息区域
            // 标题 + 视频链接
            Element titleLinkEl = card.selectFirst("h4.video-title a[href]");
            if (titleLinkEl != null) {
                String href = getAbsUrl(titleLinkEl, "href");
                info.setVideoUrl(href);
                info.setVideoCode(extractVideoCode(href));
                info.setTitle(titleLinkEl.text().trim());
            }

            // 上传者 + 上传时间
            Element metaLinkEl = card.selectFirst("div.video-meta-data a[href]");
            if (metaLinkEl != null) {
                info.setUploaderUrl(getAbsUrl(metaLinkEl, "href"));
                String metaText = metaLinkEl.text().trim();
                String[] parts = metaText.split("\\s*•\\s*");
                if (parts.length >= 1) {
                    info.setUploader(parts[0].trim());
                }
                if (parts.length >= 2) {
                    info.setUploadTime(parts[1].trim());
                }
            }

            list.add(info);
        }
        return list;
    }

    public static void parsePagination(Element element, Pagination entity) {
        Element paginationElem = element.selectFirst("ul.pagination");
        if (paginationElem != null) {
            // 解析是否有上一页：上一页按钮存在且未被禁用
            Element prevBtn = paginationElem.selectFirst("[aria-label='pagination.previous']");
            if (prevBtn != null) {
                boolean isDisabled = prevBtn.hasClass("disabled") || "true".equals(prevBtn.attr("aria-disabled"));
                entity.setHasPrevPage(!isDisabled);
            }

            // 解析是否有下一页：下一页按钮存在且未被禁用
            Element nextBtn = paginationElem.selectFirst("[aria-label='pagination.next']");
            if (nextBtn != null) {
                boolean isDisabled = nextBtn.hasClass("disabled") || "true".equals(nextBtn.attr("aria-disabled"));
                entity.setHasNextPage(!isDisabled);
            }

            Element currentPageElem = paginationElem.selectFirst("[aria-current='page']");
            if (currentPageElem != null) {
                entity.setCurrentPage(Integer.parseInt(currentPageElem.text().trim()));
            }

            // 解析总页数：遍历所有 a.page-link，提取数字文本取最大值
            Elements pageLinks = paginationElem.select("a.page-link");
            int maxPage = 1;
            for (Element link : pageLinks) {
                String text = link.ownText().trim();
                if (text.equals("‹") || text.equals("›")) {
                    continue;
                }
                try {
                    int pageNum = Integer.parseInt(text);
                    if (pageNum > maxPage) {
                        maxPage = pageNum;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            entity.setTotalPage(maxPage);
        }
    }

    public static VideosResult parseUploadVideos(Document doc) {
        List<VideoInfo> videoInfos = parseVideoList(doc);
        VideosResult result = new VideosResult(videoInfos);
        parsePagination(doc, result);
        return result;
    }

    public static PlaylistsResult parsePlaylists(Document doc) {
        List<PlaylistItem> playlistItems = HtmlParser.parsePlaylistItems(doc);
        PlaylistsResult result = new PlaylistsResult(playlistItems);
        parsePagination(doc, result);
        return result;
    }
}
