package ch.vd.uniregctb.interfaces.service;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdressesPM;
import ch.vd.uniregctb.adresse.AdressesPMHisto;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;

public interface ServicePersonneMoraleService {

	public PersonneMorale getPersonneMorale(Long id);

	/**
	 * Retourne les adresses PM valide à la date donnée.
	 *
	 * @param noEntreprise
	 *            l'entreprise dont on recherche les adresses.
	 * @param date
	 *            la date de référence (attention, la précision est l'année !), ou null pour obtenir toutes les adresses existantes.
	 * @return les adresses civiles de l'entreprise spécifiée.
	 */
	public AdressesPM getAdresses(long noEntreprise, RegDate date);

	/**
	 * Retourne l'historique des adresses PM d'une entreprise donnée. Les adresses sont triées par ordre chronologique croissant.
	 *
	 * @param noEntreprise
	 *            l'entreprise dont on recherche les adresses.
	 * @return l'historique des adresses PM de l'entreprise spécifiée.
	 */
	public AdressesPMHisto getAdressesHisto(long noEntreprise);
}
