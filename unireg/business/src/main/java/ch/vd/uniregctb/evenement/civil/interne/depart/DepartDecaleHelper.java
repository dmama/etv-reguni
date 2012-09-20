package ch.vd.uniregctb.evenement.civil.interne.depart;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.uniregctb.adresse.AdressesCivilesHisto;

public abstract class DepartDecaleHelper {

	@Nullable
	public static Adresse getAdresseResidenceTerminee(RegDate dateEvenementDepart, int decalageAutorise, AdressesCivilesHisto adressesHisto) {

		if (adressesHisto == null) {
			return null;
		}

		// recherche du matching parfait entre les dates : adresses principales puis secondaires
		final Adresse adressePrincipaleParfaite = findAdresseTermineeSansDecalage(dateEvenementDepart, adressesHisto.principales);
		if (adressePrincipaleParfaite != null) {
			return adressePrincipaleParfaite;
		}
		final Adresse adresseSecondaireParfaite = findAdresseTermineeSansDecalage(dateEvenementDepart, adressesHisto.secondaires);
		if (adresseSecondaireParfaite != null) {
			return adresseSecondaireParfaite;
		}

		// pas de matching parfait... on boucle d'abord sur toutes les adresses principales jusqu'à en trouver une dont la date de fin est assez proche de la date de l'événement
		// compte tenu du décalage autorisé...
		final Adresse adressePrincipaleTerminee = findAdresseTermineeAvecDecalage(dateEvenementDepart, decalageAutorise, adressesHisto.principales);
		if (adressePrincipaleTerminee != null) {
			return adressePrincipaleTerminee;
		}

		// ... puis on fait la même chose sur les adresses secondaires si aucune adresse principale ne semble correspondre
		return findAdresseTermineeAvecDecalage(dateEvenementDepart, decalageAutorise, adressesHisto.secondaires);
	}

	@Nullable
	private static Adresse findAdresseTermineeSansDecalage(RegDate dateEvenementDepart, List<Adresse> adresses) {
		if (adresses != null && adresses.size() > 0) {
			final Adresse adresseValable = DateRangeHelper.rangeAt(adresses, dateEvenementDepart);
			if (adresseValable != null && adresseValable.getDateFin() == dateEvenementDepart) {
				return adresseValable;
			}
		}
		return null;
	}

	@Nullable
	private static Adresse findAdresseTermineeAvecDecalage(RegDate dateEvenementDepart, int decalageAutorise, List<Adresse> adresses) {
		if (adresses != null && adresses.size() > 0) {
			for (int decalage = 1 ; decalage <= decalageAutorise ; ++ decalage) {
				for (Adresse adresse : adresses) {
					if (adresse.getDateFin() == dateEvenementDepart.addDays(-decalage)) {
						return adresse;
					}
				}
			}
		}
		return null;
	}
}
