/**
 *
 */
package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <!-- begin-user-doc -->
 * <pre>
 * +-------+                                           +-------+
 * | Tiers | (représenté)               (représentant) | Tiers |
 * +-------+                                           +-------+
 *     ^                                                   ^
 *     ¦  sujet  +-------------------------------+  objet  ¦
 *     +---------| RepresentationConventionnelle |---------+
 *               +-------------------------------+
 * </pre>
 * <!-- end-user-doc -->
 * @author msi
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ZV_O4BFLEd2nzO4G1YQacw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ZV_O4BFLEd2nzO4G1YQacw"
 */
@Entity
@DiscriminatorValue("RepresentationConventionnelle")
public class RepresentationConventionnelle extends RapportEntreTiers {

	private static final long serialVersionUID = 7957450690807693403L;

	public RepresentationConventionnelle() {
		// empty
	}

	public RepresentationConventionnelle(RepresentationConventionnelle representation) {
		super(representation);
		this.extensionExecutionForcee = representation.getExtensionExecutionForcee();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_et5uwBFLEd2nzO4G1YQacw"
	 */
	private Boolean extensionExecutionForcee = false;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the extensionExecutionForcee
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_et5uwBFLEd2nzO4G1YQacw?GETTER"
	 */
	@Column(name = "EXTENSION_EXECUTION_FORCEE")
	public Boolean getExtensionExecutionForcee() {
		// begin-user-code
		return extensionExecutionForcee;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theExtensionExecutionForcee the extensionExecutionForcee to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_et5uwBFLEd2nzO4G1YQacw?SETTER"
	 */
	public void setExtensionExecutionForcee(Boolean theExtensionExecutionForcee) {
		// begin-user-code
		extensionExecutionForcee = theExtensionExecutionForcee;
		// end-user-code
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.tiers.RapportEntreTiers#duplicate()
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