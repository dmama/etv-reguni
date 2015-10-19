package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.EnumSet;
import java.util.Set;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

@Entity
@DiscriminatorValue("ENVOI_DI_PP")
public class TacheEnvoiDeclarationImpotPP extends TacheEnvoiDeclarationImpot {

	/**
	 * Tous les types de documents acceptés pour une tâche d'envoi de DI PP
	 */
	private static final Set<TypeDocument> TYPES_DOCUMENTS_AUTORISES = EnumSet.of(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
	                                                                              TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL,
	                                                                              TypeDocument.DECLARATION_IMPOT_DEPENSE,
	                                                                              TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE,
	                                                                              TypeDocument.DECLARATION_IMPOT_VAUDTAX);

	/**
	 * Type de contribuable (précalculé) pour la déclaration à envoyer.
	 */
	private TypeContribuable typeContribuable;

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
	protected TacheEnvoiDeclarationImpotPP() {
	}

	public TacheEnvoiDeclarationImpotPP(TypeEtatTache etat, RegDate dateEcheance, ContribuableImpositionPersonnesPhysiques contribuable, RegDate dateDebut, RegDate dateFin, TypeContribuable typeContribuable,
	                                    TypeDocument typeDocument, Qualification qualification, Integer codeSegment, TypeAdresseRetour adresseRetour, CollectiviteAdministrative collectivite) {
		super(etat, dateEcheance, contribuable, dateDebut, dateFin, typeDocument, collectivite);
		this.typeContribuable = typeContribuable;
		this.qualification = qualification;
		this.codeSegment = codeSegment;
		this.adresseRetour = adresseRetour;

		if (!TYPES_DOCUMENTS_AUTORISES.contains(typeDocument)) {
			throw new IllegalArgumentException("Le type de document " + typeDocument + " n'est pas accepté pour une tâche d'envoi de déclaration PP.");
		}
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

	@Transient
	@Override
	public ContribuableImpositionPersonnesPhysiques getContribuable() {
		return (ContribuableImpositionPersonnesPhysiques) super.getContribuable();
	}

	@Transient
	@Override
	public TypeTache getTypeTache() {
		return TypeTache.TacheEnvoiDeclarationImpotPP;
	}
}
