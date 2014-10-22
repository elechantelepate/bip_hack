package com.example.puntobip;

import android.app.Dialog;
import android.content.Context;

public class TapDialog extends Dialog {
	private int tag = 0;
	public TapDialog(Context context) {
		super(context);
		this.setContentView(R.layout.tap_dialog);
	}
	public int getTag() {
		return tag;
	}
	public void setTag(int tag) {
		this.tag = tag;
	}
}
