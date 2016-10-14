package ch.vd.uniregctb.adresse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;
import ch.vd.uniregctb.type.TypeAdresseCivil;

/**
 * Contient toutes les adresses civiles d'un individu regroupées par type
 */
public class AdressesCivilesHisto {
	public List<Adresse> principales = new ArrayList<>();
	public List<Adresse> courriers = new ArrayList<>();
	public List<Adresse> secondaires = new ArrayList<>();
	public List<Adresse> tutelles = new ArrayList<>();

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
		final Comparator<Adresse> comparator = (a1, a2) -> NullDateBehavior.EARLIEST.compare(a1.getDateDebut(), a2.getDateDebut());
		Collections.sort(principales, comparator);
		Collections.sort(courriers, comparator);
		Collections.sort(secondaires, comparator);
		Collections.sort(tutelles, comparator);
	}

	/**
	 * Constructeur permettant de construire une AdresseCivileHisto à partir d'une adresseCivilesHistoriques et d'y appliquer les traitements propres à  AdresseCivileHisto
	 *
	 * @param adressesHistoriques les adresses historiques
	 * @param strict              si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger
	 *                            les données (dans la mesure du possible) pour ne pas lever d'exception.
	 * @throws DonneesCivilesException
	 */
	public AdressesCivilesHisto(AdressesCivilesHistoriques adressesHistoriques, boolean strict) throws DonneesCivilesException {
		this.principales = adressesHistoriques.principales;
		this.courriers = adressesHistoriques.courriers;
		this.secondaires = adressesHistoriques.secondaires;
		this.tutelles = adressesHistoriques.tutelles;
		finish(strict);
	}

	public AdressesCivilesHisto() {
	}

	/**
	 * Valide les adresses et lève une exception s'il y a des incohérences.
	 *
	 * @param strict si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *               (dans la mesure du possible) pour ne pas lever d'exception.
	 * @throws DonneesCivilesException en cas d'incohérence des données
	 */
	private void validate(boolean strict) throws DonneesCivilesException {
		validateNonChevauchement(principales, "principale", strict);
		validateNonChevauchement(courriers, "courrier", strict);
		// [SIFISC-6942] les adresses secondaires *peuvent* se chevaucher
		// validateNonChevauchement(secondaires, "secondaire", strict);
		validateNonChevauchement(tutelles, "tutelle", strict);
	}

	private static void validateNonChevauchement(List<Adresse> adresses, String typeAdresse, boolean strict) throws DonneesCivilesException {
		if (adresses == null) {
			return;
		}

		// Les plages de validité des adresses ne doivent pas se chevaucher
		for (int i = 1; i < adresses.size(); ++i) {
			final Adresse previous = adresses.get(i - 1);
			final Adresse current = adresses.get(i);
			if (DateRangeHelper.intersect(previous, current)) {
				if (strict) {
					final String message = buildMessageErreurChevauchement(typeAdresse, previous, current);
					throw new DonneesCivilesException(message);
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
						ch.vd.uniregctb.interfaces.model.AdresseAdapter adapted = new ch.vd.uniregctb.interfaces.model.AdresseAdapter(previous, previous.getDateDebut(), nouvelleFin);
						adresses.set(i - 1, adapted);
					}
					else {
						// on adapte la date de début de l'adresse courante
						final RegDate nouveauDebut = finPrecedente.getOneDayAfter();
						ch.vd.uniregctb.interfaces.model.AdresseAdapter adapted = new ch.vd.uniregctb.interfaces.model.AdresseAdapter(current, nouveauDebut, current.getDateFin());
						adresses.set(i, adapted);
					}
				}
			}
		}
	}

	private static String buildMessageErreurChevauchement(String typeAdresse, Adresse previous, Adresse current) {

		final StringBuilder s = new StringBuilder();

		s.append("L'adresse civile ").append(typeAdresse);

		if (current.getDateDebut() == null) {
			s.append(" dont la date de début est inconnue");
		}
		else {
			s.append(" qui commence le ").append(RegDateHelper.dateToDisplayString(current.getDateDebut()));
		}

		if (current.getDateFin() == null) {
			s.append(" (et qui est toujours active)");
		}
		else {
			s.append(" et finit le ").append(RegDateHelper.dateToDisplayString(current.getDateFin()));
		}

		s.append(" chevauche l'adresse précédente ");

		if (previous.getDateDebut() == null) {
			s.append(" dont la date de début est inconnue");
		}
		else {
			s.append("qui commence le ").append(RegDateHelper.dateToDisplayString(previous.getDateDebut()));
		}

		if (previous.getDateFin() == null) {
			s.append(" (et qui est toujours active)");
		}
		else {
			s.append(" et finit le ").append(RegDateHelper.dateToDisplayString(previous.getDateFin()));
		}

		return s.toString();
	}

	/**
	 * @return la première date définie dans l'historique des adresses courriers ou principales, ou <b>lateDate</b> si aucune adresse n'est définie.
	 */
	public RegDate getVeryFirstDate() {
		RegDate first = RegDateHelper.getLateDate();
		if (principales != null && !principales.isEmpty()) {
			Adresse a = principales.get(0);
			first = RegDateHelper.minimum(first, a.getDateDebut(), NullDateBehavior.EARLIEST);
		}
		if (courriers != null && !courriers.isEmpty()) {
			Adresse a = courriers.get(0);
			first = RegDateHelper.minimum(first, a.getDateDebut(), NullDateBehavior.EARLIEST);
		}
		return first;
	}

	/**
	 * @return la dernière date définie dans l'historique des adresses courriers ou principales, ou <b>earlyDate</b> si aucune adresse n'est définie.
	 */
	public RegDate getVeryLastDate() {
		RegDate last = RegDateHelper.getEarlyDate();
		if (principales != null && !principales.isEmpty()) {
			Adresse a = principales.get(principales.size() - 1);
			last = RegDateHelper.maximum(last, a.getDateFin(), NullDateBehavior.LATEST);
		}
		if (courriers != null && !courriers.isEmpty()) {
			Adresse a = courriers.get(courriers.size() - 1);
			last = RegDateHelper.maximum(last, a.getDateFin(), NullDateBehavior.LATEST);
		}
		return last;
	}
	/**
	 * @param date la date de validité demandée
	 * @return l'adresse fiscale valide (et non-annulée) à une date donnée.
	 */
	public AdressesCiviles at(RegDate date) throws DonneesCivilesException {
		final AdressesCiviles adresses = new AdressesCiviles();
		for (TypeAdresseCivil type : TypeAdresseCivil.values()) {
			final List<Adresse> list = ofType(type);
			if (list != null) {
				for (Adresse a : list) {
					if (a.isValidAt(date)) {
						adresses.set(a, true);
						break;
					}
				}
			}
		}
		return adresses;
	}


}
