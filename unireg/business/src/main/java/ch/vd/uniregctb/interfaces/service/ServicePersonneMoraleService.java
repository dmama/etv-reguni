package ch.vd.uniregctb.interfaces.service;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdressesPM;
import ch.vd.uniregctb.adresse.AdressesPMHisto;
import ch.vd.uniregctb.interfaces.model.EvenementPM;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;

import java.util.List;

public interface ServicePersonneMoraleService {

	public static final String SERVICE_NAME = "ServicePM";

	/**
	 * @return tous les ids des personnes morales qui existent dans la base de données du host.
	 */
	List<Long> getAllIds();

	/**
	 * Charge une personne morale spécifiée par son id.
	 *
	 * @param id    l'id de la personne à charger
	 * @param parts les attributs multivalués à renseigner sur la personne morale.
	 * @return une personne morale; ou <b>null</b> si cette personne n'existe pas.
	 */
	PersonneMorale getPersonneMorale(Long id, PartPM... parts);

	/**
	 * Charge un lot de personnes morales spécifiées par leurs ids.
	 *
	 * @param ids   les ids des personnes à charger
	 * @param parts les attributs multivalués à renseigner sur les personnes morales.
	 * @return une liste contenant zéro, une ou plusieurs personnes morales.
	 */
	List<PersonneMorale> getPersonnesMorales(List<Long> ids, PartPM... parts);

	/**
	 * Retourne les adresses PM valide à la date donnée.
	 *
	 * @param noEntreprise l'entreprise dont on recherche les adresses.
	 * @param date         la date de référence (attention, la précision est l'année !), ou null pour obtenir toutes les adresses existantes.
	 * @return les adresses civiles de l'entreprise spécifiée.
	 */
	AdressesPM getAdresses(long noEntreprise, RegDate date);

	/**
	 * Retourne l'historique des adresses PM d'une entreprise donnée. Les adresses sont triées par ordre chronologique croissant.
	 *
	 * @param noEntreprise l'entreprise dont on recherche les adresses.
	 * @return l'historique des adresses PM de l'entreprise spécifiée.
	 */
	AdressesPMHisto getAdressesHisto(long noEntreprise);

	/**
	 * Recherche les événements PM qui correspondent aux critères spécifiés.
	 *
	 * @param numeroEntreprise critère sur le numéro d'entreprise (optionel)
	 * @param code             critère sur le code des événements (optionnel)
	 * @param minDate          critère de valeur minimale de la date d'événement (optionel)
	 * @param maxDate          critère de valeur maximale de la date d'événement (optionel)
	 * @return une liste d'événements
	 */
	List<EvenementPM> findEvenements(Long numeroEntreprise, String code, RegDate minDate, RegDate maxDate);
}
