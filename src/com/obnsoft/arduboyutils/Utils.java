package com.obnsoft.arduboyutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.obnsoft.arduboyutils.MyAsyncTaskWithDialog.Result;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class Utils {

    private static final int BUFFER_SIZE = 1024 * 1024;

    public static void showCustomDialog(
            Context context, int iconId, int titleId, View view, final OnClickListener listener) {
        final AlertDialog dlg = new AlertDialog.Builder(context)
                .setIcon(iconId)
                .setTitle(titleId)
                .setView(view)
                .setPositiveButton(android.R.string.ok, listener)
                .create();
        if (listener != null) {
            dlg.setButton(AlertDialog.BUTTON_NEGATIVE,
                    context.getText(android.R.string.cancel), (OnClickListener) null);
        }
        if (view instanceof EditText) {
            EditText editText = (EditText) view;
            editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        dlg.getWindow().setSoftInputMode(
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    }
                }
            });
            if (listener != null) {
                editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            listener.onClick(dlg, AlertDialog.BUTTON_POSITIVE);
                            return true;
                        }
                        return false;
                    }
                });
            }
        }
        dlg.show();
    }

    public static void showToast(Context context, int msgId) {
        Toast.makeText(context, msgId, Toast.LENGTH_SHORT).show();
    }

    public static void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static String getPathFromUri(final Context context, final Uri uri) {
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return generateTempFile(context, uri, false);
        } else if ("arduboy".equalsIgnoreCase(uri.getScheme())) {
            return generateTempFile(context, Uri.parse(uri.getEncodedSchemeSpecificPart()), true);
        }
        return null;
    }

    public static String generateTempFile(final Context context, final Uri uri,
            final boolean isNet) {
        final File file = new File(context.getCacheDir(), uri.getLastPathSegment());
        MyAsyncTaskWithDialog.ITask task = new MyAsyncTaskWithDialog.ITask() {
            private boolean mIsCancelled = false;
            @Override
            public Boolean task(ProgressDialog dialog) {
                InputStream in = null;
                OutputStream out = null;
                try {
                    if (isNet) {
                        HttpClient httpclient = new DefaultHttpClient();
                        HttpResponse httpResponse = httpclient.execute(new HttpGet(uri.toString()));
                        in = httpResponse.getEntity().getContent();
                    } else {
                        in = context.getContentResolver().openInputStream(uri);
                    }
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int length;
                    out = new FileOutputStream(file);
                    while ((length = in.read(buffer)) >= 0 && !mIsCancelled) {
                        out.write(buffer, 0, length);
                    }
                    out.close(); 
                    in.close();
                } catch (Exception e){
                    e.printStackTrace();
                    file.delete();
                    return false;
                }
                return true;
            };
            @Override
            public void cancel() {
                mIsCancelled = true;
            }
            @Override
            public void post(Result result) {
                switch (result) {
                case FAILED:
                    Utils.showToast(context, R.string.messageDownloadFailed);
                default:
                case CANCELLED:
                    file.delete();
                case SUCCEEDED:
                    break;
                }
            }
        };
        MyAsyncTaskWithDialog.execute(context, R.string.messageDownloading, task);
        return file.getAbsolutePath();
    }

    public static void cleanCacheFiles(Context context) {
        for (File file : context.getCacheDir().listFiles()) {
            file.delete();
        }
    }

    public static void showVersion(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View aboutView = inflater.inflate(R.layout.about, new ScrollView(context));
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            TextView textView = (TextView) aboutView.findViewById(R.id.textAboutVersion);
            textView.setText("Version " + packageInfo.versionName);

            StringBuilder buf = new StringBuilder();
            InputStream in = context.getResources().openRawResource(R.raw.license);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String str;
            while((str = reader.readLine()) != null) {
                buf.append(str).append('\n');
            }
            textView = (TextView) aboutView.findViewById(R.id.textAboutMessage);
            textView.setText(buf.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_menu_info_details)
                .setTitle(R.string.menuAbout)
                .setView(aboutView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

}
