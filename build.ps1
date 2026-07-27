[CmdletBinding()]
param(
	[Parameter(Mandatory = $true)] [string]$EclipseHome,
	[Parameter(Mandatory = $true)] [string]$JavaHome
)

$ErrorActionPreference = 'Stop'
$ProjectDirectory = $PSScriptRoot
$SourceDirectory = Join-Path $ProjectDirectory 'src'
$OutputDirectory = Join-Path $ProjectDirectory 'bin'
$ResultDirectory = Join-Path $ProjectDirectory 'dist'
$PluginsDirectory = Join-Path $EclipseHome 'plugins'
$JavacExecutable = Join-Path $JavaHome 'bin\javac.exe'
$JarExecutable = Join-Path $JavaHome 'bin\jar.exe'
$ArgumentsFile = Join-Path $ProjectDirectory 'javac.args'
$JarName = 'it.fabrizio.eclipse.customcommand_1.3.9.jar'

if (-not (Test-Path $PluginsDirectory -PathType Container)) { throw "Cartella Eclipse non valida: $EclipseHome" }
if (-not (Test-Path $JavacExecutable -PathType Leaf)) { throw "JDK non valido: $JavacExecutable" }
$Required = @('org.eclipse.core.commands_*.jar','org.eclipse.core.resources_*.jar','org.eclipse.core.jobs_*.jar','org.eclipse.core.runtime_*.jar','org.eclipse.core.variables_*.jar','org.eclipse.debug.core_*.jar','org.eclipse.debug.ui_*.jar','org.eclipse.equinox.common_*.jar','org.eclipse.jface_*.jar','org.eclipse.osgi_*.jar','org.eclipse.swt.*_*.jar','org.eclipse.ui_*.jar','org.eclipse.ui.workbench_*.jar')
$PluginJars = [Collections.Generic.List[string]]::new()
foreach ($Pattern in $Required) {
	$Found = @(Get-ChildItem $PluginsDirectory -Filter $Pattern -File)
	if ($Found.Count -eq 0) { throw "Plug-in richiesto non trovato: $Pattern" }
	foreach ($Jar in $Found) { if (-not $PluginJars.Contains($Jar.FullName)) { $PluginJars.Add($Jar.FullName) } }
}
$SourceFiles = @(
	'CustomCommandConstants.java',
	'CustomCommandMainTab.java',
	'CustomCommandTabGroup.java',
	'CustomCommandLaunchDelegate.java',
	'CustomCommandLaunchShortcut.java'
)
$PackageDirectory = Join-Path $SourceDirectory 'it\fabrizio\eclipse\customcommand'
$Sources = @($SourceFiles | ForEach-Object {
	$SourcePath = Join-Path $PackageDirectory $_
	if (-not (Test-Path -LiteralPath $SourcePath -PathType Leaf)) {
		throw "Sorgente richiesto non trovato: $SourcePath"
	}
	$SourcePath
})
Remove-Item $OutputDirectory,$ResultDirectory,$ArgumentsFile -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $OutputDirectory,$ResultDirectory | Out-Null
$Classpath = ($PluginJars | ForEach-Object { $_.Replace('\','/') }) -join [IO.Path]::PathSeparator
$Args = @('-encoding','UTF-8','--release','21','-classpath',('"{0}"' -f $Classpath),'-d',('"{0}"' -f $OutputDirectory.Replace('\','/')))
$Args += $Sources | ForEach-Object { '"{0}"' -f $_.Replace('\','/') }
[IO.File]::WriteAllLines($ArgumentsFile,$Args,[Text.UTF8Encoding]::new($false))
try { & $JavacExecutable "@$ArgumentsFile"; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } } finally { Remove-Item $ArgumentsFile -Force -ErrorAction SilentlyContinue }
Copy-Item (Join-Path $ProjectDirectory 'META-INF') $OutputDirectory -Recurse
Copy-Item (Join-Path $ProjectDirectory 'plugin.xml') $OutputDirectory
$JarPath = Join-Path $ResultDirectory $JarName
Push-Location $OutputDirectory
try { & $JarExecutable --create --no-manifest --file $JarPath .; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } } finally { Pop-Location }
Write-Host "Creato: $JarPath"
