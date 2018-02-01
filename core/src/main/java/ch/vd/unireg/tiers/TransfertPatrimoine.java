package ch.vd.unireg.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * <pre>
 *   +----------------------+                +-----------------------+
 *   | Entreprise émettrice |                | Entreprise réceptrice |
 *   +----------------------+                +-----------------------+
 *           ^                                        ^
 *           ¦  sujet  +--------------------+  objet  ¦
 *           +---------| TranfertPatrimoine |---------+
 *                     +--------------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("TransfertPatrimoine")
public class TransfertPatrimoine extends RapportEntreTiers {

	private static final String EMETTRICE = "entreprise émettrice du patrimoine transféré";
	private static final String RECEPTRICE = "entreprise réceptrice du patrimoine transféré";

	public TransfertPatrimoine() {
		// empty
	}

	public TransfertPatrimoine(RegDate dateDebut, RegDate dateFin, Entreprise emettrice, Entreprise receptrice) {
		super(dateDebut, dateFin, emettrice, receptrice);
	}

	protected TransfertPatrimoine(TransfertPatrimoine src) {
		super(src);
	}

	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.TRANSFERT_PATRIMOINE;
	}

	@Override
	@Transient
	public String getDescriptionTypeObjet() {
		return RECEPTRICE;
	}

	@Override
	@Transient
	public String getDescriptionTypeSujet() {
		return EMETTRICE;
	}

	@Override
	public RapportEntreTiers duplicate() {
		return new TransfertPatrimoine(this);
	}

}
