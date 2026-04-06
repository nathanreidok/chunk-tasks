package com.chunktasks.services;

import com.chunktasks.ChunkTasksConfig;
import com.chunktasks.tasks.ChunkTask;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import okhttp3.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static net.runelite.http.api.RuneLiteAPI.GSON;

@Singleton
@Slf4j
public class ChunkTasksSyncService {

    private static final String FIREBASE_API_KEY = "AIzaSyDr94cgRwmo3glVTirQ9LPAA4HI9cyYUCg";
    private static final String FIREBASE_DB_URL = "https://chunkpicker.firebaseio.com";
    private static final String FIREBASE_AUTH_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword";
    private static final String TASKS_MAP_URL = "https://source-chunk.github.io/chunk-picker-v2/tasksMap.json";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    // Cached reverse tasks map: short ID (e.g. "t_12246") → full name (e.g. "Kill X ~|Air elemental|~")
    private Map<String, String> tasksMapReverse;

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private ChunkTasksConfig config;

    /**
     * Authenticates with Firebase using the map code and password.
     * Returns the ID token for subsequent API calls.
     */
    public String authenticate() throws IOException {
        String mapCode = config.mapCode().toLowerCase();
        String password = config.chunkPickerPassword();
        String email = "sourcechunk+" + mapCode + "@yandex.com";
        String firebasePassword = password + mapCode;

        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", firebasePassword);
        body.addProperty("returnSecureToken", true);

        Request request = new Request.Builder()
                .url(FIREBASE_AUTH_URL + "?key=" + FIREBASE_API_KEY)
                .post(RequestBody.create(JSON_MEDIA_TYPE, body.toString()))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Firebase auth failed: " + response.code());
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Empty auth response");
            }
            JsonObject json = GSON.fromJson(responseBody.string(), JsonObject.class);
            return json.get("idToken").getAsString();
        }
    }

    /**
     * Reads the pluginOutput from Firebase.
     * Returns the raw JSON string of the plugin output array.
     */
    public String readPluginOutput(String authToken) throws IOException {
        String mapCode = config.mapCode().toLowerCase();
        String url = FIREBASE_DB_URL + "/maps/" + mapCode + "/pluginOutput.json?auth=" + authToken;

        Request request = new Request.Builder().url(url).build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to read pluginOutput: " + response.code());
            }
            ResponseBody body = response.body();
            return body == null ? "null" : body.string();
        }
    }

    /**
     * Writes the full pluginOutput array to Firebase.
     */
    public void writePluginOutput(String authToken, List<ChunkTask> tasks) throws IOException {
        String mapCode = config.mapCode().toLowerCase();
        String url = FIREBASE_DB_URL + "/maps/" + mapCode + "/pluginOutput.json?auth=" + authToken;

        // Build the plugin output JSON matching the website's format
        // Exclude backlogged tasks since they are not part of pluginOutput
        List<Map<String, Object>> pluginOutput = new ArrayList<>();
        for (ChunkTask task : tasks) {
            if (task.isBacklogged) {
                continue;
            }
            Map<String, Object> taskObj = new LinkedHashMap<>();
            taskObj.put("name", task.name);
            taskObj.put("isComplete", task.isComplete);
            taskObj.put("taskGroup", task.taskGroup != null ? task.taskGroup.name() : "OTHER");
            taskObj.put("items", task.items);
            taskObj.put("output", task.output);

            // Convert skills map: Skill enum keys → UPPERCASE string keys
            if (task.skills != null && !task.skills.isEmpty()) {
                Map<String, Integer> skillsMap = new LinkedHashMap<>();
                for (Map.Entry<Skill, Integer> entry : task.skills.entrySet()) {
                    skillsMap.put(entry.getKey().name(), entry.getValue());
                }
                taskObj.put("skills", skillsMap);
            } else {
                taskObj.put("skills", null);
            }

            taskObj.put("prefix", task.prefix != null ? task.prefix : "");
            pluginOutput.add(taskObj);
        }

        String json = GSON.toJson(pluginOutput);

        Request request = new Request.Builder()
                .url(url)
                .put(RequestBody.create(JSON_MEDIA_TYPE, json))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to write pluginOutput: " + response.code());
            }
        }
    }

    /**
     * Patches checkedChallenges in Firebase for newly completed tasks.
     * Uses Firebase PATCH to merge without overwriting existing entries.
     */
    public void patchCheckedChallenges(String authToken, Map<String, Set<String>> newlyCompletedBySkill) throws IOException {
        String mapCode = config.mapCode().toLowerCase();

        for (Map.Entry<String, Set<String>> entry : newlyCompletedBySkill.entrySet()) {
            String skill = encodeForFirebase(entry.getKey());
            String url = FIREBASE_DB_URL + "/maps/" + mapCode + "/chunkinfo/checkedChallenges/" + skill + ".json?auth=" + authToken;

            // Build the patch body: { "encodedTaskName": true, ... }
            Map<String, Boolean> patchBody = new LinkedHashMap<>();
            for (String taskName : entry.getValue()) {
                patchBody.put(encodeForFirebase(taskName), true);
            }

            String json = GSON.toJson(patchBody);

            Request request = new Request.Builder()
                    .url(url)
                    .patch(RequestBody.create(JSON_MEDIA_TYPE, json))
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("Failed to patch checkedChallenges for skill {}: {}", entry.getKey(), response.code());
                }
            }
        }
    }

    /**
     * Determines the skill/category name used as a key in checkedChallenges
     * based on the task's taskGroup and skills fields.
     */
    public static String getCheckedChallengesKey(ChunkTask task) {
        if (task.taskGroup == null) {
            return "Extra";
        }

        switch (task.taskGroup) {
            case QUEST:
                return "Quest";
            case DIARY:
                return "Diary";
            case BIS:
                return "BiS";
            case OTHER:
                return "Extra";
            case SKILL:
                // For SKILL tasks, get the skill name from the skills map
                if (task.skills != null && !task.skills.isEmpty()) {
                    Skill skill = task.skills.keySet().iterator().next();
                    // Convert MINING → Mining, HITPOINTS → Hitpoints, etc.
                    String name = skill.name();
                    return name.charAt(0) + name.substring(1).toLowerCase();
                }
                return "Extra";
            default:
                return "Extra";
        }
    }

    /**
     * Reads the backlog data from Firebase.
     * Returns a map of skill → set of task names that are backlogged.
     * Short IDs (e.g. t_12246) are resolved back to full task names using tasksMap.
     */
    public Map<String, Set<String>> readBacklog(String authToken) throws IOException {
        String mapCode = config.mapCode().toLowerCase();
        String url = FIREBASE_DB_URL + "/maps/" + mapCode + "/chunkinfo/backlog.json?auth=" + authToken;

        Request request = new Request.Builder().url(url).build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to read backlog: " + response.code());
            }
            ResponseBody body = response.body();
            String json = body == null ? "null" : body.string();
            if (json.equals("null") || json.isEmpty()) {
                return Collections.emptyMap();
            }

            // Ensure we have the reverse tasks map loaded
            ensureTasksMapLoaded();

            // Parse: { "skill": { "encodedTaskName": "noteOrValue", ... }, ... }
            Type mapType = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
            Map<String, Map<String, Object>> raw = GSON.fromJson(json, mapType);
            Map<String, Set<String>> backlog = new LinkedHashMap<>();
            if (raw != null) {
                for (Map.Entry<String, Map<String, Object>> entry : raw.entrySet()) {
                    String skill = decodeFromFirebase(entry.getKey());
                    Set<String> taskNames = new LinkedHashSet<>();
                    if (entry.getValue() != null) {
                        for (String encodedName : entry.getValue().keySet()) {
                            String decoded = decodeFromFirebase(encodedName);
                            // Resolve short ID to full task name if applicable
                            taskNames.add(resolveTaskId(decoded));
                        }
                    }
                    backlog.put(skill, taskNames);
                }
            }
            return backlog;
        }
    }

    /**
     * Fetches the tasksMap.json from the Chunk Picker website and builds
     * a reverse map (short ID → full task name). Cached after first fetch.
     */
    private void ensureTasksMapLoaded() {
        if (tasksMapReverse != null) {
            return;
        }
        try {
            Request request = new Request.Builder().url(TASKS_MAP_URL).build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("Failed to fetch tasksMap.json: {}", response.code());
                    tasksMapReverse = Collections.emptyMap();
                    return;
                }
                ResponseBody body = response.body();
                String json = body == null ? "{}" : body.string();
                Type mapType = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> tasksMap = GSON.fromJson(json, mapType);
                // Reverse: {fullName: shortId} → {shortId: fullName}
                tasksMapReverse = new LinkedHashMap<>();
                if (tasksMap != null) {
                    for (Map.Entry<String, String> entry : tasksMap.entrySet()) {
                        tasksMapReverse.put(entry.getValue(), entry.getKey());
                    }
                }
                log.debug("Loaded tasksMap with {} entries", tasksMapReverse.size());
            }
        } catch (IOException e) {
            log.warn("Failed to fetch tasksMap.json: {}", e.getMessage());
            tasksMapReverse = Collections.emptyMap();
        }
    }

    /**
     * Resolves a task identifier. If it's a short ID (e.g. "t_12246"),
     * returns the full task name. Otherwise returns the input as-is.
     */
    public String resolveTaskId(String nameOrId) {
        if (tasksMapReverse != null && tasksMapReverse.containsKey(nameOrId)) {
            return tasksMapReverse.get(nameOrId);
        }
        return nameOrId;
    }

    /**
     * Decodes a Firebase-encoded key back to the original string.
     * Reverses the encodeForFirebase encoding: -_- → %, then URL-decode,
     * then restore Firebase-specific replacements.
     */
    public static String decodeFromFirebase(String str) {
        // Step 1: Replace -_- back to %
        str = str.replace("-_-", "%");

        // Step 2: Restore Firebase-specific pre-replacements
        str = str.replace("%2G", "/").replace("%2F", "#").replace("%2E", ".");

        // Step 3: URL-decode remaining percent-encoded sequences
        try {
            str = java.net.URLDecoder.decode(str, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            // If decode fails, return as-is
        }

        return str;
    }

    /**
     * Encodes a string for use as a Firebase key, matching the website's
     * encodeRFC5987ValueChars(str, true) function.
     */
    public static String encodeForFirebase(String str) {
        // Step 1: Pre-replace chars that are invalid in Firebase keys
        str = str.replace(".", "%2E").replace("#", "%2F").replace("/", "%2G");

        // Step 2: URI-encode matching JavaScript's encodeURIComponent behavior
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '!' || c == '~') {
                // Safe chars: kept literal
                encoded.append(c);
            } else if (c == '|' || c == '`' || c == '^') {
                // These are decoded back to literal in the website's encoding
                encoded.append(c);
            } else if (c < 0x80) {
                // ASCII chars that need encoding
                encoded.append(String.format("%%%02X", (int) c));
            } else {
                // Multi-byte UTF-8 characters
                byte[] bytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                for (byte b : bytes) {
                    encoded.append(String.format("%%%02X", b & 0xFF));
                }
            }
        }

        // Step 3: Replace %25 with % (undo double-encoding of pre-replaced chars)
        String result = encoded.toString().replace("%25", "%");

        // Step 4: Replace % with -_- for Firebase storage
        result = result.replace("%", "-_-");

        return result;
    }
}
