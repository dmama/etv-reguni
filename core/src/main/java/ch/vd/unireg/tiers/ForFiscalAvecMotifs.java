package ch.vd.unireg.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.ComparisonHelper;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAutoriteFiscale;

@Entity
public abstract class ForFiscalAvecMotifs extends ForFiscal {

	private MotifFor motifOuverture;
	private MotifFor motifFermeture;

	protected ForFiscalAvecMotifs() {
	}

	protected ForFiscalAvecMotifs(RegDate ouverture, MotifFor motifOuverture, RegDate fermeture, MotifFor motifFermeture,
	                              GenreImpot genreImpot, Integer numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale) {
		super(ouverture, fermeture, genreImpot, numeroOfsAutoriteFiscale, typeAutoriteFiscale);
		this.motifOuverture = motifOuverture;
		this.motifFermeture = motifFermeture;
	}

	protected ForFiscalAvecMotifs(ForFiscalAvecMotifs ff) {
		super(ff);
		this.motifOuverture = ff.motifOuverture;
		this.motifFermeture = ff.motifFermeture;
	}

	@Column(name = "MOTIF_OUVERTURE", length = LengthConstants.FOR_MOTIF)
	@Type(type = "ch.vd.unireg.hibernate.MotifForUserType")
	public MotifFor getMotifOuverture() {
		return motifOuverture;
	}

	public void setMotifOuverture(MotifFor theMotifOuverture) {
		motifOuverture = theMotifOuverture;
	}

	@Column(name = "MOTIF_FERMETURE", length = LengthConstants.FOR_MOTIF)
	@Type(type = "ch.vd.unireg.hibernate.MotifForUserType")
	public MotifFor getMotifFermeture() {
		return motifFermeture;
	}

	public void setMotifFermeture(@Nullable MotifFor theMotifFermeture) {
		motifFermeture = theMotifFermeture;
	}

	@Override
	protected void dumpForDebug(int nbTabs) {
		super.dumpForDebug(nbTabs);
		ddump(nbTabs, "Motif ouv: "+motifOuverture);
		ddump(nbTabs, "Motif ferm: " + motifFermeture);
	}

	@Override
	public boolean equalsTo(ForFiscal obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equalsTo(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		final ForFiscalAvecMotifs other = (ForFiscalAvecMotifs) obj;
		return ComparisonHelper.areEqual(motifFermeture, other.motifFermeture)
				&& ComparisonHelper.areEqual(motifOuverture, other.motifOuverture);
	}
}
