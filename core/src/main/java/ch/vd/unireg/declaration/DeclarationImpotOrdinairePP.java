package ch.vd.unireg.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.type.Qualification;

@Entity
@DiscriminatorValue("DI")
public class DeclarationImpotOrdinairePP extends DeclarationImpotOrdinaire {

	/**
	 * [SIFISC-1368] première année où le retour par courrier électronique des déclarations d'impôt est possible.
	 */
	public static final int PREMIERE_ANNEE_RETOUR_ELECTRONIQUE = 2011;

	private Integer numeroOfsForGestion;

	private Qualification qualification;

	@Column(name = "QUALIFICATION", length = LengthConstants.DI_QUALIF)
	@Type(type = "ch.vd.unireg.hibernate.QualificationUserType")
	public Qualification getQualification() {
		return qualification;
	}

	public void setQualification(Qualification qualification) {
		this.qualification = qualification;
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

}
