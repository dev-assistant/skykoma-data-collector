package cn.hylstudio.skykoma.data.collector.entity.neo4j;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.util.Objects;

@Data
@RelationshipProperties
public class ClassEntityReferRel {
    @RelationshipId
    private Long id;
    @TargetNode
    private PsiElementEntity source;//incoming relation
    @Property
    private String type;//declare refer
    @CreatedDate
    @Property
    private Long createdAt;

    @LastModifiedDate
    @Property
    private Long updatedAt;

    public ClassEntityReferRel() {
    }

    public ClassEntityReferRel(PsiElementEntity source, String type) {
        this.source = source;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassEntityReferRel that = (ClassEntityReferRel) o;
        return source.equals(that.source) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, type);
    }
}
