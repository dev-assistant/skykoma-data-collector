package cn.hylstudio.skykoma.data.collector.entity.neo4j;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.*;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassEntityParentRel that = (ClassEntityParentRel) o;
        return parent.equals(that.parent) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, type);
    }
}
