package de.catma.ui.modules.main.signup;

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
	
	@Override
	public String toString() {
		return "GoogleVerificationResult [success=" + success + ", score=" + score + ", action=" + action + "]";
	}
	
	
}
