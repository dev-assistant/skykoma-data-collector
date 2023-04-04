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
public class ClassEntity {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;
    @Property
    private String qualifiedName;
    @Property
    private String canonicalText;
    @Property
    private Boolean isInterface;
    @Property
    private List<String> superTypeCanonicalTexts;
    @Relationship(type = "HAS_PARENT", direction = Relationship.Direction.OUTGOING)
    private List<ClassEntityParentRel> parents;
    @Property
    private List<String> extendsCanonicalTextsList;
    @Property
    private List<String> extendsSuperTypeCanonicalTextsList;
    @Property
    private List<String> implementsCanonicalTextsList;
    @Property
    private List<String> implementsSuperTypeCanonicalTextsList;
    @CreatedDate
    @Property
    private Long createdAt;
    @LastModifiedDate
    @Property
    private Long updatedAt;

    public ClassEntity() {

    }

}