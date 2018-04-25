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
		return immeubles.containsKey(Immeuble.buildKey(immeuble));
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
			final String key = Droit.buildKey(droit);
			final String sourceKey = AyantDroit.buildKey(droit.getAyantDroit());
			final String destinationKey = Immeuble.buildKey(droit.getImmeuble());

			addAyantDroit(ayantDroits, droit.getAyantDroit());
			droits.add(new Droit(key, sourceKey, destinationKey, label, color, "solid"));
		}
		else {
			// en cas de communauté, on crée une copie de l'ayant-droit pour l'associer à la communauté
			final Communaute comm = (Communaute) addAyantDroit(ayantDroits, communaute);
			final AyantDroit membre = comm.addMembre(droit.getAyantDroit());
			final String key = Droit.buildKey(droit);
			final String sourceKey = membre.key;
			final String destinationKey = Immeuble.buildKey(droit.getImmeuble());
			droits.add(new Droit(key, sourceKey, destinationKey, label, color, "dashed"));
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

	private static AyantDroit addAyantDroit(@NotNull Map<String, AyantDroit> ayantDroits, @NotNull AyantDroitRF ayantDroit) {
		final String key = AyantDroit.buildKey(ayantDroit);
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
