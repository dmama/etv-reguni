package ch.vd.uniregctb.utils;

public class TestModeHelper {

	private static boolean testMode;
	
	public static boolean isTestMode() {
		return testMode;
	}

	public void setTestMode(String testMode) {
		TestModeHelper.testMode = "true".equalsIgnoreCase(testMode) || "1".equals(testMode) || "yes".equalsIgnoreCase(testMode);
	}
	
}
