package de.catma.ui.events;

/**
 * Event fired when an invalid token has been suplied
 * @author db
 *
 */
public class TokenInvalidEvent {

	private final String reason;
	public TokenInvalidEvent(String reason ) {
		this.reason = reason;
	}
	
	public String getReason() {
		return reason;
	}

	@Override
	public String toString() {
		return getReason();
	}
}
