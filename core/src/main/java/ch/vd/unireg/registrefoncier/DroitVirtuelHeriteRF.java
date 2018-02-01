package ch.vd.unireg.registrefoncier;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * Droit virtuel généré à la volée pour un tiers qui hérite des droits d'un défunt (ou pour une entreprise en cas de fusion).
 */
public class DroitVirtuelHeriteRF extends DroitVirtuelRF {

	/**
	 * L'id du décédé Unireg duquel provient le droit de référence.
	 */
	private long decedeId;

	/**
	 * L'id de l'héritier Unireg auquel est rattaché ce droit.
	 */
	private long heritierId;

	/**
	 * Le nombre d'héritiers du décédé.
	 */
	private int nombreHeritiers;

	/**
	 * Le droit de référence tel qu'il est défini sur le décédé.
	 */
	private DroitRF reference;

	public long getDecedeId() {
		return decedeId;
	}

	public void setDecedeId(long decedeId) {
		this.decedeId = decedeId;
	}

	public long getHeritierId() {
		return heritierId;
	}

	public void setHeritierId(long heritierId) {
		this.heritierId = heritierId;
	}

	public int getNombreHeritiers() {
		return nombreHeritiers;
	}

	public void setNombreHeritiers(int nombreHeritiers) {
		this.nombreHeritiers = nombreHeritiers;
	}

	public DroitRF getReference() {
		return reference;
	}

	public void setReference(DroitRF reference) {
		this.reference = reference;
	}

	@Override
	@NotNull
	public TypeDroit getTypeDroit() {
		return reference.getTypeDroit();
	}

	@Override
	public @NotNull List<AyantDroitRF> getAyantDroitList() {
		return Collections.emptyList();
	}

	@Override
	public @NotNull List<ImmeubleRF> getImmeubleList() {
		return reference.getImmeubleList();
	}
}
