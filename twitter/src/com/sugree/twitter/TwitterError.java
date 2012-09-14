package com.sugree.twitter;

public class TwitterError extends Throwable {
	private static final long serialVersionUID = 6626439442641443626L;

	private int errorCode = 0;
	private String errorType;

	public TwitterError(String message) {
		super(message);
	}

	public TwitterError(String message, String errorType, int errorCode) {
		super(message);
		this.errorType = errorType;
		this.errorCode = errorCode;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public String getErrorType() {
		return errorType;
	}

}
