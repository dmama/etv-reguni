package ch.vd.uniregctb.webservices.tiers2.stats;

import java.util.HashMap;
import java.util.Map;

public class Colors {
	public final static String DEFAULT_COLORS[] = {"FF0000", "00FF00", "0000FF", "008000", "224499", "49188F", "80C65A", "FF9900", "BBCCED"};

	public final static Map<String, String> USER_COLORS = new HashMap<String, String>();

	static {
		USER_COLORS.put("sipf", "AA0033");
		USER_COLORS.put("tao-pp", "FF00FF");
		USER_COLORS.put("tao-ba", "008000");
		USER_COLORS.put("tao-is", "00FF00");
		USER_COLORS.put("tao-com", "00FFFF");
		USER_COLORS.put("empaci", "FF9900");
		USER_COLORS.put("cat", "76A4FB");
		USER_COLORS.put("cedi", "224499");
		USER_COLORS.put("acicom", "0052F3");
		USER_COLORS.put("cor-it", "585858");
		USER_COLORS.put("unireg", "C0C000");
		USER_COLORS.put("sesam", "C05600");
	}

	public static String forUser(String user) {
		String color = USER_COLORS.get(user);
		if (color == null) {
			color = DEFAULT_COLORS[Math.abs(user.hashCode()) % DEFAULT_COLORS.length];
		}
		return color;
	}
}
