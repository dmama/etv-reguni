package ch.vd.uniregctb.validation.rapport;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.validation.EntityValidatorImpl;

public abstract class RapportEntreTiersValidator<T extends RapportEntreTiers> extends EntityValidatorImpl<T> {

	@Override
	public ValidationResults validate(T ret) {
		final ValidationResults vr = new ValidationResults();

		if (!ret.isAnnule()) {

			final RegDate dateDebut = ret.getDateDebut();
			final RegDate dateFin = ret.getDateFin();

			// La date de début doit être renseignée
			if (dateDebut == null) {
				vr.addError(String.format("Le rapport-entre-tiers %s possède une date de début nulle", toDisplayString(ret)));
			}

			// Date de début doit être avant (ou égale) la date de fin
			if (dateDebut != null && dateFin != null && dateDebut.isAfter(dateFin)) {
				vr.addError(String.format("Le rapport-entre-tiers %s possède une date de début qui est après la date de fin: début = %s, fin = %s",
				                          toDisplayString(ret), RegDateHelper.dateToDisplayString(dateDebut), RegDateHelper.dateToDisplayString(dateFin)));
			}
		}

		return vr;
	}

	/**
	 * @param ret le rapport entre tiers dont on veut une description textuelle affichable
	 * @return une chaîne de caractères un peu plus explicite que le {@link RapportEntreTiers#toString() toString()}, notamment
	 * avec des informations utiles dans les messages d'erreurs envoyés à l'utilisateur
	 */
	protected static String toDisplayString(RapportEntreTiers ret) {
		return String.format("%s entre le tiers %s %s et le tiers %s %s",
		                     ret,
		                     ret.getDescriptionTypeSujet(), FormatNumeroHelper.numeroCTBToDisplay(ret.getSujetId()),
		                     ret.getDescriptionTypeObjet(), FormatNumeroHelper.numeroCTBToDisplay(ret.getObjetId()));
	}

}
