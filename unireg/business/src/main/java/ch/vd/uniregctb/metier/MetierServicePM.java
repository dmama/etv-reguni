package ch.vd.uniregctb.metier;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Ensemble de services métiers de haut niveau, incluant toutes les régles fiscales nécessaires pour maintenir la cohérence des données.
 */
public interface MetierServicePM {

	AjustementForsSecondairesResult calculAjustementForsSecondairesPourEtablissementsVD(Entreprise entreprise, RegDate dateAuPlusTot) throws MetierServiceException;

	/**
	 * Méthode effectuant le rattachement d'une entreprise en base avec son équivalent civil représenté par son numéro cantonal. La méthode
	 * se charge aussi de rattacher l'établissement principal de l'entreprise.
	 *
	 * Les données civiles de l'entreprises (raison sociale, forme juridique et capital) sont terminées au jour précédant, ainsi que le domicile
	 * de l'établissement principal.
	 *
	 * La méthode vérifie que les établissements principaux fiscal et civil se trouvent sur la même commune fiscale avant d'associer l'identifiant cantonal
	 * à l'établissement. Par contre, la raison sociale n'entre pas dans le contrôle, la proportion de faux négatif que cela entraînerait étant jugée
	 * trop importante.
	 *
	 * NOTE: Les établissements secondaires ne sont pas supportés à ce jour. Tous les résultats sont donc partiels.
	 *
	 * La méthode n'effectue aucune création et ne s'occupe que d'entreprises et d'établissements existants.
	 * @param organisation l'organisation civile à rattacher
	 * @param entreprise l'entreprise rapprochée
	 * @param date la date à laquelle le rapprochement prend effet
	 * @return le résultat de l'opération de rattachement
	 */
	RattachementOrganisationResult rattacheOrganisationEntreprise(Organisation organisation, Entreprise entreprise, RegDate date) throws MetierServiceException;

	/**
	 * Opération de faillite
	 * @param entreprise l'entreprise qui part en faillite
	 * @param datePrononceFaillite date du prononcé de faillite
	 * @param remarqueAssociee [optionnelle] éventuelle remarque à ajouter à l'entreprise en même temps
	 * @throws MetierServiceException en cas de problème
	 */
	void faillite(Entreprise entreprise, RegDate datePrononceFaillite, String remarqueAssociee) throws MetierServiceException;
}
