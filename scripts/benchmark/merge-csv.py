import pandas as pd
import glob
import os
from collections import defaultdict

# 1. Define folder/pattern
file_paths = glob.glob("benchmark_data/*.csv")  # Adjust path if needed

keys_to_ignore = ['run', 'trial', 'timestamp']
grouped_files = defaultdict(list)

for file_path in file_paths:
    filename = os.path.basename(file_path)
    
    # Remove the '.csv' extension first
    filename_no_ext = os.path.splitext(filename)[0]
    
    # 2. Split by underscore to isolate the blocks
    # e.g., ['config', 'odometryPriorWeight-0,5', 'locationDetectionWeight-0,5']
    parts = filename_no_ext.split('_')
    
    file_params = {}
    for part in parts:
        # If the block contains a hyphen, it's a parameter key-value pair
        if '-' in part:
            # Split by the first hyphen (just in case values have hyphens too)
            key, val = part.split('-', 1)
            
            if key not in keys_to_ignore:
                file_params[key] = val
                
    # Skip files that don't have any matching parameters
    if not file_params:
        continue
        
    # Group purely by the parameter names
    group_key = tuple(sorted(file_params.keys()))
    
    grouped_files[group_key].append((file_path, file_params))

# 3. Process each group separately
for group_key, file_data_list in grouped_files.items():
    dataframes = []
    
    for file_path, file_params in file_data_list:
        df = pd.read_csv(file_path)
        
        # 4. Add this specific file's parameter values as new columns
        for param_name, param_value in file_params.items():
            try:
                num_val = float(param_value.replace(',', '.'))
                df[param_name] = num_val
            except ValueError:
                df[param_name] = param_value
                
        dataframes.append(df)
        
    # 5. Merge the group and save to a new file
    if dataframes:
        merged_df = pd.concat(dataframes, ignore_index=True)
        
        out_filename = f"merged_by_{'_'.join(group_key)}.csv"
        
        merged_df.to_csv(out_filename, index=False)
        print(f"Merged {len(file_data_list)} files into '{out_filename}'")