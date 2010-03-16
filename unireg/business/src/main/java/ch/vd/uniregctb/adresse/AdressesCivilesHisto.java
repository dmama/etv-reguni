package ch.vd.uniregctb.adresse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.interfaces.model.Adresse;

/**
 * Contient toutes les adresses civiles d'un individu regroupées par type
 */
public class AdressesCivilesHisto {
	public List<Adresse> principales = new ArrayList<Adresse>();
	public List<Adresse> courriers = new ArrayList<Adresse>();
	public List<Adresse> secondaires = new ArrayList<Adresse>();
	public List<Adresse> tutelles = new ArrayList<Adresse>();

	public List<Adresse> ofType(EnumTypeAdresse type) {
		if (EnumTypeAdresse.PRINCIPALE.equals(type)) {
			return principales;
		}
		else if (EnumTypeAdresse.COURRIER.equals(type)) {
			return courriers;
		}
		else if (EnumTypeAdresse.SECONDAIRE.equals(type)) {
			return secondaires;
		}
		else {
			Assert.isTrue(EnumTypeAdresse.TUTELLE.equals(type));
			return tutelles;
		}
	}

	public void add(Adresse adresse) {
		if (adresse.getTypeAdresse().equals(EnumTypeAdresse.PRINCIPALE)) {
			principales.add(adresse);
		}
		else if (adresse.getTypeAdresse().equals(EnumTypeAdresse.COURRIER)) {
			courriers.add(adresse);
		}
		else if (adresse.getTypeAdresse().equals(EnumTypeAdresse.SECONDAIRE)) {
			secondaires.add(adresse);
		}
		else if (adresse.getTypeAdresse().equals(EnumTypeAdresse.TUTELLE)) {
			tutelles.add(adresse);
		}
		else {
			Assert.fail();
		}
	}

	/**
	 * Ordonne toutes les adresses par date de début de validité croissant
	 */
	public void sort() {
		Comparator<Adresse> comparator = new Comparator<Adresse>() {
			public int compare(Adresse left, Adresse right) {
				final RegDate dateRight = right.getDateDebutValidite();
				final RegDate dateLeft = left.getDateDebutValidite();

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
	 * @return la première date définie dans l'historique des adresses courriers ou principales, ou <b>lateDate</b> si aucune adresse n'est
	 *         définie.
	 */
	public RegDate getVeryFirstDate() {
		RegDate first = RegDate.getLateDate();
		if (principales != null && principales.size() > 0) {
			Adresse a = principales.get(0);
			first = RegDateHelper.minimum(first, a.getDateDebutValidite(), NullDateBehavior.EARLIEST);
		}
		if (courriers != null && courriers.size() > 0) {
			Adresse a = courriers.get(0);
			first = RegDateHelper.minimum(first, a.getDateDebutValidite(), NullDateBehavior.EARLIEST);
		}
		return first;
	}

	/**
	 * @return la dernière date définie dans l'historique des adresses courriers ou principales, ou <b>earlyDate</b> si aucune adresse
	 *         n'est définie.
	 */
	public RegDate getVeryLastDate() {
		RegDate last = RegDate.getEarlyDate();
		if (principales != null && principales.size() > 0) {
			Adresse a = principales.get(principales.size() - 1);
			last = RegDateHelper.maximum(last, a.getDateFinValidite(), NullDateBehavior.LATEST);
		}
		if (courriers != null && courriers.size() > 0) {
			Adresse a = courriers.get(courriers.size() - 1);
			last = RegDateHelper.maximum(last, a.getDateFinValidite(), NullDateBehavior.LATEST);
		}
		return last;
	}
}
