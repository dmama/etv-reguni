package ch.vd.uniregctb.mandataire;

import ch.vd.uniregctb.iban.IbanHelper;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.TiersService;

public class MandatairePerceptionView extends MandataireView {

	private final long idMandataire;
	private final String iban;

	public MandatairePerceptionView(Mandat mandat, TiersService tiersService) {
		super(mandat.getId(), mandat, mandat.getTypeMandat(), getNomRaisonSociale(mandat.getObjetId(), tiersService));
		this.idMandataire = mandat.getObjetId();
		this.iban = IbanHelper.normalize(mandat.getCoordonneesFinancieres().getIban());
	}

	public long getIdMandataire() {
		return idMandataire;
	}

	public String getIban() {
		return IbanHelper.toDisplayString(iban);
	}
}
