package com.spring.springsftp;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;


@RunWith(SpringRunner.class)
@SpringBootTest
public class SftpConfigTests {

    private static EmbeddedSftpServer server;

    private static Path sftpFolder;

    private static String cpsLocation = "/home/towfiq/multibank_sftp_test/cps/";
    private static String destinationLocation = "/home/towfiq/workspace/spring-sftp/multibank_sftp_test/cps_raw_files/";

    private static String host = "192.168.1.164";
    private static String user = "towfiq";
    private static String pass = "1234";
    private static int port = 22;

    @BeforeClass
    public static void startServer() throws Exception {
        server = new EmbeddedSftpServer();
        server.setHost(host);
        server.setUsername(user);
        server.setPassword(pass);
        server.setPort(port);
        sftpFolder = Files.createTempDirectory("SFTP_SERVER_DIR");
        server.afterPropertiesSet();
        server.setHomeFolder(sftpFolder);
        // Starting SFTP
        if (!server.isRunning()) {
            server.start();
        }
    }

    @Test
    public void testDownload() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		// Prepare phase
//		Path tempFile = Files.createTempFile(sftpFolder, "TEST_DOWNLOAD_", ".xxx");
        Path tempFile = Paths.get(cpsLocation, "*.csv");
//		assertTrue(Files.notExists(tempFile));

		// Run async task to wait for expected files to be downloaded to a file
		// system from a remote SFTP server
        Future<Boolean> future = Executors.newSingleThreadExecutor().submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Path expectedFile = Paths.get(cpsLocation).resolve(tempFile.getFileName());
                while (!Files.exists(expectedFile)) {
                    Thread.sleep(200);
                }
                return true;
            }
        });

		// Validation phase
        assertTrue(future.get(15, TimeUnit.SECONDS));
        assertTrue(Files.notExists(tempFile));
    }

    @AfterClass
    public static void stopServer() {
        if (server.isRunning()) {
            server.stop();
        }
    }

}
