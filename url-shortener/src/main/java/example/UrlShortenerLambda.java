package example;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.google.gson.Gson;

public class UrlShortenerLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final DynamoDB dynamoDB;
    private final String tableName = "URLShortener";

    public UrlShortenerLambda() {
        // Initialize the DynamoDB client and log the initialization step
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
        this.dynamoDB = new DynamoDB(client);
        System.out.println("DynamoDB client initialized successfully");
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Starting Lambda execution for UrlShortenerLambda");

        // Log the full event received from API Gateway
        context.getLogger().log("Received event: " + new Gson().toJson(event));

        // Extract the body from the event and log it
        String body = (String) event.get("body");
        context.getLogger().log("Received body: " + body);

        // Parse the request body to extract longUrl
        Request input;
        try {
            input = new Gson().fromJson(body, Request.class);
        } catch (Exception e) {
            context.getLogger().log("Error parsing request body: " + e.getMessage());
            return createErrorResponse(400, "Request body is invalid. Ensure it's in JSON format.");
        }

        // Validate the longUrl field
        String longUrl = input != null ? input.getLongUrl() : null;
        context.getLogger().log("Extracted longUrl: " + longUrl);
        
        if (longUrl == null || longUrl.trim().isEmpty()) {
            context.getLogger().log("Error: longUrl is null or empty");
            return createErrorResponse(400, "longUrl cannot be null or empty.");
        }

        // Generate a unique shortUrlId and log it
        String shortUrlId = UUID.randomUUID().toString().substring(0, 8);
        context.getLogger().log("Generated shortUrlId: " + shortUrlId);

        // Store the shortUrlId and longUrl in DynamoDB
        Table table = dynamoDB.getTable(tableName);
        try {
            context.getLogger().log("Storing shortUrlId and longUrl in DynamoDB");
            Item item = new Item().withPrimaryKey("shortUrlId", shortUrlId).withString("longUrl", longUrl);
            PutItemOutcome outcome = table.putItem(item);
            context.getLogger().log("Successfully stored shortUrlId: " + shortUrlId + " with longUrl: " + longUrl + " in DynamoDB");
        } catch (Exception e) {
            context.getLogger().log("Error storing data in DynamoDB: " + e.getMessage());
            return createErrorResponse(500, "Failed to store URL in DynamoDB.");
        }

        // Retrieve the API Gateway URL from environment variables
        String apiGatewayUrl = System.getenv("API_GATEWAY_URL");
        if (apiGatewayUrl == null || apiGatewayUrl.isEmpty()) {
            apiGatewayUrl = "https://sskddc3em1.execute-api.us-east-1.amazonaws.com/test"; // Fallback URL
            context.getLogger().log("API_GATEWAY_URL environment variable not set. Using default: " + apiGatewayUrl);
        } else {
            context.getLogger().log("Retrieved API Gateway URL from environment variable: " + apiGatewayUrl);
        }

        // Construct the shortened URL with "/shorten/" path and log it
        String shortUrl = apiGatewayUrl + "/shorten/" + shortUrlId;
        context.getLogger().log("Generated shortUrl: " + shortUrl);

        // Return the successful response containing the shortened URL
        return createSuccessResponse(200, shortUrl);
    }

    private Map<String, Object> createSuccessResponse(int statusCode, String shortUrl) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        response.put("headers", headers);

        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("shortUrl", shortUrl);
        response.put("body", new Gson().toJson(responseBody));

        return response;
    }

    private Map<String, Object> createErrorResponse(int statusCode, String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        response.put("headers", headers);

        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("error", errorMessage);
        response.put("body", new Gson().toJson(responseBody));

        return response;
    }
}
