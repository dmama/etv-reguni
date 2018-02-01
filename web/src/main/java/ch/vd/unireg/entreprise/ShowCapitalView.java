package ch.vd.unireg.entreprise;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.common.MontantMonetaireView;
import ch.vd.unireg.tiers.CapitalHisto;
import ch.vd.unireg.tiers.MontantMonetaire;
import ch.vd.unireg.tiers.Source;
import ch.vd.unireg.tiers.Sourced;

public class ShowCapitalView implements Sourced<Source>, Annulable, CollatableDateRange<ShowCapitalView> {

	private final Long id;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final MontantMonetaireView capitalLibere;
	private final Source source;
	private final boolean annule;
	private boolean dernierElement;

	public ShowCapitalView(CapitalHisto capital) {
		this(capital.getId(), capital.isAnnule(), capital.getDateDebut(), capital.getDateFin(), capital.getMontant(), capital.getSource());
	}

	private ShowCapitalView(Long id, boolean annule, RegDate dateDebut, RegDate dateFin, MontantMonetaire capitalLibere, Source source) {
		this(id, annule, dateDebut, dateFin, buildMontantMonetaire(capitalLibere), source);
	}

	private ShowCapitalView(Long id, boolean annule, RegDate dateDebut, RegDate dateFin, MontantMonetaireView capitalLibere, Source source) {
		this.id = id;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.capitalLibere = capitalLibere;
		this.source = source;
		this.annule = annule;
		this.dernierElement = false;
	}

	@Nullable
	private static MontantMonetaireView buildMontantMonetaire(MontantMonetaire mm) {
		if (mm == null) {
			return null;
		}
		return new MontantMonetaireView(mm);
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

	public MontantMonetaireView getCapitalLibere() {
		return capitalLibere;
	}

	public Source getSource() {
		return source;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && CollatableDateRange.super.isValidAt(date);
	}

	@Override
	public boolean isCollatable(ShowCapitalView next) {
		return DateRangeHelper.isCollatable(this, next)
				&& isSameValue(id, next.id)
				&& isSameValue(capitalLibere, next.capitalLibere)
				&& annule == next.annule
				&& source == next.source;
	}

	private static <T> boolean isSameValue(T one, T two) {
		return one == two || (one != null && two != null && one.equals(two));
	}

	@Override
	public ShowCapitalView collate(ShowCapitalView next) {
		if (!isCollatable(next)) {
			throw new IllegalArgumentException("Ranges are not collatable!");
		}
		return new ShowCapitalView(id, annule, dateDebut, next.getDateFin(), capitalLibere, source);
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public boolean isDernierElement() {
		return dernierElement;
	}

	public void setDernierElement(boolean dernierElement) {
		this.dernierElement = dernierElement;
	}
}
