# CARLA Runtime Modes & Flags

Run CARLA with:
```bash
./CarlaUnreal.sh [flags...]
```

CARLA can operate in different modes depending on hardware (GPU, display) and purpose (development, server, visualization). This guide covers the most common flags and their behavior on typical setups.

## Key Flags

### `-nullrhi`
- **RHI** = Rendering Hardware Interface
- Disables all rendering → no GPU use, no visuals
- YES: Physics simulation, actors, control, (optionally) lidar
- NO: RGB camera, segmentation, depth maps → **Carlaviz cannot work**
- Safe for Intel-only laptops or systems without discrete GPU
- Works on headless servers (no DISPLAY required)

### `-carla-server`
- Runs CARLA as a backend simulation server
- No Unreal UI window
- Accepts remote control via Python API (`carla.Client()`)
- Works over SSH
- Works in Docker
- Does not need DISPLAY

### `-RenderOffScreen`
- Allows Unreal to render without showing a window
- Keeps GPU rendering active → RGB/depth/segmentation work
- **Required for Carlaviz to work headlessly**
- Requires GPU present
- Still needs GPU drivers and CUDA support

### `-carla-host 0.0.0.0`
- Sets the network bind address
- `0.0.0.0` = accept connections from any interface
- Use for remote access or Docker exposure
- Use `127.0.0.1` to limit to localhost only

### `-carla-port 2000`
- TCP port for CARLA server (default: 2000)
- Change if running multiple CARLA instances or avoiding port conflicts

## Platform-Specific Guidance

| System Type | `-nullrhi` | `-RenderOffScreen` | `-carla-server` | Notes |
|-------------|------------|-------------------|-----------------|-------|
| **Laptop (Intel-only, no GPU)** | YES | NO | YES | No rendering possible, physics-only scenarios |
| **Server (GPU + Display)** | NO | Optional | YES | Use `-RenderOffScreen` if running via SSH |
| **Server (GPU, headless/SSH)** | NO | YES | YES | Required for headless rendering (Carlaviz). No DISPLAY needed |

## Example Commands

### Headless physics-only (CI/testing, Intel laptops)
```bash
./CarlaUnreal.sh -nullrhi -carla-server -carla-host 0.0.0.0 -carla-port 2000
```

### Full rendering (local machine with display)
```bash
./CarlaUnreal.sh -carla-server
```

### Headless rendering (GPU server over SSH)
```bash
./CarlaUnreal.sh -RenderOffScreen -carla-server -carla-host 0.0.0.0 -carla-port 2000
```

### With quality settings (GPU server)
```bash
./CarlaUnreal.sh -opengl -RenderOffScreen -carla-server -carla-host 0.0.0.0 -carla-port 2000 -quality-level Low
```

## Troubleshooting

### Segmentation fault on Intel graphics
- Use `-nullrhi` flag to bypass graphics initialization
- Intel integrated graphics cannot run full CARLA rendering

### "Connection refused" errors
- Check if CARLA is actually listening: `netstat -tulpn | grep :2000`
- Verify process is running: `ps aux | grep CarlaUnreal`
- Try connecting to `127.0.0.1` instead of `localhost`

### No images in Carlaviz
- Don't use `-nullrhi` if you need camera sensors
- Ensure `-RenderOffScreen` is used on headless systems
- Verify GPU drivers are installed and working

### Multiple CARLA instances
- Use different ports: `-carla-port 2002`, `-carla-port 2004`, etc.
- Each instance needs 2 consecutive ports (main + streaming)

## Notes
- **Carlaviz compatibility**: Requires rendering enabled (no `-nullrhi`)
- **Docker**: Use `--gpus all` for GPU passthrough when using rendering
- **Development**: `-nullrhi` mode is perfect for testing Python API logic
- **Production**: Use `-RenderOffScreen` for headless servers with visualization needs