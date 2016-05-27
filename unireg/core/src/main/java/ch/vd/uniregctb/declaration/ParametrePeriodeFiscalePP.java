package ch.vd.uniregctb.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.type.TypeContribuable;

@Entity
@DiscriminatorValue(value = "PP")
public class ParametrePeriodeFiscalePP extends ParametrePeriodeFiscaleDI implements Duplicable<ParametrePeriodeFiscalePP> {

	private RegDate dateFinEnvoiMasseDI;
	private RegDate termeGeneralSommationReglementaire;
	private RegDate termeGeneralSommationEffectif;

	// nécessaire pour hibernate
	public ParametrePeriodeFiscalePP() {
	}

	public ParametrePeriodeFiscalePP(ParametrePeriodeFiscalePP right) {
		super(right);
		this.dateFinEnvoiMasseDI = right.dateFinEnvoiMasseDI;
		this.termeGeneralSommationReglementaire = right.termeGeneralSommationReglementaire;
		this.termeGeneralSommationEffectif = right.termeGeneralSommationEffectif;
	}

	public ParametrePeriodeFiscalePP(TypeContribuable typeContribuable, RegDate dateFinEnvoiMasseDI, RegDate termeGeneralSommationReglementaire, RegDate termeGeneralSommationEffectif, PeriodeFiscale periodefiscale) {
		super(periodefiscale, typeContribuable);
		this.dateFinEnvoiMasseDI = dateFinEnvoiMasseDI;
		this.termeGeneralSommationReglementaire = termeGeneralSommationReglementaire;
		this.termeGeneralSommationEffectif = termeGeneralSommationEffectif;
		checkTypeContribuable(typeContribuable);
	}

	private static void checkTypeContribuable(TypeContribuable typeContribuable) {
		if (typeContribuable != null && !typeContribuable.isUsedForPP()) {
			throw new IllegalArgumentException("Le type de contribuable " + typeContribuable + " n'est pas utilisable pour les PP.");
		}
	}

	@Override
	public void setTypeContribuable(TypeContribuable typeContribuable) {
		checkTypeContribuable(typeContribuable);
		super.setTypeContribuable(typeContribuable);
	}

	@Column(name = "DATE_FIN_ENVOI_MASSE")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFinEnvoiMasseDI() {
		return dateFinEnvoiMasseDI;
	}

	public void setDateFinEnvoiMasseDI(RegDate theDateFinEnvoiMasseDI) {
		dateFinEnvoiMasseDI = theDateFinEnvoiMasseDI;
	}

	@Column(name = "TERME_GEN_SOMM_REGL")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getTermeGeneralSommationReglementaire() {
		return termeGeneralSommationReglementaire;
	}

	public void setTermeGeneralSommationReglementaire(RegDate theTermeGeneralSommationReglementaire) {
		termeGeneralSommationReglementaire = theTermeGeneralSommationReglementaire;
	}

	@Column(name = "TERME_GEN_SOMM_EFFECT")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getTermeGeneralSommationEffectif() {
		return termeGeneralSommationEffectif;
	}

	public void setTermeGeneralSommationEffectif(RegDate theTermeGeneralSommationEffectif) {
		termeGeneralSommationEffectif = theTermeGeneralSommationEffectif;
	}

	@Override
	public ParametrePeriodeFiscalePP duplicate() {
		return new ParametrePeriodeFiscalePP(this);
	}
}