package com.example.gardiafit.service.aws

import android.content.Context
import android.util.Log
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.PutItemRequest
import com.amazonaws.services.dynamodbv2.model.QueryRequest
import com.example.gardiafit.model.aws.HealthRecord
import com.example.gardiafit.model.aws.User
import com.example.gardiafit.model.aws.WorkoutSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID


class AWSService(private val context: Context) {

    //  REPLACE THIS with your actual Identity Pool ID from Step 1
    private val identityPoolId = "YOUR_IDENTITY_POOL_ID"  // Format: "us-east-1:xxxx-xxxx-xxxx"
    private val region = Regions.US_EAST_1

    private var credentialsProvider: CognitoCachingCredentialsProvider? = null
    private var dynamoDBClient: AmazonDynamoDBClient? = null
    private var dynamoDBMapper: DynamoDBMapper? = null

    /**
     * Initialize AWS SDK
     * Call this once when app starts
     */
    suspend fun initializeAWS(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Initializing AWS SDK...")

            // Validate Identity Pool ID
            if (identityPoolId == "YOUR_IDENTITY_POOL_ID") {
                Log.e(TAG, "ERROR: Identity Pool ID not configured!")
                Log.e(TAG, "Please replace YOUR_IDENTITY_POOL_ID with your actual ID from AWS Console")
                return@withContext false
            }

            // Create credentials provider
            credentialsProvider = CognitoCachingCredentialsProvider(
                context,
                identityPoolId,
                region
            )

            // Create DynamoDB client
            dynamoDBClient = AmazonDynamoDBClient(credentialsProvider).apply {
                setRegion(Region.getRegion(region))
            }

            // Create DynamoDB mapper for easy object mapping
            dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(dynamoDBClient)
                .build()

            Log.d(TAG, "AWS initialized successfully")
            Log.d(TAG, "Region: ${region.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "AWS initialization failed", e)
            Log.e(TAG, "Error details: ${e.message}")
            false
        }
    }

    /**
     * Save user to DynamoDB Users table
     */
    suspend fun saveUser(user: User): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            checkInitialized()

            val itemValues = mutableMapOf<String, AttributeValue>()
            itemValues["userId"] = AttributeValue().withS(user.userId)
            itemValues["username"] = AttributeValue().withS(user.username)
            itemValues["emergencyContact"] = AttributeValue().withS(user.emergencyContact)
            itemValues["emergencyNumber"] = AttributeValue().withS(user.emergencyNumber)
            itemValues["emergencyNote"] = AttributeValue().withS(user.emergencyNote)
            itemValues["createdAt"] = AttributeValue().withN(user.createdAt.toString())
            itemValues["isActive"] = AttributeValue().withBOOL(user.isActive)

            // Optional fields
            user.email?.let { itemValues["email"] = AttributeValue().withS(it) }
            user.phone?.let { itemValues["phone"] = AttributeValue().withS(it) }
            user.lastLogin?.let { itemValues["lastLogin"] = AttributeValue().withN(it.toString()) }
            user.deviceId?.let { itemValues["deviceId"] = AttributeValue().withS(it) }

            val request = PutItemRequest()
                .withTableName("Users")
                .withItem(itemValues)

            dynamoDBClient?.putItem(request)

