package ch.vd.uniregctb.adresse;

import ch.vd.common.model.EnumTypeAdresse;
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

	public void set(Adresse adresse) {
		if (adresse.getTypeAdresse().equals(EnumTypeAdresse.PRINCIPALE)) {
			if (principale != null) {
				throw new DonneesCivilesException("Plus d'une adresse 'principale' détectée");
			}
			principale = adresse;
		}
		else if (adresse.getTypeAdresse().equals(EnumTypeAdresse.COURRIER)) {
			if (courrier != null) {
				throw new DonneesCivilesException("Plus d'une adresse 'courrier' détectée");
			}
			courrier = adresse;
		}
		else if (adresse.getTypeAdresse().equals(EnumTypeAdresse.SECONDAIRE)) {
			if (secondaire != null) {
				throw new DonneesCivilesException("Plus d'une adresse 'secondaire' détectée");
			}
			secondaire = adresse;
		}
		else if (adresse.getTypeAdresse().equals(EnumTypeAdresse.TUTELLE)) {
			if (tutelle != null) {
				throw new DonneesCivilesException("Plus d'une adresse 'tutelle' détectée");
			}
			tutelle = adresse;
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