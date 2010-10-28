package ch.vd.uniregctb.interfaces.model;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.type.TypeAdresseCivil;

/**
 * Contient les adresses civiles à un instant donné d'individu regroupées par type
 */
public class AdressesCivilesActives {
	public Adresse principale;
	public Adresse courrier;
	public List<Adresse> secondaires;
	public Adresse tutelle;

	public void set(Adresse adresse, boolean strict) throws DonneesCivilesException {
		if (adresse.getTypeAdresse().equals(TypeAdresseCivil.PRINCIPALE)) {
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
		else if (adresse.getTypeAdresse().equals(TypeAdresseCivil.COURRIER)) {
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
		else if (adresse.getTypeAdresse().equals(TypeAdresseCivil.SECONDAIRE)) {
			if (secondaires == null) {
				secondaires = new ArrayList<Adresse>();
			}
			secondaires.add(adresse);

		}
		else if (adresse.getTypeAdresse().equals(TypeAdresseCivil.TUTEUR)) {
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

	public Adresse ofType(TypeAdresseCivil type) {
		if (TypeAdresseCivil.PRINCIPALE.equals(type)) {
			return principale;
		}
		else if (TypeAdresseCivil.COURRIER.equals(type)) {
			return courrier;
		}
		else if (TypeAdresseCivil.SECONDAIRE.equals(type)) {
			return null;
		}
		else {
			Assert.isTrue(TypeAdresseCivil.TUTEUR.equals(type));
			return tutelle;
		}
	}

}
