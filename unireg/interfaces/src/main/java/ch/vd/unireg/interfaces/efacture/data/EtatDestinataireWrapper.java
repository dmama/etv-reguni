package ch.vd.unireg.interfaces.efacture.data;

import org.springframework.util.Assert;

import ch.vd.evd0025.v1.PayerSituationHistoryEntry;
import ch.vd.evd0025.v1.PayerStatus;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.XmlUtils;

public class EtatDestinataireWrapper {
	private String champLibre;
	private RegDate dateObtention;
	private String descriptionRaison;
	private Integer codeRaison;
	private TypeStatusDestinataire statusDestinataire;

	public EtatDestinataireWrapper(PayerSituationHistoryEntry payerSituationHistoryEntry) {
		this.champLibre = payerSituationHistoryEntry.getCustomField();
		this.dateObtention = XmlUtils.xmlcal2regdate(payerSituationHistoryEntry.getDate());
		this.descriptionRaison = payerSituationHistoryEntry.getReasonDescription();
		this.statusDestinataire = determineStatusDestinataire(payerSituationHistoryEntry.getStatus());
		this.codeRaison = payerSituationHistoryEntry.getReasonCode();

	}


	public static class Comparator implements java.util.Comparator<EtatDestinataireWrapper> {

		@Override
		public int compare(EtatDestinataireWrapper o1, EtatDestinataireWrapper o2) {

			final RegDate dateObtention1 = o1.getDateObtention();
			final RegDate dateObtention2 = o2.getDateObtention();
			Assert.notNull(dateObtention1);
			Assert.notNull(dateObtention2);

			if (dateObtention1 != dateObtention2) {
				// cas normal
				return dateObtention1.compareTo(dateObtention2);
			}

			// cas exceptionnel : deux états obtenu le même jour.
			final TypeStatusDestinataire status1 = o1.getStatusDestinataire();
			final TypeStatusDestinataire status2 = o2.getStatusDestinataire();
			Assert.notNull(status1);
			Assert.notNull(status2);

			// l'ordre est simplement l'ordre logique de l'enum
			if (status1 != status2) {
				return status1.compareTo(status2);
			}

			return 0;
		}
	}

	public String getChampLibre() {
		return champLibre;
	}

	public RegDate getDateObtention() {
		return dateObtention;
	}

	public String getDescriptionRaison() {
		return descriptionRaison;
	}

	public TypeStatusDestinataire getStatusDestinataire() {
		return statusDestinataire;
	}

	public Integer getCodeRaison() {
		return codeRaison;
	}

	private TypeStatusDestinataire determineStatusDestinataire(PayerStatus status) {
		if(status == null){
			return null;

		}
		switch (status) {
		case DESINSCRIT:
			return TypeStatusDestinataire.DESINSCRIT;
		case DESINSCRIT_SUSPENDU:
			return TypeStatusDestinataire.DESINSCRIT_SUSPENDU;
		case INSCRIT:
			return TypeStatusDestinataire.INSCRIT;
		case INSCRIT_SUSPENDU:
			return TypeStatusDestinataire.INSCRIT_SUSPENDU;
		default:
			throw new IllegalArgumentException("Le statut du destinataire suivant n'est pas reconnu " + status);

		}
	}
}
