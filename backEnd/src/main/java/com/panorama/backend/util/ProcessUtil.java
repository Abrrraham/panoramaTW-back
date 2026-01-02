package com.panorama.backend.util;

import com.panorama.backend.model.node.ModelNode;
import com.panorama.backend.model.node.TaskNode;
import com.panorama.backend.model.resource.DefaultDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static String pgBinPath = "";
    private static volatile String lastProcessError = null;

    @Value("${path.pgBin:}")
    public void setPgBinPath(String pgBinPath) {
        ProcessUtil.pgBinPath = pgBinPath == null ? "" : pgBinPath.trim();
    }

    public static boolean shp2pgProcess(String shpPath, String tableName, DefaultDataSource defaultDataSource, int srid) throws IOException, InterruptedException {
        // 构建 ProcessBuilder
        ProcessBuilder processBuilder = new ProcessBuilder();

        String url = defaultDataSource.getUrl();
        String dbHost = url.split("//")[1].split(":")[0];
        String dbName = url.substring(url.lastIndexOf("/") + 1);

        String shp2pgsql = resolveExecutable("shp2pgsql");
        String psql = resolveExecutable("psql");
        if (!executableExists(shp2pgsql) || !executableExists(psql)) {
            lastProcessError = "shp2pgsql/psql not found. Set path.pgBin to Postgres bin (e.g. E:\\Geo_database\\bin).";
            log.error(lastProcessError);
            return false;
        }

        processBuilder.environment().put("PGPASSWORD", defaultDataSource.getPassword());
        String command = String.format(
                "%s -I -s %s %s %s | %s -h %s -U %s -d %s",
                quote(shp2pgsql),
                srid,
                quote(shpPath),
                quote(tableName),
                quote(psql),
                dbHost,
                defaultDataSource.getUsername(),
                dbName
        );
        processBuilder.command(sysCmdExeStr, sysLinkStr, command);
        processBuilder.redirectErrorStream(true);

        // 启动进程
        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        // 读取命令行输出
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
                log.info(line); // 输出命令行结果
            }
        }

        // 等待进程结束
        int exitCode = process.waitFor();
        if (exitCode == 0) {
            lastProcessError = null;
            return true;
        }
        lastProcessError = String.format("shp2pgsql exit code %s. Output: %s", exitCode, output);
        log.error(lastProcessError);
        return false;
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

    public static String getLastProcessError() {
        return lastProcessError;
    }

    private static String resolveExecutable(String name) {
        String base = pgBinPath;
        if (base == null || base.isBlank()) {
            String env = System.getenv("PG_BIN");
            if (env != null && !env.isBlank()) {
                base = env.trim();
            }
        }
        String exeName = isWindows() ? name + ".exe" : name;
        if (base != null && !base.isBlank()) {
            return Paths.get(base, exeName).toString();
        }
        return exeName;
    }

    private static boolean executableExists(String exePath) {
        if (exePath == null || exePath.isBlank()) {
            return false;
        }
        boolean looksLikePath = exePath.contains("\\") || exePath.contains("/") || exePath.contains(":");
        if (!looksLikePath) {
            return true;
        }
        return Files.exists(Paths.get(exePath));
    }

    private static String quote(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        if (input.startsWith("\"") && input.endsWith("\"")) {
            return input;
        }
        return "\"" + input + "\"";
    }

    private static boolean isWindows() {
        return System.getProperties().getProperty("os.name").toLowerCase().contains("win");
    }
}
