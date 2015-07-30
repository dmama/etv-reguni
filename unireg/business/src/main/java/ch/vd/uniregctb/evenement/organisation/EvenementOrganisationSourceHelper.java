package ch.vd.uniregctb.evenement.organisation;

import org.apache.commons.lang3.StringUtils;

import ch.vd.technical.esb.EsbMessage;

public class EvenementOrganisationSourceHelper {

	private static final String METADATA_EVENT_SOURCE = "eventSource";
	private static final String DEFAULT_BUSINESS_USER = "JMSEvtOrganisation-SansVisa";

	/**
	 * @param msg message reçu (= événement organisation)
	 * @return La chaîne de caractères à utiliser pour le visa de création de l'événement organisation en base Unireg
	 */
	public static String getVisaCreation(EsbMessage msg) {

		// temporaire pour choper les ECH99 // FIXME: Toujours d'actualité pour les organisation?
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
}
