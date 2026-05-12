package com.shikou.model.entities.page;

import com.shikou.model.entities.Pagination;
import com.shikou.model.entities.Profile;
import com.shikou.model.entities.VideoInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 用户上传影片页
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUploadedPage extends Pagination {
    /**
     * 用户信息
     */
    private Profile profile;

    /**
     * 排序方式
     */
    private Map<String, String> sort;

    /**
     * 影片
     */
    private List<VideoInfo> videoList;
}
