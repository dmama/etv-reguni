package ch.vd.unireg.entreprise;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.tiers.RaisonSocialeHisto;
import ch.vd.unireg.tiers.Source;
import ch.vd.unireg.tiers.Sourced;

public class ShowRaisonSocialeView implements Sourced<Source>, Annulable, DateRange {

	private Long id;
	private RegDate dateDebut;
	private RegDate dateFin;
	private String raisonSociale;
	private Source source;
	private boolean annule;
	private boolean dernierElement;

	public ShowRaisonSocialeView() {}

	public ShowRaisonSocialeView(RaisonSocialeHisto nom, boolean dernierElement) {
		this(nom.getId(), nom.isAnnule(), nom.getDateDebut(), nom.getDateFin(), nom.getRaisonSociale(), nom.getSource(), dernierElement);
	}

	public ShowRaisonSocialeView(Long id, boolean annule, RegDate dateDebut, RegDate dateFin, String raisonSociale, Source source, boolean dernierElement) {
		this.id = id;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.raisonSociale = raisonSociale;
		this.source = source;
		this.annule = annule;
		this.dernierElement = dernierElement;
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
		return !isAnnule() && DateRange.super.isValidAt(date);
	}

	private static <T> boolean isSameValue(T one, T two) {
		return one == two || (one != null && one.equals(two));
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	@Override
	public Source getSource() {
		return source;
	}

	public boolean isDernierElement() {
		return dernierElement;
	}

	public void setDernierElement(boolean dernierElement) {
		this.dernierElement = dernierElement;
	}
}
