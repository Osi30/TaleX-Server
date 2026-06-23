package com.talex.server.configs;

import com.talex.server.configs.properties.MediaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.mediaconvert.MediaConvertClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@RequiredArgsConstructor
public class AwsConfig {

    private final MediaProperties mediaProperties;

    @Bean
    public S3Client s3Client() {
        MediaProperties.Aws aws = mediaProperties.getAws();
        if (aws.getAccessKeyId().isBlank()) {
            return S3Client.builder()
                    .region(Region.of(aws.getRegion()))
                    .build();
        }
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                aws.getAccessKeyId(), aws.getSecretAccessKey());
        return S3Client.builder()
                .region(Region.of(aws.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        MediaProperties.Aws aws = mediaProperties.getAws();
        if (aws.getAccessKeyId().isBlank()) {
            return S3Presigner.builder()
                    .region(Region.of(aws.getRegion()))
                    .build();
        }
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                aws.getAccessKeyId(), aws.getSecretAccessKey());
        return S3Presigner.builder()
                .region(Region.of(aws.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    @Bean
    public MediaConvertClient mediaConvertClient() {
        MediaProperties.Aws aws = mediaProperties.getAws();
        var builder = MediaConvertClient.builder()
                .region(Region.of(aws.getRegion()));

        // MediaConvert requires account-specific endpoint URL
        if (aws.getMediaConvertEndpoint() != null && !aws.getMediaConvertEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(aws.getMediaConvertEndpoint()));
        }

        if (!aws.getAccessKeyId().isBlank()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(
                    aws.getAccessKeyId(), aws.getSecretAccessKey());
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        }

        return builder.build();
    }

    @Bean
    public SqsClient sqsClient() {
        MediaProperties.Aws aws = mediaProperties.getAws();
        if (aws.getAccessKeyId().isBlank()) {
            return SqsClient.builder()
                    .region(Region.of(aws.getRegion()))
                    .build();
        }
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                aws.getAccessKeyId(), aws.getSecretAccessKey());
        return SqsClient.builder()
                .region(Region.of(aws.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    @Bean
    public CloudFrontUtilities cloudFrontUtilities() {
        return CloudFrontUtilities.create();
    }
}
