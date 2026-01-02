package com.panorama.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

@Slf4j
public class RasterUtil {

    private RasterUtil() {}

    public static double[] readGeoTiffBBox(String tifPath) {
        if (tifPath == null || tifPath.isBlank()) {
            return null;
        }
        File file = new File(tifPath);
        if (!file.exists()) {
            return null;
        }
        GeoTiffReader reader = null;
        try {
            reader = new GeoTiffReader(file);
            GridCoverage2D coverage = reader.read(null);
            Envelope2D env = coverage.getEnvelope2D();
            CoordinateReferenceSystem source = coverage.getCoordinateReferenceSystem2D();
            CoordinateReferenceSystem target = CRS.decode("EPSG:4326");
            double minX = env.getMinX();
            double minY = env.getMinY();
            double maxX = env.getMaxX();
            double maxY = env.getMaxY();
            if (source != null && !CRS.equalsIgnoreMetadata(source, target)) {
                MathTransform transform = CRS.findMathTransform(source, target, true);
                double[] ll = new double[]{minX, minY};
                double[] ur = new double[]{maxX, maxY};
                double[] llOut = new double[2];
                double[] urOut = new double[2];
                transform.transform(ll, 0, llOut, 0, 1);
                transform.transform(ur, 0, urOut, 0, 1);
                minX = Math.min(llOut[0], urOut[0]);
                minY = Math.min(llOut[1], urOut[1]);
                maxX = Math.max(llOut[0], urOut[0]);
                maxY = Math.max(llOut[1], urOut[1]);
            }
            return new double[]{minX, minY, maxX, maxY};
        } catch (Exception e) {
            log.warn("Failed to read GeoTIFF bbox: {}", e.getMessage());
            return null;
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

    public static String formatBBox(double[] bbox) {
        if (bbox == null || bbox.length != 4) {
            return null;
        }
        return String.format(Locale.ROOT, "[%.8f,%.8f,%.8f,%.8f]", bbox[0], bbox[1], bbox[2], bbox[3]);
    }

    public static String detectTileType(String outputPath) {
        if (outputPath == null || outputPath.isBlank()) {
            return null;
        }
        Path root = Paths.get(outputPath);
        if (!Files.isDirectory(root)) {
            return null;
        }
        if (hasDashTiles(root)) {
            return "land";
        }
        if (hasZxyTiles(root)) {
            return "land_gdal";
        }
        return null;
    }

    private static boolean hasDashTiles(Path root) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) {
                    String name = p.getFileName().toString();
                    if (name.matches("\\d+-\\d+-\\d+\\.png")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Tile pattern scan failed: {}", e.getMessage());
        }
        return false;
    }

    private static boolean hasZxyTiles(Path root) {
        try (DirectoryStream<Path> zDirs = Files.newDirectoryStream(root)) {
            for (Path zDir : zDirs) {
                if (!Files.isDirectory(zDir) || !zDir.getFileName().toString().matches("\\d+")) {
                    continue;
                }
                try (DirectoryStream<Path> xDirs = Files.newDirectoryStream(zDir)) {
                    for (Path xDir : xDirs) {
                        if (!Files.isDirectory(xDir) || !xDir.getFileName().toString().matches("\\d+")) {
                            continue;
                        }
                        try (DirectoryStream<Path> yFiles = Files.newDirectoryStream(xDir)) {
                            for (Path yFile : yFiles) {
                                if (Files.isRegularFile(yFile) && yFile.getFileName().toString().matches("\\d+\\.png")) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("ZXY tile scan failed: {}", e.getMessage());
        }
        return false;
    }
}
