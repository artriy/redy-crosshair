[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = 'Low')]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^\d+\.\d+(?:\.\d+)?$')]
    [string]$MinecraftVersion,

    [string]$BaseProject,
    [string]$MinecraftDependency,
    [string]$ModMenuVersion,
    [ValidatePattern('^\d+\.\d+\.\d+$')]
    [string]$ModVersion,
    [int]$JavaVersion,
    [string]$LoaderVersion,
    [string]$LoomVersion,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = $PSScriptRoot
$SettingsPath = Join-Path $ProjectRoot 'settings.gradle'
$PropertiesPath = Join-Path $ProjectRoot 'gradle.properties'
$VersionsDirectory = Join-Path $ProjectRoot 'versions'
$Settings = [IO.File]::ReadAllText($SettingsPath)
$ProjectMatches = [regex]::Matches($Settings, "(?m)^include 'versions:(mc[^']+)'\s*$")

if ($ProjectMatches.Count -eq 0) {
    throw 'settings.gradle does not contain a Minecraft version project.'
}

if ([string]::IsNullOrWhiteSpace($BaseProject)) {
    $BaseProject = $ProjectMatches[$ProjectMatches.Count - 1].Groups[1].Value
}

if ($BaseProject.StartsWith('versions:')) {
    $BaseProject = $BaseProject.Substring('versions:'.Length)
}

$KnownProjects = @($ProjectMatches | ForEach-Object { $_.Groups[1].Value })
if ($KnownProjects -notcontains $BaseProject) {
    throw "Base project '$BaseProject' is not included by settings.gradle."
}

$NewProject = 'mc' + $MinecraftVersion.Replace('.', '_')
$NewProjectDirectory = Join-Path $VersionsDirectory $NewProject
$NewBuildPath = Join-Path $NewProjectDirectory 'build.gradle'
if (Test-Path $NewProjectDirectory) {
    throw "Version project '$NewProject' already exists."
}

if ([string]::IsNullOrWhiteSpace($MinecraftDependency)) {
    $MinecraftDependency = ">=$MinecraftVersion"
}

foreach ($Value in @($MinecraftDependency, $ModMenuVersion, $ModVersion, $LoaderVersion, $LoomVersion)) {
    if ($Value -and $Value.Contains("'")) {
        throw "Version values cannot contain a single quote: $Value"
    }
}

function Set-ExtProperty {
    param(
        [string]$Text,
        [string]$Name,
        [string]$Value
    )

    $Expression = [regex]::new("^ext\.$([regex]::Escape($Name)) = '[^']*'(?=\r?$)", [Text.RegularExpressions.RegexOptions]::Multiline)
    if ($Expression.Matches($Text).Count -ne 1) {
        throw "Expected one ext.$Name assignment in the base project."
    }
    return $Expression.Replace($Text, "ext.$Name = '$Value'", 1)
}

function Set-IntegerExtProperty {
    param(
        [string]$Text,
        [string]$Name,
        [int]$Value
    )

    $Expression = [regex]::new("^ext\.$([regex]::Escape($Name)) = \d+(?=\r?$)", [Text.RegularExpressions.RegexOptions]::Multiline)
    if ($Expression.Matches($Text).Count -ne 1) {
        throw "Expected one integer ext.$Name assignment in the base project."
    }
    return $Expression.Replace($Text, "ext.$Name = $Value", 1)
}

function Set-GradleProperty {
    param(
        [string]$Text,
        [string]$Name,
        [string]$Value
    )

    $Expression = [regex]::new("^$([regex]::Escape($Name))=.*?(?=\r?$)", [Text.RegularExpressions.RegexOptions]::Multiline)
    if ($Expression.Matches($Text).Count -ne 1) {
        throw "Expected one $Name assignment in gradle.properties."
    }
    return $Expression.Replace($Text, "$Name=$Value", 1)
}

function Convert-MinecraftVersion {
    param([string]$Value)

    $Parts = $Value.Split('.')
    $Normalized = if ($Parts.Count -eq 2) { "$Value.0" } else { $Value }
    return [version]$Normalized
}

