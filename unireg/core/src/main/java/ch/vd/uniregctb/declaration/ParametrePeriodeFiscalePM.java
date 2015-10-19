package ch.vd.uniregctb.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import ch.vd.uniregctb.type.TypeContribuable;

@Entity
@DiscriminatorValue(value = "PM")
public class ParametrePeriodeFiscalePM extends ParametrePeriodeFiscale {

	private int delaiImprimeDepuisBouclement;
	private int delaiImprimeAvecMandataireDepuisBouclement;
	private int delaiEffectifDepuisBouclement;
	private int delaiEffectifAvecMandataireDepuisBouclement;

	// nécessaire pour hibernate
	protected ParametrePeriodeFiscalePM() {
	}

	public ParametrePeriodeFiscalePM(TypeContribuable typeContribuable, int delaiImprimeDepuisBouclement, int delaiImprimeAvecMandataireDepuisBouclement, int delaiEffectifDepuisBouclement,
	                                 int delaiEffectifAvecMandataireDepuisBouclement, PeriodeFiscale periodefiscale) {
		super(periodefiscale, typeContribuable);
		this.delaiImprimeDepuisBouclement = delaiImprimeDepuisBouclement;
		this.delaiImprimeAvecMandataireDepuisBouclement = delaiImprimeAvecMandataireDepuisBouclement;
		this.delaiEffectifDepuisBouclement = delaiEffectifDepuisBouclement;
		this.delaiEffectifAvecMandataireDepuisBouclement = delaiEffectifAvecMandataireDepuisBouclement;
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

	@Column(name = "PM_DELAI_IMPRIME")
	public int getDelaiImprimeDepuisBouclement() {
		return delaiImprimeDepuisBouclement;
	}

	public void setDelaiImprimeDepuisBouclement(int delaiImprimeDepuisBouclement) {
		this.delaiImprimeDepuisBouclement = delaiImprimeDepuisBouclement;
	}

	@Column(name = "PM_DELAI_IMPRIME_MANDATAIRE")
	public int getDelaiImprimeAvecMandataireDepuisBouclement() {
		return delaiImprimeAvecMandataireDepuisBouclement;
	}

	public void setDelaiImprimeAvecMandataireDepuisBouclement(int delaiImprimeAvecMandataireDepuisBouclement) {
		this.delaiImprimeAvecMandataireDepuisBouclement = delaiImprimeAvecMandataireDepuisBouclement;
	}

	@Column(name = "PM_DELAI_EFF")
	public int getDelaiEffectifDepuisBouclement() {
		return delaiEffectifDepuisBouclement;
	}

	public void setDelaiEffectifDepuisBouclement(int delaiEffectifDepuisBouclement) {
		this.delaiEffectifDepuisBouclement = delaiEffectifDepuisBouclement;
	}

	@Column(name = "PM_DELAI_EFF_MANDATAIRE")
	public int getDelaiEffectifAvecMandataireDepuisBouclement() {
		return delaiEffectifAvecMandataireDepuisBouclement;
	}

	public void setDelaiEffectifAvecMandataireDepuisBouclement(int delaiEffectifAvecMandataireDepuisBouclement) {
		this.delaiEffectifAvecMandataireDepuisBouclement = delaiEffectifAvecMandataireDepuisBouclement;
	}
}