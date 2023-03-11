package cn.hylstudio.skykoma.data.collector.service;

import cn.hylstudio.skykoma.data.collector.model.ProjectDto;
import cn.hylstudio.skykoma.data.collector.model.payload.ProjectInfoQueryPayload;
import cn.hylstudio.skykoma.data.collector.model.payload.ProjectInfoUploadPayload;

public interface IBizProjectInfoService {
    ProjectDto queryProject(ProjectInfoQueryPayload payload);

    ProjectDto uploadProjectInfo(ProjectInfoUploadPayload payload);
}
