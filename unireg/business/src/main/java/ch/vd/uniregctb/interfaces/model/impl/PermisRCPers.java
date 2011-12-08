package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.evd0001.v3.ResidencePermit;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.type.TypePermis;

public class PermisRCPers implements Permis, Serializable {

	private static final long serialVersionUID = -8450922328242757605L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final RegDate dateAnnulation;
	private final TypePermis typePermis;

	public PermisRCPers(ResidencePermit permit) {
		this.dateDebut = null; // TODO (rcpers) demander à RCPers de fournir cette information
		this.dateFin = XmlUtils.xmlcal2regdate(permit.getResidencePermitTill());
		this.dateAnnulation = null; // TODO (rcpers)
		this.typePermis = TypePermis.get(permit.getResidencePermit());
	}

	public static Permis get(ResidencePermit permit) {
		if (permit == null) {
			return null;
		}
		return new PermisRCPers(permit);
	}

	@Override
	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFinValidite() {
		return dateFin;
	}

	@Override
	public RegDate getDateAnnulation() {
		return dateAnnulation;
	}

	@Override
	public TypePermis getTypePermis() {
		return typePermis;
	}
}
