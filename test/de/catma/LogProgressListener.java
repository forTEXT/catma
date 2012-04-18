package de.catma;

import java.util.Arrays;

import de.catma.backgroundservice.ProgressListener;

public class LogProgressListener implements ProgressListener {

	public void setProgress(String value, Object... args) {
		System.out.println(
			value + ((args != null)? (" " +Arrays.toString(args)):""));
	}

	public void setException(Throwable t) {
		t.printStackTrace();
	}

}
