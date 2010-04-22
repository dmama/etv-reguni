package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.ForeignKey;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.Validateable;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author msi
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_9HdzUG7DEd2HlNPAVeri9w"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_9HdzUG7DEd2HlNPAVeri9w"
 */
@Entity
@DiscriminatorValue("ANNUL_DI")
public class TacheAnnulationDeclarationImpot extends Tache implements Validateable {

	private static final long serialVersionUID = -4247341112110453868L;

	// Ce constructeur est requis par Hibernate
	protected TacheAnnulationDeclarationImpot() {
	}

	public TacheAnnulationDeclarationImpot(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, DeclarationImpotOrdinaire declarationImpotOrdinaire,
	                                       CollectiviteAdministrative collectiviteAdministrativeAssignee) {
		super(etat, dateEcheance, contribuable, collectiviteAdministrativeAssignee);
		this.declarationImpotOrdinaire = declarationImpotOrdinaire;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_PeEFQW7GEd2HlNPAVeri9w"
	 */
	private DeclarationImpotOrdinaire declarationImpotOrdinaire;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the DeclarationImpotOrdinaire
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_PeEFQW7GEd2HlNPAVeri9w?GETTER"
	 */
	@ManyToOne
	@JoinColumn(name = "DECLARATION_ID")
	@ForeignKey(name = "FK_TACH_DECL_ID")
	public DeclarationImpotOrdinaire getDeclarationImpotOrdinaire() {
		// begin-user-code
		return declarationImpotOrdinaire;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDeclarationImpotOrdinaire the DeclarationImpotOrdinaire to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_PeEFQW7GEd2HlNPAVeri9w?SETTER"
	 */
	public void setDeclarationImpotOrdinaire(DeclarationImpotOrdinaire theDeclarationImpotOrdinaire) {
		// begin-user-code
		declarationImpotOrdinaire = theDeclarationImpotOrdinaire;
		// end-user-code
	}

	public ValidationResults validate() {

		ValidationResults results = super.validate();

		if (declarationImpotOrdinaire == null) {
			results.addError("La déclaration ne peut pas être nulle.");
		}

		return results;
	}

	@Transient
	@Override
	public TypeTache getTypeTache() {
		return TypeTache.TacheAnnulationDeclarationImpot;
	}
}
