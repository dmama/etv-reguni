package ch.vd.unireg.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.ComparisonHelper;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

@Entity
public abstract class ForFiscalRevenuFortune extends ForFiscalAvecMotifs {

	private MotifRattachement motifRattachement;

	public ForFiscalRevenuFortune() {
		setGenreImpot(GenreImpot.REVENU_FORTUNE);
	}

	public ForFiscalRevenuFortune(RegDate ouverture, MotifFor motifOuverture, RegDate fermeture, MotifFor motifFermeture,
	                              Integer numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, MotifRattachement motifRattachement) {
		super(ouverture, motifOuverture, fermeture, motifFermeture, GenreImpot.REVENU_FORTUNE, numeroOfsAutoriteFiscale, typeAutoriteFiscale);
		setMotifRattachement(motifRattachement); // virtual
	}

	public ForFiscalRevenuFortune(ForFiscalRevenuFortune ffrf) {
		super(ffrf);
		setMotifRattachement(ffrf.getMotifRattachement()); // virtual
	}

	@Column(name = "MOTIF_RATTACHEMENT", length = LengthConstants.FOR_RATTACHEMENT)
	@Type(type = "ch.vd.unireg.hibernate.MotifRattachementUserType")
	public MotifRattachement getMotifRattachement() {
		return motifRattachement;
	}

	public void setMotifRattachement(MotifRattachement theMotifRattachement) {
		motifRattachement = theMotifRattachement;
	}


	@Override
	protected void dumpForDebug(int nbTabs) {
		super.dumpForDebug(nbTabs);
		ddump(nbTabs, "Motif rattach: "+motifRattachement);
	}

	/* (non-Javadoc)
	 * @see ch.vd.unireg.tiers.ForFiscal#equalsTo(java.lang.Object)
	 */
	@Override
	public boolean equalsTo(ForFiscal obj) {
		if (this == obj)
			return true;
		if (!super.equalsTo(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;

		final ForFiscalRevenuFortune other = (ForFiscalRevenuFortune) obj;
		return ComparisonHelper.areEqual(motifRattachement, other.motifRattachement);
	}
}
