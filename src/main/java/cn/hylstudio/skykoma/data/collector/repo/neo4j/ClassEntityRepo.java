package cn.hylstudio.skykoma.data.collector.repo.neo4j;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.ClassEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.List;

public interface ClassEntityRepo extends Neo4jRepository<ClassEntity, String> {
    List<ClassEntity> findByScanId(String scanId);
}
