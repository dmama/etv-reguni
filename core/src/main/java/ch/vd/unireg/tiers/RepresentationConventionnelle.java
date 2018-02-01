package ch.vd.unireg.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * <pre>
 * +-------+                                           +-------+
 * | Tiers | (représenté)               (représentant) | Tiers |
 * +-------+                                           +-------+
 *     ^                                                   ^
 *     ¦  sujet  +-------------------------------+  objet  ¦
 *     +---------| RepresentationConventionnelle |---------+
 *               +-------------------------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("RepresentationConventionnelle")
public class RepresentationConventionnelle extends RapportEntreTiers {

	private static final String REPRESENTE = "représenté";
	private static final String REPRESENTANT = "représentant";

	private Boolean extensionExecutionForcee = false;

	public RepresentationConventionnelle() {
		// empty
	}

	public RepresentationConventionnelle(RepresentationConventionnelle representation) {
		super(representation);
		this.extensionExecutionForcee = representation.getExtensionExecutionForcee();
	}

	@Override
	@Transient
	public String getDescriptionTypeObjet() {
		return REPRESENTANT;
	}

	@Override
	@Transient
	public String getDescriptionTypeSujet() {
		return REPRESENTE;
	}

	@Column(name = "EXTENSION_EXECUTION_FORCEE")
	public Boolean getExtensionExecutionForcee() {
		return extensionExecutionForcee;
	}

	public void setExtensionExecutionForcee(Boolean theExtensionExecutionForcee) {
		extensionExecutionForcee = theExtensionExecutionForcee;
	}

	/* (non-Javadoc)
	 * @see ch.vd.unireg.tiers.RapportEntreTiers#duplicate()
	 */
	@Override
	public RapportEntreTiers duplicate() {
		return new RepresentationConventionnelle(this);
	}

	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.REPRESENTATION;
	}
}