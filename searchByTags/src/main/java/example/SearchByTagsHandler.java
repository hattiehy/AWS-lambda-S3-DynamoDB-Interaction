package example;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import example.model.TagRequest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import example.model.TagRecord;
import example.model.TagRepose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SearchByTagsHandler implements RequestHandler<TagRequest, Object> {

    private static final Logger logger = LoggerFactory.getLogger(SearchByTagsHandler.class);
    Gson gson = new GsonBuilder().create();
    AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    DynamoDBMapper mapper = new DynamoDBMapper(client);
    Regions clientRegion = Regions.AP_SOUTHEAST_2;
    String bucketName = "image-store-tagtag";
    //String objectKey = "*** Object key ***";

    @Override
    public Object handleRequest(TagRequest event, Context context) {
        logger.info("SearchByTagsHandler receive request");

        logger.info("Event body:" + gson.toJson(event));
        String uid = event.getUid();
        List<String> tags = event.getTags();

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":val1", new AttributeValue().withS(uid));

        DynamoDBQueryExpression<TagRecord> queryExpression = new DynamoDBQueryExpression<TagRecord>()
                .withKeyConditionExpression("uid = :val1").withExpressionAttributeValues(eav);

        List<TagRecord> records = mapper.query(TagRecord.class, queryExpression);
        logger.info("Query result:" + gson.toJson(records));

        TagRepose tagRepose = new TagRepose();

        for (TagRecord tagRecord : records) {
            if (tagRecord.getTags().containsAll(tags)) {
                URL url = generatePresignedUrl(tagRecord.getObjectId());
                tagRecord.setUrl(url);
                tagRepose.getObjects().add(tagRecord);
            }
        }

        return tagRepose;
    }


    private URL generatePresignedUrl(String objectKey) {
//        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
//                .withRegion(clientRegion)
//                .withCredentials(new ProfileCredentialsProvider())
//                .build();
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .build();

        // Set the presigned URL to expire after one hour.
        Date expiration = Date.from(Instant.now().plusSeconds(3600*24));

        // Generate the presigned URL.
        System.out.println("Generating pre-signed URL.");
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, objectKey)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(expiration);
        URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);

        System.out.println("Pre-Signed URL: " + url.toString());

        return url;
    }


}