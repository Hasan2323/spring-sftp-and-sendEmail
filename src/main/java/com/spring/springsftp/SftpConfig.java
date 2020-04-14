package com.spring.springsftp;

import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.integration.annotation.*;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizer;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizingMessageSource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.messaging.MessageHandler;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Configuration
public class SftpConfig {

    @Value("${sftp.host}")
    private String sftpHost;

    @Value("${sftp.user}")
    private String sftpUser;

    @Value("${sftp.pass}")
    private String sftpPassword;

    @Value("${sftp.port}")
    private int sftpPort;

    @Value("${sftp.privateKey:#{null}}")
    private Resource sftpPrivateKey;

    @Value("${sftp.publicKey:#{null}}")
    private Resource sftpPublicKey;

    @Value("${sftp.privateKeyPassphrase:}")
    private String sftpPrivateKeyPassphrase;

    @Value("${sftp.remote.main}")
    private String sftpRemoteMain;

    @Value("${sftp.remote.cps}")
    private String sftpRemoteCPS;

    @Value("${project.sftp}")
    private String projectSftpDirectory;

    @Value("${project.cps.raw.files}")
    private String projectCpsRawFiles;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    LocalDate today = LocalDate.now();
    String todayFormattedDate = formatter.format(today);


    // Connection Establishing
    @Bean
    public SessionFactory<LsEntry> sftpSessionFactory() {
        try{
            DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
            factory.setHost(sftpHost);
            factory.setPort(sftpPort);
            factory.setUser(sftpUser);
//        if (sftpPassword != null) {
            factory.setPassword(sftpPassword);
//        } else {
//            factory.setPrivateKey(sftpPrivateKey);
//            factory.setPrivateKeyPassphrase(sftpPrivateKeyPassphrase);
//        }
            factory.setAllowUnknownKeys(true);
            return new CachingSessionFactory<>(factory);
        }catch (Exception e) {
            throw new IllegalStateException("failed to create SFTP Connection", e);
        }
    }

    //Source File To Local

    @Bean
    public SftpInboundFileSynchronizer sftpInboundFileSynchronizer() {
        String todayFiles = "*" + todayFormattedDate + "*.csv"; // today's file
        String allFiles = "*.csv"; // All files
        String fileFormat = todayFiles;
        String cpsLocation = sftpRemoteMain + sftpRemoteCPS;
        SftpInboundFileSynchronizer fileSynchronizer = new SftpInboundFileSynchronizer(sftpSessionFactory());
        fileSynchronizer.setDeleteRemoteFiles(false);
        fileSynchronizer.setRemoteDirectory(cpsLocation);
        fileSynchronizer.setFilter(new SftpSimplePatternFileListFilter(fileFormat));
        return fileSynchronizer;
    }

    @Bean
    @InboundChannelAdapter(channel = "fromSftpChannel", poller = @Poller(cron = "0/5 * * * * *"))
    public MessageSource<File> sftpInboundMessageSource() {
        String localRawFileLocation = projectSftpDirectory + projectCpsRawFiles;
        SftpInboundFileSynchronizingMessageSource source =
                new SftpInboundFileSynchronizingMessageSource(sftpInboundFileSynchronizer());
        source.setLocalDirectory(new File(localRawFileLocation));
        source.setAutoCreateLocalDirectory(true);
        source.setLocalFilter(new AcceptOnceFileListFilter<File>());
        return source;
    }

    @Bean
    @ServiceActivator(inputChannel = "fromSftpChannel")
    public MessageHandler inboundResultFileHandler() {
        return message -> System.err.println(message.getPayload());
    }


//    // Local to Destination
//    @Bean
//    public SftpOutboundFileSynchronizer sftpOutboundFileSynchronizer() {
//        SftpOutboundFileSynchronizer fileSynchronizer = new SftpOutboundFileSynchronizer(sftpSessionFactory());
//        fileSynchronizer.setDeleteRemoteFiles(false);
//        fileSynchronizer.setRemoteDirectory(sftpRemoteDirectory);
//        fileSynchronizer.setFilter(new SftpSimplePatternFileListFilter("*.*"));
//        return fileSynchronizer;
//    }
//    @Bean
//    @OutboundChannelAdapter(channel = "toSftpChannel", poller = @Poller(cron = "0/5 * * * * *"))
//    public MessageSource<File> sftpOutboundMessageSource() {
//        SftpOutboundFileSynchronizingMessageSource source =
//                new SftpOutboundFileSynchronizingMessageSource(sftpOutboundFileSynchronizer());
//        source.setLocalDirectory(new File(localDirectory));
//        source.setAutoCreateLocalDirectory(true);
//        source.setLocalFilter(new AcceptOnceFileListFilter<File>());
//        return source;
//    }
//
//    @Bean
//    @ServiceActivator(inputChannel = "toSftpChannel")
//    public MessageHandler outboundResultFileHandler() {
//        return message -> System.err.println(message.getPayload());
//    }

}