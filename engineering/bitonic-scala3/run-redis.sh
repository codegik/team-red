podman build -f Dockerfile.redis -t bitonic-redis .
podman run -d --name bitonic-redis -p 6379:6379 bitonic-redis