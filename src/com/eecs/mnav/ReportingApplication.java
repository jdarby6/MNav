package com.eecs.mnav;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import android.app.Application;
import android.content.Context;

@ReportsCrashes(formKey = "dFlQSEk0V25uMUdQblhLZkRMT2VyMUE6MQ",
mode = ReportingInteractionMode.TOAST,
forceCloseDialogAfterToast = false, // optional, default false
resToastText = R.string.crash_toast_text,
logcatArguments = { "-t", "100", "-v", "time" }) 
public class ReportingApplication extends Application {
	private static Context context;

	@Override
	public void onCreate() {
		super.onCreate();
		context = getApplicationContext();

		// The following line triggers the initialization of ACRA
		ACRA.init(this);
	}
	
	//This allows us to get the application's context anywhere
    public static Context getAppContext() {
        return context;
    }
}
