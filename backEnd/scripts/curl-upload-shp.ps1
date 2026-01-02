param(
  [string]$BaseUrl = "http://localhost:8765/api/v0",
  [string]$Token = "",
  [string]$ZipPath = "",
  [string]$ParentId = "",
  [string]$TableName = "shp_upload",
  [string]$LayerName = "SHP Upload",
  [string]$Srid = "4326",
  [string]$VectorType = "polygon"
)

if ([string]::IsNullOrWhiteSpace($Token)) {
  Write-Host "Token is required. Example: -Token <jwt>"
  exit 1
}

if ([string]::IsNullOrWhiteSpace($ZipPath) -or -not (Test-Path $ZipPath)) {
  Write-Host "ZipPath is required and must exist. Example: -ZipPath C:\\data\\sample.zip"
  exit 1
}

if ([string]::IsNullOrWhiteSpace($ParentId)) {
  Write-Host "ParentId is required. Use layerNode root id from /api/v0/node/layerNode/getLayerTree"
  exit 1
}

$info = @{
  parent_id = $ParentId
  tableName = $TableName
  layerName = $LayerName
  usage = @{
    srid = $Srid
    type = $VectorType
  }
  propertyType = @{}
} | ConvertTo-Json -Compress

$tmp = New-TemporaryFile
Set-Content -Path $tmp -Value $info -NoNewline -Encoding utf8

try {
  curl.exe -X POST "$BaseUrl/admin/vector/layer/shp" `
    -H "Authorization: Bearer $Token" `
    -F "file=@$ZipPath" `
    -F "info=@$tmp;type=application/json"
} finally {
  Remove-Item $tmp -ErrorAction SilentlyContinue
}
