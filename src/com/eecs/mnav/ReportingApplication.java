package com.eecs.mnav;
import org.acra.*;
import org.acra.annotation.*;

import android.app.Application;
@ReportsCrashes(formKey = "dFlQSEk0V25uMUdQblhLZkRMT2VyMUE6MQ",
				mode = ReportingInteractionMode.TOAST,
				forceCloseDialogAfterToast = false, // optional, default false
				resToastText = R.string.crash_toast_text,
				logcatArguments = { "-t", "50", "-v", "time" }) 
public class ReportingApplication extends Application{
	 @Override
	  public void onCreate() {
	      super.onCreate();

	      // The following line triggers the initialization of ACRA
	      ACRA.init(this);
	  }
}
