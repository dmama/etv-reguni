package ch.vd.unireg.registrefoncier.immeuble.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.CollectivitePubliqueRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonneRF;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.PersonneMoraleRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;

/**
 * Classe qui permet de générer un fichier DOT (http://www.graphviz.org) des liens entre immeubles (y compris les propriétaires) à partir d'un immeuble de départ.
 */
public class ImmeubleGraph {

	private final Map<String, Immeuble> immeubles = new HashMap<>();
	private final Map<String, AyantDroit> ayantDroits = new HashMap<>();
	private final List<Droit> droits = new ArrayList<>();

	public void process(@NotNull ImmeubleRF immeuble, boolean remonterLesLiens) {

		if (isProcessed(immeuble)) {
			return;
		}
		addProcessed(immeuble);

		final RegDate today = RegDate.get();

		// liens de propriété vers cet immeuble
		if (remonterLesLiens) {
			immeuble.getDroitsPropriete().stream()
					.filter(d -> d.isValidAt(today))
					.filter(d -> !(d instanceof DroitProprieteImmeubleRF))
					.forEach(this::addDroitPropriete);
			immeuble.getDroitsPropriete().stream()
					.filter(d -> d.isValidAt(today))
					.filter(DroitProprieteImmeubleRF.class::isInstance)
					.map(DroitProprieteImmeubleRF.class::cast)
					.map(DroitProprieteRF::getAyantDroit)
					.map(ImmeubleBeneficiaireRF.class::cast)
					.map(ImmeubleBeneficiaireRF::getImmeuble)
					.forEach(i -> process(i, true));
		}

		// liens de propriétés depuis cet immeuble
		final ImmeubleBeneficiaireRF beneficiaire = immeuble.getEquivalentBeneficiaire();
		if (beneficiaire != null) {
			beneficiaire.getDroitsPropriete().stream()
					.filter(d -> d.isValidAt(today))
					.forEach(this::addDroitPropriete);
			beneficiaire.getDroitsPropriete().stream()
					.filter(d -> d.isValidAt(today))
					.forEach(d -> process(d.getImmeuble(), remonterLesLiens));
		}
	}

	/**
	 * Ajoute les droits de propriété d'un contribuable dans le graphe.
	 *
	 * @param droits les droits de propriété
	 */
	public void process(List<DroitProprieteRF> droits) {

		final RegDate today = RegDate.get();

		// liens de propriété du contribuable
		droits.stream()
				.filter(d -> d.isValidAt(today))
				.forEach(this::addDroitPropriete);
		droits.stream()
				.filter(d -> d.isValidAt(today))
				.forEach(d -> process(d.getImmeuble(), false));
	}

	private boolean isProcessed(@NotNull ImmeubleRF immeuble) {
		return immeubles.containsKey(buildKey(immeuble));
	}

	private void addProcessed(@NotNull ImmeubleRF immeuble) {
		final Immeuble i = new Immeuble(immeuble);
		immeubles.put(i.getKey(), i);
	}

	private void addDroitPropriete(DroitProprieteRF droit) {
		final String label = droit.getRegime().name() + " (" + droit.getPart() + ")";
		final String color = getDroitColor(droit);

		final CommunauteRF communaute = getCommunaute(droit);
		if (communaute == null) {
			// un droit normal
			final String sourceKey = buildKey(droit.getAyantDroit());
			final String destinationKey = buildKey(droit.getImmeuble());

			addAyantDroit(ayantDroits, droit.getAyantDroit());
			droits.add(new Droit(droit.getId(), sourceKey, destinationKey, label, color, "solid"));
		}
		else {
			// en cas de communauté, on crée une copie de l'ayant-droit pour l'associer à la communauté
			final Communaute comm = (Communaute) addAyantDroit(ayantDroits, communaute);
			final AyantDroit membre = comm.addMembre(droit.getAyantDroit());
			final String sourceKey = membre.key;
			final String destinationKey = buildKey(droit.getImmeuble());
			droits.add(new Droit(droit.getId(), sourceKey, destinationKey, label, color, "dashed"));
		}
	}

	@Nullable
	private static CommunauteRF getCommunaute(DroitProprieteRF droit) {
		final CommunauteRF communaute;
		if (droit instanceof DroitProprietePersonneRF) {
			final DroitProprietePersonneRF dpp = (DroitProprietePersonneRF) droit;
			communaute = dpp.getCommunaute();
		}
		else {
			communaute = null;
		}
		return communaute;
	}

