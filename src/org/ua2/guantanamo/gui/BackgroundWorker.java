package org.ua2.guantanamo.gui;

import android.content.Context;

public interface BackgroundWorker {
	public void during(Context context) throws Exception;
	public void after();
}
