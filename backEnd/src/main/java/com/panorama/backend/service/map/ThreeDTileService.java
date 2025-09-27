package com.panorama.backend.service.map;

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
import com.panorama.backend.service.resource.AsyncTaskService;
import com.panorama.backend.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: DMK
 * @description:
 * @date: 2024-10-19 10:39:17
 * @version: 1.0
 */
@Service
@Slf4j
public class ThreeDTileService {

    @Value("${path.temp}")
    private String temp;

    @Value("${path.3DTile}")
    private String tilePath;

    private LayerNodeService layerNodeService;
    private TaskNodeService taskNodeService;
    private ModelNodeService modelNodeService;
    private AsyncTaskService asyncTaskService;

    @Autowired
    public void setThreeDTileService(LayerNodeService layerNodeService, TaskNodeService taskNodeService, ModelNodeService modelNodeService, AsyncTaskService asyncTaskService) {
        this.layerNodeService = layerNodeService;
        this.taskNodeService = taskNodeService;
        this.modelNodeService = modelNodeService;
        this.asyncTaskService = asyncTaskService;
    }

    public GeneralResult update3DTileLayer(LayerNode layerNode, InfoDTO infoDTO){
        if (layerNodeService.updateLayer(layerNode, infoDTO)){
            return GeneralResult.builder().status(GenerateResultStatus.SUCCESS).message("update 3DTile layer successfully").build();
        }else{
            return GeneralResult.builder().status(GenerateResultStatus.ERROR).message("failed to update 3DTile layer").build();
        }
    }

    public GeneralResult delete3DTileLayer(LayerNode layerNode){

        String url = layerNode.getDataSource().get("url");
        TaskNode taskNode = TaskNode.builder().status(TaskStatus.NONE).layerNode(layerNode).type(TaskType.DELETE).build();
        ModelNode modelNode;
        String taskNodeId = taskNodeService.saveTaskNode(taskNode);
        Map<String, String> params = new HashMap<>();

        try {
            String tableName = layerNode.getTableName();
            String path = String.join(File.separator, url, tableName);
            params.put("path", path);
            modelNode = modelNodeService.getModelNodeByName("deleteDirectory");
            taskNode.setParams(params);
            taskNode.setModelNode(modelNode);
            taskNodeService.saveTaskNode(taskNode);
            asyncTaskService.systemTaskAsync(taskNodeId);
            return GeneralResult.builder().status(GenerateResultStatus.RUNNING).message(taskNodeId).build();
        } catch (Exception e) {
            return GeneralResult.builder().status(GenerateResultStatus.ERROR).message("failed to delete 3DTile layer").build();
        }
    }

    public GeneralResult upload3DTileLayer(LayerNode parentNode, MultipartFile multipartFile, InfoDTO infoDTO) {
        try {
            //M2F
            File zipFile = FileUtil.convertMultipartFileToFile(multipartFile, temp);
            String sourcePath = zipFile.getParent();
            //解压
            FileUtil.unZipFiles(zipFile, sourcePath);
            boolean result = zipFile.delete();
            if (!result){
                log.error("failed to delete zip file");
            }
            //找到待上传文件
            String tilesetPath = FileUtil.findFileWithExtension(sourcePath, "json");
            if (tilesetPath.isEmpty()){
                return GeneralResult.builder().status("error").message("tileset not found").build();
            }else {
                String tableName = infoDTO.getTableName();
                long sameCount = FileUtil.countFilesWithPrefix(tilePath, tableName);
                String uniqueTableName;
                if (sameCount == 0){
                    uniqueTableName = tableName;
                }else {
                    uniqueTableName = tableName + "_" + sameCount;
                }
                Map<String, String> dataSourceMap = new HashMap<>();
                dataSourceMap.put("url", tilePath);
                Map<String, String> usage = new HashMap<>();
                usage.put("filename", tilesetPath.substring(tilesetPath.lastIndexOf("/") + 1));
                LayerNode layerNode = LayerNode.builder()
                        .tableName(uniqueTableName).layerName(infoDTO.getLayerName())
                        .category("3DTile").usage(usage)
                        .path(layerNodeService.getNodePath(parentNode)).dataSource(dataSourceMap)
                        .build();
                //移动并重命名
                String destinationPath = String.join(File.separator, tilePath, uniqueTableName);
                FileUtil.moveFile(sourcePath, destinationPath);
                layerNodeService.saveLayerNode(layerNode);

                return GeneralResult.builder().status(GenerateResultStatus.SUCCESS).message("upload 3DTile layer successfully").build();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return GeneralResult.builder().status(GenerateResultStatus.ERROR).message("failed to upload 3DTile layer").build();
        }
    }

}
