package cn.hylstudio.skykoma.data.collector.service.impl;

import cn.hylstudio.skykoma.data.collector.repo.neo4j.ProjectEntityRepo;
import cn.hylstudio.skykoma.data.collector.service.IBizDataAnalyzeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class BizDataAnalyzeServiceImpl implements IBizDataAnalyzeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BizDataAnalyzeServiceImpl.class);

    @Autowired
    private ProjectEntityRepo projectEntityRepo;

    @Async
    @Override
    public void triggerAnalyzeAsync(String scanId) {
        LOGGER.info("triggerAnalyzeAsync, scanId = [{}]", scanId);
        projectEntityRepo.symbolLinkModuleRootToFileTree(scanId);
    }
}
