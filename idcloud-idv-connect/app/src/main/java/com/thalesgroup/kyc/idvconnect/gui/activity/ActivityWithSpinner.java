/*
  Copyright (C) 2018 Aware, Inc - All Rights Reserved
 */
package com.thalesgroup.kyc.idvconnect.gui.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;

import com.thalesgroup.kyc.idvconnect.R;


public class ActivityWithSpinner extends AppCompatActivity {
	ProgressDialog hourglass;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	public void ShowHourglass()
	{
		runOnUiThread(new Runnable() {
			public void run() {
				hourglass = new ProgressDialog(ActivityWithSpinner.this);
				hourglass.setMessage(getResources().getString(R.string.processing));
				hourglass.setIndeterminate(true);
				hourglass.setCancelable(false);
				hourglass.show();
			}
		});
	}


	public void ShowHourglassDirect()
	{
		hourglass = new ProgressDialog(ActivityWithSpinner.this);
		hourglass.setMessage("Processing...");
		hourglass.setIndeterminate(true);
		hourglass.setCancelable(false);
		hourglass.show();
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

	void HideKeyboard()
	{
		View view = this.getCurrentFocus();
		if (view != null) {
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}
}
