package com.example.instagramdown;

import static com.example.instagramdown.utils.FileHelper.getMp4FilesFromFolder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.instagramdown.adapter.AdapterMP4_1;
import com.example.instagramdown.admob.GoogleMobileAdsConsentManager;
import com.example.instagramdown.admob.MyApplication;
import com.example.instagramdown.model.MP4model;
import com.example.instagramdown.utils.NetworkUtils;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.admanager.AdManagerAdView;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    TextInputEditText edtsetLink;
    private Disposable disposable;
    private Dialog dialog;
    private String nameVideo = "";
    List<MP4model> itemMp4;
    boolean isceck = false;
    private static final int DELAY_MILLIS = 1000;
    private Handler handler;
    private ListView lv_JustDownload;
    private final AtomicBoolean isMobileAdsInitializeCalled = new AtomicBoolean(false);
    private GoogleMobileAdsConsentManager googleMobileAdsConsentManager;
    private final AtomicBoolean initialLayoutComplete = new AtomicBoolean(false);
    static final String AD_UNIT = "/30497360/adaptive_banner_test_iu/backfill";
    private static final String TAG = "MyActivity";
    private AdManagerAdView adView;
    private FrameLayout adContainerView;
    private static final long COUNTER_TIME_MILLISECONDS = 5000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);
        DialogLoading1(this);
//        getSupportActionBar().hide();
//        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        adContainerView = findViewById(R.id.ad_view_container);
        lv_JustDownload = findViewById(R.id.lv_just_download);
        edtsetLink = findViewById(R.id.edt_setLink);
        TextView txtNamApp = findViewById(R.id.txt_nameApp);
        setStatusBarGradiant(this);
        googleMobileAdsConsentManager =
                GoogleMobileAdsConsentManager.getInstance(getApplicationContext());
        googleMobileAdsConsentManager.gatherConsent(
                this,
                consentError -> {
                    if (consentError != null) {
                        Log.w(
                                "LOG_TAG",
                                String.format(
                                        "%s: %s", consentError.getErrorCode(), consentError.getMessage()));
                    }

                    if (googleMobileAdsConsentManager.canRequestAds()) {
                        initializeMobileAdsSdk();
                        initializeMobileAdsSdkBanner();
                    }

                });
        if (googleMobileAdsConsentManager.canRequestAds()) {
            initializeMobileAdsSdk();
            initializeMobileAdsSdkBanner();
        }
        adContainerView
                .getViewTreeObserver()
                .addOnGlobalLayoutListener(
                        () -> {
                            if (!initialLayoutComplete.getAndSet(true)
                                    && googleMobileAdsConsentManager.canRequestAds()) {
                                loadBanner();
                            }
                        });

        MobileAds.setRequestConfiguration(
                new RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("ABCDEF012345")).build());
        createTimer(COUNTER_TIME_MILLISECONDS);

        if (isStoragePermissionGranted()) {
            File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), getString(R.string.app_name));
            if (!directory.exists()) {
                directory.mkdirs();
            }
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);
        itemMp4 = new ArrayList<>();
        handler = new Handler();
        findViewById(R.id.btn_paste).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                String pasteData;

                if (!(clipboard.hasPrimaryClip())) {
                } else if (!(clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))) {
                } else {
                    ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                    pasteData = item.getText().toString();
                    edtsetLink.setText(pasteData);
                }
            }
        });
        findViewById(R.id.btn_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isStoragePermissionGranted()) {
                    if (!NetworkUtils.isWifiConnected(MainActivity.this) && !NetworkUtils.isMobileDataConnected(MainActivity.this)) {
                        Toast.makeText(MainActivity.this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
                    } else {
                        String url = edtsetLink.getText().toString();
                        if (!url.isEmpty() && url.contains("ig") || url.contains("instagram")) {
                            Observable<String> observable = getResponseBody(url);
                            Observer<String> observer = getObserverBody();
                            observable.subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(observer);
                            dialog.show();
                            nameVideo ="CT_Video"+ extractVideoId(url)+System.currentTimeMillis()+".mp4";
                        } else {
                            Toast.makeText(MainActivity.this, "Enter a Valid URL!!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });
        findViewById(R.id.btn_help).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                FragmentManager fragmentManager = getSupportFragmentManager();
//                fragmentManager
//                        .beginTransaction()
//                        .replace(R.id.layout_main,new HelpsFragment())
//                        .commit();
                Intent intent = new Intent(MainActivity.this, HelpsActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);

            }
        });
        findViewById(R.id.btn_download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, DownloadActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//                builder.setView(R.layout.);
            }
        });
        TextPaint paint = txtNamApp.getPaint();
        float width = paint.measureText(txtNamApp.getText().toString());

        Shader textShader = new LinearGradient(0, 0, width, txtNamApp.getTextSize(),
                new int[]{
                        Color.parseColor("#FFCB52"),
                        Color.parseColor("#FF7B02"),
                }, null, Shader.TileMode.CLAMP);
        txtNamApp.getPaint().setShader(textShader);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setStatusBarGradiant(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            Drawable background = activity.getResources().getDrawable(R.color.white);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(activity.getResources().getColor(android.R.color.transparent));
//            window.setNavigationBarColor(activity.getResources().getColor(android.R.color.transparent));
            window.setBackgroundDrawable(background);
        }
    }

    private void DialogLoading() {
        ProgressDialog pd = new ProgressDialog(MainActivity.this);
        pd.setMessage("loading");
//        pd.setProgressDrawable();
        pd.show();
        Window view1 = pd.getWindow();
        view1.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
// to get rounded corners and border for dialog window
        view1.setBackgroundDrawableResource(R.drawable.boder_progressdialog);
    }

    public void DialogDownload() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_download, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();
