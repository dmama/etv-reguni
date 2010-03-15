/**
 *
 */
package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TarifImpotSource;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author msi
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#__U_BEBxAEd2SDKWRJy7Z3g"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#__U_BEBxAEd2SDKWRJy7Z3g"
 */
@Entity
@DiscriminatorValue("SituationFamilleMenageCommun")
public class SituationFamilleMenageCommun extends SituationFamille {

	private static final long serialVersionUID = 552273853607746105L;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_EwQNgOxKEdy6n58hR-kALg"
	 */
	private TarifImpotSource tarifApplicable;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_94tjkfYxEdyw0I40oDFBsg"
	 */
	private Contribuable contribuablePrincipal;

	public SituationFamilleMenageCommun() {
	}
	
	public SituationFamilleMenageCommun(SituationFamilleMenageCommun situationFamilleMenageCommun) {
		super(situationFamilleMenageCommun);
		
		this.contribuablePrincipal = situationFamilleMenageCommun.contribuablePrincipal;
		this.tarifApplicable = situationFamilleMenageCommun.tarifApplicable;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the contribuablePrincipal
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_94tjkfYxEdyw0I40oDFBsg?GETTER"
	 */
	@ManyToOne()
	@JoinColumn(name = "TIERS_PRINCIPAL_ID")
	@Index(name = "IDX_SIT_FAM_MC_CTB_ID", columnNames = "TIERS_PRINCIPAL_ID")
	@ForeignKey(name = "FK_SIT_FAM_MC_CTB_ID")
	public Contribuable getContribuablePrincipal() {
		// begin-user-code
		return contribuablePrincipal;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theContribuablePrincipal the contribuablePrincipal to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_94tjkfYxEdyw0I40oDFBsg?SETTER"
	 */
	public void setContribuablePrincipal(Contribuable theContribuablePrincipal) {
		// begin-user-code
		contribuablePrincipal = theContribuablePrincipal;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the tarifApplicable
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_EwQNgOxKEdy6n58hR-kALg?GETTER"
	 */
	@Column(name = "TARIF_APPLICABLE", length = LengthConstants.SITUATIONFAMILLE_TARIF)
	@Type(type = "ch.vd.uniregctb.hibernate.TarifImpotSourceUserType")
	public TarifImpotSource getTarifApplicable() {
		// begin-user-code
		return tarifApplicable;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theTarifApplicable the tarifApplicable to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_EwQNgOxKEdy6n58hR-kALg?SETTER"
	 */
	public void setTarifApplicable(TarifImpotSource theTarifApplicable) {
		// begin-user-code
		tarifApplicable = theTarifApplicable;
		// end-user-code
	}

	@Override
	public SituationFamille duplicate() {
		return new SituationFamilleMenageCommun(this);
	}
}
