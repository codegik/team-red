#!/bin/bash
echo "Stopping and removing containers, networks, volumes..."
docker-compose down -v --rmi local

echo "Pruning dangling images..."
docker image prune -f

echo "Done."
