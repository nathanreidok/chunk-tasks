package com.chunktasks.managers;

import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static net.runelite.http.api.RuneLiteAPI.GSON;

@Slf4j
public abstract class JsonFileManager {

    private static final File DOWNLOAD_DIR = new File(RuneLite.RUNELITE_DIR.getPath() + File.separator + "chunk-tasks-json");
    private static final String DELETE_WARNING_FILENAME = "EXTRA_FILES_WILL_BE_DELETED_BUT_FOLDERS_WILL_REMAIN";
    private static final File DELETE_WARNING_FILE = new File(DOWNLOAD_DIR, DELETE_WARNING_FILENAME);
    private static final HttpUrl RAW_GITHUB = HttpUrl.parse("https://raw.githubusercontent.com/nathanreidok/resources/main/chunk-tasks");
    private static final String NAME_TO_ID_FILENAME = "Name_To_ID.json";

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void ensureDownloadDirectoryExists() {
        if (!DOWNLOAD_DIR.exists()) {
            DOWNLOAD_DIR.mkdirs();
        }
        try {
            DELETE_WARNING_FILE.createNewFile();
        } catch (IOException ignored) { }
    }

    public static void downloadJsonFiles(final OkHttpClient okHttpClient) {
        File[] downloadDirFiles = DOWNLOAD_DIR.listFiles();

        // Get set of existing files in our dir - existing sounds will be skipped, unexpected files (not dirs) will be deleted
        Set<String> filesPresent = new HashSet<>();
        if (downloadDirFiles != null && downloadDirFiles.length > 0) {
            Arrays.stream(downloadDirFiles)
                    .filter(file -> !file.isDirectory())
                    .map(File::getName)
                    .filter(filename -> !DELETE_WARNING_FILENAME.equals(filename))
                    .forEach(filesPresent::add);
        }

        for (String filename : filesPresent) {
            File toDelete = new File(DOWNLOAD_DIR, filename);
            //noinspection ResultOfMethodCallIgnored
            toDelete.delete();
        }


        if (RAW_GITHUB == null) {
            // Hush intellij, it's okay, the potential NPE can't hurt you now
            log.debug("Chunk Tasks could not download sounds due to an unexpected null RAW_GITHUB value");
            return;
        }
        HttpUrl nameToIdUrl = RAW_GITHUB.newBuilder().addPathSegment(NAME_TO_ID_FILENAME).build();
        Path outputPath = Paths.get(DOWNLOAD_DIR.getPath(), NAME_TO_ID_FILENAME);
        try (Response res = okHttpClient.newCall(new Request.Builder().url(nameToIdUrl).build()).execute()) {
            if (res.body() != null)
                Files.copy(new BufferedInputStream(res.body().byteStream()), outputPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.debug("Chunk Tasks could not download Name_To_Id json file", e);
        }
    }

    public static Map<String, Integer> getNameToIdMap() {
        try {
            InputStream stream = new BufferedInputStream(new FileInputStream(new File(DOWNLOAD_DIR, NAME_TO_ID_FILENAME)));
            Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            Type type = new TypeToken<Map<String, Integer>>() {}.getType();
            return GSON.fromJson(reader, type);
        } catch (Exception ex) {
            return new HashMap<>();
        }
    }
}
