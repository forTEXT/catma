package de.catma.ui.module.main.auth;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * reCaptcha verification result bean
 * 
 * @author db
 *
 */
public class GoogleVerificationResult {

	private boolean success = false;
	private String hostname = "localhost";
	private float score = 0f;
	private String action = "";
	private String challenge_ts = "";
	private String[] errorCodes;
	
	public String getChallenge_ts() {
		return challenge_ts;
	}
	public void setChallenge_ts(String challenge_ts) {
		this.challenge_ts = challenge_ts;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public boolean isSuccess() {
		return success;
	}
	public void setSuccess(boolean success) {
		this.success = success;
	}
	public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	public float getScore() {
		return score;
	}
	public void setScore(float score) {
		this.score = score;
	}
	
	@JsonProperty("error-codes")
	public void setErrorCodes(String[] errorCodes) {
		this.errorCodes = errorCodes;
	}
	
	public String[] getErrorCodes() {
		return errorCodes == null?new String[] {}:errorCodes;
	}
	
	@Override
	public String toString() {
		return "GoogleVerificationResult "
			+ "[success=" + success + ", "
			+ "score=" + score + ", "
			+ "action=" + action + ", "
			+ "error-codes=" + (errorCodes==null?"[]":Arrays.asList(errorCodes)) + "]";
	}
	
	
}
