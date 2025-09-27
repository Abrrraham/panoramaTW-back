package com.panorama.backend.service.resource;

import com.panorama.backend.DTO.InfoDTO;
import com.panorama.backend.model.Constant.GenerateResultStatus;
import com.panorama.backend.model.Constant.TaskStatus;
import com.panorama.backend.model.Constant.TaskType;
import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.model.node.ModelNode;
import com.panorama.backend.model.node.TaskNode;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.service.node.LayerNodeService;
import com.panorama.backend.service.node.ModelNodeService;
import com.panorama.backend.service.node.TaskNodeService;
import com.panorama.backend.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-27 17:06:36
 * @version: 1.0
 */
@Service
@Slf4j
public class StaticFileService {
    @Value("${path.temp}")
    private String temp;

    @Value("${path.static}")
    private String staticPath;

    private LayerNodeService layerNodeService;
    private TaskNodeService taskNodeService;
    private ModelNodeService modelNodeService;
    private AsyncTaskService asyncTaskService;

    @Autowired
    public void setStaticFileService(LayerNodeService layerNodeService, TaskNodeService taskNodeService, ModelNodeService modelNodeService, AsyncTaskService asyncTaskService) {
        this.layerNodeService = layerNodeService;
        this.taskNodeService = taskNodeService;
        this.modelNodeService = modelNodeService;
        this.asyncTaskService = asyncTaskService;
    }

    public byte[] getStaticFileByte(LayerNode layerNode) throws IOException {
        String url = layerNode.getDataSource().get("url");
        String suffix = layerNode.getUsage().get("type");
        String staticFilePath = String.join(File.separator, url, layerNode.getTableName() + "." + suffix);
        File file = new File(staticFilePath);
        return Files.readAllBytes(file.toPath());
    }

    public GeneralResult updateStaticLayer(LayerNode layerNode, InfoDTO infoDTO){
        if (layerNodeService.updateLayer(layerNode, infoDTO)){
            return GeneralResult.builder().status(GenerateResultStatus.SUCCESS).message("update static layer successfully").build();
        }else{
            return GeneralResult.builder().status(GenerateResultStatus.ERROR).message("failed to update static layer").build();
        }
    }

    public GeneralResult deleteStaticLayer(LayerNode layerNode){
        TaskNode taskNode = TaskNode.builder().status(TaskStatus.NONE).layerNode(layerNode).type(TaskType.DELETE).build();
        ModelNode modelNode;
        String taskNodeId = taskNodeService.saveTaskNode(taskNode);
        Map<String, String> params = new HashMap<>();
        try {
            String url = layerNode.getDataSource().get("url");
            String tableName = layerNode.getTableName();
            String suffix = layerNode.getUsage().get("type");
            String path = String.join(File.separator, url, tableName + "." + suffix);
            params.put("path", path);
            modelNode = modelNodeService.getModelNodeByName("deleteFile");
            taskNode.setParams(params);
            taskNode.setModelNode(modelNode);
            taskNodeService.saveTaskNode(taskNode);
            asyncTaskService.systemTaskAsync(taskNodeId);
            return GeneralResult.builder().status(GenerateResultStatus.RUNNING).message(taskNodeId).build();
        } catch (Exception e) {
            return GeneralResult.builder().status(GenerateResultStatus.ERROR).message("failed to delete static layer").build();
        }
    }

    public GeneralResult uploadStaticLayer(LayerNode parentNode, MultipartFile multipartFile, InfoDTO infoDTO) {
        try {
            //M2F
            File zipFile = FileUtil.convertMultipartFileToFile(multipartFile, temp);
            String parentPath = zipFile.getParent();
            //解压
            List<String> list = FileUtil.unZipFiles(zipFile, parentPath);
            boolean result = zipFile.delete();
            if (!result){
                log.error("failed to delete zip file");
            }
            //找到待上传文件
            String type = infoDTO.getUsage().get("type");
            String sourcePath = FileUtil.findFileWithExtension(list, type);
            if (sourcePath.isEmpty()){
                return GeneralResult.builder().status("error").message("file not found").build();
            }else {
                String path = String.join(File.separator, staticPath, type);
                String tableName = infoDTO.getTableName();
                long sameCount = FileUtil.countFilesWithPrefix(path, tableName);
                String uniqueTableName;
                if (sameCount == 0){
                    uniqueTableName = tableName;
                }else {
                    uniqueTableName = tableName + "_" + sameCount;
                }
                Map<String, String> dataSourceMap = new HashMap<>();
                dataSourceMap.put("url", path);
                LayerNode layerNode = LayerNode.builder()
                        .tableName(uniqueTableName).layerName(infoDTO.getLayerName())
                        .category("static").usage(infoDTO.getUsage())
                        .path(layerNodeService.getNodePath(parentNode)).dataSource(dataSourceMap)
                        .build();
                //移动并重命名
                String destinationPath = String.join(File.separator, path, uniqueTableName + "." + type);
                FileUtil.moveFile(sourcePath, destinationPath);
                FileUtil.deleteDirectory(Path.of(parentPath));
                layerNodeService.saveLayerNode(layerNode);

                return GeneralResult.builder().status(GenerateResultStatus.SUCCESS).message("upload static layer successfully").build();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return GeneralResult.builder().status(GenerateResultStatus.ERROR).message("failed to upload static layer").build();
        }
    }
}
