package com.example.sobti.aws;

import android.os.AsyncTask;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;
import java.util.HashMap;
import java.util.Map;

public class DynamoDBManager {

    private static final String TABLE_NAME = "SobtiUsers";
    private AmazonDynamoDBClient ddbClient;

    public DynamoDBManager(AmazonDynamoDBClient client) {
        this.ddbClient = client;
    }

    // Check if user exists
    public void checkUserExists(String email, UserCheckCallback callback) {
        new CheckUserTask(callback).execute(email);
    }

    // Save new user
    public void saveUser(UserData userData, SaveUserCallback callback) {
        new SaveUserTask(callback).execute(userData);
    }

    // Update health data
    public void updateHealthData(String email, int heartRate, int steps, String location, UpdateCallback callback) {
        new UpdateHealthTask(callback).execute(email, String.valueOf(heartRate), String.valueOf(steps), location);
    }

    // Get user data
    public void getUserData(String email, GetUserCallback callback) {
        new GetUserTask(callback).execute(email);
    }

    // Callbacks
    public interface UserCheckCallback {
        void onResult(boolean exists, UserData userData);
        void onError(Exception e);
    }

    public interface SaveUserCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface GetUserCallback {
        void onSuccess(UserData userData);
        void onError(Exception e);
    }

    // AsyncTask for checking user
    private class CheckUserTask extends AsyncTask<String, Void, UserData> {
        private UserCheckCallback callback;
        private Exception error;

        CheckUserTask(UserCheckCallback callback) {
            this.callback = callback;
        }

        @Override
        protected UserData doInBackground(String... params) {
            try {
                String email = params[0];

                HashMap<String, AttributeValue> key = new HashMap<>();
                key.put("email", new AttributeValue().withS(email));

                GetItemRequest request = new GetItemRequest()
                        .withTableName(TABLE_NAME)
                        .withKey(key);

                GetItemResult result = ddbClient.getItem(request);

                if (result.getItem() != null && !result.getItem().isEmpty()) {
                    return parseUserData(result.getItem());
                }
                return null;
            } catch (Exception e) {
                error = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(UserData userData) {
            if (error != null) {
                callback.onError(error);
            } else {
                callback.onResult(userData != null, userData);
            }
        }
    }

    // AsyncTask for saving user
    private class SaveUserTask extends AsyncTask<UserData, Void, Boolean> {
        private SaveUserCallback callback;
        private Exception error;

        SaveUserTask(SaveUserCallback callback) {
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(UserData... params) {
            try {
                UserData userData = params[0];

                Map<String, AttributeValue> item = new HashMap<>();
                item.put("email", new AttributeValue().withS(userData.email));
                item.put("name", new AttributeValue().withS(userData.name));
                item.put("age", new AttributeValue().withN(String.valueOf(userData.age)));
                item.put("height", new AttributeValue().withN(String.valueOf(userData.height)));
                item.put("weight", new AttributeValue().withN(String.valueOf(userData.weight)));
                item.put("emergencyNumber", new AttributeValue().withS(userData.emergencyNumber));
                item.put("createdAt", new AttributeValue().withN(String.valueOf(System.currentTimeMillis())));

                PutItemRequest request = new PutItemRequest()
                        .withTableName(TABLE_NAME)
                        .withItem(item);

                ddbClient.putItem(request);
                return true;
            } catch (Exception e) {
                error = e;
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (error != null) {
                callback.onError(error);
            } else {
                callback.onSuccess();
            }
        }
    }

    // AsyncTask for updating health data
    private class UpdateHealthTask extends AsyncTask<String, Void, Boolean> {
        private UpdateCallback callback;
        private Exception error;

        UpdateHealthTask(UpdateCallback callback) {
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                String email = params[0];
                String heartRate = params[1];
                String steps = params[2];
                String location = params[3];

                HashMap<String, AttributeValue> key = new HashMap<>();
                key.put("email", new AttributeValue().withS(email));

                HashMap<String, AttributeValueUpdate> updates = new HashMap<>();
                updates.put("lastHeartRate", new AttributeValueUpdate()
                        .withValue(new AttributeValue().withN(heartRate))
                        .withAction(AttributeAction.PUT));
                updates.put("lastSteps", new AttributeValueUpdate()
                        .withValue(new AttributeValue().withN(steps))
                        .withAction(AttributeAction.PUT));
                updates.put("lastLocation", new AttributeValueUpdate()
                        .withValue(new AttributeValue().withS(location))
                        .withAction(AttributeAction.PUT));
                updates.put("lastUpdated", new AttributeValueUpdate()
                        .withValue(new AttributeValue().withN(String.valueOf(System.currentTimeMillis())))
                        .withAction(AttributeAction.PUT));

                UpdateItemRequest request = new UpdateItemRequest()
                        .withTableName(TABLE_NAME)
                        .withKey(key)
                        .withAttributeUpdates(updates);

                ddbClient.updateItem(request);
                return true;
            } catch (Exception e) {
                error = e;
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (error != null) {
                callback.onError(error);
            } else {
                callback.onSuccess();
            }
        }
    }

    // AsyncTask for getting user data
    private class GetUserTask extends AsyncTask<String, Void, UserData> {
        private GetUserCallback callback;
        private Exception error;

        GetUserTask(GetUserCallback callback) {
            this.callback = callback;
        }

        @Override
        protected UserData doInBackground(String... params) {
            try {
                String email = params[0];

                HashMap<String, AttributeValue> key = new HashMap<>();
                key.put("email", new AttributeValue().withS(email));

                GetItemRequest request = new GetItemRequest()
                        .withTableName(TABLE_NAME)
                        .withKey(key);

                GetItemResult result = ddbClient.getItem(request);

                if (result.getItem() != null) {
                    return parseUserData(result.getItem());
                }
                return null;
            } catch (Exception e) {
                error = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(UserData userData) {
            if (error != null) {
                callback.onError(error);
            } else {
                callback.onSuccess(userData);
            }
        }
    }

    private UserData parseUserData(Map<String, AttributeValue> item) {
        UserData userData = new UserData();
        userData.email = item.get("email").getS();
        userData.name = item.get("name").getS();
        userData.age = Integer.parseInt(item.get("age").getN());
        userData.height = Integer.parseInt(item.get("height").getN());
        userData.weight = Integer.parseInt(item.get("weight").getN());
        userData.emergencyNumber = item.get("emergencyNumber").getS();

        if (item.containsKey("lastHeartRate")) {
            userData.lastHeartRate = Integer.parseInt(item.get("lastHeartRate").getN());
        }
        if (item.containsKey("lastSteps")) {
            userData.lastSteps = Integer.parseInt(item.get("lastSteps").getN());
        }
        if (item.containsKey("lastLocation")) {
            userData.lastLocation = item.get("lastLocation").getS();
        }

        return userData;
    }

    // User Data Model
    public static class UserData {
        public String email;
        public String name;
        public int age;
        public int height;
        public int weight;
        public String emergencyNumber;
        public int lastHeartRate;
        public int lastSteps;
        public String lastLocation;
    }
}