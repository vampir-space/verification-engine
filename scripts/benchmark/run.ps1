$ErrorActionPreference = "Stop"

$BaseConfig = "src/main/resources/VEConfig/defaultConfig.json"
$BenchmarkOutputDir = "benchmark"
$OutputDir = Join-Path $BenchmarkOutputDir "generated_configs"

New-Item -ItemType Directory -Force -Path $BenchmarkOutputDir | Out-Null
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

# -----------------------------------------------------------------------------
# Parameters to sweep.
#
# Add new parameters by:
#   $Params += "newKey"
#   $Values["newKey"] = @(v1, v2, v3)
# -----------------------------------------------------------------------------

$Params = @(
    "yoloRange",
    "yoloAngleOfView"
    "yoloMinConfidence"
    "gnssConfidenceRangeMultiplier"

    "odometryPriorWeight"
    "locationDetectionWeight"
    "yoloDetectionWeight"

    "objectDetectionStrategy"
    "objectTypeStrategy"
)

$Values = @{
    yoloRange = @(15,17.5,20)
    yoloAngleOfView = @(0.01)
    yoloMinConfidence = @(0.35,0.4,0.45)
    gnssConfidenceRangeMultiplier = @(0.5,0.75,1.0)

    odometryPriorWeight = @(0.5)
    locationDetectionWeight = @(1.0)
    yoloDetectionWeight = @(1.0,2.0,5.0)

    objectDetectionStrategy = @(1,2)
    objectTypeStrategy = @(1,2)
}

# -----------------------------------------------------------------------------
# Gradle command.
# -----------------------------------------------------------------------------

$ExperimentCmd = @(
    "./gradlew",
    "experiment"
)

# -----------------------------------------------------------------------------
# Application arguments (passed to Gradle via --args="...").
# -----------------------------------------------------------------------------

$ExperimentCmdArgs = @(
    "--map", "/Krisztina/Krisztina.json",
    "--replay-json", "recording/json/measurement.jsonl",
    "--no-visualization"
)

# -----------------------------------------------------------------------------

$CurrentValues = @{}

function Generate([int]$Index)
{
    if ($Index -eq $Params.Count)
    {
        $runName = "config"

        # Read the base configuration.
        $config = Get-Content $BaseConfig -Raw | ConvertFrom-Json

        foreach ($key in $Params)
        {
            $value = $CurrentValues[$key]

            # Update JSON.
            $config.$key = $value

            # Build readable filename.
            $safeValue = $value.ToString().Replace(".", "_")
            $runName += "_${key}-${safeValue}"
        }

        $configFile = Join-Path $OutputDir "$runName.json"
        $evaluationFile = Join-Path $BenchmarkOutputDir "$runName.csv"

        $config |
            ConvertTo-Json -Depth 100 |
            Set-Content -Encoding UTF8 $configFile

        Write-Host "Running experiment with $configFile"

        # Build the application argument string that Gradle expects.
        $appArgs = @($ExperimentCmdArgs)
        $appArgs += "--veConfig `"$configFile`""
        $appArgs += "--output-evaluation `"$evaluationFile`""

        $gradleArgs = @($ExperimentCmd[1..($ExperimentCmd.Length - 1)])
        $gradleArgs += "--args=$($appArgs -join ' ')"

        & $ExperimentCmd[0] @gradleArgs

        if ($LASTEXITCODE -ne 0)
        {
            exit $LASTEXITCODE
        }

        return
    }

    $key = $Params[$Index]

    foreach ($value in $Values[$key])
    {
        $CurrentValues[$key] = $value
        Generate ($Index + 1)
    }
}

Generate 0
