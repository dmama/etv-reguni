package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.uniregctb.type.*;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.Validateable;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.LengthConstants;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author msi
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_i6Jx4G7DEd2HlNPAVeri9w"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_i6Jx4G7DEd2HlNPAVeri9w"
 */
@Entity
@DiscriminatorValue("ENVOI_DI")
public class TacheEnvoiDeclarationImpot extends Tache implements DateRange, Validateable {

	private static final long serialVersionUID = 6038437798535074010L;

	/**
	 * <!-- begin-user-doc -->
	 * Date de début d'imposition (précalculé) pour la déclaration à envoyer.
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0UCO0G7DEd2HlNPAVeri9w"
	 */
	private RegDate dateDebut;

	/**
	 * <!-- begin-user-doc -->
	 * Date de fin d'imposition (précalculé) pour la déclaration à envoyer.
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0UL_0G7DEd2HlNPAVeri9w"
	 */
	private RegDate dateFin;

	/**
	 * <!-- begin-user-doc -->
	 * Type de contribuable (précalculé) pour la déclaration à envoyer.
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_6b8oQG7DEd2HlNPAVeri9w"
	 */
	private TypeContribuable typeContribuable;

	/**
	 * <!-- begin-user-doc -->
	 * Type de document (précalculé) pour la déclaration à envoyer.
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ljgu4G7FEd2HlNPAVeri9w"
	 */
	private TypeDocument typeDocument;

	/**
	 * <!-- begin-user-doc -->
	 * Qualification.
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ljgu4G7FEd2HlNPAVeri9w"
	 */
	private Qualification qualification;

	private TypeAdresseRetour adresseRetour;

	// Ce constructeur est requis par Hibernate
	protected TacheEnvoiDeclarationImpot() {
	}

	public TacheEnvoiDeclarationImpot(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, RegDate dateDebut, RegDate dateFin, TypeContribuable typeContribuable,
	                                  TypeDocument typeDocument, Qualification qualification, TypeAdresseRetour adresseRetour,CollectiviteAdministrative collectivite) {
		super(etat, dateEcheance, contribuable,collectivite);
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.typeContribuable = typeContribuable;
		this.typeDocument = typeDocument;
		this.qualification = qualification;
		this.adresseRetour = adresseRetour;

	}

	public TacheEnvoiDeclarationImpot(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, RegDate dateDebut, RegDate dateFin, TypeContribuable typeContribuable,
	                                  TypeDocument typeDocument, Qualification qualification, TypeAdresseRetour adresseRetour){
		   	this(etat, dateEcheance, contribuable, dateDebut, dateFin, typeContribuable, typeDocument, qualification, adresseRetour,null);
	}

	

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the dateDebut
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0UCO0G7DEd2HlNPAVeri9w?GETTER"
	 */
	@Column(name = "DECL_DATE_DEBUT")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		// begin-user-code
		return dateDebut;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDateDebut the dateDebut to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0UCO0G7DEd2HlNPAVeri9w?SETTER"
	 */
	public void setDateDebut(RegDate theDateDebut) {
		// begin-user-code
		dateDebut = theDateDebut;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the dateFin
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0UL_0G7DEd2HlNPAVeri9w?GETTER"
	 */
	@Column(name = "DECL_DATE_FIN")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		// begin-user-code
		return dateFin;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDateFin the dateFin to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0UL_0G7DEd2HlNPAVeri9w?SETTER"
	 */
	public void setDateFin(RegDate theDateFin) {
		// begin-user-code
		dateFin = theDateFin;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the typeContribuable
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_6b8oQG7DEd2HlNPAVeri9w?GETTER"
	 */
	@Column(name = "DECL_TYPE_CTB", length = LengthConstants.DI_TYPE)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeContribuableUserType")
	@Index(name = "IDX_TACHE_TYPE_CTB")
	public TypeContribuable getTypeContribuable() {
		// begin-user-code
		return typeContribuable;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theTypeContribuable the typeContribuable to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_6b8oQG7DEd2HlNPAVeri9w?SETTER"
	 */
	public void setTypeContribuable(TypeContribuable theTypeContribuable) {
		// begin-user-code
		typeContribuable = theTypeContribuable;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the typeDocument
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ljgu4G7FEd2HlNPAVeri9w?GETTER"
	 */
	@Column(name = "DECL_TYPE_DOC", length = LengthConstants.MODELEDOC_TYPE)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeDocumentUserType")
	@Index(name = "IDX_TACHE_TYPE_DOC")
	public TypeDocument getTypeDocument() {
		// begin-user-code
		return typeDocument;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theTypeDocument the typeDocument to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ljgu4G7FEd2HlNPAVeri9w?SETTER"
	 */
	public void setTypeDocument(TypeDocument theTypeDocument) {
		// begin-user-code
		typeDocument = theTypeDocument;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the qualification
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_sGA-oDfHEd2EkOqealhanQ?GETTER"
	 */
	@Column(name = "QUALIFICATION", length = LengthConstants.DI_QUALIF )
	@Type(type = "ch.vd.uniregctb.hibernate.QualificationUserType")
	public Qualification getQualification() {
		return qualification;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theQualification the qualification to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ljgu4G7FEd2HlNPAVeri9w?SETTER"
	 */
	public void setQualification(Qualification theQualification) {
		this.qualification = theQualification;
	}

	@Column(name = "DECL_ADRESSE_RETOUR", length = LengthConstants.DI_ADRESSE_RETOUR)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeAdresseRetourUserType")
	public TypeAdresseRetour getAdresseRetour() {
		return adresseRetour;
	}

	public void setAdresseRetour(TypeAdresseRetour adresseRetour) {
		this.adresseRetour = adresseRetour;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	/**
	 * {@inheritDoc}
	 */
	public ValidationResults validate() {

		ValidationResults results = new ValidationResults();

		DateRangeHelper.validate(this, false, false, results);

		if (typeContribuable == null) {
			results.addError("Le type de contribuable ne peut pas être nul.");
		}

		if (typeDocument == null) {
			results.addError("Le type de document ne peut pas être nul.");
		}

		return results;
	}

	@Transient
	@Override
	public TypeTache getTypeTache() {
		return TypeTache.TacheEnvoiDeclarationImpot;
	}
}
