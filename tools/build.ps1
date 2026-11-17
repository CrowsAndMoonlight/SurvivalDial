param([Parameter(Mandatory=$true)][string]$StarsectorPath,[switch]$Package)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$core=Join-Path $StarsectorPath 'starsector-core'
$jdk=Join-Path $StarsectorPath 'jre\bin'
if(!(Test-Path -LiteralPath "$jdk\javac.exe")){throw 'The game JRE must contain javac.exe (Java 17), or adjust the script to use a Java 17 JDK.'}
$classes=Join-Path $root 'build\classes'
# A fresh, bounded staging directory avoids stale classes in release builds.
if(Test-Path -LiteralPath $classes){
    $resolved=[IO.Path]::GetFullPath($classes)
    $boundary=[IO.Path]::GetFullPath((Join-Path $root 'build'))+[IO.Path]::DirectorySeparatorChar
    if(!$resolved.StartsWith($boundary,[StringComparison]::OrdinalIgnoreCase)){throw 'Unsafe class output path'}
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
New-Item -ItemType Directory -Force $classes,"$root\jars" | Out-Null
$source=@(Get-ChildItem -LiteralPath "$root\src" -Recurse -Filter *.java | ForEach-Object FullName)
& "$jdk\javac.exe" --release 17 -encoding UTF-8 -cp "$core\*" -d $classes @source
if($LASTEXITCODE -ne 0){throw 'Compilation failed'}

$testSources=@(Get-ChildItem -LiteralPath "$root\tests" -Filter *.java | ForEach-Object FullName)
$testClasses=Join-Path $root 'build\test-classes'
New-Item -ItemType Directory -Force $testClasses | Out-Null
& "$jdk\javac.exe" --release 17 -encoding UTF-8 -cp "$core\*;$classes" -d $testClasses @testSources
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
& "$jdk\java.exe" -cp "$core\*;$classes;$testClasses" survivaldial.SettingsTest "$root\data\config\survival_dial_catalog.json" "$root\build\preset-table.md"
if($LASTEXITCODE -ne 0){throw 'Tests failed'}
$readmePath=Join-Path $root 'README.md'
$readme=[IO.File]::ReadAllText($readmePath)
$start='<!-- presets:start -->'; $end='<!-- presets:end -->'
$startIndex=$readme.IndexOf($start); $endIndex=$readme.IndexOf($end)
if($startIndex -lt 0 -or $endIndex -le $startIndex){throw 'README preset-table markers are missing'}
$table=[IO.File]::ReadAllText((Join-Path $root 'build\preset-table.md'))
$readme=$readme.Substring(0,$startIndex+$start.Length)+"`n`n"+$table+"`n"+$readme.Substring($endIndex)
[IO.File]::WriteAllText($readmePath,$readme,(New-Object Text.UTF8Encoding($false)))
# Publish the compiled artifact only after tests succeed.
& "$jdk\jar.exe" --create --file "$root\jars\survival-dial.jar" -C $classes .
if($LASTEXITCODE -ne 0){throw 'JAR packaging failed'}
if($Package){
    $version=(Get-Content -LiteralPath "$root\mod_info.json" -Raw | ConvertFrom-Json).version
    $stage=Join-Path $root ('build\package-'+[Guid]::NewGuid().ToString('N'))
    $mod=Join-Path $stage 'SurvivalDial'
    New-Item -ItemType Directory -Force $mod,"$root\dist" | Out-Null
    # Explicit allowlist: no game JARs, saves, API sources, or unrelated workspace files.
    foreach($name in @('mod_info.json','README.md','CHANGELOG.md','data','docs','jars')){Copy-Item -LiteralPath (Join-Path $root $name) -Destination $mod -Recurse}
    $zip=Join-Path $root "dist\SurvivalDial-$version.zip"
    if(Test-Path -LiteralPath $zip){Remove-Item -LiteralPath $zip -Force}
    Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem
    $archive=[IO.Compression.ZipFile]::Open($zip,[IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach($file in Get-ChildItem -LiteralPath $mod -Recurse -File){
            $entryName=$file.FullName.Substring($stage.Length+1).Replace('\','/')
            [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,$file.FullName,$entryName,[IO.Compression.CompressionLevel]::Optimal) | Out-Null
        }
    } finally { $archive.Dispose() }
    Write-Output "Release candidate: $zip"
}
