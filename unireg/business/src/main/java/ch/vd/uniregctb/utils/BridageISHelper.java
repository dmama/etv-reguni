package ch.vd.uniregctb.utils;

public class BridageISHelper {
private static boolean bridageIS;

	public static boolean isBridageIS() {
		return bridageIS;
	}

	public void setBridageIS(String bridageIS) {
		BridageISHelper.bridageIS = "true".equalsIgnoreCase(bridageIS) || "1".equals(bridageIS) || "yes".equalsIgnoreCase(bridageIS);
	}

}
