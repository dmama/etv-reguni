package ch.vd.unireg.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.type.TarifImpotSource;

@Entity
@DiscriminatorValue("SituationFamilleMenageCommun")
public class SituationFamilleMenageCommun extends SituationFamille {

	private TarifImpotSource tarifApplicable;
	private Long contribuablePrincipalId;

	public SituationFamilleMenageCommun() {
	}
	
	public SituationFamilleMenageCommun(SituationFamilleMenageCommun situationFamilleMenageCommun) {
		super(situationFamilleMenageCommun);
		
		this.contribuablePrincipalId = situationFamilleMenageCommun.contribuablePrincipalId;
		this.tarifApplicable = situationFamilleMenageCommun.tarifApplicable;
	}

	@Column(name = "TIERS_PRINCIPAL_ID")
	public Long getContribuablePrincipalId() {
		return contribuablePrincipalId;
	}

	public void setContribuablePrincipalId(Long theContribuablePrincipal) {
		contribuablePrincipalId = theContribuablePrincipal;
	}

	@Column(name = "TARIF_APPLICABLE", length = LengthConstants.SITUATIONFAMILLE_TARIF)
	@Type(type = "ch.vd.unireg.hibernate.TarifImpotSourceUserType")
	public TarifImpotSource getTarifApplicable() {
		return tarifApplicable;
	}

	public void setTarifApplicable(TarifImpotSource theTarifApplicable) {
		tarifApplicable = theTarifApplicable;
	}

	@Override
	public SituationFamille duplicate() {
		return new SituationFamilleMenageCommun(this);
	}
}
