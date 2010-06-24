package ch.vd.uniregctb.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidatorUtils {
	/**
	 * Valide une adresse email
	 * @param email
	 * @return
	 */
	public static boolean validateEmail(String email) {
		Pattern pCourrier = Pattern.compile("^([\\w\\-\\.]+)@((\\[([0-9]{1,3}\\.){3}[0-9]{1,3}\\])|(([\\w\\-]+\\.)+)([a-zA-Z]{2,4}))$");
		Matcher m = pCourrier.matcher(email);
		return m.matches();		
	}
	
	/**
	 * Valide un nouveau ou ancien numero AVS
	 * @param avs
	 * @return
	 */
	public static boolean validateAvs(String avs) {
		Pattern pAvsAncien = Pattern.compile("\\d{3}\\.\\d{2}\\.\\d{3}\\.\\d{3}");
		Pattern pAvsNouveau = Pattern.compile("\\d{3}\\.\\d{4}\\.\\d{4}\\.\\d{2}");
		Matcher mAvsAncient = pAvsAncien.matcher(avs);
		Matcher mAvsNouveau = pAvsNouveau.matcher(avs);

		return !(!mAvsNouveau.matches() && !mAvsAncient.matches());
	}
}
