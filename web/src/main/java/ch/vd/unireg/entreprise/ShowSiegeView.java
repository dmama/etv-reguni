package ch.vd.unireg.entreprise;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.interfaces.model.TypeNoOfs;
import ch.vd.unireg.tiers.DomicileHisto;
import ch.vd.unireg.tiers.Source;
import ch.vd.unireg.tiers.Sourced;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public class ShowSiegeView implements Sourced<Source>, DateRange, Annulable {

	private final Long id;
	private final boolean annule;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final int noOfsSiege;
	private final TypeNoOfs type;
	private final Source source;
	private boolean dernierElement;
	private boolean peutEditerDateFin;

	public ShowSiegeView(DomicileHisto siege) {
		this.id = siege.getId();
		this.dateDebut = siege.getDateDebut();
		this.dateFin = siege.getDateFin();
		this.noOfsSiege = siege.getNumeroOfsAutoriteFiscale();
		this.type = siege.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS ? TypeNoOfs.PAYS_HS : TypeNoOfs.COMMUNE_CH;
		this.annule = siege.isAnnule();
		this.source = siege.getSource();
		this.dernierElement = false;
		this.peutEditerDateFin = false;
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

	public int getNoOfsSiege() {
		return noOfsSiege;
	}

	public TypeNoOfs getType() {
		return type;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return !annule && DateRange.super.isValidAt(date);
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

	public boolean isPeutEditerDateFin() {
		return peutEditerDateFin;
	}

	public void setPeutEditerDateFin(boolean peutEditerDateFin) {
		this.peutEditerDateFin = peutEditerDateFin;
	}
}
