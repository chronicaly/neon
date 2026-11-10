param([int]$Start=0,[int]$Count=12,[string]$Output="$PSScriptRoot/../runtime/quality-sheet-$Start.png")
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$folder=Join-Path $PSScriptRoot '../runtime/smoke/game/screenshots'
$files=@(Get-ChildItem -LiteralPath $folder -Filter 'neon-quality-page-*.png' | Where-Object {
    $_.Name -match '^neon-quality-page-(\d+)-' -and [int]$Matches[1] -ge $Start -and [int]$Matches[1] -lt $Start+$Count
} | Group-Object { [regex]::Match($_.Name,'^neon-quality-page-(\d+)-').Groups[1].Value } | ForEach-Object { $_.Group | Sort-Object LastWriteTime -Descending | Select-Object -First 1 } | Sort-Object Name)
# Native-size evidence, not a mock-up. The audit filenames identify GUI scale and window size.
$cellWidth=598; $cellHeight=396; $columns=3; $rows=[Math]::Ceiling($files.Count/3)
$sheet=[Drawing.Bitmap]::new($columns*$cellWidth,$rows*$cellHeight)
$graphics=[Drawing.Graphics]::FromImage($sheet); $graphics.Clear([Drawing.Color]::FromArgb(32,36,42))
$font=[Drawing.Font]::new('Segoe UI',9)
try {
    for($i=0;$i -lt $files.Count;$i++) {
        $x=($i%3)*$cellWidth+8; $y=[int][Math]::Floor($i/3)*$cellHeight+22
        $source=[Drawing.Image]::FromFile($files[$i].FullName)
        try { $graphics.DrawImageUnscaled($source,$x,$y) } finally { $source.Dispose() }
        $graphics.DrawString($files[$i].Name.Replace('neon-quality-page-',''),$font,[Drawing.Brushes]::White,$x,$y-19)
    }
    $sheet.Save([IO.Path]::GetFullPath($Output),[Drawing.Imaging.ImageFormat]::Png)
} finally { $font.Dispose(); $graphics.Dispose(); $sheet.Dispose() }
Write-Output ([IO.Path]::GetFullPath($Output))
