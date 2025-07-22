import os
import subprocess
from flask import Flask, render_template, jsonify, request
from pathlib import Path

app = Flask(__name__, template_folder='templates')
BAG_ROOT = Path('/bags')

def bag_info(bag_dir: Path) -> str:
  try:
    out = subprocess.check_output(
      ['ros2', 'bag', 'info', str(bag_dir)],
      stderr=subprocess.STDOUT,
      timeout=5
    )
    return out.decode()
  except Exception as e:
    return f"Error getting ros2 bag info: {e}"

@app.route('/')
def index():
  bag_dirs = [p.parent for p in BAG_ROOT.rglob('*.db3')]
  bags = []
  for d in bag_dirs:
    info = bag_info(d)
    bags.append({
      'path': str(d),
      'info': info
    })
  return render_template('index.html', bags=bags)

@app.route('/play', methods=['POST'])
def play():
  bag = request.form['bag']
  subprocess.run(['pkill', '-f', 'ros2 bag play'], check=False)
  subprocess.Popen(['ros2', 'bag', 'play', bag, '--clock'])
  return jsonify(status='playing', bag=bag)

if __name__ == '__main__':
  app.run(host='0.0.0.0', port=4000, debug=False)
