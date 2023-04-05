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
    private String psiType; //Class Annotation unknown
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
    private String qualifiedName;//psiClass, psiAnnotation
//    @Relationship(type = "IS_CLASS", direction = Relationship.Direction.OUTGOING)
//    private ClassEntity classInfo;
    // @Relationship(type = "HAS_ANNOTATION", direction = Relationship.Direction.OUTGOING)
    // private List<AnnotationEntity> annotaions;
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

}