//        BottomSheetDialog dialog1 = new BottomSheetDialog(MainActivity.this);
//        dialog1.setContentView(R.layout.dialog_download);
//        dialog1.show();
        Window view1 = dialog.getWindow();
        view1.setGravity(Gravity.BOTTOM);
        view1.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
// to get rounded corners and border for dialog window
        view1.setBackgroundDrawableResource(R.drawable.boder_progressdialog);
    }

    public void DialogLoading1() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.progress_dialog);
        dialog.show();
        Window view1 = dialog.getWindow();
        view1.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
// to get rounded corners and border for dialog window
        view1.setBackgroundDrawableResource(R.drawable.boder_progressdialog);
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            return true;
        }
    }

    private Observable<String> getResponseBody(String url) {

        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<String> emitter) throws Throwable {
                OkHttpClient client = new OkHttpClient();
                MediaType mediaType = MediaType.parse("application/json");
                RequestBody body = RequestBody.create(mediaType, "{\n\"video_url\": \"" + url + "\"\r}");
                Request request = new Request.Builder()
                        .url(getString(R.string.url_api))
                        .post(body)
                        .addHeader("content-type", "application/json")
                        .addHeader("X-RapidAPI-Key", getString(R.string.api_key))
                        .addHeader("X-RapidAPI-Host", getString(R.string.api_host))
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful() || response == null) {
                        if (!emitter.isDisposed()) {
                            emitter.onError(new IOException());
                        }
                    }
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        if (!emitter.isDisposed()) {
                            emitter.onNext(responseBody);
                            System.out.println("Response Body: " + responseBody);
                        }
                    } else {
                        System.out.println("Error: " + response.code() + " - " + response.message());
                    }
                    if (!emitter.isDisposed()) {
                        emitter.onComplete();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private Observer<String> getObserverBody() {
        return new Observer<String>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                Log.e("onSubscribe", "onSubscribe");
                disposable = d;
            }

            @Override
            public void onNext(@NonNull String s) {
                dialog.dismiss();
                Log.e("onNext", "onNext" + s);
                processJsonResponse(s);
                if (disposable != null) {
                    disposable.dispose();
                }

            }

            @Override
            public void onError(@NonNull Throwable e) {
                Log.e("onError", "onError");
            }

            @Override
            public void onComplete() {
                Log.e("onComplete", "onComplete");
            }
        };
    }

    public void DialogLoading1(Context context) {
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.progress_dialog);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        Window view1 = dialog.getWindow();
        view1.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        view1.setBackgroundDrawableResource(R.drawable.boder_progressdialog);
    }

    private void processJsonResponse(String jsonResponse) {
        JsonObject jsonObject = new Gson().fromJson(jsonResponse, JsonObject.class);
        if (jsonResponse != null) {
            String videoUrl = jsonObject.get("download_url").getAsString();
//            showBottomDialogMaatootz(videoUrl, musicUrl, videoid);
            Log.e("videoUrl", videoUrl);
            startDownload(videoUrl,nameVideo);
        }
    }

    public static String extractVideoId(String instagramUrl) {
        String pattern = "/reel/([A-Za-z0-9_-]+)/";
        Pattern r = Pattern.compile(pattern);

        Matcher m = r.matcher(instagramUrl);
        if (m.find()) {
            return m.group(1);
        } else {
            return null;
        }
    }

    private void startDownload(String url, String namefile) {
        dialog.show();
        itemMp4.clear();
        String destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + getString(R.string.app_name) + "/";
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), getString(R.string.app_name));

        if (!directory.exists()) {
            directory.mkdirs();
        }
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        request.setTitle(getString(R.string.download));
        request.setDescription(namefile);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            final Uri uri = Uri.parse("file://" + destination + namefile);
            request.setDestinationUri(uri);
        } else {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS + "/" + getString(R.string.app_name), namefile);
        }
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            long downloadID = downloadManager.enqueue(request);
            isceck = true;
            startDownloadStatusBroadcastLoop(downloadID, namefile);
        }
    }

    private void sendDownloadStatusBroadcast(long downloadId, String namefile) {
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            try (Cursor cursor = downloadManager.query(query)) {
                if (cursor.moveToFirst()) {
                    int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int status = cursor.getInt(statusIndex);

                    handleDownloadStatus(status, namefile);
                }
            }
        }
    }

    private void startDownloadStatusBroadcastLoop(long downloadID, String namefile) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isceck) {
                    sendDownloadStatusBroadcast(downloadID, namefile);
                    handler.postDelayed(this, DELAY_MILLIS);
                }

            }
        }, DELAY_MILLIS);
    }

    private void stopDownloadStatusBroadcastLoop() {
        dialog.dismiss();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            isceck = false;
        }
    }
    private void handleDownloadStatus(int status, String namefile1) {
        if (status == DownloadManager.STATUS_SUCCESSFUL) {

            if (namefile1.contains(".")) {
                String result = namefile1.substring(namefile1.lastIndexOf(".") + 1);
                System.out.println(result); // In ra phần mở rộng của tệp

                String folderPath1 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + getString(R.string.app_name);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                         if (result.equals("mp4")) {
                            itemMp4 = new ArrayList<>();
                            List<File> mp4Files = getMp4FilesFromFolder(folderPath1);
                            for (File mp4File : mp4Files) {
                                if (mp4File.getName().equals(namefile1)) {
                                    itemMp4.add(new MP4model(mp4File.getName(), mp4File.getAbsolutePath(), mp4File.getParent()));
                                    AdapterMP4_1 adapterMP3 = new AdapterMP4_1(MainActivity.this, itemMp4);
                                    lv_JustDownload.setAdapter(adapterMP3);
                                }
                            }

                        }
                    } else {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    }
                }

            }
            stopDownloadStatusBroadcastLoop();
        } else {
        }
    }
    private void showInterstitialAds(){
        Application application = getApplication();
        ((MyApplication) application)
                .showAdIfAvailable(
                        this,
                        new MyApplication.OnShowAdCompleteListener() {
                            @Override
                            public void onShowAdComplete() {
                                if (googleMobileAdsConsentManager.canRequestAds()) {

                                }
                            }
                        });
    }
    private void initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return;
        }

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this);

        // Load an ad.
        Application application = getApplication();
        ((MyApplication) application).loadAd(this);
    }
    private void loadBanner() {
        adView = new AdManagerAdView(this);
        adView.setAdUnitId(AD_UNIT);
        adView.setAdSize(getAdSize());

        adContainerView.removeAllViews();
        adContainerView.addView(adView);

        AdManagerAdRequest adRequest = new AdManagerAdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    private void initializeMobileAdsSdkBanner() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return;
        }

        MobileAds.initialize(
                this,
                new OnInitializationCompleteListener() {
                    @Override
                    public void onInitializationComplete(InitializationStatus initializationStatus) {
                    }
                });

        // Load an ad.
        if (initialLayoutComplete.get()) {
            loadBanner();
        }
    }

    private AdSize getAdSize() {

        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = outMetrics.density;

        float adWidthPixels = adContainerView.getWidth();

        if (adWidthPixels == 0) {
            adWidthPixels = outMetrics.widthPixels;
        }

        int adWidth = (int) (adWidthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }
    private void createTimer(long time) {

        CountDownTimer countDownTimer =
                new CountDownTimer(time, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    @Override
                    public void onFinish() {
                        showInterstitialAds();
                    }
                };
        countDownTimer.start();
    }
}
