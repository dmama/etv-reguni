package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.uniregctb.common.XmlUtils;

public class PaysImpl extends EntiteOFSImpl implements Pays, Serializable {

	private static final DateRange ETERNITY = new DateRangeHelper.Range(null, null);

	private static final long serialVersionUID = -2492428068671784907L;

	private final boolean valide;
	private final DateRange validityRange;
	private final boolean etatSouverain;
	private final Integer ofsEtatSouverainParent;
	private final String codeIso2;
	private final String codeIso3;

	public static PaysImpl get(ch.vd.infrastructure.model.Pays target) {
		if (target == null) {
			return null;
		}
		return new PaysImpl(target);
	}

	public static PaysImpl get(ch.vd.fidor.ws.v2.Pays target) {
		if (target == null) {
			return null;
		}
		return new PaysImpl(target);
	}

	public static Pays get(ch.vd.evd0007.v1.Country target) {
		if (target == null) {
			return null;
		}
		return new PaysImpl(target.getCountry(), target.getCountryAddOn(), target.getValidityDates());
	}

	private PaysImpl(ch.vd.infrastructure.model.Pays target) {
		super(target);
		this.valide = true; // tous les pays retournés par host-interfaces sont valides
		this.validityRange = ETERNITY;  // tous les pays retournés par host-interfaces sont valides
		this.etatSouverain = true; // cette information n'est pas disponible dans host-interface
		this.ofsEtatSouverainParent = null; // cette information n'est pas disponible dans host-interface
		this.codeIso2 = null; // cette information n'est pas disponible dans host-interface
		this.codeIso3 = null; // cette information n'est pas disponible dans host-interface
	}

	private PaysImpl(ch.vd.fidor.ws.v2.Pays target) {
		super(target.getOfsId(), target.getNomCourtFr(), target.getNomOfficielFr(), target.getIso2Id());
		this.valide = target.isValide();
		this.validityRange = ETERNITY;      // cette information n'était pas présente dans la v2 de FiDoR
		this.etatSouverain = target.isEtat() != null && target.isEtat();
		this.ofsEtatSouverainParent = target.getEtatSuperieur();
		this.codeIso2 = target.getIso2Id();
		this.codeIso3 = target.getIso3Id();
	}

	public PaysImpl(ch.ech.ech0072.v1.Country target, ch.vd.evd0007.v1.CountryAddOn addOn, ch.vd.evd0007.v1.ValidityDate validityDates) {
		super(target.getId(), target.getShortNameFr(), target.getOfficialNameFr(), target.getIso2Id());
		this.valide = target.isEntryValid();

		if (validityDates != null) {
			final RegDate dateDebutValidite = XmlUtils.xmlcal2regdate(validityDates.getAdmissionDate());
			final RegDate dateFinValidite = XmlUtils.xmlcal2regdate(validityDates.getAbolitionDate());
			if (dateDebutValidite == null && dateFinValidite == null) {
				this.validityRange = ETERNITY;
			}
			else {
				this.validityRange = new DateRangeHelper.Range(dateDebutValidite, dateFinValidite);
			}
		}
		else {
			this.validityRange = ETERNITY;
		}

		this.etatSouverain = target.isState();
		this.ofsEtatSouverainParent = target.getAreaState();
		this.codeIso2 = target.getIso2Id();
		this.codeIso3 = target.getIso3Id();
	}

	@Override
	public boolean isSuisse() {
		return getNoOFS() == ServiceInfrastructureRaw.noOfsSuisse;
	}

	@Override
	public boolean isValide() {
		return valide;
	}

	@Override
	public String getCodeIso2() {
		return codeIso2;
	}

	@Override
	public String getCodeIso3() {
		return codeIso3;
	}

	@Override
	public boolean isEtatSouverain() {
		return etatSouverain;
	}

	@Override
	public int getNoOfsEtatSouverain() {
		return ofsEtatSouverainParent != null ? ofsEtatSouverainParent : getNoOFS();
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return valide && validityRange.isValidAt(date);
	}

	@Override
	public RegDate getDateDebut() {
		return validityRange.getDateDebut();
	}

	@Override
	public RegDate getDateFin() {
		return validityRange.getDateFin();
	}
}
