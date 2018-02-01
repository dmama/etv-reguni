package ch.vd.unireg.registrefoncier.immeuble;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.CollectivitePubliqueRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.unireg.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonneRF;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.EstimationRF;
import ch.vd.unireg.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.MineRF;
import ch.vd.unireg.registrefoncier.PartCoproprieteRF;
import ch.vd.unireg.registrefoncier.PersonneMoraleRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.ProprieteParEtageRF;
import ch.vd.unireg.registrefoncier.SituationRF;

/**
 * Classe qui permet de générer un fichier DOT (http://www.graphviz.org) des liens entre immeubles (y compris les propriétaires) à partir d'un immeuble de départ.
 */
public class ImmeubleGraph {

	private static final DecimalFormat MONTANT_FORMAT = new DecimalFormat("###,###", DecimalFormatSymbols.getInstance(new Locale("fr", "CH")));

	private static class Immeuble {

		private final String name;
		private final String type;
		private final String label;
		private final String estimationFiscale;

		public Immeuble(@NotNull ImmeubleRF immeuble) {

			final RegDate today = RegDate.get();

			this.name = getName(immeuble);
			final SituationRF situation = immeuble.getSituations().stream()
					.filter(s -> s.isValidAt(today))
					.findFirst()
					.orElseThrow(IllegalArgumentException::new);

			final String type;
			if (immeuble instanceof BienFondsRF) {
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
			this.type = type;

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
			this.label = label;

			estimationFiscale = immeuble.getEstimations().stream()
					.filter(e -> e.isValidAt(today))
					.findFirst()
					.map(EstimationRF::getMontant)
					.filter(Objects::nonNull)
					.map(montant -> MONTANT_FORMAT.format(montant) + " CHF")
					.orElse(null);
		}

		public String toDot(boolean showEstimationFiscales) {
			String s = name + " [shape=record, label=\"" + type + "|" + label;
			if (showEstimationFiscales && StringUtils.isNotBlank(estimationFiscale)) {
				s += "|" + estimationFiscale;
			}
			s += "\", style=filled, color=sienna2]";
			return s;
		}
	}

	private static class AyantDroit {
		protected final String type;
		protected final String name;
		protected final String label;

		public AyantDroit(String type, String name, String label) {
			this.type = type;
			this.name = name;
			this.label = label;
		}

		public String toDot() {
			return name + " [shape=oval, label=\"" + type + "\n" + label + "\", style=filled, color=lightblue1]";
		}
	}

	private static class Communaute extends AyantDroit {

		private final Map<String, AyantDroit> membres = new HashMap<>();

		public Communaute(String name, String label) {
			super("COM", name, label);
		}

		public AyantDroit addMembre(AyantDroitRF m) {
			final String collName = name;
			if (m instanceof PersonnePhysiqueRF) {
				final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) m;
				final String ppName = "PP" + pp.getId() + collName;
				return membres.computeIfAbsent(ppName, n -> new PersonnePhysique(ppName, pp.getPrenom() + " " + pp.getNom()));
			}
			else if (m instanceof PersonneMoraleRF) {
				final PersonneMoraleRF pm = (PersonneMoraleRF) m;
				final String ppName = "PM" + pm.getId() + collName;
				return membres.computeIfAbsent(ppName, n -> new PersonneMorale(ppName, pm.getRaisonSociale()));
			}
			else {
				throw new IllegalArgumentException("Type de membre de communauté non-supporté = [" + m.getClass().getSimpleName() + "]");
			}
		}

		public String toDot() {
			final StringBuilder s = new StringBuilder();
			s.append("subgraph ").append(name).append(" {\n")
					.append("    node [style=filled];\n")
					.append("    color=lightblue4;\n");
			s.append("    ").append(super.toDot());
			membres.values().forEach(obj -> s.append("    ").append(obj.toDot()).append("\n"));
			s.append("}");
			return s.toString();
		}
	}

	private static class PersonnePhysique extends AyantDroit {
		public PersonnePhysique(String name, String label) {
			super("PP", name, label);
		}
	}

