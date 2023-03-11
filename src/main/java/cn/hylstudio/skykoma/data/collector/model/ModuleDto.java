package cn.hylstudio.skykoma.data.collector.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ModuleDto {
    private Module module;
    private String name;
    private List<FileDto> srcRoots;
    private List<FileDto> testSrcRoots;
    private List<FileDto> resRoots;
    private List<FileDto> testResRoots;

    public ModuleDto(Module module) {
        this.module = module;
    }
}
