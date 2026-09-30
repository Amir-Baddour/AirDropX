package org.example;
import org.example.Api.Admin.AdminApi;
import org.example.Api.Airdrop.AirdropApi;
import org.example.Api.Claim.ClaimApi;
import org.example.Api.Company.CompanyApi;
import org.example.Api.Public.PublicClaimApi;
import org.example.Api.User.UserApi;
import org.example.Config.Config;
import org.example.Core.Payout.MockPayoutProvider;
import org.example.Core.Worker.AirdropWorker;
import org.example.Infra.DatabaseMigration;
import org.example.Resources.DataPopulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static spark.Spark.*;
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class.getName());
    private static final CountDownLatch stopLatch = new CountDownLatch(1);
    private static final ExecutorService postgresExecutor = Executors.newCachedThreadPool();
    private static AirdropWorker airdropWorker;
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                logger.error("Uncaught exception in thread {}", thread.getName(), throwable)
        );
        logger.info("Starting application initialization...");
        try {
            DatabaseMigration databaseMigration = new DatabaseMigration();
            DataPopulation dataPopulation = new DataPopulation();
            databaseMigration.init();
            logger.info("Database migration completed successfully");
            dataPopulation.init();
            logger.info("Data population completed successfully");
            startServer();
        } catch (Exception e) {
            logger.error("Fatal error in main", e);
            System.exit(1);
        }
    }
    private static void startServer() {
        try {
            logger.info("Starting server initialization...");
            port(8080);
            threadPool(8);
            initializeApi();
            Thread.sleep(2000);
            awaitInitialization();
            logger.info("Server successfully started on port 8080");
            airdropWorker = new AirdropWorker(
                    new MockPayoutProvider(Config.getMockPayoutFailureRate()),
                    Config.getWorkerBatchSize(),
                    Config.getWorkerIntervalSeconds());
            airdropWorker.start();
            setupShutdownHook();
            try {
                stopLatch.await();
            } catch (InterruptedException e) {
                logger.warn("Server interrupted", e);
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            logger.error("Fatal error occurred during server startup", e);
            shutdown();
            System.exit(1);
        }
    }
    private static void initializeApi() {
        try {
            logger.info("Starting API initialization...");
            options("/*", (request, response) -> {
                String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
                if (accessControlRequestHeaders != null) {
                    response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
                }
                String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
                if (accessControlRequestMethod != null) {
                    response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
                }
                return "OK";
            });
            before((request, response) -> {
                String origin = request.headers("Origin");
                List<String> allowedOrigins = List.of(
                        "http://localhost:5173",
                        "https://l1-portal-d.vercel.app",
                        "https://l1-portal.vercel.app"
                );
                if (origin != null && allowedOrigins.contains(origin)) {
                    response.header("Access-Control-Allow-Origin", origin);
                    response.header("Access-Control-Allow-Credentials", "true");
                }
                response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                response.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Content-Length, Accept, Origin");
                response.header("Cross-Origin-Opener-Policy", "same-origin");
                response.header("Cross-Origin-Embedder-Policy", "require-corp");
                if ("OPTIONS".equalsIgnoreCase(request.requestMethod())) {
                    response.status(200);
                    halt();
                }
            });
            options("/*", (request, response) -> {
                String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
                if (accessControlRequestHeaders != null) {
                    response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
                }
                String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
                if (accessControlRequestMethod != null) {
                    response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
                }
                response.status(200);
                return "OK";
            });
            UserApi userApi = new UserApi();
            logger.info("I created my instance");
            userApi.initializeRoutes();
            logger.info("I created my routes");
            new CompanyApi().initializeRoutes();
            new AirdropApi().initializeRoutes();
            new ClaimApi().initializeRoutes();
            new PublicClaimApi().initializeRoutes();
            new AdminApi().initializeRoutes();
            get("/health", (req, res) -> {
                res.type("text/plain");
                return "OK";
            });
//            awaitInitialization();
            logger.info("API initialization completed successfully");
        } catch (Exception e) {
            logger.error("Error during API initialization", e);
            throw e;
        }
    }
    private static void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                logger.info("Received shutdown signal...");
                shutdown();
            } finally {
                stopLatch.countDown();
            }
        }));
    }
    private static void shutdown() {
        logger.info("Beginning shutdown process...");
        try {
            if (airdropWorker != null) {
                airdropWorker.stop();
            }
            logger.info("Stopping Spark server...");
            stop();
            logger.info("Shutting down PostgreSQL connections...");
            postgresExecutor.shutdown();
            if (!postgresExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                postgresExecutor.shutdownNow();
            }
            logger.info("Waiting for server to stop...");
            awaitStop();
            logger.info("Server stopped successfully");

        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }
}
