param(
    [Parameter(Mandatory)][string]$Before,
    [Parameter(Mandatory)][string]$After,
    [string]$Output = "$PSScriptRoot/../runtime/quality-measurements.md"
)
$ErrorActionPreference = 'Stop'
$culture = [Globalization.CultureInfo]::InvariantCulture
function Percentile($Values, [double]$Fraction) {
    $sorted = @($Values | Sort-Object)
    return $sorted[[Math]::Max(0,[Math]::Ceiling($sorted.Count*$Fraction)-1)]
}
function Average($Values) { return ($Values | Measure-Object -Average).Average }
function Number([double]$Value) { return $Value.ToString('0.000',$culture) }
function Analyze([string]$Path,[string]$Version) {
    $rows = Import-Csv -LiteralPath $Path
    foreach($group in ($rows | Group-Object scenario,run)) {
        $r = $group.Group
        [pscustomobject]@{
            version=$Version; scenario=$r[0].scenario; run=[int]$r[0].run; frames=$r.Count
            median=Percentile @($r | ForEach-Object {[double]::Parse($_.frame_ms,$culture)}) .5
            p95=Percentile @($r | ForEach-Object {[double]::Parse($_.frame_ms,$culture)}) .95
            p99=Percentile @($r | ForEach-Object {[double]::Parse($_.frame_ms,$culture)}) .99
            cpu_us=Average @($r | ForEach-Object {[double]::Parse($_.neon_cpu_us,$culture)})
            scope_us=Average @($r | ForEach-Object {[double]::Parse($_.neon_scope_us,$culture)})
            alloc_bytes=Average @($r | ForEach-Object {[double]::Parse($_.neon_alloc_bytes,$culture)})
            upload_bytes=Average @($r | ForEach-Object {[double]::Parse($_.upload_bytes,$culture)})
            copy_pixels=Average @($r | ForEach-Object {[double]::Parse($_.copy_pixels,$culture)})
            passes=Average @($r | ForEach-Object {[double]::Parse($_.render_passes,$culture)})
        }
    }
}
$stats = @(Analyze $Before 'Before') + @(Analyze $After 'After')
$lines = [Collections.Generic.List[string]]::new()
$lines.Add('# Neon quality measurements')
$lines.Add('')
$lines.Add('Three repeated runs per version; 240 warm-up frames then 480 sampled frames per scenario. Percentiles use nearest rank. Frame times include the configured frame limiter. CPU is sampled with ThreadMXBean (coarse Windows thread-clock resolution); scope time is the monotonic wall time inside the same Neon scopes. Allocations and rendering counters cover instrumented Neon work, not the entire client.')
$lines.Add('')
$lines.Add('| Version | Scenario | Run | Median ms | p95 ms | p99 ms | CPU µs/frame | Scope µs/frame | Bytes/frame | Upload bytes/frame | Copy pixels/frame | Passes/frame |')
$lines.Add('|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|')
foreach($s in $stats) {
    $values = @($s.version,$s.scenario,$s.run,(Number $s.median),(Number $s.p95),(Number $s.p99),(Number $s.cpu_us),(Number $s.scope_us),(Number $s.alloc_bytes),(Number $s.upload_bytes),(Number $s.copy_pixels),(Number $s.passes))
    $lines.Add('| ' + ($values -join ' | ') + ' |')
}
$lines.Add('')
$lines.Add('## Comparison')
$lines.Add('')
$lines.Add('Values below use the median of the three per-run results. A repeated regression means an increase above 5% in at least two paired runs; investigate it before delivery.')
$lines.Add('')
$lines.Add('| Scenario | Median Δ | p95 Δ | p99 Δ | Allocations before → after | Upload before → after | Scope before → after µs | Repeated >5% frame regression |')
$lines.Add('|---|---:|---:|---:|---:|---:|---:|---|')
foreach($scenario in ($stats.scenario | Select-Object -Unique)) {
    $b=@($stats | Where-Object { $_.version -eq 'Before' -and $_.scenario -eq $scenario } | Sort-Object run)
    $a=@($stats | Where-Object { $_.version -eq 'After' -and $_.scenario -eq $scenario } | Sort-Object run)
    $deltas=@(); $flags=@()
    foreach($metric in @('median','p95','p99')) {
        $deltas+= (Number ((Percentile $a.$metric .5)/(Percentile $b.$metric .5)*100-100))+'%'
        $count=0; for($i=0;$i -lt 3;$i++){if($a[$i].$metric -gt $b[$i].$metric*1.05){$count++}}
        if($count -ge 2){$flags+=$metric}
    }
    $alloc=(Number (Percentile $b.alloc_bytes .5))+' → '+(Number (Percentile $a.alloc_bytes .5))
    $upload=(Number (Percentile $b.upload_bytes .5))+' → '+(Number (Percentile $a.upload_bytes .5))
    $scope=(Number (Percentile $b.scope_us .5))+' → '+(Number (Percentile $a.scope_us .5))
    $flag=if($flags.Count){$flags -join ', '}else{'None'}
    $lines.Add('| '+(@($scenario)+$deltas+@($alloc,$upload,$scope,$flag) -join ' | ')+' |')
}
[IO.File]::WriteAllLines([IO.Path]::GetFullPath($Output),$lines)
$stats | ConvertTo-Json | Set-Content -LiteralPath ([IO.Path]::ChangeExtension($Output,'.json'))
Get-Content -LiteralPath $Output | Select-Object -Last 12
