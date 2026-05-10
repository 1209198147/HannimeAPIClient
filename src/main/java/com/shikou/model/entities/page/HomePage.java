package com.shikou.model.entities.page;

import com.shikou.model.entities.Banner;
import com.shikou.model.entities.HomePageSection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 首页数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomePage {

    /** 轮播Banner */
    private Banner banner;

    /** 首页各个部分的视频信息 */
    private List<HomePageSection> sections;
}
