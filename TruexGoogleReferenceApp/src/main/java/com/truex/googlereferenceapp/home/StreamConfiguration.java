package com.truex.googlereferenceapp.home;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class StreamConfiguration implements Parcelable {
    private static String CLASSTAG = StreamConfiguration.class.getSimpleName();

    private String title;
    private String description;
    private String coverURL;
    private String previewURL;
    private String contentID;
    private String videoID;

    static void requestStreamConfigurations(OkHttpClient httpClient, String url,
                                                   RequestSuccessListener successListener,
                                                   RequestErrorListener errorListener) {
        Request req = new Request.Builder()
                .url(url)
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                errorListener.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful()) {
                    Exception e = new Exception("Request Error Response: " + response.code());
                    errorListener.onError(e);
                    return;
                }

                try {
                    // Retrieve the request body
                    String responseBody = response.body().string();
                    JSONArray jsonArray = new JSONArray(responseBody);

                    // Parse the stream configurations array
                    List<StreamConfiguration> streamConfigurations = getStreamConfigurations(jsonArray);

                    // If the stream configurations array is empty, throw an error
                    if (streamConfigurations.isEmpty()) {
                        Exception e = new Exception("Missing or invalid stream configuration");
                        errorListener.onError(e);
                        return;
                    }

                    // Share the stream configurations with the success listener
                    successListener.onSuccess(streamConfigurations);
                } catch (JSONException e) {
                    Log.e(CLASSTAG, "Error parsing response as JSON");
                    errorListener.onError(e);
                } catch (Exception e) {
                    Log.e(CLASSTAG, "Error retrieving stream configurations from response");
                    errorListener.onError(e);
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    private static List<StreamConfiguration> getStreamConfigurations(JSONArray jsonArray) {
        List<StreamConfiguration> streamConfigurations = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.optJSONObject(i);
            if (jsonObject == null) {
                continue;
            }
            StreamConfiguration streamConfiguration = getStreamConfiguration(jsonObject);
            if (streamConfiguration == null) {
                continue;
            }
            streamConfigurations.add(streamConfiguration);
        }
        return streamConfigurations;
    }

    public static StreamConfiguration getStreamConfiguration(JSONObject jsonObject) {
        try {
            StreamConfiguration streamConfiguration = new StreamConfiguration();
            streamConfiguration.title = jsonObject.getString("title");
            streamConfiguration.description = jsonObject.getString("description");
            streamConfiguration.coverURL = jsonObject.getString("cover");
            streamConfiguration.previewURL = jsonObject.getString("preview");
            streamConfiguration.contentID = jsonObject.getString("google_content_id");
            streamConfiguration.videoID = jsonObject.getString("google_video_id");
            return streamConfiguration;
        } catch (Exception e) {
            Log.d(CLASSTAG, "Unable to parse stream configuration JSON");
        }
        return null;
    }

    private StreamConfiguration() {
    }

    private StreamConfiguration(Parcel in) {
        String[] data = new String[6];

        in.readStringArray(data);

        title = data[0];
        description = data[1];
        coverURL = data[2];
        previewURL = data[3];
        contentID = data[4];
        videoID = data[5];
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCoverURL() {
        return coverURL;
    }

    public String getPreviewURL() {
        return previewURL;
    }

    public String getContentID() {
        return contentID;
    }

    public String getVideoID() {
        return videoID;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {
                title,
                description,
                coverURL,
                previewURL,
                contentID,
                videoID
        });
    }

    @Override
    public String toString() {
        return title;
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public StreamConfiguration createFromParcel(Parcel in) {
            return new StreamConfiguration(in);
        }

        public StreamConfiguration[] newArray(int size) {
            return new StreamConfiguration[size];
        }
    };

    public interface RequestSuccessListener {
        void onSuccess(List<StreamConfiguration> streamConfigurations);
    }

    public interface RequestErrorListener {
        void onError(Exception error);
    }
}
