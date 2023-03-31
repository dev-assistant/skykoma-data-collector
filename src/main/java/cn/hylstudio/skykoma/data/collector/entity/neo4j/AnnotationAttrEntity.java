package cn.hylstudio.skykoma.data.collector.entity.neo4j;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.util.ArrayList;
import java.util.List;

@Node
@Data
public class AnnotationAttrEntity {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;
    @Property
    private String name;
    @Property
    private List<String> values;
    @CreatedDate
    @Property
    private Long createdAt;
    @LastModifiedDate
    @Property
    private Long updatedAt;

    public AnnotationAttrEntity() {
    }

    public AnnotationAttrEntity(JsonElement attribute) {
        JsonObject attrObj = attribute.getAsJsonObject();
        this.name = attrObj.get("name").getAsString();;
        JsonArray values = attrObj.get("values").getAsJsonArray();
        this.values = new ArrayList<>(values.size());
        for (JsonElement value : values) {
            this.values.add(value.getAsString());
        }
    }
}
