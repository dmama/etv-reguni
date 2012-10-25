package ch.vd.uniregctb.admin;

import java.util.List;

import ch.vd.uniregctb.tiers.NatureTiers;

/**
 * Information de base sur un tiers
 */
public class InfoTiers {

	private final long numero;
	private final NatureTiers type;
	private final String nomsPrenoms;

	public InfoTiers(long numero, NatureTiers type, List<String> nomsPrenoms) {
		this.numero = numero;
		this.type = type;

		StringBuilder b = new StringBuilder();
		for (String n : nomsPrenoms) {
			if (b.length() != 0) {
				b.append(" / ");
			}
			b.append(n);
		}
		this.nomsPrenoms = b.toString();
	}

	public long getNumero() {
		return numero;
	}

	public NatureTiers getType() {
		return type;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public String getNomsPrenoms() {
		return nomsPrenoms;
	}
}
