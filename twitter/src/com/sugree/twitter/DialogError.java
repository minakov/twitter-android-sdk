package com.sugree.twitter;

public class DialogError extends Throwable {
	private static final long serialVersionUID = -992704825747001028L;

	private final int errorCode;
	private final String failingUrl;

	public DialogError(String message, int errorCode, String failingUrl) {
		super(message);
		this.errorCode = errorCode;
		this.failingUrl = failingUrl;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public String getFailingUrl() {
		return failingUrl;
	}
}