	private static String getDroitColor(DroitProprieteRF droit) {
		final String color;
		switch (droit.getRegime()) {
		case INDIVIDUELLE:
			color = "black";
			break;
		case COMMUNE:
			color = "blue";
			break;
		case COPROPRIETE:
			color = "magenta2";
			break;
		case PPE:
			color = "green4";
			break;
		case FONDS_DOMINANT:
			color = "orange";
			break;
		default:
			throw new IllegalArgumentException();
		}
		return color;
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
			return buildKey(beneficiaire.getImmeuble());
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

	@Nullable
	public static Long parseAyantDroitId(@NotNull String elementKey) {
		if (elementKey.startsWith("PP")) {

			String id = elementKey.substring(2);

			final int cluster = id.indexOf("clusterComm");
			if (cluster >= 0) {
				// en cas de communauté, l'élément représentant la PP possède le suffice 'clusterComm' (ex: 'PP206992222clusterComm206992437')
				id = id.substring(0, cluster);
			}

			return Long.valueOf(id);
		}
		else if (elementKey.startsWith("PM")) {

			String id = elementKey.substring(2);

			final int cluster = id.indexOf("clusterComm");
			if (cluster >= 0) {
				// en cas de communauté, l'élément représentant la PM possède le suffice 'clusterComm' (ex: 'PM206992222clusterComm206992437')
				id = id.substring(0, cluster);
			}

			return Long.valueOf(id);
		}
		else if (elementKey.startsWith("COLL")) {
			return Long.valueOf(elementKey.substring(4));
		}
		else if (elementKey.startsWith("clusterComm")) {
			return Long.valueOf(elementKey.substring("clusterComm".length()));
		}
		else {
			return null;    // pas un ayant-droit
		}
	}

	private static AyantDroit addAyantDroit(@NotNull Map<String, AyantDroit> ayantDroits, @NotNull AyantDroitRF ayantDroit) {
		final String key = buildKey(ayantDroit);
		if (ayantDroit instanceof CommunauteRF) {
			final CommunauteRF communaute = (CommunauteRF) ayantDroit;
			return ayantDroits.computeIfAbsent(key, n -> new Communaute(key, "Communauté #" + communaute.getId()));
		}
		else if (ayantDroit instanceof ImmeubleBeneficiaireRF) {
			return null;
		}
		else if (ayantDroit instanceof PersonnePhysiqueRF) {
			final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) ayantDroit;
			return ayantDroits.computeIfAbsent(key, n -> new PersonnePhysique(key, pp.getPrenom() + " " + pp.getNom()));
		}
		else if (ayantDroit instanceof PersonneMoraleRF) {
			final PersonneMoraleRF pm = (PersonneMoraleRF) ayantDroit;
			return ayantDroits.computeIfAbsent(key, n -> new PersonneMorale(key, pm.getRaisonSociale()));
		}
		else if (ayantDroit instanceof CollectivitePubliqueRF) {
			final CollectivitePubliqueRF cp = (CollectivitePubliqueRF) ayantDroit;
			return ayantDroits.computeIfAbsent(key, n -> new Collectivite(key, cp.getRaisonSociale()));
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Construit la clé d'identification dans le graphe de l'immeuble spécifié.
	 *
	 * @param immeuble un immeuble
	 * @return la clé d'identification unique de l'immeuble
	 */
	public static String buildKey(@NotNull ImmeubleRF immeuble) {
		final String egrid = immeuble.getEgrid();
		return egrid == null ? immeuble.getIdRF() : egrid;
	}

	@Nullable
	public static String parseImmeubleEgrid(@NotNull String elementKey) {
		if (elementKey.startsWith("CH")) {
			return elementKey;
		}
		else {
			return null;    // pas un immeuble
		}
	}

	@Nullable
	public static Long parseLien(@NotNull String elementKey) {
		if (elementKey.startsWith("link")) {
			return Long.valueOf(elementKey.substring(4));
		}
		else {
			return null;    // pas un lien
		}
	}

	public String toDot(boolean showEstimationFiscales) {
		StringBuilder s = new StringBuilder();
		s.append("digraph {\n");
		s.append("rankdir=LR;\n");
		immeubles.values().forEach(obj -> s.append(obj.toDot(showEstimationFiscales)).append("\n"));
		ayantDroits.values().forEach(obj -> s.append(obj.toDot()).append("\n"));
		s.append("\n");
		droits.forEach(obj -> s.append(obj.toDot()).append("\n"));
		s.append("}\n");
		return s.toString();
	}
}
