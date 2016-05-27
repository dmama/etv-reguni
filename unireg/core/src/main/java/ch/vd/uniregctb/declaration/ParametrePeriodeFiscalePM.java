package ch.vd.uniregctb.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeContribuable;

@Entity
@DiscriminatorValue(value = "PM")
public class ParametrePeriodeFiscalePM extends ParametrePeriodeFiscaleDI {

	/**
	 * Date de référence à prende en compte pour le délai initial
	 */
	public enum ReferencePourDelai {

		/**
		 * La fin de la période d'imposition correspondant à la déclaration émise
		 */
		FIN_PERIODE,

		/**
		 * La date d'émission de la déclaration
		 */
		EMISSION
	}

	private int delaiImprimeMois;           // en mois
	private boolean delaiImprimeRepousseFinDeMois;
	private ReferencePourDelai referenceDelaiInitial;
	private int delaiToleranceJoursEffective;                // en jours
	private boolean delaiTolereRepousseFinDeMois;

	// nécessaire pour hibernate
	protected ParametrePeriodeFiscalePM() {
	}

	public ParametrePeriodeFiscalePM(TypeContribuable typeContribuable,
	                                 int delaiImprimeMois, boolean delaiImprimeRepousseFinDeMois,
	                                 int delaiToleranceJoursEffective, boolean delaiTolereRepousseFinDeMois,
	                                 ReferencePourDelai referenceDelaiInitial,
	                                 PeriodeFiscale periodefiscale) {
		super(periodefiscale, typeContribuable);
		this.delaiImprimeMois = delaiImprimeMois;
		this.delaiImprimeRepousseFinDeMois = delaiImprimeRepousseFinDeMois;
		this.delaiToleranceJoursEffective = delaiToleranceJoursEffective;
		this.delaiTolereRepousseFinDeMois = delaiTolereRepousseFinDeMois;
		this.referenceDelaiInitial = referenceDelaiInitial;
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
	public int getDelaiImprimeMois() {
		return delaiImprimeMois;
	}

	public void setDelaiImprimeMois(int delaiImprimeMois) {
		this.delaiImprimeMois = delaiImprimeMois;
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

	@Column(name = "PM_REF_DELAI", length = LengthConstants.PARAMETRE_PF_REF_DELAI)
	@Enumerated(value = EnumType.STRING)
	public ReferencePourDelai getReferenceDelaiInitial() {
		return referenceDelaiInitial;
	}

	public void setReferenceDelaiInitial(ReferencePourDelai referenceDelaiInitial) {
		this.referenceDelaiInitial = referenceDelaiInitial;
	}
}