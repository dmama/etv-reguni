package ch.vd.unireg.rapport;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.tiers.RapportEntreTiersKey;
import ch.vd.unireg.type.TypeRapportEntreTiers;

public abstract class RapportHelper {

	/**
	 * L'ensemble des rapports entre tiers accessibles avec la visualisation complète (dans le tableau général "rapports entre tiers")
	 */
	public static final Set<RapportEntreTiersKey> ALLOWED_VISU_COMPLETE = buildAllAllowedTypes();

	/**
	 * L'ensemble des rapports entre tiers accessibles avec la visualisation limitée
	 */
	public static final Set<RapportEntreTiersKey> ALLOWED_VISU_LIMITEE = buildAllowedTypesEnVisuLimitee();

	/**
	 * L'ensemble des rapports entre tiers visibles dans un tableau "établissements"
	 */
	public static final Set<RapportEntreTiersKey> ALLOWED_ETABLISSEMENTS = buildAllowedEtablissementsTypes();

	/**
	 * L'ensemble des rapports entre tiers visibles dans un tableau "liens de parenté"
	 */
	public static final Set<RapportEntreTiersKey> ALLOWED_PARENTES = buildAllowedParentesTypes();

	/**
	 * @param type type de rapport entre tiers
	 * @return ensemble qui contient toutes les clés disponibles pour ce type de rapport
	 */
	@NotNull
	private static Set<RapportEntreTiersKey> buildAllForType(TypeRapportEntreTiers type) {
		final Set<RapportEntreTiersKey> set = new HashSet<>(2);
		set.add(new RapportEntreTiersKey(type, RapportEntreTiersKey.Source.OBJET));
		set.add(new RapportEntreTiersKey(type, RapportEntreTiersKey.Source.SUJET));
		return Collections.unmodifiableSet(set);
	}

	/**
	 * @return l'ensemble des rapports visibles en visualisation limitée -> seulement les liens de ménage
	 */
	@NotNull
	private static Set<RapportEntreTiersKey> buildAllowedTypesEnVisuLimitee() {
		return buildAllForType(TypeRapportEntreTiers.APPARTENANCE_MENAGE);
	}

	/**
	 * @return l'ensemble des rapports visibles dans le tableau des établissements (activités économiques)
	 */
	@NotNull
	private static Set<RapportEntreTiersKey> buildAllowedEtablissementsTypes() {
		return buildAllForType(TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE);
	}

	/**
	 * @return l'ensemble des rapports visibles dans le tableau des liens de parenté
	 */
	@NotNull
	private static Set<RapportEntreTiersKey> buildAllowedParentesTypes() {
		return buildAllForType(TypeRapportEntreTiers.PARENTE);
	}

	@NotNull
	private static Set<RapportEntreTiersKey> buildAllAllowedTypes() {
		final Set<RapportEntreTiersKey> set = new HashSet<>(RapportEntreTiersKey.maxCardinality());
//		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE, RapportEntreTiersKey.Source.OBJET));
//		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE, RapportEntreTiersKey.Source.SUJET));
//		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.ADMINISTRATION_ENTREPRISE, RapportEntreTiersKey.Source.OBJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.ADMINISTRATION_ENTREPRISE, RapportEntreTiersKey.Source.SUJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.ANNULE_ET_REMPLACE, RapportEntreTiersKey.Source.OBJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.ANNULE_ET_REMPLACE, RapportEntreTiersKey.Source.SUJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.APPARTENANCE_MENAGE, RapportEntreTiersKey.Source.OBJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.APPARTENANCE_MENAGE, RapportEntreTiersKey.Source.SUJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.ASSUJETTISSEMENT_PAR_SUBSTITUTION, RapportEntreTiersKey.Source.OBJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.ASSUJETTISSEMENT_PAR_SUBSTITUTION, RapportEntreTiersKey.Source.SUJET));
//		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.CONSEIL_LEGAL, RapportEntreTiersKey.Source.OBJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.CONSEIL_LEGAL, RapportEntreTiersKey.Source.SUJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE, RapportEntreTiersKey.Source.OBJET));
//		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE, RapportEntreTiersKey.Source.SUJET));
//		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.CURATELLE, RapportEntreTiersKey.Source.OBJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.CURATELLE, RapportEntreTiersKey.Source.SUJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.FUSION_ENTREPRISES, RapportEntreTiersKey.Source.OBJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.FUSION_ENTREPRISES, RapportEntreTiersKey.Source.SUJET));
//		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.MANDAT, RapportEntreTiersKey.Source.OBJET));
//		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.MANDAT, RapportEntreTiersKey.Source.SUJET));
//		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.PARENTE, RapportEntreTiersKey.Source.OBJET));
//		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.PARENTE, RapportEntreTiersKey.Source.SUJET));
//		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.PRESTATION_IMPOSABLE, RapportEntreTiersKey.Source.OBJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.PRESTATION_IMPOSABLE, RapportEntreTiersKey.Source.SUJET));
//		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.REPRESENTATION, RapportEntreTiersKey.Source.OBJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.REPRESENTATION, RapportEntreTiersKey.Source.SUJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.SCISSION_ENTREPRISE, RapportEntreTiersKey.Source.OBJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.SCISSION_ENTREPRISE, RapportEntreTiersKey.Source.SUJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.SOCIETE_DIRECTION, RapportEntreTiersKey.Source.OBJET));
//		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.SOCIETE_DIRECTION, RapportEntreTiersKey.Source.SUJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.TRANSFERT_PATRIMOINE, RapportEntreTiersKey.Source.OBJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.TRANSFERT_PATRIMOINE, RapportEntreTiersKey.Source.SUJET));
//		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.TUTELLE, RapportEntreTiersKey.Source.OBJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.TUTELLE, RapportEntreTiersKey.Source.SUJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.HERITAGE, RapportEntreTiersKey.Source.OBJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.HERITAGE, RapportEntreTiersKey.Source.SUJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.LIENS_ASSOCIES_ET_SNC, RapportEntreTiersKey.Source.OBJET));
		set.add(new RapportEntreTiersKey(TypeRapportEntreTiers.LIENS_ASSOCIES_ET_SNC, RapportEntreTiersKey.Source.SUJET));
		return Collections.unmodifiableSet(set);
	}
}
