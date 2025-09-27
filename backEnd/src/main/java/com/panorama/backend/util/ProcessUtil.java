package com.panorama.backend.util;

import com.panorama.backend.model.node.ModelNode;
import com.panorama.backend.model.node.TaskNode;
import com.panorama.backend.model.resource.DefaultDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-24 15:35:53
 * @version: 1.0
 */
@Slf4j
@Component
public class ProcessUtil {

    static String condaStr = "conda activate ";
    static String sysCmdExeStr = (System.getProperties().getProperty("os.name").toLowerCase().contains("win"))? "cmd.exe":"bash";
    static String sysLinkStr = (System.getProperties().getProperty("os.name").toLowerCase().contains("win"))? "/c":"-c";
    static String sysDeleteFileStr = (System.getProperties().getProperty("os.name").toLowerCase().contains("win"))? "del":"rm -f";
    static String sysDeleteDirectoryStr = (System.getProperties().getProperty("os.name").toLowerCase().contains("win"))? "rmdir /s /q":"rm -rf";

    public static boolean shp2pgProcess(String shpPath, String tableName, DefaultDataSource defaultDataSource, int srid) throws IOException, InterruptedException {
        // 构建 ProcessBuilder
        ProcessBuilder processBuilder = new ProcessBuilder();

        String url = defaultDataSource.getUrl();
        String dbHost = url.split("//")[1].split(":")[0];
        String dbName = url.substring(url.lastIndexOf("/") + 1);

        processBuilder.environment().put("PGPASSWORD", defaultDataSource.getPassword());
        processBuilder.command(
                sysCmdExeStr, sysLinkStr,
                String.format(
                        "shp2pgsql -I -s %s %s %s | psql -h %s -U %s -d %s"
                        , srid, shpPath, tableName, dbHost, defaultDataSource.getUsername(), dbName
                )
        );

        // 启动进程
        Process process = processBuilder.start();

        // 读取命令行输出
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line); // 输出命令行结果
            }
        }

        // 等待进程结束
        int exitCode = process.waitFor();
        return exitCode == 0;
    }

    public static Process buildSystemProcess(TaskNode taskNode) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            ModelNode modelNode = taskNode.getModelNode();
            List<String> paramKeys = modelNode.getParamKey();
            String modelName = modelNode.getName();
            List<String> commands = new ArrayList<>();
            commands.add(sysCmdExeStr);
            commands.add(sysLinkStr);
            if (modelName.equals("deleteFile")){
                commands.addAll(Arrays.asList(sysDeleteFileStr.split(" ")));
            }else if (modelName.equals("deleteDirectory")){
                commands.addAll(Arrays.asList(sysDeleteDirectoryStr.split(" ")));
            }
            for (String paramKey : paramKeys) {
                commands.add(taskNode.getParams().get(paramKey));
            }
            processBuilder.command(commands);
            return processBuilder.start();
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public static Process buildModelProcess(TaskNode taskNode) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            ModelNode modelNode = taskNode.getModelNode();
            List<String> paramKeys = modelNode.getParamKey();
            List<String> commands = new ArrayList<>();
            commands.add(sysCmdExeStr);
            commands.add(sysLinkStr);
            commands.add(condaStr + modelNode.getCondaEnv() + " &&");
            commands.add(modelNode.getExePrefix());
            commands.add(modelNode.getProgram());
            for (String paramKey : paramKeys) {
                commands.add(taskNode.getParams().get(paramKey));
            }
            processBuilder.command(commands);
            return processBuilder.start();
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }
}
