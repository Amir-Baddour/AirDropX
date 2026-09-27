package org.example.Helpers;
import org.example.Config.Config;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class S3Helper {
    private static final String s3BucketEndpoint = Config.getS3BucketEndpoint();
    private static final String S3BucketName = Config.getS3BucketName();
    private static final String S3AccessKey = Config.getS3AccessKey();
    private static final String S3SecretKey = Config.getS3SecretKey();
    private static final String S3BucketRegion = Config.getS3BucketRegion();

    public static void uploadToS3(InputStream inputStream, String key, String contentType) throws IOException {

        S3Configuration config = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build();

        S3Client s3 = S3Client.builder()
                .endpointOverride(URI.create(s3BucketEndpoint))
                .serviceConfiguration(config)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                S3AccessKey,
                                S3SecretKey
                        )
                ))
                .region(software.amazon.awssdk.regions.Region.of(S3BucketRegion))
                .build();
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(S3BucketName)
                    .key(key)
                    .contentType(contentType)
                    .acl("public-read")
                    .build();

            s3.putObject(request, RequestBody.fromInputStream(inputStream, inputStream.available()));
        }
        catch (Exception e){
            throw e;
        }finally {
            s3.close();
        }
    }
    public static void deleteFromS3(String key) {
        S3Configuration config = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build();

        S3Client s3 = S3Client.builder()
                .endpointOverride(URI.create(s3BucketEndpoint))
                .serviceConfiguration(config)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(S3AccessKey, S3SecretKey)
                ))
                .region(software.amazon.awssdk.regions.Region.of(S3BucketRegion))
                .build();

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(S3BucketName)
                    .key(key)
                    .build();

            s3.deleteObject(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete object from S3", e);
        } finally {
            s3.close();
        }
    }
}