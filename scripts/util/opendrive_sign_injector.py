import xml.etree.ElementTree as ET
import numpy as np
from scipy.optimize import minimize_scalar
import csv
import argparse
import os

class OpenDriveSignInjecter:
    def __init__(self, xodr_path):
        self.xodr_path = xodr_path
        self.tree = ET.parse(xodr_path)
        self.root = self.tree.getroot()
        self.roads = self._parse_road_geometries()

    def _find_highest_integer_id(self):
        """
        Scans all elements and attributes in the XML to find the largest 
        existing integer ID, ensuring the new sign IDs are uniquely higher.
        """
        max_id = 99  # Default baseline so that counter starts at 100 if map IDs are small
        
        for element in self.root.iter():
            # Check standard 'id' attribute common to roads, signals, junctions, etc.
            element_id = element.get('id')
            if element_id is not None:
                try:
                    # Strip any common string prefixes if present, or parse directly
                    # e.g., if someone used "road_42" or just "42"
                    cleaned_id = ''.join(filter(str.isdigit, element_id))
                    if cleaned_id:
                        val = int(cleaned_id)
                        if val > max_id:
                            max_id = val
                except ValueError:
                    pass
        
        return max_id

    def _parse_road_geometries(self):
        """Parses basic road geometry vectors for distance mapping."""
        roads_data = {}
        for road in self.root.findall('.//road'):
            road_id = road.get('id')
            geometries = []
            
            # Gather all geometry blocks for the road reference line
            for geo in road.findall('.//geometry'):
                s = float(geo.get('s'))
                x = float(geo.get('x'))
                y = float(geo.get('y'))
                hdg = float(geo.get('hdg'))
                length = float(geo.get('length'))
                
                # Check geometry type (Assuming linear/arc segments for this calculation base)
                # For complex clothoids, a dedicated library like esmini is ideal, 
                # but standard linear sampling covers most analytical maps.
                line = geo.find('line')
                arc = geo.find('arc')
                
                geometries.append({
                    's': s, 'x': x, 'y': y, 'hdg': hdg, 'length': length,
                    'type': 'arc' if arc is not None else 'line',
                    'curvature': float(arc.get('curvature')) if arc is not None else 0.0
                })
            
            # Gather elevation profile if it exists
            elevation_profile = []
            for elev in road.findall('.//elevationProfile/elevation'):
                elevation_profile.append({
                    's': float(elev.get('s')),
                    'a': float(elev.get('a')),
                    'b': float(elev.get('b')),
                    'c': float(elev.get('c')),
                    'd': float(elev.get('d'))
                })
                
            roads_data[road_id] = {
                'geometries': geometries,
                'elevations': elevation_profile,
                'element': road
            }
        return roads_data

    def _get_absolute_road_pos(self, geo, s_offset):
        """Calculates global (X,Y) along a road geometry at a specific s offset."""
        if geo['type'] == 'line':
            x = geo['x'] + s_offset * np.cos(geo['hdg'])
            y = geo['y'] + s_offset * np.sin(geo['hdg'])
            hdg = geo['hdg']
        elif geo['type'] == 'arc':
            c = geo['curvature']
            r = 1.0 / c
            # Center of the circle
            cx = geo['x'] - r * np.sin(geo['hdg'])
            cy = geo['y'] + r * np.cos(geo['hdg'])
            
            # Current angle
            angle = geo['hdg'] - np.pi/2 + c * s_offset
            x = cx + r * np.cos(angle)
            y = cy + r * np.sin(angle)
            hdg = geo['hdg'] + c * s_offset
        return x, y, hdg

    def _get_road_elevation(self, road_data, s):
        """Calculates road surface absolute Z at distance s."""
        if not road_data['elevations']:
            return 0.0
        # Find active elevation element
        active_elev = road_data['elevations'][0]
        for elev in road_data['elevations']:
            if s >= elev['s']:
                active_elev = elev
            else:
                break
        ds = s - active_elev['s']
        # Cubic polynomial for elevation: ds^3*d + ds^2*c + ds*b + a
        return (ds**3 * active_elev['d']) + (ds**2 * active_elev['c']) + (ds * active_elev['b']) + active_elev['a']

    def project_point_to_map(self, target_x, target_y, target_z):
        """Finds the closest road, exact s, exact t, and relative zOffset."""
        best_road_id = None
        best_s = 0.0
        best_t = 0.0
        best_z_offset = 0.0
        min_distance = float('inf')
        best_hdg = 0.0

        for road_id, data in self.roads.items():
            for geo in data['geometries']:
                
                # Distance function to minimize along the segment length
                def dist_fn(s_offset):
                    x, y, _ = self._get_absolute_road_pos(geo, s_offset)
                    return (x - target_x)**2 + (y - target_y)**2

                res = minimize_scalar(dist_fn, bounds=(0, geo['length']), method='bounded')
                
                if res.fun < min_distance:
                    min_distance = res.fun
                    best_road_id = road_id
                    absolute_s = geo['s'] + res.x
                    best_s = absolute_s
                    
                    # Calculate final vectors for t (lateral offset)
                    ref_x, ref_y, ref_hdg = self._get_absolute_road_pos(geo, res.x)
                    best_hdg = ref_hdg
                    
                    # Vector from reference line to target
                    dx = target_x - ref_x
                    dy = target_y - ref_y
                    
                    # Lateral normal vector (90 deg left of heading)
                    nx = -np.sin(ref_hdg)
                    ny = np.cos(ref_hdg)
                    
                    # Dot product gives sign and magnitude of t
                    best_t = dx * nx + dy * ny
                    
                    # Z offset calculation relative to road elevation profile
                    road_z = self._get_road_elevation(data, absolute_s)
                    best_z_offset = target_z - road_z

        return best_road_id, best_s, best_t, best_z_offset, best_hdg

    def inject_signs(self, coordinates_list):
        # 1. Dynamically discover largest ID in map, then increment by 1
        highest_id = self._find_highest_integer_id()
        current_id_number = highest_id + 1
        print(f"Scanned map. Highest existing integer ID found: {highest_id}. Starting sign IDs at: {current_id_number}\n")

        for idx, coord in enumerate(coordinates_list):
            tx, ty, tz = coord['x'], coord['y'], coord['z']
            sign_type = coord.get('type', '101')
            
            # Map inertial space directly to OpenDRIVE parameters
            road_id, s, t, z_offset, hdg = self.project_point_to_map(tx, ty, tz)
            
            if road_id is None:
                print(f"Skipping coordinate {idx}: Could not map to any road layer.")
                continue

            road_element = self.roads[road_id]['element']
            signals_container = road_element.find('signals')
            if signals_container is None:
                signals_container = ET.SubElement(road_element, 'signals')
            
            # Pure integer assignment mapped to string format for standard XML export compatibility
            signal_id = str(current_id_number)

            signal_attrib = {
                'id': signal_id,
                'name': f"Sign_{sign_type}",
                's': f"{s:.6f}",
                't': f"{t:.6f}",
                'zOffset': f"{z_offset:.6f}",
                'type': str(sign_type),
                'subtype': '-1',
                'orientation': '+',
                'dynamic': 'no',
                'hOffset': f"{hdg:.6f}"
            }
            
            signal_node = ET.SubElement(signals_container, 'signal', signal_attrib)
            
            # Essential: Inject exact raw coordinates as a fallback override
            # matrix for precise simulators (CARLA, ASAM OpenDRIVE standard checker)
            pos_inertial_attrib = {
                'x': f"{tx:.6f}",
                'y': f"{ty:.6f}",
                'z': f"{tz:.6f}",
                'hdg': f"{hdg:.6f}",
                'pitch': "0.0",
                'roll': "0.0"
            }
            ET.SubElement(signal_node, 'positionInertial', pos_inertial_attrib)
            
            print(f"Placed {signal_id} (Type {sign_type}) -> Road {road_id} (s={s:.3f}, t={t:.3f})")
            
            # Increment the counter for the next traffic sign
            current_id_number += 1

    def save(self, output_path):
        # Indent tree for readability
        ET.indent(self.tree, space="    ", level=0)
        self.tree.write(output_path, encoding='utf-8', xml_declaration=True)
        print(f"\nSaved file successfully to: {output_path}")


