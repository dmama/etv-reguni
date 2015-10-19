package ch.vd.uniregctb.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.TypeContribuable;

@Entity
@DiscriminatorValue("DI")
public class DeclarationImpotOrdinairePP extends DeclarationImpotOrdinaire {

	/**
	 * [SIFISC-1368] première année où le retour par courrier électronique des déclarations d'impôt est possible.
	 */
	public static final int PREMIERE_ANNEE_RETOUR_ELECTRONIQUE = 2011;

	private Integer numeroOfsForGestion;

	private TypeContribuable typeContribuable;

	private Qualification qualification;

	/**
	 * [SIFISC-2100] Code de segmentation, ou Code Segment, fourni par TAO et utilisé lors de l'émission de la DI suivante
	 */
	private Integer codeSegment;

	@Column(name = "TYPE_CTB", length = LengthConstants.DI_TYPE_CTB)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeContribuableUserType")
	public TypeContribuable getTypeContribuable() {
		return typeContribuable;
	}

	public void setTypeContribuable(TypeContribuable theTypeContribuable) {
		typeContribuable = theTypeContribuable;
	}

	@Column(name = "QUALIFICATION", length = LengthConstants.DI_QUALIF)
	@Type(type = "ch.vd.uniregctb.hibernate.QualificationUserType")
	public Qualification getQualification() {
		return qualification;
	}

	public void setQualification(Qualification qualification) {
		this.qualification = qualification;
	}

	@Column(name = "CODE_SEGMENT")
	public Integer getCodeSegment() {
		return codeSegment;
	}

	public void setCodeSegment(Integer codeSegment) {
		this.codeSegment = codeSegment;
	}

	@Column(name = "NO_OFS_FOR_GESTION")
	public Integer getNumeroOfsForGestion() {
		return numeroOfsForGestion;
	}

	public void setNumeroOfsForGestion(Integer theNumeroOfsForGestion) {
		numeroOfsForGestion = theNumeroOfsForGestion;
	}

	@Transient
	@Override
	public ContribuableImpositionPersonnesPhysiques getTiers() {
		return (ContribuableImpositionPersonnesPhysiques) super.getTiers();
	}

	/**
	 * @return un nouveau code de contrôle d'une lettre et de cinq chiffres aléatoires
	 */
	public static String generateCodeControle() {
		return generateCodeControleUneLettreCinqChiffres();
	}
}
