package ch.vd.uniregctb.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;

@Entity
@DiscriminatorValue(value = "SNC")
public class ParametrePeriodeFiscaleSNC extends ParametrePeriodeFiscale {

	private RegDate termeGeneralRappelImprime;
	private RegDate termeGeneralRappelEffectif;

	// nécessaire pour Hibernate
	protected ParametrePeriodeFiscaleSNC() {
	}

	public ParametrePeriodeFiscaleSNC(ParametrePeriodeFiscaleSNC right) {
		super(right);
		this.termeGeneralRappelEffectif = right.termeGeneralRappelEffectif;
		this.termeGeneralRappelImprime = right.termeGeneralRappelImprime;
	}

	public ParametrePeriodeFiscaleSNC(PeriodeFiscale periodefiscale, RegDate dateRappelImprime, RegDate dateRappelEffectif) {
		super(periodefiscale);
		this.termeGeneralRappelImprime = dateRappelImprime;
		this.termeGeneralRappelEffectif = dateRappelEffectif;
	}

	@Column(name = "SNC_RAPPEL_IMPRIME")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getTermeGeneralRappelImprime() {
		return termeGeneralRappelImprime;
	}

	public void setTermeGeneralRappelImprime(RegDate theTermeGeneralSommationReglementaire) {
		termeGeneralRappelImprime = theTermeGeneralSommationReglementaire;
	}

	@Column(name = "SNC_RAPPEL_EFFECTIF")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getTermeGeneralRappelEffectif() {
		return termeGeneralRappelEffectif;
	}

	public void setTermeGeneralRappelEffectif(RegDate theTermeGeneralSommationEffectif) {
		termeGeneralRappelEffectif = theTermeGeneralSommationEffectif;
	}

}