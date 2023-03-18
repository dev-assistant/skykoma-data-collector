package cn.hylstudio.skykoma.data.collector.repo.neo4j;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.VCSEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface VcsEntityRepo extends Neo4jRepository<VCSEntity, String> {
    VCSEntity findByPath(String path);
}
