# Makefile for Car Pooling Service (Java Spring Boot)
# vim: set ft=make ts=8 noet
# Copyright Cabify.com
# Licence MIT

# Variables
UNAME := $(shell uname -s)
IMAGE := car-pooling
CONTAINER := car-pooling-dev
PORT := 9091

.EXPORT_ALL_VARIABLES:

.PHONY: help debug ssh dev logs stop build run clean test \
        status info clean-all

# Help
help:	### Show this help screen
ifeq ($(UNAME), Linux)
	@grep -P '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'
else
	@# Mac version
	@awk -F ':.*###' '$$0 ~ FS {printf "%15s%s\n", $$1 ":", $$2}' \
		$(MAKEFILE_LIST) | grep -v '@awk' | sort
endif

# Utils
debug:	### Debug Makefile variables
	@echo "UNAME: $(UNAME)"
	@echo "IMAGE: $(IMAGE)"
	@echo "CONTAINER: $(CONTAINER)"
	@echo "PORT: $(PORT)"

ssh:	### SSH into the development container
	@if docker ps --format '{{.Names}}' | grep -q "^$(CONTAINER)$$"; then \
		docker exec -it $(CONTAINER) /bin/bash; \
	else \
		echo "⚠️  Container $(CONTAINER) is not running"; \
		echo "💡 Run 'make dev' first"; \
	fi

# Development (Simple Docker - no docker-compose needed)
dev:	### Start development server (simple Docker)
	@echo "🚀 Starting development server..."
	@docker stop $(CONTAINER) 2>/dev/null || true
	@docker rm $(CONTAINER) 2>/dev/null || true
	@docker build -t $(IMAGE):latest .
	@docker run -d \
		--name $(CONTAINER) \
		-p $(PORT):9091 \
		$(IMAGE):latest
	@echo "⏳ Waiting for service to start..."
	@sleep 5
	@echo "✅ Dev server running at http://localhost:$(PORT)"
	@echo ""
	@echo "📋 Quick commands:"
	@echo "   make logs    - View logs"
	@echo "   make status  - Check health"
	@echo "   make test-api - Test endpoints"
	@echo "   make ssh     - Enter container"
	@echo "   make stop    - Stop server"

logs:	### Show development server logs
	@docker logs -f $(CONTAINER)

stop:	### Stop development server
	@echo "🛑 Stopping development server..."
	@docker stop $(CONTAINER) 2>/dev/null || true
	@docker rm $(CONTAINER) 2>/dev/null || true
	@echo "✅ Server stopped"

restart:	### Restart development server
	@echo "🔄 Restarting..."
	@make stop
	@make dev

# Testing
test:	### Run all tests inside container
	@echo "🧪 Running all tests..."
	@docker build -t $(IMAGE):test --target build .
	@docker run --rm $(IMAGE):test mvn test

test-api:	### Test API endpoints (requires running server)
	@echo "🧪 Testing API endpoints..."
	@echo ""
	@echo "1️⃣  Testing GET /status"
	@curl -s -o /dev/null -w "   Status: %{http_code}\n" http://localhost:$(PORT)/status
	@echo ""
	@echo "2️⃣  Testing PUT /cars"
	@curl -s -o /dev/null -w "   Status: %{http_code}\n" \
		-X PUT http://localhost:$(PORT)/cars \
		-H "Content-Type: application/json" \
		-d '[{"id":1,"seats":4},{"id":2,"seats":6}]'
	@echo ""
	@echo "3️⃣  Testing POST /journey"
	@curl -s -o /dev/null -w "   Status: %{http_code}\n" \
		-X POST http://localhost:$(PORT)/journey \
		-H "Content-Type: application/json" \
		-d '{"id":1,"people":4}'
	@echo ""
	@echo "4️⃣  Testing POST /locate"
	@curl -s -w "   Status: %{http_code}\n   Body: %{stdout}\n" \
		-X POST http://localhost:$(PORT)/locate \
		-H "Content-Type: application/x-www-form-urlencoded" \
		-d "ID=1"
	@echo ""
	@echo "5️⃣  Testing POST /dropoff"
	@curl -s -o /dev/null -w "   Status: %{http_code}\n" \
		-X POST http://localhost:$(PORT)/dropoff \
		-H "Content-Type: application/x-www-form-urlencoded" \
		-d "ID=1"
	@echo ""
	@echo "✅ API tests completed"

