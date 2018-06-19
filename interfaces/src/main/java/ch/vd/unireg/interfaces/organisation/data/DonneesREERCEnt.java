package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.List;

import ch.vd.registre.base.date.RegDate;

/**
 *   Utilisez les méthodes des helpers pour produire les données des accesseurs.
 *
 *   EntrepriseHelper fournit les méthodes nécessaires à l'accès par date:
 *   valuesForDate(), valueForDate() et dateRangeForDate(), à utiliser en priorité.
 */
public class DonneesREERCEnt implements DonneesREE, Serializable {

	private static final long serialVersionUID = 3097868283486480371L;

	private final List<DateRanged<InscriptionREE>> inscription;

	public DonneesREERCEnt(List<DateRanged<InscriptionREE>> inscription) {
		this.inscription = inscription;
	}

	@Override
	public List<DateRanged<InscriptionREE>> getInscriptionREE() {
		return inscription;
	}

	@Override
	public InscriptionREE getInscriptionREE(RegDate date) {
		return EntrepriseHelper.valueForDate(inscription, date);
	}
}
