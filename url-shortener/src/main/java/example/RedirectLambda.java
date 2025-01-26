package example;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.Map;

public class RedirectLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final DynamoDB dynamoDB;
    private final String tableName = "URLShortener";

    public RedirectLambda() {
        // Initialize DynamoDB client
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
        this.dynamoDB = new DynamoDB(client);
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("RedirectLambda execution started");

        // Extract path parameter 'shortUrlId'
        Map<String, String> pathParameters = (Map<String, String>) event.get("pathParameters");
        String shortUrlId = pathParameters.get("shortUrlId");
        context.getLogger().log("Received shortUrlId: " + shortUrlId);

        // Fetch the long URL from DynamoDB
        Table table = dynamoDB.getTable(tableName);
        Item item = table.getItem("shortUrlId", shortUrlId);

        if (item == null) {
            context.getLogger().log("No long URL found for shortUrlId: " + shortUrlId);
            return createErrorResponse(404, "URL not found for the given shortUrlId.");
        }

        String longUrl = item.getString("longUrl");
        context.getLogger().log("Successfully retrieved longUrl: " + longUrl);

        // Return a 301 Redirect response
        return createRedirectResponse(301, longUrl);
    }

    private Map<String, Object> createRedirectResponse(int statusCode, String locationUrl) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("statusCode", statusCode);
        
        Map<String, String> headers = new java.util.HashMap<>();
        headers.put("Location", locationUrl);
        response.put("headers", headers);

        return response;
    }

    private Map<String, Object> createErrorResponse(int statusCode, String errorMessage) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("statusCode", statusCode);
        
        Map<String, String> headers = new java.util.HashMap<>();
        headers.put("Content-Type", "application/json");
        response.put("headers", headers);

        Map<String, String> responseBody = new java.util.HashMap<>();
        responseBody.put("error", errorMessage);
        response.put("body", new com.google.gson.Gson().toJson(responseBody));

        return response;
    }
}