# Production
build:	### Build production image
	@echo "🏗  Building production image..."
	@docker build -t $(IMAGE):latest .
	@echo "✅ Image built: $(IMAGE):latest"

run: build	### Run production container
	@make dev

# Health checks
status:	### Check service health
	@echo "🏥 Checking service health..."
	@if curl -sf http://localhost:$(PORT)/status > /dev/null; then \
		echo "✅ Service is healthy (200 OK)"; \
		echo ""; \
		echo "🔗 Service URL: http://localhost:$(PORT)"; \
		echo ""; \
		echo "📋 Available endpoints:"; \
		echo "   GET  /status"; \
		echo "   PUT  /cars"; \
		echo "   POST /journey"; \
		echo "   POST /dropoff"; \
		echo "   POST /locate"; \
	else \
		echo "❌ Service is not responding"; \
		echo "💡 Run 'make dev' to start the service"; \
	fi

# Cleanup
clean:	### Remove all containers and images
	@echo "🧹 Cleaning up..."
	@docker stop $(CONTAINER) 2>/dev/null || true
	@docker rm $(CONTAINER) 2>/dev/null || true
	@docker rmi $(IMAGE):latest 2>/dev/null || true
	@docker rmi $(IMAGE):test 2>/dev/null || true
	@echo "✅ Cleanup complete"

clean-all: clean	### Deep clean (including Maven cache)
	@echo "🧹 Deep cleaning..."
	@rm -rf target/
	@docker system prune -f
	@echo "✅ Deep cleanup complete"

# Info
info:	### Show project information
	@echo "╔════════════════════════════════════════════╗"
	@echo "║  Car Pooling Service - Java Spring Boot   ║"
	@echo "╚════════════════════════════════════════════╝"
	@echo ""
	@echo "📦 Project Info:"
	@echo "   Port:      $(PORT)"
	@echo "   Image:     $(IMAGE)"
	@echo "   Container: $(CONTAINER)"
	@echo ""
	@echo "🐳 Docker Images:"
	@docker images | grep $(IMAGE) | awk '{printf "   %-30s %s\n", $$1":"$$2, $$7}' || echo "   No images built yet"
	@echo ""
	@echo "📦 Container Status:"
	@if docker ps -a --format '{{.Names}}' | grep -q "^$(CONTAINER)$$"; then \
		docker ps -a --filter "name=$(CONTAINER)" --format "   {{.Names}}: {{.Status}}"; \
	else \
		echo "   No containers running"; \
	fi
	@echo ""
	@echo "🚀 Quick Start:"
	@echo "   make dev     - Start server"
	@echo "   make status  - Check health"
	@echo "   make help    - Show all commands"

# Docker Compose (optional - only if you have access to Cabify harness)
compose-up:	### Start with docker-compose (includes harness if available)
	@echo "🚀 Starting with docker-compose..."
	@docker-compose up -d
	@echo "✅ Services started"

compose-down:	### Stop docker-compose services
	@echo "🛑 Stopping docker-compose services..."
	@docker-compose down
	@echo "✅ Services stopped"

compose-logs:	### Show docker-compose logs
	@docker-compose logs -f

# Quick local development (requires Maven installed locally)
quick-test:	### Quick test without Docker (requires local Maven)
	@echo "🧪 Running quick test (local)..."
	@mvn test 2>/dev/null || echo "⚠️  Maven not found locally. Use 'make test' instead"

quick-run:	### Quick run without Docker (requires local Maven)
	@echo "🚀 Running local server..."
	@mvn spring-boot:run 2>/dev/null || echo "⚠️  Maven not found locally. Use 'make dev' instead"
