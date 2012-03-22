package org.ua2.guantanamo.gui;

import android.content.Context;

public interface BackgroundWorker {
	public void during() throws Exception;
	public void after();
	
	public Context getContext();
}
