package com.shikou.model.entities.page;

import com.shikou.model.entities.Pagination;
import com.shikou.model.entities.VideoInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 搜索页面
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchPage extends Pagination {
    List<String> genres;
    Map<String, List<String>> tagsMap;
    List<String> sortTypes;
    Map<String, String> dates = Map.of(
            "全部", "",
            "过去 24 小时", "過去+24+小時",
            "过去 2 天", "過去+2+天",
            "过去 1 周", "過去+1+週",
            "过去 1 个月", "過去+1+個月",
            "过去 3 个月", "過去+3+個月",
            "过去 1 年", "過去+1+年",
            "其他", "%s+年+%s+月"
    );
    Map<String, String> durations = Map.of(
            "全部", "",
            "1分钟+", "1+分鐘+%2B",
            "5分钟+", "5+分鐘+%2B",
            "10分钟+", "10+分鐘+%2B",
            "20分钟+", "20+分鐘+%2B",
            "30分钟+", "30+分鐘+%2B",
            "60分钟+", "60+分鐘+%2B",
            "0-10分钟", "0+-+10+分鐘",
            "0-20分钟", "0+-+20+分鐘"
    );

    List<VideoInfo> videos;
}
