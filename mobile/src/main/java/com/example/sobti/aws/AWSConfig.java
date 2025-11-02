package com.example.sobti.aws;

import android.content.Context;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

public class AWSConfig {

    // TODO: Replace with your AWS credentials
    private static final String COGNITO_POOL_ID = "us-east-1:YOUR-COGNITO-IDENTITY-POOL-ID";
    private static final Regions REGION = Regions.US_EAST_1;

    private static CognitoCachingCredentialsProvider credentialsProvider;
    private static AmazonDynamoDBClient ddbClient;

    public static void initialize(Context context) {
        if (credentialsProvider == null) {
            credentialsProvider = new CognitoCachingCredentialsProvider(
                    context.getApplicationContext(),
                    COGNITO_POOL_ID,
                    REGION
            );
        }

        if (ddbClient == null) {
            ddbClient = new AmazonDynamoDBClient(credentialsProvider);
            ddbClient.setRegion(com.amazonaws.regions.Region.getRegion(REGION));
        }
    }

    public static AmazonDynamoDBClient getDDBClient() {
        return ddbClient;
    }

    public static CognitoCachingCredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }
}