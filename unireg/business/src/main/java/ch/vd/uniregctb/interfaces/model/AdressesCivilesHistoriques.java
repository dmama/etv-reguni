package ch.vd.uniregctb.interfaces.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.type.TypeAdresseCivil;

/**
 * Contient toutes les adresses civiles d'un individu regroupées par type
 */
public class AdressesCivilesHistoriques {
	public List<Adresse> principales = new ArrayList<Adresse>();
	public List<Adresse> courriers = new ArrayList<Adresse>();
	public List<Adresse> secondaires = new ArrayList<Adresse>();
	public List<Adresse> tutelles = new ArrayList<Adresse>();

	public List<Adresse> ofType(TypeAdresseCivil type) {
		if (TypeAdresseCivil.PRINCIPALE == type) {
			return principales;
		}
		else if (TypeAdresseCivil.COURRIER == type) {
			return courriers;
		}
		else if (TypeAdresseCivil.SECONDAIRE == type) {
			return secondaires;
		}
		else {
			Assert.isTrue(TypeAdresseCivil.TUTEUR == type);
			return tutelles;
		}
	}

	public void add(Adresse adresse) {
		if (adresse.getTypeAdresse() == TypeAdresseCivil.PRINCIPALE) {
			principales.add(adresse);
		}
		else if (adresse.getTypeAdresse() == TypeAdresseCivil.COURRIER) {
			courriers.add(adresse);
		}
		else if (adresse.getTypeAdresse() == TypeAdresseCivil.SECONDAIRE) {
			secondaires.add(adresse);
		}
		else if (adresse.getTypeAdresse() == TypeAdresseCivil.TUTEUR) {
			tutelles.add(adresse);
		}
		else {
			Assert.fail();
		}
	}

	/**
	 * @param strict si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *               (dans la mesure du possible) pour ne pas lever d'exception.
	 * @throws ch.vd.uniregctb.common.DonneesCivilesException
	 *          en cas d'incohérence des données
	 */
	public void finish(boolean strict) throws DonneesCivilesException {
		sort();
		validate(strict);
	}

	/**
	 * Ordonne toutes les adresses par date de début de validité croissant
	 */
	private void sort() {
		Comparator<Adresse> comparator = new Comparator<Adresse>() {
			public int compare(Adresse left, Adresse right) {
				final RegDate dateRight = right.getDateDebut();
				final RegDate dateLeft = left.getDateDebut();

				if (dateLeft == null && dateRight == null) {
					return 0;
				}
				else if (dateLeft == null) {
					return -1;
				}
				else if (dateRight == null) {
					return 1;
				}
				else {
					return dateLeft.compareTo(dateRight);
				}
			}
		};
		Collections.sort(principales, comparator);
		Collections.sort(courriers, comparator);
		Collections.sort(secondaires, comparator);
		Collections.sort(tutelles, comparator);
	}

	/**
	 * Valide les adresses et lève une exception s'il y a des incohérences.
	 *
	 * @param strict si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *               (dans la mesure du possible) pour ne pas lever d'exception.
	 * @throws ch.vd.uniregctb.common.DonneesCivilesException
	 *          en cas d'incohérence des données
	 */
	private void validate(boolean strict) throws DonneesCivilesException {
		validateList(principales, "principale", strict);
		validateList(courriers, "courrier", strict);
		//on ne recalcule pas les dates des adresses secondaires
		validateList(tutelles, "tutelle", strict);
	}

	private static void validateList(List<Adresse> adresses, String typeAdresse, boolean strict) throws DonneesCivilesException {
		if (adresses == null) {
			return;
		}

		// Les plages de validité des adresses ne doivent pas se chevaucher
		for (int i = 1; i < adresses.size(); ++i) {
			final Adresse previous = adresses.get(i - 1);
			final Adresse current = adresses.get(i);
			if (DateRangeHelper.intersect(previous, current)) {
				if (strict) {
					throw new DonneesCivilesException("L'adresse civile " + typeAdresse + " qui commence le " + current.getDateDebut() + " et finit le " + current.getDateFin()
							+ " chevauche l'adresse précédente qui commence le " + previous.getDateDebut() + " et finit le " + previous.getDateFin());
				}
				else {

						// on essaie de corriger les données à la volée
						final RegDate finPrecedente = previous.getDateFin();
						final RegDate debutSuivante = current.getDateDebut();

						if (debutSuivante == null && finPrecedente == null) {
							// on supprime l'adresse précédente
							adresses.remove(i - 1);
							--i;
						}
						else if (debutSuivante != null) {
							// on adapte la date de fin de l'adresse précédente
							final RegDate nouvelleFin = debutSuivante.getOneDayBefore();
							AdresseAdapter adapted = new AdresseAdapter(previous, previous.getDateDebut(), nouvelleFin);
							adresses.set(i - 1, adapted);
						}
						else {
							// on adapte la date de début de l'adresse courante
							final RegDate nouveauDebut = finPrecedente.getOneDayAfter();
							AdresseAdapter adapted = new AdresseAdapter(current, nouveauDebut, current.getDateFin());
							adresses.set(i, adapted);
						}   

				}
			}
		}
	}
}
