package cn.hylstudio.skykoma.data.collector.service;

import cn.hylstudio.skykoma.data.collector.repo.neo4j.ProjectEntityRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

public interface IBizDataAnalyzeService {
    void triggerAnalyzeAsync(String scanId);
}
