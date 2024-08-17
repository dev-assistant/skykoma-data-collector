package cn.hylstudio.skykoma.data.collector.repo.neo4j;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.ApiEndpointEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface ApiEndpointEntityRepo extends Neo4jRepository<ApiEndpointEntity, String> {
}
