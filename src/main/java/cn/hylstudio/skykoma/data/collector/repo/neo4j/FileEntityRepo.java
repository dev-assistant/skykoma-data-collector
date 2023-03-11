package cn.hylstudio.skykoma.data.collector.repo.neo4j;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.FileEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface FileEntityRepo extends Neo4jRepository<FileEntity, String> {
    FileEntity findByRelativePath(String relativePath);
}
