package ch.vd.unireg.interfaces.model;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.common.DonneesCivilesException;
import ch.vd.unireg.type.TypeAdresseCivil;

/**
 * Contient les adresses civiles à un instant donné d'individu ou d'une entreprise regroupées par type
 */
public class AdressesCiviles {
	public Adresse principale;
	public Adresse courrier;
	public List<Adresse> secondaires;
	/**
	 * L'adresse secondaire courante, c'est-à-dire l'adresse secondaire valide la plus récente (= la dernière de la liste).
	 */
	public Adresse secondaireCourante;
	public Adresse tutelle;
	public Adresse casePostale;

	public void set(Adresse adresse, boolean strict) throws DonneesCivilesException {
		switch (adresse.getTypeAdresse()) {
		case PRINCIPALE:
			setPrincipale(adresse, strict);
			break;
		case COURRIER:
			setCourrier(adresse, strict);
			break;
		case SECONDAIRE:
			addSecondaire(adresse);
			break;
		case TUTEUR:
			setTuteur(adresse, strict);
			break;
		case CASE_POSTALE:
			setCasePostale(adresse, strict);
			break;
		default:
			throw new IllegalArgumentException("Type d'adresse inconnu = [" + adresse.getTypeAdresse() + "]");
		}
	}

	private void setPrincipale(Adresse adresse, boolean strict) throws DonneesCivilesException {
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

	private void setCourrier(Adresse adresse, boolean strict) throws DonneesCivilesException {
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

	private void addSecondaire(Adresse adresse) {

		if (secondaires == null) {
			secondaires = new ArrayList<>();
		}
		secondaires.add(adresse);

		if (secondaireCourante == null) {
			secondaireCourante = adresse;
		}
		else {
			// RegPP permet actuellement d'ouvrir plusieurs adresses secondaires pour un individu dans le civil
			// UNIREG ne gère pas encore la liste des adresses secondaires(besoin de spécifications). Afin d'éviter un crash web
			// on ne prend en compte que la dernière adresse secondaire renseignée.

			// deux adresses valides à la même date -> on prend la plus récente en espérant que ce soit la plus juste
			if (DateRangeComparator.compareRanges(secondaireCourante, adresse) > 0) {
				// on ne change rien
			}
			else {
				secondaireCourante = adresse;
			}
		}
	}

	private void setTuteur(Adresse adresse, boolean strict) throws DonneesCivilesException {
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

	private void setCasePostale(Adresse adresse, boolean strict) throws DonneesCivilesException {
		if (casePostale == null) {
			casePostale = adresse;
		}
		else {
			if (strict) {
				throw new DonneesCivilesException("Plus d'une adresse 'case postale' détectée");
			}

			// deux adresses valides à la même date -> on prend la plus récente en espérant que ce soit la plus juste
			if (DateRangeComparator.compareRanges(casePostale, adresse) > 0) {
				// on ne change rien
			}
			else {
				casePostale = adresse;
			}
		}
	}

	public Adresse ofType(TypeAdresseCivil type) {
		switch (type) {
		case PRINCIPALE:
			return principale;
		case COURRIER:
			return courrier;
		case SECONDAIRE:
			return secondaireCourante;
		case TUTEUR:
			return tutelle;
		case CASE_POSTALE:
			return casePostale;
		default:
			throw new IllegalArgumentException("Type d'adresse inconnu = [" + type + "]");
		}
	}
}
