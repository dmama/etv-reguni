package ch.vd.unireg.registrefoncier.immeuble.graph;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.CollectivitePubliqueRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.unireg.registrefoncier.PersonneMoraleRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;

/**
 * Elément qui représente un ayant-droit dans le graphe des liens entre immeubles et ayants-droits.
 */
public class AyantDroit {
	/**
	 * La clé d'identification unique dans le graphe de l'ayant-droit
	 */
	protected final String key;
	protected final String type;
	protected final String label;

	public AyantDroit(String key, String type, String label) {
		this.key = key;
		this.type = type;
		this.label = label;
	}

	/**
	 * Construit la clé d'identification dans le graphe de l'ayant-droit spécifié.
	 *
	 * @param ayantDroit un ayant-droit
	 * @return la clé d'identification unique de l'ayant-droit
	 */
	public static String buildKey(@NotNull AyantDroitRF ayantDroit) {
		if (ayantDroit instanceof CommunauteRF) {
			final CommunauteRF communaute = (CommunauteRF) ayantDroit;
			return "clusterComm" + communaute.getId();
		}
		else if (ayantDroit instanceof ImmeubleBeneficiaireRF) {
			final ImmeubleBeneficiaireRF beneficiaire = (ImmeubleBeneficiaireRF) ayantDroit;
			return Immeuble.buildKey(beneficiaire.getImmeuble());
		}
		else if (ayantDroit instanceof PersonnePhysiqueRF) {
			final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) ayantDroit;
			return "PP" + pp.getId();
		}
		else if (ayantDroit instanceof PersonneMoraleRF) {
			final PersonneMoraleRF pm = (PersonneMoraleRF) ayantDroit;
			return "PM" + pm.getId();
		}
		else if (ayantDroit instanceof CollectivitePubliqueRF) {
			final CollectivitePubliqueRF cp = (CollectivitePubliqueRF) ayantDroit;
			return "COLL" + cp.getId();
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Parse la clé spécifiée pour en extraire l'id de l'ayant-droit.
	 *
	 * @param key une clé
	 * @return l'id de l'ayant-droit <b>si</b> la clé est celle d'un ayant-droit; ou <b>null</b> s'il s'agit d'une autre clé.
	 */
	@Nullable
	public static Long parseKey(@NotNull String key) {
		if (key.startsWith("PP")) {

			String id = key.substring(2);

			final int cluster = id.indexOf("clusterComm");
			if (cluster >= 0) {
				// en cas de PP dans une communauté, la clé de la PP possède un suffixe 'clusterComm' qu'il faut ignorer (ex: 'PP206992222clusterComm206992437')
				id = id.substring(0, cluster);
			}

			return Long.valueOf(id);
		}
		else if (key.startsWith("PM")) {

			String id = key.substring(2);

			final int cluster = id.indexOf("clusterComm");
			if (cluster >= 0) {
				// en cas de PM dans une communauté, la clé de la PM possède un suffixe 'clusterComm' qu'il faut ignorer (ex: 'PP206992222clusterComm206992437')
				id = id.substring(0, cluster);
			}

			return Long.valueOf(id);
		}
		else if (key.startsWith("COLL")) {
			return Long.valueOf(key.substring(4));
		}
		else if (key.startsWith("clusterComm")) {
			return Long.valueOf(key.substring("clusterComm".length()));
		}
		else {
			return null;    // pas un ayant-droit
		}
	}

	public String toDot() {
		return key + " [shape=oval, label=\"" + type + "\n" + label + "\", style=filled, color=lightblue1]";
	}
}
