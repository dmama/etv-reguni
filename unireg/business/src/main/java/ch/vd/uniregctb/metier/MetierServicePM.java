package ch.vd.uniregctb.metier;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Ensemble de services métiers de haut niveau, incluant toutes les régles fiscales nécessaires pour maintenir la cohérence des données.
 */
public interface MetierServicePM {

	AjustementForsSecondairesResult calculAjustementForsSecondairesPourEtablissementsVD(Entreprise entreprise, RegDate dateAuPlusTot) throws MetierServiceException;

	/**
	 * <p>
	 *     Méthode effectuant le rattachement d'une entreprise en base avec son équivalent civil représenté par son numéro cantonal. La méthode
	 *     se charge aussi de rattacher l'établissement principal de l'entreprise.
	 * </p>
	 * <p>
	 *     Les données civiles de l'entreprises (raison sociale, forme juridique et capital) sont terminées au jour précédant, ainsi que le domicile
	 *     de l'établissement principal.
	 * </p>
	 * <p>
	 *     La méthode vérifie que les établissements principaux fiscal et civil se trouvent sur la même commune fiscale avant d'associer l'identifiant cantonal
	 *     à l'établissement. Par contre, la raison sociale n'entre pas dans le contrôle, la proportion de faux négatif que cela entraînerait étant jugée
	 *     trop importante.
	 * </p>
	 * <p>
	 *     Les établissements secondaires sont rattachés dans la mesure du possible. Les établissements et les sites sont catégorisés en fonction
	 *     de la paire domicile - statut d'activité.
	 * </p>
	 * <ul>
	 *     <li>Un site civil est réputé actif s'il est rapporté par au moins une source comme étant en activité (non radié, etc...)</li>
	 *     <li>Un établissement Unireg est réputé actif s'il est rattaché à l'entreprise par un rapport d'activité économique à la date</li>
	 * </ul>
	 * <p>
	 *     L'algorithme suivant est appliqué:
	 * </p>
	 * <ul>
	 *     <li>Les sites et les établissements sont indexé dans des listes avec comme clé la paire domicile - statut d'activité. Les établissements sans domicile
	 *     sont écartés d'emblée</li>
	 *     <li>Les paires porteuses de plus d'un site ou établissement sont éliminées</li>
	 *     <li>Les établissements et les sites restant sont rapprochés. Si un numéro IDE est présent coté Unireg, on vérifie qu'il correspond</li>
	 *     <li>Si le rapprochement est concluant, l'établissement est rattaché au site civil</li>
	 * </ul>
	 * <p>
	 *     Les établissements et les sites qui n'ont pas été rattaché sont rapportés dans le résultat.
	 * </p>
	 * <p>
	 *     La méthode n'effectue aucune création et ne s'occupe que d'entreprises et d'établissements existants.
	 * </p>
	 * <p>
	 *     Note: Dans l'éventualité improbable où il y aurait plusieurs établissements principaux dans l'historique civil, les établissements
	 *     passés ne seront pas rattachés à leurs contreparties Unireg, même si celles-ci existent.
	 * </p>
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
	void faillite(Entreprise entreprise, RegDate datePrononceFaillite, @Nullable String remarqueAssociee) throws MetierServiceException;

	/**
	 * Opération de déménagement de siège
	 * @param entreprise l'entreprise qui déménage
	 * @param dateDebutNouveauSiege date de début de validité du nouveau siège
	 * @param taf type d'autorité fiscale du nouveau siège
	 * @param noOfs numéro OFS de la commune/du pays du nouveau siège
	 * @throws MetierServiceException en cas de problème
	 */
	void demenageSiege(Entreprise entreprise, RegDate dateDebutNouveauSiege, TypeAutoriteFiscale taf, int noOfs) throws MetierServiceException;

	/**
	 * Opération de fin d'activité
	 * @param entreprise l'entreprise qui cesse son activité
	 * @param dateFinActivite date de la fin d'activité
	 * @param remarqueAssociee [optionnelle] éventuelle remarque à ajouter à l'entreprise en même temps
	 * @throws MetierServiceException en cas de problème
	 */
	void finActivite(Entreprise entreprise, RegDate dateFinActivite, @Nullable String remarqueAssociee) throws MetierServiceException;

	/**
	 * Opération de fusion d'entreprises
	 * @param absorbante l'entreprise absorbante
	 * @param absorbees les entreprises absorbées
	 * @param dateContratFusion la date de contrat de fusion
	 * @param dateBilanFusion la date de bilan de fusion
	 * @throws MetierServiceException en cas de problème
	 */
	void fusionne(Entreprise absorbante, List<Entreprise> absorbees, RegDate dateContratFusion, RegDate dateBilanFusion) throws MetierServiceException;
}
