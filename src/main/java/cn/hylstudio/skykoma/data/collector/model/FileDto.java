package cn.hylstudio.skykoma.data.collector.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class FileDto {
    private String name;
    private String type;//file folder
    private String relativePath;
    private List<FileDto> subFiles;

}