function Get-MinecraftDependencyRange {
    param([string]$Expression)

    $ExactMatch = [regex]::Match($Expression, '^(\d+\.\d+(?:\.\d+)?)$')
    if ($ExactMatch.Success) {
        $Version = Convert-MinecraftVersion $ExactMatch.Groups[1].Value
        return [pscustomobject]@{
            Lower = $Version
            LowerText = $ExactMatch.Groups[1].Value
            Upper = $Version
            UpperInclusive = $true
            Exact = $true
        }
    }

    $RangeMatch = [regex]::Match($Expression, '^>=(\d+\.\d+(?:\.\d+)?)(?:\s+(<|<=)(\d+\.\d+(?:\.\d+)?))?$')
    if (-not $RangeMatch.Success) {
        throw "Unsupported Minecraft dependency '$Expression'. Use an exact version, >=VERSION, >=VERSION <VERSION, or >=VERSION <=VERSION."
    }

    $Lower = Convert-MinecraftVersion $RangeMatch.Groups[1].Value
    $Upper = if ($RangeMatch.Groups[3].Success) {
        Convert-MinecraftVersion $RangeMatch.Groups[3].Value
    } else {
        $null
    }
    $UpperInclusive = $RangeMatch.Groups[2].Value -eq '<='
    if ($null -ne $Upper) {
        $Comparison = $Upper.CompareTo($Lower)
        if ($Comparison -lt 0 -or ($Comparison -eq 0 -and -not $UpperInclusive)) {
            throw "Minecraft dependency '$Expression' has an empty or reversed range."
        }
    }

    return [pscustomobject]@{
        Lower = $Lower
        LowerText = $RangeMatch.Groups[1].Value
        Upper = $Upper
        UpperInclusive = $UpperInclusive
        Exact = $false
    }
}

function Test-MinecraftVersionInRange {
    param(
        [object]$Range,
        [version]$Version
    )

    if ($Version.CompareTo($Range.Lower) -lt 0) {
        return $false
    }
    if ($null -eq $Range.Upper) {
        return $true
    }
    $Comparison = $Version.CompareTo($Range.Upper)
    return $Comparison -lt 0 -or ($Comparison -eq 0 -and $Range.UpperInclusive)
}

$RequestedMinecraftVersion = Convert-MinecraftVersion $MinecraftVersion
$NewDependencyRange = Get-MinecraftDependencyRange $MinecraftDependency
if ($NewDependencyRange.Lower.CompareTo($RequestedMinecraftVersion) -ne 0) {
    throw "Minecraft dependency '$MinecraftDependency' must start at the compiled version $MinecraftVersion."
}

$ImplementationVersions = Get-ChildItem $VersionsDirectory -Filter 'build.gradle' -File -Recurse | ForEach-Object {
    $Text = [IO.File]::ReadAllText($_.FullName)
    $Match = [regex]::Match($Text, "(?m)^ext\.impl_version = '(\d+)\.(\d+)\.(\d+)'(?=\r?$)")
    if (-not $Match.Success) {
        throw "Could not read ext.impl_version from $($_.FullName)."
    }
    [version]$Match.Groups[0].Value.Substring("ext.impl_version = '".Length).TrimEnd("'")
}
$LatestImplementation = $ImplementationVersions | Sort-Object -Descending | Select-Object -First 1
$NextImplementation = "$($LatestImplementation.Major).$($LatestImplementation.Minor).$($LatestImplementation.Build + 1)"

$BaseBuildPath = Join-Path (Join-Path $VersionsDirectory $BaseProject) 'build.gradle'
$BaseBuild = [IO.File]::ReadAllText($BaseBuildPath)
$BaseVersionMatch = [regex]::Match($BaseBuild, "(?m)^ext\.minecraft_version = '([^']+)'(?=\r?$)")
$BaseDependencyMatch = [regex]::Match($BaseBuild, "(?m)^ext\.minecraft_dependency = '([^']+)'(?=\r?$)")
if (-not $BaseVersionMatch.Success) {
    throw "Could not read ext.minecraft_version from $BaseBuildPath."
}
if (-not $BaseDependencyMatch.Success) {
    throw "Could not read ext.minecraft_dependency from $BaseBuildPath."
}
$BaseMinecraftVersion = Convert-MinecraftVersion $BaseVersionMatch.Groups[1].Value
if ($RequestedMinecraftVersion.CompareTo($BaseMinecraftVersion) -le 0) {
    throw "Minecraft version $MinecraftVersion must be newer than base project version $($BaseVersionMatch.Groups[1].Value)."
}

