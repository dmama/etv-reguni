package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

@Entity
@DiscriminatorValue("ENVOI_DI")
public class TacheEnvoiDeclarationImpot extends Tache implements DateRange {

	/**
	 * Date de début d'imposition (précalculé) pour la déclaration à envoyer.
	 */
	private RegDate dateDebut;

	/**
	 * Date de fin d'imposition (précalculé) pour la déclaration à envoyer.
	 */
	private RegDate dateFin;

	/**
	 * Type de contribuable (précalculé) pour la déclaration à envoyer.
	 */
	private TypeContribuable typeContribuable;

	/**
	 * Type de document (précalculé) pour la déclaration à envoyer.
	 */
	private TypeDocument typeDocument;

	/**
	 * Qualification.
	 */
	private Qualification qualification;

	/**
	 * [SIFISC-2100] introduction des codes "segment" dès la DI 2011
	 */
	private Integer codeSegment;

	private TypeAdresseRetour adresseRetour;

	// Ce constructeur est requis par Hibernate
	protected TacheEnvoiDeclarationImpot() {
	}

	public TacheEnvoiDeclarationImpot(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, RegDate dateDebut, RegDate dateFin, TypeContribuable typeContribuable,
	                                  TypeDocument typeDocument, Qualification qualification, Integer codeSegment, TypeAdresseRetour adresseRetour, CollectiviteAdministrative collectivite) {
		super(etat, dateEcheance, contribuable, collectivite);
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.typeContribuable = typeContribuable;
		this.typeDocument = typeDocument;
		this.qualification = qualification;
		this.codeSegment = codeSegment;
		this.adresseRetour = adresseRetour;
	}

	@Override
	@Column(name = "DECL_DATE_DEBUT")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate theDateDebut) {
		dateDebut = theDateDebut;
	}

	@Override
	@Column(name = "DECL_DATE_FIN")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate theDateFin) {
		dateFin = theDateFin;
	}

	@Column(name = "DECL_TYPE_CTB", length = LengthConstants.DI_TYPE)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeContribuableUserType")
	@Index(name = "IDX_TACHE_TYPE_CTB")
	public TypeContribuable getTypeContribuable() {
		return typeContribuable;
	}

	public void setTypeContribuable(TypeContribuable theTypeContribuable) {
		typeContribuable = theTypeContribuable;
	}

	@Column(name = "DECL_TYPE_DOC", length = LengthConstants.MODELEDOC_TYPE)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeDocumentUserType")
	@Index(name = "IDX_TACHE_TYPE_DOC")
	public TypeDocument getTypeDocument() {
		return typeDocument;
	}

	public void setTypeDocument(TypeDocument theTypeDocument) {
		typeDocument = theTypeDocument;
	}

	@Column(name = "QUALIFICATION", length = LengthConstants.DI_QUALIF )
	@Type(type = "ch.vd.uniregctb.hibernate.QualificationUserType")
	public Qualification getQualification() {
		return qualification;
	}

	public void setQualification(Qualification theQualification) {
		this.qualification = theQualification;
	}

	@Column(name = "CODE_SEGMENT")
	public Integer getCodeSegment() {
		return codeSegment;
	}

	public void setCodeSegment(Integer codeSegment) {
		this.codeSegment = codeSegment;
	}

	@Column(name = "DECL_ADRESSE_RETOUR", length = LengthConstants.DI_ADRESSE_RETOUR)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeAdresseRetourUserType")
	public TypeAdresseRetour getAdresseRetour() {
		return adresseRetour;
	}

	public void setAdresseRetour(TypeAdresseRetour adresseRetour) {
		this.adresseRetour = adresseRetour;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Transient
	@Override
	public TypeTache getTypeTache() {
		return TypeTache.TacheEnvoiDeclarationImpot;
	}
}
