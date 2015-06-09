package com.material.management.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.util.Linkify;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import com.material.management.R;
import com.material.management.data.DeviceInfo;
import com.material.management.utils.Utility;

public class AboutDialog extends Dialog{

    private Context mContext = null;

    public AboutDialog(Context context) {
        super(context);
        mContext = context;
    }

    /**
     * This is the standard Android on create method that gets called when the activity initialized.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Window window = getWindow();
        float dialogWidth = mContext.getResources().getDimension(R.dimen.dialog_about_width_size);
        DeviceInfo deviceInfo = Utility.getDeviceInfo();

        setContentView(R.layout.dialog_about_layout);
        setTitle(R.string.title_app_about);

        setHtmlTextView((TextView) findViewById(R.id.legal_text), mContext.getString(R.string.about_legal));
        setHtmlTextView((TextView)findViewById(R.id.info_text), mContext.getString(R.string.about_info, deviceInfo.getAppVersion()));
        window.setLayout((int)dialogWidth, WindowManager.LayoutParams.WRAP_CONTENT);
    }
    
    private void setHtmlTextView(TextView tv, String htmlString) {
        tv.setText(Html.fromHtml(htmlString));
        tv.setLinkTextColor(Color.BLUE);
        Linkify.addLinks(tv, Linkify.ALL);
    }

//    private String readRawTextFile(int id) {
//        InputStream inputStream = mContext.getResources().openRawResource(id);
//        InputStreamReader in = new InputStreamReader(inputStream);
//        BufferedReader buf = new BufferedReader(in);
//        String line;
//        StringBuilder text = new StringBuilder();
//        try {
//            while (( line = buf.readLine()) != null) text.append(line);
//        } catch (IOException e) {
//            return null;
//        } finally {
//            try {
//                if(buf != null)
//                    buf.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        return text.toString();
//    }

}
