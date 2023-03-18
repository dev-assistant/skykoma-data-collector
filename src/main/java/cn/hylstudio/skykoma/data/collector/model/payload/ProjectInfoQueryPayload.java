package cn.hylstudio.skykoma.data.collector.model.payload;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProjectInfoQueryPayload {
    private String key;
    private String name;
    private Boolean createIfNotExists;
}
