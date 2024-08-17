package cn.hylstudio.skykoma.data.collector.service;

import cn.hylstudio.skykoma.data.collector.model.ProjectInfoDto;
import cn.hylstudio.skykoma.data.collector.model.ScanRecordDto;
import cn.hylstudio.skykoma.data.collector.model.payload.*;

public interface IBizProjectInfoService {
    ProjectInfoDto queryProject(ProjectInfoQueryPayload payload);

    void updateProjectBasicInfoAsync(ProjectInfoUploadPayload payload);

    void updateProjectFileInfoAsync(ProjectFileInfoUploadPayload payload);

    ScanRecordDto queryScan(QueryScanPayload payload);
    ScanRecordDto beginScan(BeginScanPayload payload);
    ScanRecordDto updateScanStatus(UpdateScanStatusPayload payload);

}
