package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <!-- begin-user-doc -->
 * <pre>
 * +--------------+                        +-----------------------------+
 * | Contribuable |                        | DebiteurPrestationImposable |
 * +--------------+                        +-----------------------------+
 *        ^                                                ^
 *        ¦  sujet  +----------------------------+  objet  ¦
 *        +---------| RapportPrestationImposable |---------+
 *                  +----------------------------+
 * </pre>
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_uAPHcNjDEdyFDMrjDUGsjQ"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_uAPHcNjDEdyFDMrjDUGsjQ"
 */
@Entity
@DiscriminatorValue("RapportPrestationImposable")
public class RapportPrestationImposable extends RapportEntreTiers {

	private static final String EMPLOYEUR = "employeur";
	private static final String SOURCIER = "sourcier";

	public RapportPrestationImposable() {
		// empty
	}

	public RapportPrestationImposable(RegDate dateDebut, RegDate dateFin, Contribuable sujet, DebiteurPrestationImposable objet) {
		super(dateDebut, dateFin, sujet, objet);
	}

	public RapportPrestationImposable(RapportPrestationImposable rapport) {
		super(rapport);
		this.finDernierElementImposable = rapport.getFinDernierElementImposable();
	}

	@Override
	@Transient
	public String getDescriptionTypeObjet() {
		return EMPLOYEUR;
	}

	@Override
	@Transient
	public String getDescriptionTypeSujet() {
		return SOURCIER;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_u27e4NjDEdyFDMrjDUGsjQ"
	 */
	private RegDate finDernierElementImposable;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the finDernierElementImposable
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_u27e4NjDEdyFDMrjDUGsjQ?GETTER"
	 */
	@Column(name = "DATE_FIN_DER_ELE_IMP")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getFinDernierElementImposable() {
		// begin-user-code
		return finDernierElementImposable;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theFinDernierElementImposable the finDernierElementImposable to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_u27e4NjDEdyFDMrjDUGsjQ?SETTER"
	 */
	public void setFinDernierElementImposable(RegDate theFinDernierElementImposable) {
		// begin-user-code
		finDernierElementImposable = theFinDernierElementImposable;
		// end-user-code
	}

	/*
	 * (non-Javadoc)
	 * @see ch.vd.uniregctb.tiers.RapportEntreTiers#getType()
	 */
	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.PRESTATION_IMPOSABLE;
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.tiers.RapportEntreTiers#duplicate()
	 */
	@Override
	public RapportEntreTiers duplicate() {
		return new RapportPrestationImposable(this);
	}
}
