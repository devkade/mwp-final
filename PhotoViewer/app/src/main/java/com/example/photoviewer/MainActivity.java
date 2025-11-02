package com.example.photoviewer;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PICK_IMAGE_REQUEST = 1;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ImageAdapter imageAdapter;
    private List<Post> postList = new ArrayList<>();
    private TextView textView;
    private Uri selectedImageUri;
    private ProgressBar progressBar;

    private final String site_url = "http://10.0.2.2:8000/";
    private final String token = "7aba936bb4d969ede07dbaf5c1c9a14a37e21fb0";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        imageAdapter = new ImageAdapter(postList, this::onPostClicked);
        recyclerView.setAdapter(imageAdapter);

        // Pull to Refresh 설정
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Swipe refresh triggered");
            onClickDownload(null);
        });
    }

    public void onClickDownload(View v) {
        Toast.makeText(getApplicationContext(), "이미지 동기화 중...", Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            List<Post> downloadedPosts = new ArrayList<>();
            try {
                URL url = new URL(site_url + "api_root/Post/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();
                    conn.disconnect();

                    JSONArray aryJson = new JSONArray(result.toString());
                    Log.d(TAG, "Total posts received: " + aryJson.length());

                    for (int i = 0; i < aryJson.length(); i++) {
                        JSONObject post_json = aryJson.getJSONObject(i);
                        int id = post_json.optInt("id", -1);
                        String title = post_json.optString("title", "No title");
                        String text = post_json.optString("text", "");
                        String imageUrl = post_json.getString("image");

                        Log.d(TAG, "Post #" + (i+1) + ": " + title);
                        Log.d(TAG, "Image URL: " + imageUrl);

                        if (imageUrl != null && !imageUrl.equals("null") && !imageUrl.isEmpty()) {
                            try {
                                Log.d(TAG, "Attempting to download image #" + (i+1));
                                URL myImageUrl = new URL(imageUrl);
                                HttpURLConnection imgConn = (HttpURLConnection) myImageUrl.openConnection();
                                imgConn.setConnectTimeout(5000);
                                imgConn.setReadTimeout(5000);

                                int imgResponseCode = imgConn.getResponseCode();
                                Log.d(TAG, "Image response code: " + imgResponseCode);

                                if (imgResponseCode == HttpURLConnection.HTTP_OK) {
                                    InputStream imgStream = imgConn.getInputStream();
                                    Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);

                                    if (imageBitmap != null) {
                                        Post post = new Post(id, title, text, imageUrl, imageBitmap);
                                        downloadedPosts.add(post);
                                        Log.d(TAG, "✓ Image #" + (i+1) + " downloaded successfully");
                                    } else {
                                        Log.e(TAG, "✗ Image #" + (i+1) + " decode failed - bitmap is null");
                                    }
                                    imgStream.close();
                                } else {
                                    Log.e(TAG, "✗ Image #" + (i+1) + " download failed - HTTP " + imgResponseCode);
                                }
                                imgConn.disconnect();
                            } catch (Exception e) {
                                Log.e(TAG, "✗ Error downloading image #" + (i+1) + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                        } else {
                            Log.w(TAG, "Post #" + (i+1) + " has no image");
                        }
                    }
                    Log.d(TAG, "Total posts downloaded: " + downloadedPosts.size());
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error in download task: " + e.getMessage());
                e.printStackTrace();
            }

            mainHandler.post(() -> {
                // Pull to Refresh 애니메이션 중지
                swipeRefreshLayout.setRefreshing(false);
                progressBar.setVisibility(View.GONE);

                if (!downloadedPosts.isEmpty()) {
                    Log.d(TAG, "Updating RecyclerView with " + downloadedPosts.size() + " posts");
                    postList.clear();
                    postList.addAll(downloadedPosts);
                    imageAdapter.notifyDataSetChanged();
                    Log.d(TAG, "notifyDataSetChanged() called, postList size: " + postList.size());
                    textView.setText("동기화 완료! (" + downloadedPosts.size() + "개 포스트)");
                    Toast.makeText(getApplicationContext(),
                        downloadedPosts.size() + "개의 포스트를 불러왔습니다.",
                        Toast.LENGTH_SHORT).show();
                } else {
                    textView.setText("포스트를 불러오지 못했습니다.");
                    Toast.makeText(getApplicationContext(),
                        "포스트 다운로드 실패. Logcat을 확인하세요.",
                        Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void onPostClicked(Post post) {
        try {
            // Null check for post
            if (post == null) {
                Toast.makeText(this, "포스트를 표시할 수 없습니다", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "onPostClicked: post is null");
                return;
            }

            // 포스트 상세보기 다이얼로그
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_post_detail, null);

            ImageView ivPostImage = dialogView.findViewById(R.id.ivPostImage);
            TextView tvPostTitle = dialogView.findViewById(R.id.tvPostTitle);
            TextView tvPostText = dialogView.findViewById(R.id.tvPostText);

            // Post 데이터로 뷰 채우기
            if (post.getImageBitmap() != null) {
                ivPostImage.setImageBitmap(post.getImageBitmap());
            }
            tvPostTitle.setText(post.getTitle());
            tvPostText.setText(post.getText());

            // AlertDialog로 표시
            new AlertDialog.Builder(this)
                .setView(dialogView)
                .setNegativeButton("닫기", null)
                .show();

            Log.d(TAG, "onPostClicked: dialog shown for post: " + post.getTitle());

        } catch (NullPointerException e) {
            Log.e(TAG, "onPostClicked - NullPointerException: " + e.getMessage(), e);
            Toast.makeText(this, "포스트 데이터를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException e) {
            Log.e(TAG, "onPostClicked - IllegalStateException: " + e.getMessage(), e);
            Toast.makeText(this, "다이얼로그를 표시할 수 없습니다", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "onPostClicked - Unexpected error: " + e.getMessage(), e);
            Toast.makeText(this, "포스트를 표시할 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickUpload(View v) {
        // 갤러리에서 이미지 선택
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            Log.d(TAG, "Image selected: " + selectedImageUri);

            if (selectedImageUri != null) {
                // 제목/내용 입력 다이얼로그 표시
                showUploadDialog(selectedImageUri);
            }
        }
    }

    private void showUploadDialog(Uri imageUri) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_upload, null);
        TextInputEditText etTitle = dialogView.findViewById(R.id.etTitle);
        TextInputEditText etText = dialogView.findViewById(R.id.etText);

        new AlertDialog.Builder(this)
            .setTitle("게시물 작성")
            .setView(dialogView)
            .setPositiveButton("업로드", (dialog, which) -> {
                String title = etTitle.getText().toString().trim();
                String text = etText.getText().toString().trim();

                if (title.isEmpty()) {
                    Toast.makeText(this, "제목을 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (text.isEmpty()) {
                    Toast.makeText(this, "내용을 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(this, "이미지 업로드 중...", Toast.LENGTH_SHORT).show();
                uploadImage(imageUri, title, text);
            })
            .setNegativeButton("취소", null)
            .show();
    }

    private void uploadImage(Uri imageUri, String title, String text) {
        progressBar.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            InputStream inputStream = null;

            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "===boundary===" + System.currentTimeMillis() + "===";

            try {
                // ContentResolver를 통해 InputStream 직접 얻기
                inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    mainHandler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        textView.setText("이미지를 읽을 수 없습니다.");
                        Toast.makeText(MainActivity.this, "이미지 업로드 실패", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // 파일 이름 가져오기
                String fileName = getFileName(imageUri);
                Log.d(TAG, "Uploading file: " + fileName);
                Log.d(TAG, "Title: " + title);
                Log.d(TAG, "Text: " + text);
                Log.d(TAG, "Uri: " + imageUri.toString());

                // HTTP 연결 설정
                URL url = new URL(site_url + "api_root/Post/");
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                dos = new DataOutputStream(conn.getOutputStream());

                // title 필드
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"title\"" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(title + lineEnd);

                // text 필드
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"text\"" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(text + lineEnd);

                // image 필드
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"" +
                             fileName + "\"" + lineEnd);
                dos.writeBytes("Content-Type: image/*" + lineEnd);
                dos.writeBytes(lineEnd);

                // 이미지 데이터 쓰기 (InputStream에서 직접 읽기)
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
                inputStream.close();
                Log.d(TAG, "Total bytes uploaded: " + totalBytesRead);

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                dos.flush();
                dos.close();

                // 응답 확인
                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Upload response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_CREATED ||
                    responseCode == HttpURLConnection.HTTP_OK) {
                    // 성공
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    Log.d(TAG, "Upload response: " + response.toString());

                    mainHandler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        textView.setText("업로드 성공!");
                        Toast.makeText(MainActivity.this,
                            "이미지가 성공적으로 업로드되었습니다!",
                            Toast.LENGTH_LONG).show();
                        // 업로드 후 자동 동기화
                        onClickDownload(null);
                    });
                } else {
                    // 실패
                    BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();
                    Log.e(TAG, "Upload failed: " + responseCode + " - " + errorResponse.toString());

                    mainHandler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        textView.setText("업로드 실패 (HTTP " + responseCode + ")");
                        Toast.makeText(MainActivity.this,
                            "업로드 실패: " + responseCode,
                            Toast.LENGTH_LONG).show();
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Upload error: " + e.getMessage());
                e.printStackTrace();

                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    textView.setText("업로드 에러: " + e.getMessage());
                    Toast.makeText(MainActivity.this,
                        "업로드 중 오류 발생",
                        Toast.LENGTH_LONG).show();
                });
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private String getFileName(Uri uri) {
        String fileName = "image.jpg"; // 기본값
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return fileName;
    }
}
