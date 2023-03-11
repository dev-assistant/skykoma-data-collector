package cn.hylstudio.skykoma.data.collector.entity.neo4j;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.*;

@Data
@RelationshipProperties
public class ModuleRootRel {
    @RelationshipId
    private Long id;
    @TargetNode
    private FileEntity root;
    private String type;//src testSrc resources testResources
    @CreatedDate
    @Property
    private Long createdAt;

    @LastModifiedDate
    @Property
    private Long updatedAt;

    public ModuleRootRel(FileEntity root, String type) {
        this.root = root;
        this.type = type;
    }
}
