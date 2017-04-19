package ch.vd.uniregctb.registrefoncier.immeuble;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CollectivitePubliqueRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.MineRF;
import ch.vd.uniregctb.registrefoncier.PartCoproprieteRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;

/**
 * Classe qui permet de générer un fichier DOT (http://www.graphviz.org) des liens entre immeubles (y compris les propriétaires) à partir d'un immeuble de départ.
 */
public class ImmeubleGraph {

	private static class Immeuble {
		private final String name;
		private final String type;
		private final String label;

		public Immeuble(String name, String type, String label) {
			this.name = name;
			this.type = type;
			this.label = label;
		}

		@Override
		public String toString() {
			return name + " [shape=record, label=\"" + type + "|" + label + "\", style=filled, color=sienna2]";
		}
	}

	private static class AyantDroit {
		private final String type;
		private final String name;
		private final String label;

		public AyantDroit(String type, String name, String label) {
			this.type = type;
			this.name = name;
			this.label = label;
		}

		@Override
		public String toString() {
			return name + " [shape=oval, label=\"" + type + "\n" + label + "\", style=filled, color=lightblue1]";
		}
	}

	private static class Droit {
		private final String color;
		private final String source;
		private final String destination;
		private final String label;

		public Droit(String source, String destination, String label, String color) {
			this.color = color;
			this.source = source;
			this.destination = destination;
			this.label = label;
		}

		@Override
		public String toString() {
			return source + " -> " + destination + " [label=\"" + label + "\", color=" + color + "]";
		}
	}

	private final Map<String, Immeuble> immeubles = new HashMap<>();
	private final Map<String, AyantDroit> ayantDroits = new HashMap<>();
	private final List<Droit> droits = new ArrayList<>();

	public void process(@NotNull ImmeubleRF immeuble) {

		if (isProcessed(immeuble)) {
			return;
		}
		addProcessed(immeuble);

		final RegDate today = RegDate.get();

		// liens de propriété vers cet immeuble
		immeuble.getDroitsPropriete().stream()
				.filter(d -> d.isValidAt(today))
				.filter(d -> !(d instanceof DroitProprieteImmeubleRF))
				.forEach(this::addDroitPropriete);
		immeuble.getDroitsPropriete().stream()
				.filter(d -> d instanceof DroitProprieteImmeubleRF)
				.filter(d -> d.isValidAt(today))
				.map(d -> (DroitProprieteImmeubleRF) d)
				.forEach(d -> {
					final ImmeubleBeneficiaireRF beneficiaire = (ImmeubleBeneficiaireRF) d.getAyantDroit();
					process(beneficiaire.getImmeuble());
				});

		// liens de propriétés depuis cet immeuble
		final ImmeubleBeneficiaireRF beneficiaire = immeuble.getEquivalentBeneficiaire();
		if (beneficiaire != null) {
			beneficiaire.getDroitsPropriete().stream()
					.filter(d -> d.isValidAt(today))
					.forEach(this::addDroitPropriete);
			beneficiaire.getDroitsPropriete().stream()
					.filter(d -> d.isValidAt(today))
					.forEach(d -> process(d.getImmeuble()));
		}

	}

	private boolean isProcessed(@NotNull ImmeubleRF immeuble) {
		return immeubles.containsKey(getName(immeuble));
	}

	private void addProcessed(@NotNull ImmeubleRF immeuble) {

		final String name = getName(immeuble);
		final SituationRF situation = immeuble.getSituations().stream()
				.filter(s -> s.isValidAt(RegDate.get()))
				.findFirst()
				.orElseThrow(IllegalArgumentException::new);

		final String type;
		if (immeuble instanceof BienFondRF) {
			type = "BF";
		}
		else if (immeuble instanceof DroitDistinctEtPermanentRF) {
			type = "DDP";
		}
		else if (immeuble instanceof MineRF) {
			type = "Mine";
		}
		else if (immeuble instanceof PartCoproprieteRF) {
			type = "PC";
		}
		else if (immeuble instanceof ProprieteParEtageRF) {
			type = "PPE";
		}
		else {
			throw new IllegalArgumentException();
		}

		String label = situation.getCommune().getNomRf() + " / " + situation.getNoParcelle();
		if (situation.getIndex1() != null) {
			label += "-" + situation.getIndex1();
		}
		if (situation.getIndex2() != null) {
			label += "-" + situation.getIndex2();
		}
		if (situation.getIndex3() != null) {
			label += "-" + situation.getIndex3();
		}

		immeubles.put(name, new Immeuble(name, type, label));
	}

	private void addDroitPropriete(DroitProprieteRF droit) {

		final String label = droit.getRegime().name() + " (" + droit.getPart() + ")";

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

		final String source = getName(droit.getAyantDroit());
		final String destination = getName(droit.getImmeuble());

		droits.add(new Droit(source, destination, label, color));
	}

	private String getName(@NotNull AyantDroitRF ayantDroit) {
		if (ayantDroit instanceof CommunauteRF) {
			final CommunauteRF communaute = (CommunauteRF) ayantDroit;
			final String name = "Comm" + communaute.getId();
			ayantDroits.computeIfAbsent(name, n -> new AyantDroit("COM", name, "Communauté #" + communaute.getId()));
			return name;
		}
		else if (ayantDroit instanceof ImmeubleBeneficiaireRF) {
			final ImmeubleBeneficiaireRF beneficiaire = (ImmeubleBeneficiaireRF) ayantDroit;
			return getName(beneficiaire.getImmeuble());
		}
		else if (ayantDroit instanceof PersonnePhysiqueRF) {
			final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) ayantDroit;
			final String name = "PP" + pp.getId();
			ayantDroits.computeIfAbsent(name, n -> new AyantDroit("PP", name, pp.getPrenom() + " " + pp.getNom()));
			return name;
		}
		else if (ayantDroit instanceof PersonneMoraleRF) {
			final PersonneMoraleRF pm = (PersonneMoraleRF) ayantDroit;
			final String name = "PM" + pm.getId();
			ayantDroits.computeIfAbsent(name, n -> new AyantDroit("PM", name, pm.getRaisonSociale()));
			return name;
		}
		else if (ayantDroit instanceof CollectivitePubliqueRF) {
			final CollectivitePubliqueRF cp = (CollectivitePubliqueRF) ayantDroit;
			final String name = "COLL" + cp.getId();
			ayantDroits.computeIfAbsent(name, n -> new AyantDroit("COLL", name, cp.getRaisonSociale()));
			return name;
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	private String getName(@NotNull ImmeubleRF immeuble) {
		final String egrid = immeuble.getEgrid();
		return egrid == null ? immeuble.getIdRF() : egrid;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("digraph {\n");
		s.append("rankdir=LR;\n");
		immeubles.values().forEach(obj -> s.append(obj).append("\n"));
		ayantDroits.values().forEach(obj -> s.append(obj).append("\n"));
		s.append("\n");
		droits.forEach(obj -> s.append(obj).append("\n"));
		s.append("}\n");
		return s.toString();
	}
}
