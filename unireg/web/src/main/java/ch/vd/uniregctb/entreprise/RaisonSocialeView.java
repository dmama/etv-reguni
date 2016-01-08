package ch.vd.uniregctb.entreprise;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.tiers.RaisonSocialeHisto;
import ch.vd.uniregctb.tiers.Source;
import ch.vd.uniregctb.tiers.Sourced;

public class RaisonSocialeView implements Sourced<Source>, Annulable, CollatableDateRange {

	private final Long id;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final String raisonSociale;
	private final Source source;
	private final boolean annule;

	public RaisonSocialeView(RaisonSocialeHisto nom) {
		this(nom.getId(), nom.isAnnule(), nom.getDateDebut(), nom.getDateFin(), nom.getRaisonSociale(), nom.getSource());
	}

	private RaisonSocialeView(Long id, boolean annule, RegDate dateDebut, RegDate dateFin, String raisonSociale, Source source) {
		this.id = id;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.raisonSociale = raisonSociale;
		this.source = source;
		this.annule = annule;
	}

	public Long getId() {
		return id;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public String getRaisonSociale() {
		return raisonSociale;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	public boolean isCollatable(DateRange next) {
		if (next instanceof RaisonSocialeView) {
			final RaisonSocialeView nextFormeJuridique = (RaisonSocialeView) next;
			return DateRangeHelper.isCollatable(this, next)
					&& isSameValue(nextFormeJuridique.raisonSociale, raisonSociale)
					&& nextFormeJuridique.source == source
					&& nextFormeJuridique.annule == annule
					&& isSameValue(nextFormeJuridique.id, id);
		}
		return false;
	}

	private static <T> boolean isSameValue(T one, T two) {
		return one == two || (one != null && two != null && one.equals(two));
	}

	@Override
	public RaisonSocialeView collate(DateRange next) {
		if (!isCollatable(next)) {
			throw new IllegalArgumentException("Ranges non collatables!");
		}
		return new RaisonSocialeView(id, annule, dateDebut, next.getDateFin(), raisonSociale, source);
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	@Override
	public Source getSource() {
		return source;
	}
}
