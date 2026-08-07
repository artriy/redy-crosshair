$GradleArguments = @($args)

$ErrorActionPreference = 'Stop'
$ProjectRoot = $PSScriptRoot
$JdkVersion = '25.0.3+9'
$JdkSha256 = '709312cd0420296d9b9de917fe6e28a5b979e875ee5ab91783fb79bcd5857235'
$JdkUri = 'https://api.adoptium.net/v3/binary/version/jdk-25.0.3%2B9/windows/x64/jdk/hotspot/normal/eclipse'
$JdkDirectory = Join-Path $ProjectRoot ".tools\jdk25\jdk-$JdkVersion"
$JdkArchive = Join-Path $ProjectRoot '.tools\jdk25.zip'
$JavaExecutable = Join-Path $JdkDirectory 'bin\java.exe'

if (-not (Test-Path $JavaExecutable -PathType Leaf)) {
    New-Item -ItemType Directory -Path (Split-Path $JdkArchive -Parent) -Force | Out-Null

    if (Test-Path $JdkArchive -PathType Leaf) {
        $ArchiveHash = (Get-FileHash $JdkArchive -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($ArchiveHash -ne $JdkSha256) {
            Remove-Item $JdkArchive -Force
        }
    }

    if (-not (Test-Path $JdkArchive -PathType Leaf)) {
        Write-Host "Downloading Eclipse Temurin JDK $JdkVersion..."
        Invoke-WebRequest -UseBasicParsing -Uri $JdkUri -OutFile $JdkArchive
        $ArchiveHash = (Get-FileHash $JdkArchive -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($ArchiveHash -ne $JdkSha256) {
            Remove-Item $JdkArchive -Force
            throw "Downloaded JDK checksum mismatch. Expected $JdkSha256, received $ArchiveHash."
        }
    }

    $JdkParent = Split-Path $JdkDirectory -Parent
    New-Item -ItemType Directory -Path $JdkParent -Force | Out-Null
    Expand-Archive -Path $JdkArchive -DestinationPath $JdkParent -Force
}

if (-not (Test-Path $JavaExecutable -PathType Leaf)) {
    throw "JDK extraction did not create $JavaExecutable."
}

if ($null -eq $GradleArguments -or $GradleArguments.Count -eq 0) {
    $GradleArguments = @('build')
}

$env:JAVA_HOME = $JdkDirectory
$env:Path = "$(Join-Path $JdkDirectory 'bin');$env:Path"

& (Join-Path $ProjectRoot 'gradlew.bat') @GradleArguments
exit $LASTEXITCODE
