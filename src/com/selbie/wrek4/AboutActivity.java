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


package com.selbie.wrek4;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;

public class AboutActivity extends Activity
{
    public final static String TAG = AboutActivity.class.getSimpleName();
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        
        // set the app version
        String versionLabel = this.getResources().getString(R.string.version_label);
        String version = versionLabel + getVersionNumber();
        ((TextView)findViewById(R.id.tvAppVersion)).setText(version);
        
        TextView tv = (TextView) findViewById(R.id.tvGoogleGroup);
        String text = this.getString(R.string.feedback_group);
        tv.setText(Html.fromHtml(text));
        tv.setMovementMethod(LinkMovementMethod.getInstance());        
    }
    
    private String getVersionNumber()
    {
        String result = "";
        
        try
        {
            PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            result = pinfo.versionName;
        }
        catch (NameNotFoundException ex)
        {
            Log.e(TAG, "Can't get version number", ex);
        }
        
        return result;
    }
    

}
