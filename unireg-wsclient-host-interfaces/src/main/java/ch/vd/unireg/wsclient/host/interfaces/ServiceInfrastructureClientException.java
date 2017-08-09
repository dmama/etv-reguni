package ch.vd.unireg.wsclient.host.interfaces;

import javax.ws.rs.WebApplicationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class ServiceInfrastructureClientException extends RuntimeException {
	private final transient Throwable cause;
	private static final Pattern TITLE_PATTERN = Pattern.compile(".*?<title>(.*?)</title>.*", 34);

	public ServiceInfrastructureClientException(WebApplicationException e) {
		this(buildShortMessage(e), e);
	}

	protected ServiceInfrastructureClientException(String shortMessage, Throwable cause) {
		super(shortMessage);
		this.cause = cause;
	}

	private static String buildShortMessage(WebApplicationException e) {
		StringBuilder s = new StringBuilder();
		s.append("Status ").append(e.getResponse().getStatus());
		String title = extractTitle(e.getMessage());
		if(title != null) {
			s.append(" (").append(title).append(")");
		}

		return s.toString();
	}

	protected static String extractTitle(String html) {
		if(StringUtils.isBlank(html)) {
			return null;
		} else {
			Matcher matcher = TITLE_PATTERN.matcher(html);
			return matcher.matches()?matcher.group(1):null;
		}
	}

	public Throwable getCause() {
		return this.cause != null?this.cause:super.getCause();
	}
}
