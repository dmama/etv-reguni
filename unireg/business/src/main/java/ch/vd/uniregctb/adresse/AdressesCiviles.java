package ch.vd.uniregctb.adresse;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.interfaces.model.Adresse;

/**
 * Contient les adresses civiles à un instant donné d'individu regroupées par type
 */
public class AdressesCiviles {
	public Adresse principale;
	public Adresse courrier;
	public Adresse secondaire;
	public Adresse tutelle;

	public void set(Adresse adresse, boolean strict) throws DonneesCivilesException {
		if (adresse.getTypeAdresse().equals(EnumTypeAdresse.PRINCIPALE)) {
			if (principale == null) {
				principale = adresse;
			}
			else {
				if (strict) {
					throw new DonneesCivilesException("Plus d'une adresse 'principale' détectée");
				}

				// deux adresses valides à la même date -> on prend la plus récente en espérant que ce soit la plus juste
				if (DateRangeComparator.compareRanges(principale, adresse) > 0) {
					// on ne change rien
				}
				else {
					principale = adresse;
				}
			}
		}
		else if (adresse.getTypeAdresse().equals(EnumTypeAdresse.COURRIER)) {
			if (courrier == null) {
				courrier = adresse;
			}
			else {
				if (strict) {
					throw new DonneesCivilesException("Plus d'une adresse 'courrier' détectée");
				}

				// deux adresses valides à la même date -> on prend la plus récente en espérant que ce soit la plus juste
				if (DateRangeComparator.compareRanges(courrier, adresse) > 0) {
					// on ne change rien
				}
				else {
					courrier = adresse;
				}
			}
		}
		else if (adresse.getTypeAdresse().equals(EnumTypeAdresse.SECONDAIRE)) {
			if (secondaire == null) {
				secondaire = adresse;
			}
			else {
				if (strict) {
					throw new DonneesCivilesException("Plus d'une adresse 'secondaire' détectée");
				}

				// deux adresses valides à la même date -> on prend la plus récente en espérant que ce soit la plus juste
				if (DateRangeComparator.compareRanges(secondaire, adresse) > 0) {
					// on ne change rien
				}
				else {
					secondaire = adresse;
				}
			}
		}
		else if (adresse.getTypeAdresse().equals(EnumTypeAdresse.TUTELLE)) {
			if (tutelle == null) {
				tutelle = adresse;
			}
			else {
				if (strict) {
					throw new DonneesCivilesException("Plus d'une adresse 'tutelle' détectée");
				}

				// deux adresses valides à la même date -> on prend la plus récente en espérant que ce soit la plus juste
				if (DateRangeComparator.compareRanges(tutelle, adresse) > 0) {
					// on ne change rien
				}
				else {
					tutelle = adresse;
				}
			}
		}
		else {
			Assert.fail("Type d'adresse inconnue");
		}
	}

	public Adresse ofType(EnumTypeAdresse type) {
		if (EnumTypeAdresse.PRINCIPALE.equals(type)) {
			return principale;
		}
		else if (EnumTypeAdresse.COURRIER.equals(type)) {
			return courrier;
		}
		else if (EnumTypeAdresse.SECONDAIRE.equals(type)) {
			return secondaire;
		}
		else {
			Assert.isTrue(EnumTypeAdresse.TUTELLE.equals(type));
			return tutelle;
		}
	}
}