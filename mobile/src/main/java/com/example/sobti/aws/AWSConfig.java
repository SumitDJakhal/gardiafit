package com.example.sobti.aws;

import android.content.Context;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

public class AWSConfig {

    // TODO: Replace with your AWS credentials
    private static final String COGNITO_POOL_ID = "ap-south-1:90b512e3-2704-4758-a01d-053932a03e37";
    private static final Regions REGION = Regions.AP_SOUTH_1;

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