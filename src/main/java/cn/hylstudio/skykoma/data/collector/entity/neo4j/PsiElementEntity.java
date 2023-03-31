package cn.hylstudio.skykoma.data.collector.entity.neo4j;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.*;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Node
@Data
public class PsiElementEntity {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;
    @Property
    private String className;
    @Property
    private String originText;
    @Property
    private Integer startOffset;
    @Property
    private Integer endOffset;
    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    private List<PsiElementEntity> childElements;
    @Property
    private String qualifiedName;//psiClass,psiAnnotation
    @Relationship(type = "HAS_ATTR", direction = Relationship.Direction.OUTGOING)
    private List<AnnotationAttrEntity> attrs;
    @CreatedDate
    @Property
    private Long createdAt;
    @LastModifiedDate
    @Property
    private Long updatedAt;

    public PsiElementEntity() {

    }

    public PsiElementEntity(JsonElement psiElement) {
        JsonObject v = psiElement.getAsJsonObject();
        JsonArray childElements = v.get("childElements").getAsJsonArray();
        this.childElements = Collections.emptyList();
        int childSize = 0;
        if (childElements != null && childElements.size() > 0) {
            childSize = childElements.size();
            this.childElements = new ArrayList<>(childSize);
            for (JsonElement childElement : childElements) {
                this.childElements.add(new PsiElementEntity(childElement));
            }
        }
        this.className = v.get("className").getAsString();
        this.startOffset = v.get("startOffset").getAsInt();
        this.endOffset = v.get("endOffset").getAsInt();
        if (childSize <= 0) {
            this.originText = v.get("originText").getAsString();
        }
    }

}