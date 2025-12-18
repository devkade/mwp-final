# Android 앱 구현 스펙 (PhotoViewer)

## 신규 Activity 구조

```
PhotoViewer/app/src/main/java/com/example/photoviewer/
├── LoginActivity.java          # 수정: 보안키 입력으로 변경
├── MachineListActivity.java    # 신규: 운동기구 목록
├── EventListActivity.java      # 신규: 이벤트 목록
├── EventDetailActivity.java    # 신규: 이벤트 상세
├── StatsActivity.java          # 신규: 통계 (선택)
├── models/
│   ├── GymMachine.java         # 신규: 기구 모델
│   └── MachineEvent.java       # 신규: 이벤트 모델
├── adapters/
│   ├── MachineAdapter.java     # 신규: 기구 목록 어댑터
│   └── EventAdapter.java       # 신규: 이벤트 목록 어댑터
└── services/
    └── GymApiService.java      # 신규: API 호출
```

## 모델 클래스

```java
// models/GymMachine.java
public class GymMachine {
    private int id;
    private String name;
    private String machineType;
    private String location;
    private String description;
    private String thumbnailUrl;
    private int eventCount;
    private LastEvent lastEvent;

    public static class LastEvent {
        private String eventType;
        private String capturedAt;
        // getters, setters
    }
    // getters, setters
}

// models/MachineEvent.java
public class MachineEvent {
    private int id;
    private int machineId;
    private String machineName;
    private String eventType;
    private String eventTypeDisplay;
    private String imageUrl;
    private String capturedAt;
    private int personCount;
    // getters, setters
}
```

## LoginActivity 수정

```java
// LoginActivity.java 수정

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etSecurityKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etSecurityKey = findViewById(R.id.etSecurityKey);
        Button btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> login());
    }

    private void login() {
        String securityKey = etSecurityKey.getText().toString().trim();

        if (securityKey.isEmpty()) {
            Toast.makeText(this, "보안키를 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // API 호출
        executorService.execute(() -> {
            try {
                URL url = new URL(BuildConfig.API_BASE_URL + "api/auth/login/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("security_key", securityKey);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes());
                os.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    // 토큰 저장 및 화면 이동
                    InputStream is = conn.getInputStream();
                    JSONObject response = new JSONObject(readStream(is));
                    String token = response.getString("token");

                    SecureTokenManager.getInstance().saveToken(token);

                    runOnUiThread(() -> {
                        Intent intent = new Intent(this, MachineListActivity.class);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    runOnUiThread(() ->
                        Toast.makeText(this, "잘못된 보안키입니다", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
```

## MachineListActivity

```java
// MachineListActivity.java

public class MachineListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MachineAdapter adapter;
    private List<GymMachine> machines = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_machine_list);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MachineAdapter(machines, machine -> {
            // 기구 선택 시 이벤트 목록으로 이동
            Intent intent = new Intent(this, EventListActivity.class);
            intent.putExtra("machine_id", machine.getId());
            intent.putExtra("machine_name", machine.getName());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        loadMachines();
    }

    private void loadMachines() {
        executorService.execute(() -> {
            try {
                URL url = new URL(BuildConfig.API_BASE_URL + "api_root/machines/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization",
                    "Token " + SecureTokenManager.getInstance().getToken());

                // JSON 파싱 및 UI 업데이트
                // ...
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
```

## EventListActivity

```java
// EventListActivity.java

public class EventListActivity extends AppCompatActivity {
    private int machineId;
    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private List<MachineEvent> events = new ArrayList<>();

    // 필터 상태
    private String filterEventType = null;  // null, "start", "end"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        machineId = getIntent().getIntExtra("machine_id", -1);
        String machineName = getIntent().getStringExtra("machine_name");

        setTitle(machineName);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EventAdapter(events, event -> {
            // 이벤트 선택 시 상세 화면으로 이동
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra("event_id", event.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        // 필터 칩 설정
        setupFilterChips();

        loadEvents();
    }

    private void setupFilterChips() {
        ChipGroup chipGroup = findViewById(R.id.chipGroupFilter);

        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) {
                filterEventType = null;
            } else if (checkedId == R.id.chipStart) {
                filterEventType = "start";
            } else if (checkedId == R.id.chipEnd) {
                filterEventType = "end";
            }
            loadEvents();
        });
    }

    private void loadEvents() {
        executorService.execute(() -> {
            try {
                String urlStr = BuildConfig.API_BASE_URL +
                    "api/machines/" + machineId + "/events/";

                if (filterEventType != null) {
                    urlStr += "?event_type=" + filterEventType;
                }

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization",
                    "Token " + SecureTokenManager.getInstance().getToken());

                // JSON 파싱 및 UI 업데이트
                // ...
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
```

## 레이아웃 파일

### activity_machine_list.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
        android:id="@+id/swipeRefresh"
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/recyclerView"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />

    </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>

</LinearLayout>
```

### item_machine.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="8dp"
    app:cardCornerRadius="8dp"
    app:cardElevation="4dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="16dp">

        <ImageView
            android:id="@+id/ivThumbnail"
            android:layout_width="80dp"
            android:layout_height="80dp"
            android:scaleType="centerCrop" />

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="16dp"
            android:orientation="vertical">

            <TextView
                android:id="@+id/tvName"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="18sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/tvLocation"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="14sp"
                android:textColor="@android:color/darker_gray" />

            <TextView
                android:id="@+id/tvEventCount"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="12sp" />

        </LinearLayout>

        <ImageView
            android:layout_width="24dp"
            android:layout_height="24dp"
            android:layout_gravity="center_vertical"
            android:src="@drawable/ic_chevron_right" />

    </LinearLayout>

</com.google.android.material.card.MaterialCardView>
```

### item_event.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="8dp"
    app:cardCornerRadius="8dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="12dp">

        <ImageView
            android:id="@+id/ivThumbnail"
            android:layout_width="100dp"
            android:layout_height="75dp"
            android:scaleType="centerCrop" />

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="12dp"
            android:orientation="vertical">

            <com.google.android.material.chip.Chip
                android:id="@+id/chipEventType"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="12sp" />

            <TextView
                android:id="@+id/tvCapturedAt"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="14sp"
                android:layout_marginTop="4dp" />

            <TextView
                android:id="@+id/tvPersonCount"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="12sp"
                android:textColor="@android:color/darker_gray" />

        </LinearLayout>

    </LinearLayout>

</com.google.android.material.card.MaterialCardView>
```
