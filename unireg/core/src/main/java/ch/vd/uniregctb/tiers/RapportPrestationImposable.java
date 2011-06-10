package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeActivite;
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

	private static final long serialVersionUID = 6015152795899259734L;

	public RapportPrestationImposable() {
		// empty
	}

	public RapportPrestationImposable(RegDate dateDebut, RegDate dateFin, Contribuable sujet, DebiteurPrestationImposable objet) {
		super(dateDebut, dateFin, sujet, objet);
	}

	public RapportPrestationImposable(RapportPrestationImposable rapport) {
		super(rapport);
		this.typeActivite = rapport.getTypeActivite();
		this.tauxActivite = rapport.getTauxActivite();
		this.finDernierElementImposable = rapport.getFinDernierElementImposable();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_IeZIoOxJEdycMumkNMs2uQ"
	 */
	private TypeActivite typeActivite;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the typeActivite
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_IeZIoOxJEdycMumkNMs2uQ?GETTER"
	 */
	@Type(type = "ch.vd.uniregctb.hibernate.TypeActiviteUserType")
	@Column(name = "TYPE_ACTIVITE", length = LengthConstants.RAPPORT_TYPEACTIVITE)
	public TypeActivite getTypeActivite() {
		// begin-user-code
		return typeActivite;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theTypeActivite the typeActivite to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_IeZIoOxJEdycMumkNMs2uQ?SETTER"
	 */
	public void setTypeActivite(TypeActivite theTypeActivite) {
		// begin-user-code
		typeActivite = theTypeActivite;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#__5NmEOxJEdy6n58hR-kALg"
	 */
	private Integer tauxActivite;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the tauxActivite
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#__5NmEOxJEdy6n58hR-kALg?GETTER"
	 */
	@Column(name = "TAUX_ACTIVITE")
	public Integer getTauxActivite() {
		// begin-user-code
		return tauxActivite;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theTauxActivite the tauxActivite to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#__5NmEOxJEdy6n58hR-kALg?SETTER"
	 */
	public void setTauxActivite(Integer theTauxActivite) {
		// begin-user-code
		tauxActivite = theTauxActivite;
		// end-user-code
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
