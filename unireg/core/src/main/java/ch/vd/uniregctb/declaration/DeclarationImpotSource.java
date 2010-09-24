package ch.vd.uniregctb.declaration;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_5EpQAOqeEdySTq6PFlf9jQ"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_5EpQAOqeEdySTq6PFlf9jQ"
 */
@Entity
@DiscriminatorValue("LR")
public class DeclarationImpotSource extends Declaration {

	private static final long serialVersionUID = 8852365449351996537L;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_JTNrAAVnEd2-GsFUBw6pEA"
	 */
	private PeriodiciteDecompte periodicite;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_G4oe8AVnEd2-GsFUBw6pEA"
	 */
	private ModeCommunication modeCommunication;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_diY2cOOvEdyN8MKJ3LhMnw"
	 */
	private Boolean sansRappel;
	
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the periodicite
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_JTNrAAVnEd2-GsFUBw6pEA?GETTER"
	 */
	@Column(name = "PERIODICITE", length = LengthConstants.DPI_PERIODICITE)
	@Type(type = "ch.vd.uniregctb.hibernate.PeriodiciteDecompteUserType")
	public PeriodiciteDecompte getPeriodicite() {
		// begin-user-code
		return periodicite;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param thePeriodicite the periodicite to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_JTNrAAVnEd2-GsFUBw6pEA?SETTER"
	 */
	public void setPeriodicite(PeriodiciteDecompte thePeriodicite) {
		// begin-user-code
		periodicite = thePeriodicite;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the modeCommunication
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_G4oe8AVnEd2-GsFUBw6pEA?GETTER"
	 */
	@Column(name = "MODE_COM", length = LengthConstants.DPI_MODECOM)
	@Type(type = "ch.vd.uniregctb.hibernate.ModeCommunicationUserType")
	public ModeCommunication getModeCommunication() {
		// begin-user-code
		return modeCommunication;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theModeCommunication the modeCommunication to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_G4oe8AVnEd2-GsFUBw6pEA?SETTER"
	 */
	public void setModeCommunication(ModeCommunication theModeCommunication) {
		// begin-user-code
		modeCommunication = theModeCommunication;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the sansRappel
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_diY2cOOvEdyN8MKJ3LhMnw?GETTER"
	 */
	@Column(name = "SANS_RAPPEL")
	public Boolean getSansRappel() {
		return sansRappel;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theSansRappel
	 *            the sansRappel to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_diY2cOOvEdyN8MKJ3LhMnw?SETTER"
	 */
	public void setSansRappel(Boolean sansRappel) {
		this.sansRappel = sansRappel;
	}

	@Transient
	public RegDate getDateSommation() {
		RegDate dateSommation = null;
		GregorianCalendar calSommation = new GregorianCalendar();
		Set<EtatDeclaration> etatsDocument = getEtats();
		if (etatsDocument.size() == 1) {
			Iterator<EtatDeclaration> itEtat = etatsDocument.iterator();
			EtatDeclaration etat = itEtat.next();
			if (etat.getEtat().equals(TypeEtatDeclaration.EMISE)) {
				RegDate dateObtention = etat.getDateObtention();
				calSommation.setTime(dateObtention.asJavaDate());
				calSommation.add(Calendar.MONTH, 1);
				calSommation.add(Calendar.DATE, 15);
				dateSommation = RegDate.get(calSommation.getTime());
			}
		}
		return dateSommation;
	}

}