$BaseDependency = $BaseDependencyMatch.Groups[1].Value
$BaseDependencyRange = Get-MinecraftDependencyRange $BaseDependency
$UpdatedBaseBuild = $BaseBuild
$UpdatedBaseDependency = $BaseDependency
if (Test-MinecraftVersionInRange $BaseDependencyRange $RequestedMinecraftVersion) {
    if ($BaseDependencyRange.Exact) {
        throw "Base dependency '$BaseDependency' overlaps $MinecraftVersion and cannot be narrowed safely."
    }
    $UpdatedBaseDependency = ">=$($BaseDependencyRange.LowerText) <$MinecraftVersion"
    $UpdatedBaseBuild = Set-ExtProperty $BaseBuild 'minecraft_dependency' $UpdatedBaseDependency
}

$NewBuild = Set-ExtProperty $BaseBuild 'minecraft_version' $MinecraftVersion
$NewBuild = Set-ExtProperty $NewBuild 'minecraft_dependency' $MinecraftDependency
$NewBuild = Set-ExtProperty $NewBuild 'impl_version' $NextImplementation
if (-not [string]::IsNullOrWhiteSpace($ModMenuVersion)) {
    $NewBuild = Set-ExtProperty $NewBuild 'modmenu_version' $ModMenuVersion
}
if ($JavaVersion -gt 0) {
    $NewBuild = Set-IntegerExtProperty $NewBuild 'java_version' $JavaVersion
}

$LineEnding = if ($Settings.Contains("`r`n")) { "`r`n" } else { "`n" }
$NewSettings = $Settings.TrimEnd() + $LineEnding + "include 'versions:$NewProject'" + $LineEnding
$NewProperties = [IO.File]::ReadAllText($PropertiesPath)
$CurrentModVersionMatch = [regex]::Match($NewProperties, '(?m)^mod_version=(\d+)\.(\d+)\.(\d+)(?=\r?$)')
if (-not $CurrentModVersionMatch.Success) {
    throw 'Could not read a three-part mod_version from gradle.properties.'
}
if ([string]::IsNullOrWhiteSpace($ModVersion)) {
    $CurrentModVersion = [version]$CurrentModVersionMatch.Groups[0].Value.Substring('mod_version='.Length)
    $ModVersion = "$($CurrentModVersion.Major).$($CurrentModVersion.Minor).$($CurrentModVersion.Build + 1)"
}
$NewProperties = Set-GradleProperty $NewProperties 'mod_version' $ModVersion
if (-not [string]::IsNullOrWhiteSpace($LoaderVersion)) {
    $NewProperties = Set-GradleProperty $NewProperties 'loader_version' $LoaderVersion
}
if (-not [string]::IsNullOrWhiteSpace($LoomVersion)) {
    $NewProperties = Set-GradleProperty $NewProperties 'loom_version' $LoomVersion
}

Write-Host "Minecraft version:    $MinecraftVersion"
Write-Host "New project:          $NewProject"
Write-Host "Base project:         $BaseProject"
Write-Host "Minecraft dependency: $MinecraftDependency"
Write-Host "Implementation:       $NextImplementation"
Write-Host "Release version:      $ModVersion"
if ($UpdatedBaseBuild -ne $BaseBuild) {
    Write-Host "Previous dependency:  $UpdatedBaseDependency"
}

if (-not $PSCmdlet.ShouldProcess($NewBuildPath, 'Create version project and update Gradle settings')) {
    return
}

$Utf8WithoutBom = New-Object Text.UTF8Encoding($false)
[IO.Directory]::CreateDirectory($NewProjectDirectory) | Out-Null
[IO.File]::WriteAllText($NewBuildPath, $NewBuild, $Utf8WithoutBom)
[IO.File]::WriteAllText($BaseBuildPath, $UpdatedBaseBuild, $Utf8WithoutBom)
[IO.File]::WriteAllText($SettingsPath, $NewSettings, $Utf8WithoutBom)
[IO.File]::WriteAllText($PropertiesPath, $NewProperties, $Utf8WithoutBom)
Write-Host "Created $NewBuildPath"

if (-not $SkipBuild) {
    & (Join-Path $ProjectRoot 'build.ps1') 'build'
    exit $LASTEXITCODE
}
