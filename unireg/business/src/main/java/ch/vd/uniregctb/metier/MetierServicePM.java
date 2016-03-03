package ch.vd.uniregctb.metier;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;

/**
 * Ensemble de services métiers de haut niveau, incluant toutes les régles fiscales nécessaires pour maintenir la cohérence des données.
 */
public interface MetierServicePM {

	ResultatAjustementForsSecondaires calculAjustementForsSecondairesPourEtablissementsVD(Entreprise entreprise, RegDate dateAuPlusTot) throws EvenementOrganisationException;

	interface ResultatAjustementForsSecondaires {
		/**
		 * @return liste des fors à annuler
		 */
		List<ForFiscalSecondaire> getAAnnuler();

		/**
		 * @return liste des fors à fermer
		 */
		List<MetierServicePMImpl.ForAFermer> getAFermer();

		/**
		 * @return liste des fors à créer
		 */
		List<ForFiscalSecondaire> getACreer();
	}

}
