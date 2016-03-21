/*
   Copyright 2014 John Selbie

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/


package com.selbie.wrek;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class AboutActivity extends Activity
{
    public final static String TAG = AboutActivity.class.getSimpleName();

    int _clickcount = 0;
    String _version = "";
    int _versionCode = 0;

    public final static String CLICKCOUNT = "com.selbie.wrek.AboutActivity.clickcount";
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        getVersionInfo();

        String versionLabel = this.getResources().getString(R.string.version_label);
        String version = versionLabel + _version;
        ((TextView)findViewById(R.id.tvAppVersion)).setText(version);
        
        TextView tv = (TextView) findViewById(R.id.tvGoogleGroup);
        String text = this.getString(R.string.feedback_group);
        tv.setText(Html.fromHtml(text));
        tv.setMovementMethod(LinkMovementMethod.getInstance());

        TextView tvBuildInfo = (TextView)findViewById(R.id.tvBuildInfo);
        tvBuildInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _clickcount++;
                AboutActivity.this.updateBuildText();
            }
        });

        if (savedInstanceState != null) {
            _clickcount = savedInstanceState.getInt(CLICKCOUNT, 0);
        }

        updateBuildText();
    }

    protected void onSaveInstanceState(Bundle bundle) {

        bundle.putInt(CLICKCOUNT, _clickcount);
        super.onSaveInstanceState(bundle);
    }

    private void updateBuildText()
    {
        String padding = "\n\n\n"; // scrollview needs padding, else the bottom text gets cut off
        String info = "";
        TextView tvBuildInfo = (TextView) findViewById(R.id.tvBuildInfo);

        if (_clickcount >= 3) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            info += "OS: " + Build.VERSION.RELEASE + " (" + Build.VERSION.INCREMENTAL + ")" + "\n";
            info += "API: " + Build.VERSION.SDK_INT + "\n";
            info += "Package: " + getPackageName() + "\n";
            info += "Version Code: " + _versionCode + "\n";
            info += "DisplayMetrics: " + dm.toString() + "\n";
        }
        info += padding;
        tvBuildInfo.setText(info);
    }
    
    private void getVersionInfo()
    {
        try
        {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            _version = info.versionName;
            _versionCode = info.versionCode;
        }
        catch (NameNotFoundException ex)
        {
            Log.e(TAG, "Can't get version number or code", ex);
        }
    }

}
