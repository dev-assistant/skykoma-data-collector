package cn.hylstudio.skykoma.data.collector.model.payload;

import cn.hylstudio.skykoma.data.collector.model.ProjectInfoDto;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class QueryScanPayload {
    private String scanId;
    private String relativePath;
}
