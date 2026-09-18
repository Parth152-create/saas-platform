package com.yourco.saas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Path;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		loadDotEnv();
		SpringApplication.run(BackendApplication.class, args);
	}

	static void loadDotEnv() {
		Path envPath = findDotEnv();
		if (envPath == null) {
			return;
		}

		try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(envPath)) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				int eq = line.indexOf('=');
				if (eq <= 0) {
					continue;
				}
				String key = line.substring(0, eq).trim();
				String val = line.substring(eq + 1).trim();
				if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
					val = val.substring(1, val.length() - 1);
				}
				// Do not override values already set in OS environment or System properties
				if (System.getenv(key) == null && System.getProperty(key) == null) {
					// Avoid overriding local host configurations with Docker container hostnames or internal ports
					if ("SPRING_DATASOURCE_URL".equals(key) && val.contains("postgres:")) {
						continue;
					}
					if ("SPRING_DATA_REDIS_HOST".equals(key) && "redis".equals(val)) {
						continue;
					}
					if ("SPRING_DATA_REDIS_PORT".equals(key) && "6379".equals(val)) {
						continue;
					}
					System.setProperty(key, val);
				}
			}
		} catch (java.io.IOException ignored) {
		}
	}

	static Path findDotEnv() {
		Path cur = java.nio.file.Path.of("").toAbsolutePath();
		for (int i = 0; i < 4 && cur != null; i++) {
			Path candidate = cur.resolve(".env");
			if (java.nio.file.Files.exists(candidate)) {
				return candidate;
			}
			cur = cur.getParent();
		}
		return null;
	}

}
