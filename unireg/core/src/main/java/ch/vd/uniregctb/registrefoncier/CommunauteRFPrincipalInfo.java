package ch.vd.uniregctb.registrefoncier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.tiers.Contribuable;

/**
 * Informations sur un principal de communauté valable pendant une période donnée.
 */
public class CommunauteRFPrincipalInfo implements CollatableDateRange<CommunauteRFPrincipalInfo> {
	private RegDate dateDebut;
	private RegDate dateFin;
	private long ctbId;

	public CommunauteRFPrincipalInfo(RegDate dateDebut, RegDate dateFin, long ctbId) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.ctbId = ctbId;
	}

	@Nullable
	public static CommunauteRFPrincipalInfo get(@NotNull PrincipalCommunauteRF right) {
		final Contribuable ctb = right.getPrincipal().getCtbRapproche();
		if (ctb == null) {
			return null;
		}
		return new CommunauteRFPrincipalInfo(right.getDateDebut(), right.getDateFin(), ctb.getNumero());
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public long getCtbId() {
		return ctbId;
	}

	@Override
	public boolean isCollatable(CommunauteRFPrincipalInfo next) {
		return this.ctbId == next.ctbId && DateRangeHelper.isCollatable(this, next);
	}

	@Override
	public CommunauteRFPrincipalInfo collate(CommunauteRFPrincipalInfo next) {
		Assert.isTrue(isCollatable(next));
		return new CommunauteRFPrincipalInfo(this.dateDebut, next.dateFin, this.ctbId);
	}

	@NotNull
	public static CommunauteRFPrincipalInfo adapter(CommunauteRFPrincipalInfo range, RegDate debut, RegDate fin) {
		final RegDate dateDebut = (debut == null ? range.getDateDebut() : debut);
		final RegDate dateFin = (fin == null ? range.getDateFin() : fin);
		return new CommunauteRFPrincipalInfo(dateDebut, dateFin, range.getCtbId());
	}
}
