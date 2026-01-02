# panoramaTW

## Layer Upload (Admin)

- Vector: GeoJSON (`.json`/`.geojson`) and Shapefile zip (`.zip` containing `.shp/.shx/.dbf`, `.prj` recommended).
- Raster: GeoTIFF (`.tif`/`.tiff`) or zip containing GeoTIFF.
- Size limit: 1GB (`spring.servlet.multipart.*` in `backEnd/src/main/resources/application-dmk.yml` and `MultipartConfig`).
- PostGIS: required for vector upload and MVT rendering. Run `backEnd/src/main/resources/db/001_enable_postgis.sql` on the Postgres database.
- Paths: `path.temp`, `path.rasterTile`, `path.rasterFile`, `path.static`, `path.3DTile` in `backEnd/src/main/resources/application-dmk.yml`.
- SRID: GeoJSON defaults to 4326; Shapefile uses `.prj` if present, otherwise `srid` must be provided.
- Raster tiles: `usage.type` controls tile naming (`land_gdal` = `z/x/y.png`, `land` = `x-y-z.png`).
