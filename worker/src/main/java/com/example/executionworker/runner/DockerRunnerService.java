package com.example.executionworker.runner;

import com.example.executionworker.model.ExecutionEvent;
import com.example.executionworker.model.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

@Service
public class DockerRunnerService {

    private static final Logger log = LoggerFactory.getLogger(DockerRunnerService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    @Value("${app.runner-mode:docker}")
    private String runnerMode;

    public void executeCodeAndReport(ExecutionEvent event) {
        long startTime = System.currentTimeMillis();
        Path tempDir = null;
        ExecutionResult result;

        try {
            tempDir = Files.createTempDirectory("cpp_runner_");
            Path sourceFile = tempDir.resolve("main.cpp");
            Files.writeString(sourceFile, event.getCode(), StandardCharsets.UTF_8);

            String absPath = tempDir.toAbsolutePath().toString();
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            boolean isLocal = "local".equalsIgnoreCase(runnerMode);

            // Step 1: Compilation
            ProcessBuilder compilePb;
            if (isLocal) {
                log.info("Compiling C++ code locally (host-level) for executionId: {}", event.getExecutionId());
                String binaryName = isWindows ? "main.exe" : "main";
                compilePb = new ProcessBuilder(
                        "g++", "-O2", "main.cpp", "-o", binaryName
                );
            } else {
                log.info("Compiling C++ code in Docker for executionId: {}", event.getExecutionId());
                compilePb = new ProcessBuilder(
                        "docker", "run", "--rm",
                        "-v", absPath + ":/usr/src/app",
                        "cpp-runner",
                        "g++", "-O2", "main.cpp", "-o", "main"
                );
            }
            compilePb.directory(tempDir.toFile());

            Process compileProcess = compilePb.start();
            boolean compileFinished = compileProcess.waitFor(15, TimeUnit.SECONDS);

            if (!compileFinished) {
                compileProcess.destroyForcibly();
                result = new ExecutionResult(
                        event.getExecutionId(), event.getRoomId(), event.getUserId(),
                        "COMPILATION_ERROR", "", "Compilation timed out after 15 seconds.",
                        System.currentTimeMillis() - startTime
                );
                sendResultToBackend(result);
                return;
            }

            String compileErrOutput = readStream(compileProcess.getErrorStream());
            if (compileProcess.exitValue() != 0) {
                result = new ExecutionResult(
                        event.getExecutionId(), event.getRoomId(), event.getUserId(),
                        "COMPILATION_ERROR", "", compileErrOutput,
                        System.currentTimeMillis() - startTime
                );
                sendResultToBackend(result);
                return;
            }

            // Step 2: Execution inside safe Docker sandbox (or local if configured)
            ProcessBuilder runPb;
            if (isLocal) {
                log.info("Executing compiled binary locally (host-level) for executionId: {}", event.getExecutionId());
                String executablePath = isWindows ? "main.exe" : "./main";
                runPb = new ProcessBuilder(executablePath);
            } else {
                log.info("Executing compiled binary inside safe Docker sandbox for executionId: {}", event.getExecutionId());
                runPb = new ProcessBuilder(
                        "docker", "run", "--rm",
                        "-v", absPath + ":/usr/src/app",
                        "-m", "128m",
                        "--cpus", "1.0",
                        "--network", "none",
                        "cpp-runner",
                        "./main"
                );
            }
            runPb.directory(tempDir.toFile());

            long execStartTime = System.currentTimeMillis();
            Process runProcess = runPb.start();
            boolean runFinished = runProcess.waitFor(5, TimeUnit.SECONDS);
            long executionTimeMs = System.currentTimeMillis() - execStartTime;

            if (!runFinished) {
                runProcess.destroyForcibly();
                result = new ExecutionResult(
                        event.getExecutionId(), event.getRoomId(), event.getUserId(),
                        "TIMEOUT", "", "Execution timed out (exceeded 5 seconds).",
                        executionTimeMs
                );
                sendResultToBackend(result);
                return;
            }

            String stdout = readStream(runProcess.getInputStream());
            String stderr = readStream(runProcess.getErrorStream());

            if (runProcess.exitValue() != 0) {
                result = new ExecutionResult(
                        event.getExecutionId(), event.getRoomId(), event.getUserId(),
                        "RUNTIME_ERROR", stdout, stderr.isBlank() ? "Process exited with code " + runProcess.exitValue() : stderr,
                        executionTimeMs
                );
            } else {
                result = new ExecutionResult(
                        event.getExecutionId(), event.getRoomId(), event.getUserId(),
                        "SUCCESS", stdout, stderr,
                        executionTimeMs
                );
            }

            sendResultToBackend(result);

        } catch (Exception e) {
            log.error("System error during docker code execution for {}", event.getExecutionId(), e);
            result = new ExecutionResult(
                    event.getExecutionId(), event.getRoomId(), event.getUserId(),
                    "SYSTEM_ERROR", "", "Docker execution environment error: " + e.getMessage(),
                    System.currentTimeMillis() - startTime
            );
            sendResultToBackend(result);
        } finally {
            if (tempDir != null) {
                deleteDirectory(tempDir.toFile());
            }
        }
    }

    private void sendResultToBackend(ExecutionResult result) {
        try {
            String url = backendUrl + "/api/internal/execution-result";
            restTemplate.postForObject(url, result, String.class);
            log.info("Successfully delivered execution result {} to backend", result.getExecutionId());
        } catch (Exception e) {
            log.error("Failed to post execution result to backend", e);
        }
    }

    private String readStream(InputStream is) {
        try {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private void deleteDirectory(File dir) {
        try {
            Files.walk(dir.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception ignored) {}
    }
}
