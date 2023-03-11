package cn.hylstudio.skykoma.data.collector.repo.neo4j;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.ProjectEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface ProjectEntityRepo extends Neo4jRepository<ProjectEntity, String> {
    ProjectEntity findByName(String projectName);
}
