package ch.vd.uniregctb.evenement.civil.ech;

import org.apache.commons.lang3.StringUtils;

import ch.vd.technical.esb.EsbMessage;

public class EvenementCivilEchSourceHelper {

	private static final String METADATA_EVENT_SOURCE = "eventSource";
	private static final String DEFAULT_BUSINESS_USER = "JMSEvtCivil-SansVisa";

	private static final String ECH99 = "ECH99";

	/**
	 * @param msg message reçu (= événement civil)
	 * @return La chaîne de caractères à utiliser pour le visa de création de l'événement civil en base Unireg
	 */
	public static String getVisaCreation(EsbMessage msg) {

		// temporaire pour choper les ECH99
		final String src = StringUtils.trimToNull(msg.getHeader(METADATA_EVENT_SOURCE));
		if (src != null) {
			return src;
		}

		final String bu = StringUtils.trimToNull(msg.getBusinessUser());
		if (bu != null) {
			return bu;
		}

		return DEFAULT_BUSINESS_USER;
	}

	/**
	 * Temporaire, afin de détecter les événements civils ECH qui sont issus de 99
	 * @param ech l'événement civil à tester
	 * @return <code>true</code> si on sait avec certitude que l'événement civil est issu d'un ech99
	 */
	public static boolean isFromEch99(EvenementCivilEchFacade ech) {
		return ECH99.equals(ech.getLogCreationUser());
	}

	/**
	 * Temporaire, afin de détecter les événements civils ECH qui sont issus de 99
	 * @param info l'événement civil à tester
	 * @return <code>true</code> si on sait avec certitude que l'événement civil est issu d'un ech99
	 */
	public static boolean isFromEch99(EvenementCivilEchBasicInfo info) {
		return ECH99.equals(info.getCreationUser());
	}

	/**
	 * Temporaire, visa de création à assigner à un événement dont on veut signifier qu'il est issu d'un ech99
	 */
	public static String getVisaForEch99() {
		return ECH99;
	}
}
