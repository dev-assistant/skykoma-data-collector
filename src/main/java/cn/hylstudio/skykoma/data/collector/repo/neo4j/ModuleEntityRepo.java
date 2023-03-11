package cn.hylstudio.skykoma.data.collector.repo.neo4j;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.ModuleEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface ModuleEntityRepo extends Neo4jRepository<ModuleEntity, String> {
}
