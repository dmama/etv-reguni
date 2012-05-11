package ch.vd.uniregctb.adresse;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesActives;
import ch.vd.uniregctb.type.TypeAdresseCivil;

/**
 * Contient les adresses civiles à un instant donné d'individu regroupées par type
 */
public class AdressesCiviles {
	public Adresse principale;
	public Adresse courrier;
	public Adresse secondaire;
	public Adresse tutelle;


	public AdressesCiviles() {
	}

	/**Constructeur permettant de reconstruire une AdressesCiviles à partir d'une AdressesCivilesActives
	 * en initialisant l'adresse secondaire avec la dernière adresse secondaire connue.
	 *
	 * @param adressesActives
	 */
	public AdressesCiviles(AdressesCivilesActives adressesActives){
		this.principale = adressesActives.principale;
		this.courrier = adressesActives.courrier;
		this.tutelle = adressesActives.tutelle;
		if(adressesActives.secondaires!=null && !adressesActives.secondaires.isEmpty()){
			final int indexAdresse = adressesActives.secondaires.size() - 1;
			this.secondaire = adressesActives.secondaires.get(indexAdresse);			
		}
	}

	public void set(Adresse adresse, boolean strict) throws DonneesCivilesException {
		if (adresse.getTypeAdresse() == TypeAdresseCivil.PRINCIPALE) {
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
		else if (adresse.getTypeAdresse() == TypeAdresseCivil.COURRIER) {
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
		else if (adresse.getTypeAdresse() == TypeAdresseCivil.SECONDAIRE) {
			if (secondaire == null) {
				secondaire = adresse;
			}
			else {
				/*if (strict) {
					throw new DonneesCivilesException("Plus d'une adresse 'secondaire' détectée");
				}*/
				//TODO [UNIREG-2033] RegPP permet actuellement d'ouvrir plusieurs adresses secondaires pour un individu dans le civil
			//UNIREG ne gère pas encore la liste des adresses secondaires(besoin de spécifications). Afin d'éviter un crash web
			//on supprime la lever de l'exception et on ne prend en compte que la dernière adresse secondaire renseignée.
				

				// deux adresses valides à la même date -> on prend la plus récente en espérant que ce soit la plus juste
				if (DateRangeComparator.compareRanges(secondaire, adresse) > 0) {
					// on ne change rien
				}
				else {
					secondaire = adresse;
				}
			}
		}
		else if (adresse.getTypeAdresse() == TypeAdresseCivil.TUTEUR) {
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
		if (TypeAdresseCivil.PRINCIPALE == type) {
			return principale;
		}
		else if (TypeAdresseCivil.COURRIER == type) {
			return courrier;
		}
		else if (TypeAdresseCivil.SECONDAIRE == type) {
			return secondaire;
		}
		else {
			Assert.isTrue(TypeAdresseCivil.TUTEUR == type);
			return tutelle;
		}
	}
}
