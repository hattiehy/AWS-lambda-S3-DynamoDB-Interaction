package example;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import example.model.DeleteObjectRequest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DeleteImageHandler implements RequestHandler<DeleteObjectRequest, String> {

    private static final Logger logger = LoggerFactory.getLogger(DeleteImageHandler.class);
    Gson gson = new GsonBuilder().create();
    //build DynamoDB
    AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
            .build();
    DynamoDB dynamoDB = new DynamoDB(client);
    Table table = dynamoDB.getTable("TagRecord");

    // build s3
    //Region region = Region.US_EAST_1;
    final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
    String bucket_name = "assignimagestore";

    @Override
    public String handleRequest(DeleteObjectRequest event, Context context) {
        logger.info("DeleteImageHandler receive request");
        logger.info("Event body:" + gson.toJson(event));
        String uid = event.getUid();
        String objectId = event.getObjectId();

        logger.info("objectId:" + objectId);
        // delete the item in s3
        try {
            logger.info("Attempting a s3 delete...");
            s3.deleteObject(bucket_name, objectId);
            logger.info("DeleteItem succeeded");
        } catch (AmazonServiceException e) {
            logger.error(e.getErrorMessage());
            System.exit(1);
        }

        logger.info("Success deleted s3 item");

        // delete the record in dynamoDB
        DeleteItemSpec deleteItemSpec = new DeleteItemSpec()
                .withPrimaryKey(new PrimaryKey("uid", uid, "objectId", objectId));

        try {
            logger.info("Attempting a db delete...");
            table.deleteItem(deleteItemSpec);
            logger.info("DeleteItem succeeded");
        }
        catch (Exception e) {
            logger.error("Unable to delete item: " + uid + " " + objectId);
            logger.error(e.getMessage());
        }

        return "Delete Success";
    }
}