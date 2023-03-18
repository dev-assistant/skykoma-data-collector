package cn.hylstudio.skykoma.data.collector.service;

import cn.hylstudio.skykoma.data.collector.model.ProjectInfoDto;
import cn.hylstudio.skykoma.data.collector.model.payload.ProjectInfoQueryPayload;
import cn.hylstudio.skykoma.data.collector.model.payload.ProjectInfoUploadPayload;

public interface IBizProjectInfoService {
    ProjectInfoDto queryProject(ProjectInfoQueryPayload payload);

    ProjectInfoDto uploadProjectInfo(ProjectInfoUploadPayload payload);
}
