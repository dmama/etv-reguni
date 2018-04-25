package ch.vd.unireg.registrefoncier.immeuble.graph;

import java.util.HashMap;
import java.util.Map;

import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.PersonneMoraleRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;

/**
 * Elément qui représente une communauté dans le graphe des liens entre immeubles et ayants-droits.
 */
public class Communaute extends AyantDroit {

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
