package cn.hylstudio.skykoma.data.collector.entity.neo4j;

import cn.hylstudio.skykoma.data.collector.model.FileDto;
import cn.hylstudio.skykoma.data.collector.model.FileEntityDto;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.*;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Node
@Data
public class FileEntity {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;
    private String name;
    private String type;//file folder
    private String relativePath;
    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    private List<FileEntity> subFiles;
    @CreatedDate
    @Property
    private Long createdAt;

    @LastModifiedDate
    @Property
    private Long updatedAt;

    public FileEntity() {

    }
    public FileEntity(FileDto rootFolder) {
        // -------------- generated by skykoma begin --------------
        this.name = rootFolder.getName();
        this.type = rootFolder.getType();
        this.relativePath = rootFolder.getRelativePath();
        List<FileDto> subFiles = rootFolder.getSubFiles();
        if (!CollectionUtils.isEmpty(subFiles)) {
            this.subFiles = subFiles.stream().map(FileEntity::new).collect(Collectors.toList());
        }else{
            this.subFiles = Collections.emptyList();
        }
        // -------------- generated by skykoma end --------------

    }

}