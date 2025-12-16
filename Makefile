# Makefile for Car Pooling Service
# Simple and clear commands

# Variables
IMAGE := car-pooling
CONTAINER := car-pooling-dev
PORT := 9091

.PHONY: help run dev-live dev-debug logs stop test build status clean compile ssh restart

# Help
help:	### Show available commands
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

# Development & Execution
compile:	### Compile Java files (use after editing code)
	@echo "⚙️  Compiling Java files..."
	@docker exec $(CONTAINER) mvn compile -q
	@echo "✅ Compiled - DevTools will auto-restart in 2-3 seconds"

dev-live:	### Start development server with live reload
	@echo "🔥 Starting development server with live reload..."
	@docker stop $(CONTAINER) 2>/dev/null || true
	@docker rm $(CONTAINER) 2>/dev/null || true
	@docker build -t $(IMAGE):dev --target dev .
	@docker run -d --name $(CONTAINER) \
		-p $(PORT):9091 \
		-v $$(pwd)/src:/app/src \
		-v $$(pwd)/pom.xml:/app/pom.xml \
		-v maven-cache:/root/.m2 \
		$(IMAGE):dev
	@echo "✅ Server with live reload running at http://localhost:$(PORT)"
	@echo "💡 Edit code and run 'make compile' to trigger auto-reload"

dev-debug:	### Start development server with debugger (port 5005)
	@echo "🐛 Starting development server with debugger..."
	@docker stop $(CONTAINER) 2>/dev/null || true
	@docker rm $(CONTAINER) 2>/dev/null || true
	@docker build -t $(IMAGE):dev --target dev .
	@docker run -d --name $(CONTAINER) \
		-p $(PORT):9091 \
		-p 5005:5005 \
		-v $$(pwd)/src:/app/src \
		-v $$(pwd)/pom.xml:/app/pom.xml \
		-v maven-cache:/root/.m2 \
		$(IMAGE):dev \
		mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
	@echo "✅ Server with debugging running at http://localhost:$(PORT)"
	@echo "✅ Debugger listening on localhost:5005"
	@echo "💡 Connect your IDE to localhost:5005"
	@echo "💡 Edit code and run 'make compile' to trigger auto-reload"

# Utilities
logs:	### Show server logs
	@docker logs -f $(CONTAINER)

stop:	### Stop server
	@docker stop $(CONTAINER) 2>/dev/null || true
	@docker rm $(CONTAINER) 2>/dev/null || true
	@echo "✅ Server stopped"

status:	### Check if server is running
	@if curl -sf http://localhost:$(PORT)/status > /dev/null; then \
		echo "✅ Server is healthy"; \
	else \
		echo "❌ Server is not responding"; \
	fi

ssh:	### Open shell in running container
	@docker exec -it $(CONTAINER) /bin/bash || \
		docker exec -it $(CONTAINER) /bin/sh || \
		echo "❌ Container is not running"

restart:	### Restart the server container
	@echo "🔄 Restarting server..."
	@docker restart $(CONTAINER) 2>/dev/null || \
		(echo "❌ Container not running. Use 'make dev' to start it." && exit 1)
	@echo "✅ Server restarted"

# Testing
test:	### Run tests (smart: uses dev-live container or builds)
	@echo "🧪 Running tests..."
	@if docker exec $(CONTAINER) test -f /app/pom.xml 2>/dev/null; then \
		echo "📦 Using running container..."; \
		docker exec $(CONTAINER) mvn test; \
	elif docker ps -q -f name=$(CONTAINER) > /dev/null 2>&1; then \
		echo "⚠️  Container running in production mode (no source code)"; \
		echo "📦 Building test image instead..."; \
		docker build -t $(IMAGE):test --target build . && \
		docker run --rm -v maven-cache:/root/.m2 $(IMAGE):test mvn test; \
	else \
		echo "📦 Building test image..."; \
		docker build -t $(IMAGE):test --target build . && \
		docker run --rm -v maven-cache:/root/.m2 $(IMAGE):test mvn test; \
	fi

test-quick:	### Run tests in running container (fastest, requires make dev-live)
	@echo "🧪 Running quick tests..."
	@if docker exec $(CONTAINER) test -f /app/pom.xml 2>/dev/null; then \
		docker exec $(CONTAINER) mvn test; \
	else \
		echo "❌ Container not in dev mode. Start it with:"; \
		echo "   make dev-live"; \
		exit 1; \
	fi

test-ci:	### Run tests (clean build for CI/CD)
	@echo "🧪 Running tests (CI mode)..."
	@docker build --no-cache -t $(IMAGE):test --target build .
	@docker run --rm $(IMAGE):test mvn test

# Production
build:	### Build production image
	@docker build -t $(IMAGE):latest --target prod .
	@echo "✅ Image built"

run:	### Run production server (optimized JAR)
	@echo "🚀 Starting production server..."
	@docker stop $(CONTAINER) 2>/dev/null || true
	@docker rm $(CONTAINER) 2>/dev/null || true
	@docker build --no-cache -t $(IMAGE):latest --target prod .
	@docker run -d --name $(CONTAINER) -p $(PORT):9091 $(IMAGE):latest
	@echo "✅ Production server running at http://localhost:$(PORT)"

# Cleanup
clean:	### Remove containers and images
	@docker stop $(CONTAINER) 2>/dev/null || true
	@docker rm $(CONTAINER) 2>/dev/null || true
	@docker rmi $(IMAGE):latest $(IMAGE):dev $(IMAGE):test 2>/dev/null || true
	@echo "✅ Cleaned up"
