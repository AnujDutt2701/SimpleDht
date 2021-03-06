package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class OnLDumpClickListener implements View.OnClickListener {


    private static final String TAG = OnTestClickListener.class.getName();
    private static final int TEST_CNT = 50;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";

    private final TextView mTextView;
    private final ContentResolver mContentResolver;
    private final Uri mUri;

    public OnLDumpClickListener(TextView _tv, ContentResolver _cr) {
        mTextView = _tv;
        mContentResolver = _cr;
        mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    public void onClick(View v) {
        new OnLDumpClickListener.Task().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class Task extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            if (testQuery()) {
                publishProgress("Query success\n");
            } else {
                publishProgress("Query fail\n");
            }

            return null;
        }

        protected void onProgressUpdate(String... strings) {
            mTextView.append(strings[0]);

            return;
        }

        private boolean testQuery() {
            try {
                Cursor resultCursor = mContentResolver.query(mUri, null,
                        "@", null, null);
                if (resultCursor == null) {
                    Log.e(TAG, "Result null");
                    throw new Exception();
                }
            } catch (Exception e) {
                return false;
            }

            return true;
        }
    }
}
