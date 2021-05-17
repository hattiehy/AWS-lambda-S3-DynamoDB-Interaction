package example;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import example.model.AddTagRequest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import example.model.TagRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class AddExtraTagsHandler implements RequestHandler<AddTagRequest, Object> {

    private static final Logger logger = LoggerFactory.getLogger(AddExtraTagsHandler.class);
    Gson gson = new GsonBuilder().create();
    AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    DynamoDBMapper mapper = new DynamoDBMapper(client);

    @Override
    public Object handleRequest(AddTagRequest event, Context context) {
        logger.info("AddExtraTagsHandler receive request");

        logger.info("Event body:" + gson.toJson(event));
        String objectId = event.getObjectId();
        String uid = event.getUid();
        List<String> tags = event.getTags();

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":val1", new AttributeValue().withS(uid));
        eav.put(":val2", new AttributeValue().withS(objectId));

        DynamoDBQueryExpression<TagRecord> queryExpression = new DynamoDBQueryExpression<TagRecord>()
                .withKeyConditionExpression("uid = :val1 and s3_id = :val2").withExpressionAttributeValues(eav);

        List<TagRecord> records = mapper.query(TagRecord.class, queryExpression);
        logger.info("Query result:" + gson.toJson(records));

        TagRecord tagRecord = records.get(0);

        Set<String> oldTags = tagRecord.getTags();
        oldTags.addAll(tags);
        tagRecord.setTags(oldTags);

        return tagRecord;
    }
}