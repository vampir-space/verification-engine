FROM python:3.10-slim
COPY --from=carlasim/carla:0.10.0 /home/carla/PythonAPI/carla/dist/carla-0.10.0-cp310-cp310-linux_x86_64.whl /tmp/
RUN pip install /tmp/carla-0.10.0-cp310-cp310-linux_x86_64.whl
WORKDIR /app
COPY autodrive.py .
CMD ["python3", "autodrive.py"]
