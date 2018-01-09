package ch.vd.uniregctb.registrefoncier;

import java.util.Date;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.tiers.Contribuable;

/**
 * Informations sur un principal de communauté valable pendant une période donnée.
 */
public class CommunauteRFPrincipalInfo implements CollatableDateRange<CommunauteRFPrincipalInfo>, Annulable {

	/**
	 * Id technique du principal
	 */
	@Nullable
	private Long id;

	/**
	 * Id technique de l'ayant-droit
	 */
	@Nullable
	private Long ayantDroitId;

	/**
	 * Vrai si le principal est par défaut, c'est-à-dire qu'il n'a pas été explicitement élu.
	 */
	private boolean parDefaut;
	private RegDate dateDebut;
	private RegDate dateFin;
	@Nullable
	private Date annulationDate;
	private long ctbId;

	public CommunauteRFPrincipalInfo(@Nullable Long id, @Nullable Long ayantDroitId, RegDate dateDebut, RegDate dateFin, @Nullable Date annulationDate, long ctbId, boolean parDefaut) {
		this.id = id;
		this.ayantDroitId = ayantDroitId;
		this.annulationDate = annulationDate;
		this.parDefaut = parDefaut;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.ctbId = ctbId;
	}

	@Nullable
	public static CommunauteRFPrincipalInfo get(@NotNull PrincipalCommunauteRF right) {
		final TiersRF principal = right.getPrincipal();
		final Contribuable ctb = principal.getCtbRapproche();
		if (ctb == null) {
			return null;
		}
		return new CommunauteRFPrincipalInfo(right.getId(), principal.getId(), right.getDateDebut(), right.getDateFin(), right.getAnnulationDate(), ctb.getNumero(), false);
	}

	@Nullable
	public Long getId() {
		return id;
	}

	@Nullable
	public Long getAyantDroitId() {
		return ayantDroitId;
	}

	public boolean isParDefaut() {
		return parDefaut;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	@Nullable
	public Date getAnnulationDate() {
		return annulationDate;
	}

	public long getCtbId() {
		return ctbId;
	}

	@Override
	public boolean isAnnule() {
		return annulationDate != null;
	}

	@Override
	public boolean isCollatable(CommunauteRFPrincipalInfo next) {
		return this.ctbId == next.ctbId && this.isAnnule() == next.isAnnule() && DateRangeHelper.isCollatable(this, next);
	}

	@Override
	public CommunauteRFPrincipalInfo collate(CommunauteRFPrincipalInfo next) {
		Assert.isTrue(isCollatable(next));
		final Long id = (this.id == null ? next.id : this.id);
		final Long ayantDroitId = (this.ayantDroitId == null ? next.ayantDroitId : this.ayantDroitId);
		final boolean parDefaut = this.parDefaut && next.parDefaut;
		return new CommunauteRFPrincipalInfo(id, ayantDroitId, this.dateDebut, next.dateFin, this.annulationDate, this.ctbId, parDefaut);
	}

	@NotNull
	public static CommunauteRFPrincipalInfo adapter(CommunauteRFPrincipalInfo range, RegDate debut, RegDate fin) {
		final RegDate dateDebut = (debut == null ? range.getDateDebut() : debut);
		final RegDate dateFin = (fin == null ? range.getDateFin() : fin);
		return new CommunauteRFPrincipalInfo(range.getId(), range.getAyantDroitId(), dateDebut, dateFin, range.getAnnulationDate(), range.getCtbId(), range.isParDefaut());
	}
}
