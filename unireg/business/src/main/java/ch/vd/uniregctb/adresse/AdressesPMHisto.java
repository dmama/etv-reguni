package ch.vd.uniregctb.adresse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.type.TypeAdressePM;

/**
 * Contient l'historique des adresses PM d'une entreprise.
 */
public class AdressesPMHisto {

	public List<AdresseEntreprise> sieges = new ArrayList<AdresseEntreprise>();
	public List<AdresseEntreprise> courriers = new ArrayList<AdresseEntreprise>();
	public List<AdresseEntreprise> facturation = new ArrayList<AdresseEntreprise>();

	public void add(AdresseEntreprise adresse) {
		if (TypeAdressePM.SIEGE == adresse.getType()) {
			sieges.add(adresse);
		}
		else if (TypeAdressePM.COURRIER == adresse.getType()) {
			courriers.add(adresse);
		}
		else if (TypeAdressePM.FACTURATION == adresse.getType()) {
			facturation.add(adresse);
		}
		else {
			Assert.fail();
		}
	}

	public List<AdresseEntreprise> ofType(TypeAdressePM type) {
		if (TypeAdressePM.SIEGE == type) {
			return sieges;
		}
		else if (TypeAdressePM.COURRIER == type) {
			return courriers;
		}
		else {
			Assert.isTrue(TypeAdressePM.FACTURATION == type);
			return facturation;
		}
	}

	/**
	 * Ordonne toutes les adresses par date de début de validité croissant
	 */
	public void sort() {
		Comparator<AdresseEntreprise> comparator = new Comparator<AdresseEntreprise>() {
			public int compare(AdresseEntreprise left, AdresseEntreprise right) {
				return left.getDateDebutValidite().compareTo(right.getDateDebutValidite());
			}
		};

		Collections.sort(sieges, comparator);
		Collections.sort(courriers, comparator);
		Collections.sort(facturation, comparator);
	}

	/**
	 * @return la première date définie dans l'historique des adresses, ou <b>lateDate</b> si aucune adresse n'est définie.
	 */
	public RegDate getVeryFirstDate() {
		RegDate first = RegDate.getLateDate();
		if (sieges.size() > 0) {
			AdresseEntreprise a = sieges.get(0);
			first = RegDateHelper.minimum(first, a.getDateDebutValidite(), NullDateBehavior.EARLIEST);
		}
		if (courriers.size() > 0) {
			AdresseEntreprise a = courriers.get(0);
			first = RegDateHelper.minimum(first, a.getDateDebutValidite(), NullDateBehavior.EARLIEST);
		}
		if (facturation.size() > 0) {
			AdresseEntreprise a = facturation.get(0);
			first = RegDateHelper.minimum(first, a.getDateDebutValidite(), NullDateBehavior.EARLIEST);
		}
		return first;
	}

	/**
	 * @return la dernière date définie dans l'historique des adresses, ou <b>earlyDate</b> si aucune adresse n'est définie.
	 */
	public RegDate getVeryLastDate() {
		RegDate last = RegDate.getEarlyDate();
		if (sieges.size() > 0) {
			AdresseEntreprise a = sieges.get(sieges.size() - 1);
			last = RegDateHelper.maximum(last, a.getDateFinValidite(), NullDateBehavior.EARLIEST);
		}
		if (courriers.size() > 0) {
			AdresseEntreprise a = courriers.get(courriers.size() - 1);
			last = RegDateHelper.maximum(last, a.getDateFinValidite(), NullDateBehavior.EARLIEST);
		}
		if (facturation.size() > 0) {
			AdresseEntreprise a = facturation.get(facturation.size() - 1);
			last = RegDateHelper.maximum(last, a.getDateFinValidite(), NullDateBehavior.EARLIEST);
		}
		return last;
	}
}
