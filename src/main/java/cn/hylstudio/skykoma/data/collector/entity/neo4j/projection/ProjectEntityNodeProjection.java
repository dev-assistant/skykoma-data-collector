package cn.hylstudio.skykoma.data.collector.entity.neo4j.projection;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.ProjectEntity;
import lombok.Data;

@Data
public class ProjectEntityNodeProjection {

    private final String id;
    private final String key;
    private final String name;
    private final Long createdAt;
    private final Long updatedAt;

    public ProjectEntityNodeProjection(String id, String key, String name, Long createdAt, Long updatedAt) {
        this.id = id;
        this.key = key;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

}
