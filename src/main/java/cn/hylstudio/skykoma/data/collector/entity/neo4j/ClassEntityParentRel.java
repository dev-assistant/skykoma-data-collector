package cn.hylstudio.skykoma.data.collector.entity.neo4j;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.*;

@Data
@RelationshipProperties
public class ClassEntityParentRel {
    @RelationshipId
    private Long id;
    @TargetNode
    private ClassEntity parent;
    @Property
    private String type;//extend implement
    @CreatedDate
    @Property
    private Long createdAt;

    @LastModifiedDate
    @Property
    private Long updatedAt;

    public ClassEntityParentRel() {
    }

    public ClassEntityParentRel(ClassEntity parent, String type) {
        this.parent = parent;
        this.type = type;
    }
}
