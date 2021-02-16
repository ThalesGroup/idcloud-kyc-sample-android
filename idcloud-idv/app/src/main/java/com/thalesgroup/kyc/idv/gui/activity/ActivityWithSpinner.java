/*
  Copyright (C) 2018 Aware, Inc - All Rights Reserved
 */
package com.thalesgroup.kyc.idv.gui.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;

import com.thalesgroup.kyc.idv.R;


public class ActivityWithSpinner extends AppCompatActivity {
	ProgressDialog hourglass;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}


	public void ShowHourglass(final String message)
	{
		runOnUiThread(new Runnable() {
			public void run() {
				hourglass = new ProgressDialog(ActivityWithSpinner.this);
				hourglass.setMessage(message);
				hourglass.setIndeterminate(true);
				hourglass.setCancelable(false);
				hourglass.show();
			}
		});
	}
	
	public void HideHourglass()
	{
		runOnUiThread(new Runnable() {
			public void run() {
				if (hourglass != null) {
					hourglass.dismiss();
					hourglass = null;
				}
			}
		});
	}

}
