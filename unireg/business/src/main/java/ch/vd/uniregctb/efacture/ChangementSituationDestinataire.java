package ch.vd.uniregctb.efacture;

import ch.vd.evd0025.v1.PayerSituation;
import ch.vd.evd0025.v1.PayerSituationChangeEvent;
import ch.vd.evd0025.v1.PayerStatus;
import ch.vd.evd0025.v1.PayerWithSituation;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.XmlUtils;

public class ChangementSituationDestinataire extends EFactureEvent {

	/**
	 * Dans ces événements, ce qui nous intéresse est juste l'action d'inscription/désinscription, ou bien ?
	 */
	public static enum Action {
		INSCRIPTION,
		DESINSCRIPTION;

		public static Action get(PayerStatus ps) {
			switch (ps) {
				case DESINSCRIT:
				case DESINSCRIT_SUSPENDU:
					return DESINSCRIPTION;
				case INSCRIT:
				case INSCRIT_SUSPENDU:
					return INSCRIPTION;
				default:
					throw new IllegalArgumentException("Statut non supporté : " + ps);
			}
		}
	}

	final RegDate date;
	final long noTiers;
	final Action action;
	final String email;

	public ChangementSituationDestinataire(PayerSituationChangeEvent event) {
		this.date = XmlUtils.xmlcal2regdate(event.getDate());

		final PayerWithSituation payer = event.getPayer();
		this.noTiers = Long.parseLong(payer.getPayerId().getBusinessId());

		final PayerSituation situation = payer.getSituation();
		this.action = Action.get(situation.getStatus());
		this.email = situation.getEmailAddress();
	}

	public RegDate getDate() {
		return date;
	}

	public long getNoTiers() {
		return noTiers;
	}

	public Action getAction() {
		return action;
	}

	public String getEmail() {
		return email;
	}
}
