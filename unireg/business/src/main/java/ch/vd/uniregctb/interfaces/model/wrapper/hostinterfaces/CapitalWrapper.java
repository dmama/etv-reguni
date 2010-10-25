package ch.vd.uniregctb.interfaces.model.wrapper.hostinterfaces;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Capital;
import ch.vd.uniregctb.interfaces.model.EditionFosc;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class CapitalWrapper implements Capital, Serializable {

	private static final long serialVersionUID = 4245185162092655371L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final Long capitalAction;
	private final Long capitalLibere;
	private final EditionFosc editionFosc;

	public static CapitalWrapper get(ch.vd.registre.pm.model.Capital target) {
		if (target == null) {
			return null;
		}
		return new CapitalWrapper(target);
	}

	public CapitalWrapper(ch.vd.registre.pm.model.Capital target) {
		this.dateDebut = RegDate.get(target.getDateDebut());
		this.dateFin = RegDate.get(target.getDateFin());
		this.capitalAction = target.getCapitalAction();
		this.capitalLibere = target.getCapitalLibere();
		this.editionFosc = EditionFoscWrapper.get(target.getEditionFosc());
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public Long getCapitalAction() {
		return capitalAction;
	}

	public Long getCapitalLibere() {
		return capitalLibere;
	}

	public EditionFosc getEditionFosc() {
		return editionFosc;
	}
}
