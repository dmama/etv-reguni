package ch.vd.uniregctb.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import ch.vd.uniregctb.type.TypeContribuable;

@Entity
@DiscriminatorValue(value = "PM")
public class ParametrePeriodeFiscalePM extends ParametrePeriodeFiscale {

	private int delaiImprimeMoisDepuisBouclement;           // en mois
	private boolean delaiImprimeRepousseFinDeMois;
	private int delaiToleranceJoursEffective;                // en jours
	private boolean delaiTolereRepousseFinDeMois;

	// nécessaire pour hibernate
	protected ParametrePeriodeFiscalePM() {
	}

	public ParametrePeriodeFiscalePM(TypeContribuable typeContribuable,
	                                 int delaiImprimeMoisDepuisBouclement, boolean delaiImprimeRepousseFinDeMois,
	                                 int delaiToleranceJoursEffective, boolean delaiTolereRepousseFinDeMois,
	                                 PeriodeFiscale periodefiscale) {
		super(periodefiscale, typeContribuable);
		this.delaiImprimeMoisDepuisBouclement = delaiImprimeMoisDepuisBouclement;
		this.delaiImprimeRepousseFinDeMois = delaiImprimeRepousseFinDeMois;
		this.delaiToleranceJoursEffective = delaiToleranceJoursEffective;
		this.delaiTolereRepousseFinDeMois = delaiTolereRepousseFinDeMois;
		checkTypeContribuable(typeContribuable);
	}

	private static void checkTypeContribuable(TypeContribuable typeContribuable) {
		if (typeContribuable != null && !typeContribuable.isUsedForPM()) {
			throw new IllegalArgumentException("Le type de contribuable " + typeContribuable + " n'est pas utilisable pour les PM.");
		}
	}

	@Override
	public void setTypeContribuable(TypeContribuable typeContribuable) {
		checkTypeContribuable(typeContribuable);
		super.setTypeContribuable(typeContribuable);
	}

	@Column(name = "PM_DELAI_IMPRIME_MOIS")
	public int getDelaiImprimeMoisDepuisBouclement() {
		return delaiImprimeMoisDepuisBouclement;
	}

	public void setDelaiImprimeMoisDepuisBouclement(int delaiImprimeMoisDepuisBouclement) {
		this.delaiImprimeMoisDepuisBouclement = delaiImprimeMoisDepuisBouclement;
	}

	@Column(name = "PM_DELAI_IMPRIME_FIN_MOIS")
	public boolean isDelaiImprimeRepousseFinDeMois() {
		return delaiImprimeRepousseFinDeMois;
	}

	public void setDelaiImprimeRepousseFinDeMois(boolean delaiImprimeRepousseFinDeMois) {
		this.delaiImprimeRepousseFinDeMois = delaiImprimeRepousseFinDeMois;
	}

	@Column(name = "PM_TOLERANCE_JOURS")
	public int getDelaiToleranceJoursEffective() {
		return delaiToleranceJoursEffective;
	}

	public void setDelaiToleranceJoursEffective(int delaiToleranceJoursEffective) {
		this.delaiToleranceJoursEffective = delaiToleranceJoursEffective;
	}

	@Column(name = "PM_TOLERANCE_FIN_MOIS")
	public boolean isDelaiTolereRepousseFinDeMois() {
		return delaiTolereRepousseFinDeMois;
	}

	public void setDelaiTolereRepousseFinDeMois(boolean delaiTolereRepousseFinDeMois) {
		this.delaiTolereRepousseFinDeMois = delaiTolereRepousseFinDeMois;
	}
}