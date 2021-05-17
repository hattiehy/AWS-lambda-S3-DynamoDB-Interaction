package example;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import example.model.TagRequest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import example.model.TagRecord;
import example.model.TagRepose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SearchByTagsHandler implements RequestHandler<TagRequest, Object> {

    private static final Logger logger = LoggerFactory.getLogger(SearchByTagsHandler.class);
    Gson gson = new GsonBuilder().create();
    AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    DynamoDBMapper mapper = new DynamoDBMapper(client);

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
                tagRepose.getObjects().add(tagRecord);
            }
        }

        return tagRepose;
    }
}