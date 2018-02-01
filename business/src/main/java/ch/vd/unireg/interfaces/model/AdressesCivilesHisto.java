package ch.vd.uniregctb.interfaces.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.type.TypeAdresseCivil;

/**
 * Contient toutes les adresses civiles d'un individu ou d'une entreprise regroupées par type
 */
public class AdressesCivilesHisto {
	public final List<Adresse> principales;
	public final List<Adresse> courriers;
	public final List<Adresse> secondaires;
	public final List<Adresse> tutelles;
	public final List<Adresse> casesPostales;

	public AdressesCivilesHisto() {
		this.principales = new ArrayList<>();
		this.courriers = new ArrayList<>();
		this.secondaires = new ArrayList<>();
		this.tutelles = new ArrayList<>();
		this.casesPostales = new ArrayList<>();
	}

	public List<Adresse> ofType(TypeAdresseCivil type) {
		switch (type) {
		case PRINCIPALE:
			return principales;
		case COURRIER:
			return courriers;
		case SECONDAIRE:
			return secondaires;
		case TUTEUR:
			return tutelles;
		case CASE_POSTALE:
			return casesPostales;
		default:
			throw new IllegalArgumentException("Type d'adresse inconnu = [" + type + "]");
		}
	}

	public void add(Adresse adresse) {
		switch (adresse.getTypeAdresse()) {
		case PRINCIPALE:
			principales.add(adresse);
			break;
		case COURRIER:
			courriers.add(adresse);
			break;
		case SECONDAIRE:
			secondaires.add(adresse);
			break;
		case TUTEUR:
			tutelles.add(adresse);
			break;
		case CASE_POSTALE:
			casesPostales.add(adresse);
			break;
		default:
			throw new IllegalArgumentException("Type d'adresse inconnu = [" + adresse.getTypeAdresse() + "]");
		}
	}

	public List<Adresse> getAll() {
		final List<Adresse> all = new ArrayList<>(principales.size() + courriers.size() + secondaires.size() + tutelles.size() + casesPostales.size());
		all.addAll(principales);
		all.addAll(courriers);
		all.addAll(secondaires);
		all.addAll(tutelles);
		all.addAll(casesPostales);
		return all;
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
		principales.sort(comparator);
		courriers.sort(comparator);
		secondaires.sort(comparator);
		tutelles.sort(comparator);
		casesPostales.sort(comparator);
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
		validateNonChevauchement(casesPostales, "case postale", strict);
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
		if (!principales.isEmpty()) {
			Adresse a = principales.get(0);
			first = RegDateHelper.minimum(first, a.getDateDebut(), NullDateBehavior.EARLIEST);
		}
		if (!courriers.isEmpty()) {
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
		if (!principales.isEmpty()) {
			Adresse a = principales.get(principales.size() - 1);
			last = RegDateHelper.maximum(last, a.getDateFin(), NullDateBehavior.LATEST);
		}
		if (!courriers.isEmpty()) {
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