# --- Execution Block ---
if __name__ == "__main__":
    def load_coordinates(path):
        """Load coordinates from CSV, JSON or JSONL.

        Supported formats:
        - CSV with header: x,y,z,type (type optional)
        """
        if not os.path.exists(path):
            raise FileNotFoundError(path)

        if not path.lower().endswith('.csv'):
            raise ValueError('Input must be a .csv file')
        
        coords = []
        with open(path, newline='') as fh:
            reader = csv.DictReader(fh)
            for row in reader:
                try:
                    x = float(row.get('x', row.get('X')))
                    y = float(row.get('y', row.get('Y')))
                    z = float(row.get('z', row.get('Z', 0.0)))
                    typ = row.get('type') or row.get('Type') or row.get('t') or -1
                    coords.append({'x': x, 'y': y, 'z': z, 'type': typ})
                except Exception:
                    continue
        return coords


    parser = argparse.ArgumentParser(description='Inject signs into an OpenDRIVE (.xodr) from coordinate file')
    parser.add_argument('xodr', help='Path to the input .xodr file')
    parser.add_argument('coords', help='Path to coordinates file (CSV)')
    parser.add_argument('-o', '--out', help='Output .xodr path', default=None)
    args = parser.parse_args()

    coords = load_coordinates(args.coords)
    injector = OpenDriveSignInjecter(args.xodr)
    injector.inject_signs(coords)
    out_path = args.out or args.xodr.replace('.xodr', '-extended.xodr')
    injector.save(out_path)
