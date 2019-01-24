package ch.vd.unireg.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * <pre>
 *   +------------------+                   +--------------------+
 *   |     SNC          |                   |         Associe        |
 *   +------------------+                   +--------------------+
 *           ^                                           ^
 *           ¦  sujet  +-----------------------+  objet  ¦
 *           +---------| LienAssociesEtSNC    |---------+
 *                     +----------------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("LienAssociesEtSNC")
public class LienAssociesEtSNC extends RapportEntreTiers {

	private static final String ASSOCIES = "Associes";
	private static final String SNC = "SNC";

	public LienAssociesEtSNC() {
		// empty
	}

	public LienAssociesEtSNC(RegDate dateDebut, RegDate dateFin, Contribuable associe, Entreprise snc) {
		super(dateDebut, dateFin, snc, associe);
	}

	protected LienAssociesEtSNC(LienAssociesEtSNC src) {
		super(src);
	}

	@Transient
	@Override
	public String getDescriptionTypeObjet() {
		return ASSOCIES;
	}

	@Transient
	@Override
	public String getDescriptionTypeSujet() {
		return SNC;
	}

	@Transient
	@Override
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.LIENS_ASSOCIES_ET_SNC;
	}

	@Override
	public LienAssociesEtSNC duplicate() {
		return new LienAssociesEtSNC(this);
	}
}