            Log.d(TAG, "User saved: ${user.username} (${user.userId})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user: ${e.message}", e)
            false
        }
    }

    /**
     * Save health record to DynamoDB HealthRecords table
     */
    suspend fun saveHealthRecord(record: HealthRecord): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            checkInitialized()

            val itemValues = mutableMapOf<String, AttributeValue>()
            itemValues["recordId"] = AttributeValue().withS(record.recordId)
            itemValues["userId"] = AttributeValue().withS(record.userId)
            itemValues["timestamp"] = AttributeValue().withN(record.timestamp.toString())
            itemValues["heartRate"] = AttributeValue().withN(record.heartRate.toString())
            itemValues["steps"] = AttributeValue().withN(record.steps.toString())
            itemValues["calories"] = AttributeValue().withN(record.calories.toString())
            itemValues["distance"] = AttributeValue().withN(record.distance.toString())

            record.sessionId?.let { itemValues["sessionId"] = AttributeValue().withS(it) }

            val request = PutItemRequest()
                .withTableName("HealthRecords")
                .withItem(itemValues)

            dynamoDBClient?.putItem(request)

            Log.d(TAG, "Health record saved: HR=${record.heartRate}, Steps=${record.steps}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving health record: ${e.message}", e)
            false
        }
    }

    /**
     * Save workout session to DynamoDB WorkoutSessions table
     */
    suspend fun saveWorkoutSession(session: WorkoutSession): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            checkInitialized()

            val itemValues = mutableMapOf<String, AttributeValue>()
            itemValues["sessionId"] = AttributeValue().withS(session.sessionId)
            itemValues["userId"] = AttributeValue().withS(session.userId)
            itemValues["startTime"] = AttributeValue().withN(session.startTime.toString())
            itemValues["totalSteps"] = AttributeValue().withN(session.totalSteps.toString())
            itemValues["totalCalories"] = AttributeValue().withN(session.totalCalories.toString())
            itemValues["averageHeartRate"] = AttributeValue().withN(session.averageHeartRate.toString())
            itemValues["status"] = AttributeValue().withS(session.status.name)

            session.endTime?.let { itemValues["endTime"] = AttributeValue().withN(it.toString()) }

            val request = PutItemRequest()
                .withTableName("WorkoutSessions")
                .withItem(itemValues)

            dynamoDBClient?.putItem(request)

            Log.d(TAG, "Workout session saved: ${session.sessionId}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving workout session: ${e.message}", e)
            false
        }
    }

    /**
     * Get user from DynamoDB
     */
    suspend fun getUser(userId: String): User? = withContext(Dispatchers.IO) {
        return@withContext try {
            checkInitialized()

            val keyConditions = mapOf(
                ":userId" to AttributeValue().withS(userId)
            )

            val request = QueryRequest()
                .withTableName("Users")
                .withKeyConditionExpression("userId = :userId")
                .withExpressionAttributeValues(keyConditions)

            val result = dynamoDBClient?.query(request)
            val items = result?.items

            if (items.isNullOrEmpty()) {
                Log.d(TAG, "🔍 User not found: $userId")
                return@withContext null
            }

            val item = items[0]
            User(
                userId = item["userId"]?.s ?: "",
                username = item["username"]?.s ?: "",
                email = item["email"]?.s,
                phone = item["phone"]?.s,
                emergencyContact = item["emergencyContact"]?.s ?: "",
                emergencyNumber = item["emergencyNumber"]?.s ?: "",
                emergencyNote = item["emergencyNote"]?.s ?: "",
                createdAt = item["createdAt"]?.n?.toLongOrNull() ?: 0L,
                lastLogin = item["lastLogin"]?.n?.toLongOrNull(),
                deviceId = item["deviceId"]?.s,
                isActive = item["isActive"]?.bool ?: true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user: ${e.message}", e)
            null
        }
    }

    /**
     * Get health records for a user
     */
    suspend fun getHealthRecords(userId: String, limit: Int = 100): List<HealthRecord> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                checkInitialized()

                val keyConditions = mapOf(
                    ":userId" to AttributeValue().withS(userId)
                )

                val request = QueryRequest()
                    .withTableName("HealthRecords")
                    .withKeyConditionExpression("userId = :userId")
                    .withExpressionAttributeValues(keyConditions)
                    .withLimit(limit)
                    .withScanIndexForward(false) // Most recent first

                val result = dynamoDBClient?.query(request)
                val items = result?.items ?: emptyList()

                items.map { item ->
                    HealthRecord(
                        recordId = item["recordId"]?.s ?: "",
                        userId = item["userId"]?.s ?: "",
                        timestamp = item["timestamp"]?.n?.toLongOrNull() ?: 0L,
                        heartRate = item["heartRate"]?.n?.toIntOrNull() ?: 0,
                        steps = item["steps"]?.n?.toIntOrNull() ?: 0,
                        calories = item["calories"]?.n?.toIntOrNull() ?: 0,
                        distance = (item["distance"]?.n?.toFloatOrNull() ?: 0f).toInt(),
                        sessionId = item["sessionId"]?.s
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting health records: ${e.message}", e)
                emptyList()
            }
        }

    /**
     * Generate unique record ID
     */
    fun generateRecordId(): String = "rec_${UUID.randomUUID()}"

    /**
     * Generate unique session ID
     */
    fun generateSessionId(): String = "sess_${UUID.randomUUID()}"

    /**
     * Check if AWS is initialized
     */
    private fun checkInitialized() {
        if (dynamoDBClient == null) {
            throw IllegalStateException("AWS not initialized. Call initializeAWS() first.")
        }
    }

    companion object {
        private const val TAG = "AWSService"
    }
}