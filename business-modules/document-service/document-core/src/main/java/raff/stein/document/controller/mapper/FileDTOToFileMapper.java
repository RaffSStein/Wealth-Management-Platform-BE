package raff.stein.document.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.openapitools.model.FileDTO;
import org.springframework.web.multipart.MultipartFile;
import raff.stein.document.model.File;

@Mapper(config = DocumentControllerCommonMapperConfig.class)
public interface FileDTOToFileMapper {

    FileDTOToFileMapper MAPPER = Mappers.getMapper(FileDTOToFileMapper.class);

    FileDTO toFileDTO(File file);

    @Mapping(target = "multipartFile", source = "multipartFile")
    File toFile(FileDTO fileDTO, MultipartFile multipartFile);

    @Mapping(target = "multipartFile", source = "multipartFile")
    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    File toNewFile(FileDTO fileDTO, MultipartFile multipartFile);

}
