#!/usr/bin/env bash

set -euo pipefail

BASE_CONFIG="src/main/resources/VEConfig/defaultConfig.json"
BENCHMARK_OUTPUT_DIR="benchmark"
OUTPUT_DIR="$BENCHMARK_OUTPUT_DIR/generated_configs"

mkdir -p "$BENCHMARK_OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

# -----------------------------------------------------------------------------
# Parameters to sweep.
#
# Add new parameters by:
#   PARAMS+=(newKey)
#   VALUES_newKey=("v1" "v2" "v3")
# -----------------------------------------------------------------------------

PARAMS=(
    yoloRange
    yoloDetectionWeight
)

VALUES_yoloRange=(50)
VALUES_yoloDetectionWeight=(1.0)

# -----------------------------------------------------------------------------
# Experiment command prefix.
# -----------------------------------------------------------------------------

EXPERIMENT_CMD=(
  ./gradlew experiment
  --map /Krisztina/Krisztina.json
  --replay-json recording/json/rosbag2_2026_07_14-12_55_52_withYOLO.jsonl
  --no-visualization
  --veConfig
)

# -----------------------------------------------------------------------------

declare -A CURRENT_VALUES

generate() {
    local index=$1

    if (( index == ${#PARAMS[@]} )); then

        local jq_filter="."
        local run_name="config"

        for key in "${PARAMS[@]}"; do
            value="${CURRENT_VALUES[$key]}"

            # Numbers are inserted as JSON numbers.
            jq_filter+=" | .$key = $value"

            # Build readable run name.
            safe_value=${value//./_}
            run_name+="_${key}-${safe_value}"
        done

        config_file="$OUTPUT_DIR/${run_name}.json"
        evaluation_file="$BENCHMARK_OUTPUT_DIR/${run_name}.csv"

        jq "$jq_filter" "$BASE_CONFIG" > "$config_file"

        echo "Running experiment with $config_file"
        "${EXPERIMENT_CMD[@]}" \
            --output-evaluation "$evaluation_file" \
            "$config_file"

        return
    fi

    local key="${PARAMS[$index]}"

    # Obtain the array with the candidate values.
    local values_var="VALUES_${key}[@]"

    for value in "${!values_var}"; do
        CURRENT_VALUES["$key"]="$value"
        generate $((index + 1))
    done
}

generate 0