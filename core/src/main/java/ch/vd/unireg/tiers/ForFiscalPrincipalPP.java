package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ComparisonHelper;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

@Entity
@DiscriminatorValue("ForFiscalPrincipal")           // TODO [SIPM] il faudra à terme modifier la valeur du discriminant, non ?
public class ForFiscalPrincipalPP extends ForFiscalPrincipal {

	/**
	 * Rôle ordinaire (ICC/IFD)
	 * Imposé d'après la dépense (ICCD/IFDD)
	 * IS seulement
	 * IS mixte selon loi
	 * IS mixte selon pratique
	 * Taxé à zéro (indigent)
	 */
	private ModeImposition modeImposition;

	public ForFiscalPrincipalPP() {
	}

	public ForFiscalPrincipalPP(RegDate ouverture, MotifFor motifOuverture, RegDate fermeture, MotifFor motifFermeture, Integer numeroOfsAutoriteFiscale,
	                          TypeAutoriteFiscale typeAutoriteFiscale, MotifRattachement motifRattachement, ModeImposition modeImposition) {
		super(ouverture, motifOuverture, fermeture, motifFermeture, numeroOfsAutoriteFiscale, typeAutoriteFiscale, motifRattachement);
		this.modeImposition = modeImposition;
	}

	public ForFiscalPrincipalPP(ForFiscalPrincipalPP ffp) {
		super(ffp);
		this.modeImposition = ffp.getModeImposition();
	}

	@Column(name = "MODE_IMPOSITION", length = LengthConstants.FOR_IMPOSITION)
	@Type(type = "ch.vd.uniregctb.hibernate.ModeImpositionUserType")
	public ModeImposition getModeImposition() {
		return modeImposition;
	}

	public void setModeImposition(@Nullable ModeImposition theModeImposition) {
		modeImposition = theModeImposition;
	}

	@Override
	protected void dumpForDebug(int nbTabs) {
		super.dumpForDebug(nbTabs);
		ddump(nbTabs, "Mode imposition: " + modeImposition);
	}

	/*
		 * (non-Javadoc)
		 *
		 * @see ch.vd.uniregctb.tiers.ForFiscalRevenuFortune#equalsTo(java.lang.Object)
		 */
	@Override
	public boolean equalsTo(ForFiscal obj) {
		if (this == obj)
			return true;
		if (!super.equalsTo(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;

		final ForFiscalPrincipalPP other = (ForFiscalPrincipalPP) obj;
		return ComparisonHelper.areEqual(modeImposition, other.modeImposition);
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.common.Duplicable#duplicate()
	 */
	@Override
	public ForFiscal duplicate() {
		return new ForFiscalPrincipalPP(this);
	}

	@Transient
	@Override
	public ContribuableImpositionPersonnesPhysiques getTiers() {
		return (ContribuableImpositionPersonnesPhysiques) super.getTiers();
	}
}