	private static class PersonneMorale extends AyantDroit {
		public PersonneMorale(String name, String label) {
			super("PM", name, label);
		}
	}

	private static class Collectivite extends AyantDroit {
		public Collectivite(String name, String label) {
			super("COLL", name, label);
		}
	}

	private static class Droit {
		private final String color;
		private final String style;
		private final String source;
		private final String destination;
		private final String label;

		public Droit(String source, String destination, String label, String color, String style) {
			this.color = color;
			this.source = source;
			this.destination = destination;
			this.label = label;
			this.style = style;
		}

		public String toDot() {
			return source + " -> " + destination + " [label=\"" + label + "\", color=" + color + ", style=" + style + "]";
		}
	}

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
		return immeubles.containsKey(getName(immeuble));
	}

	private void addProcessed(@NotNull ImmeubleRF immeuble) {
		final Immeuble i = new Immeuble(immeuble);
		immeubles.put(i.name, i);
	}

	private void addDroitPropriete(DroitProprieteRF droit) {
		final String label = droit.getRegime().name() + " (" + droit.getPart() + ")";
		final String color = getDroitColor(droit);

		final CommunauteRF communaute = getCommunaute(droit);
		if (communaute == null) {
			// un droit normal
			final String source = getName(droit.getAyantDroit());
			final String destination = getName(droit.getImmeuble());

			addAyantDroit(ayantDroits, droit.getAyantDroit());
			droits.add(new Droit(source, destination, label, color, "solid"));
		}
		else {
			// en cas de communauté, on crée une copie de l'ayant-droit pour l'associer à la communauté
			final Communaute comm = (Communaute) addAyantDroit(ayantDroits, communaute);
			final AyantDroit membre = comm.addMembre(droit.getAyantDroit());
			final String source = membre.name;
			final String destination = getName(droit.getImmeuble());
			droits.add(new Droit(source, destination, label, color, "dashed"));
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

	private static String getName(@NotNull AyantDroitRF ayantDroit) {
		if (ayantDroit instanceof CommunauteRF) {
			final CommunauteRF communaute = (CommunauteRF) ayantDroit;
			return "clusterComm" + communaute.getId();
		}
		else if (ayantDroit instanceof ImmeubleBeneficiaireRF) {
			final ImmeubleBeneficiaireRF beneficiaire = (ImmeubleBeneficiaireRF) ayantDroit;
			return getName(beneficiaire.getImmeuble());
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

	private static AyantDroit addAyantDroit(@NotNull Map<String, AyantDroit> ayantDroits, @NotNull AyantDroitRF ayantDroit) {
		final String name = getName(ayantDroit);
		if (ayantDroit instanceof CommunauteRF) {
			final CommunauteRF communaute = (CommunauteRF) ayantDroit;
			return ayantDroits.computeIfAbsent(name, n -> new Communaute(name, "Communauté #" + communaute.getId()));
		}
		else if (ayantDroit instanceof ImmeubleBeneficiaireRF) {
			return null;
		}
		else if (ayantDroit instanceof PersonnePhysiqueRF) {
			final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) ayantDroit;
			return ayantDroits.computeIfAbsent(name, n -> new PersonnePhysique(name, pp.getPrenom() + " " + pp.getNom()));
		}
		else if (ayantDroit instanceof PersonneMoraleRF) {
			final PersonneMoraleRF pm = (PersonneMoraleRF) ayantDroit;
			return ayantDroits.computeIfAbsent(name, n -> new PersonneMorale(name, pm.getRaisonSociale()));
		}
		else if (ayantDroit instanceof CollectivitePubliqueRF) {
			final CollectivitePubliqueRF cp = (CollectivitePubliqueRF) ayantDroit;
			return ayantDroits.computeIfAbsent(name, n -> new Collectivite(name, cp.getRaisonSociale()));
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	private static String getName(@NotNull ImmeubleRF immeuble) {
		final String egrid = immeuble.getEgrid();
		return egrid == null ? immeuble.getIdRF() : egrid;
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
