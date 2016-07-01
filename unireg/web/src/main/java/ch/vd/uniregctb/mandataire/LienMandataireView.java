package ch.vd.uniregctb.mandataire;

import ch.vd.unireg.common.NomPrenom;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.iban.IbanHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.rapport.SensRapportEntreTiers;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeMandat;

public class LienMandataireView extends RapportView {

	private final TypeMandat typeMandat;
	private final String iban;
	private final String nomPrenomPersonneContact;
	private final String noTelephoneContact;
	private final Boolean withCopy;
	private final String libelleGenreImpot;

	public LienMandataireView(Mandat mandat, TiersService tiersService, AdresseService adresseService, ServiceInfrastructureService infraService) {
		super(mandat, SensRapportEntreTiers.SUJET, tiersService, adresseService);
		this.typeMandat = mandat.getTypeMandat();
		this.iban = mandat.getCoordonneesFinancieres() != null ? IbanHelper.toDisplayString(mandat.getCoordonneesFinancieres().getIban()) : null;
		this.nomPrenomPersonneContact = new NomPrenom(mandat.getNomPersonneContact(), mandat.getPrenomPersonneContact()).getNomPrenom();
		this.noTelephoneContact = mandat.getNoTelephoneContact();
		this.withCopy = mandat.getWithCopy();
		this.libelleGenreImpot = MandataireViewHelper.extractLibelleGenreImpot(mandat.getCodeGenreImpot(), infraService);
	}

	public TypeMandat getTypeMandat() {
		return typeMandat;
	}

	public String getIban() {
		return iban;
	}

	public String getNomPrenomPersonneContact() {
		return nomPrenomPersonneContact;
	}

	public String getNoTelephoneContact() {
		return noTelephoneContact;
	}

	public Boolean getWithCopy() {
		return withCopy;
	}

	public String getLibelleGenreImpot() {
		return libelleGenreImpot;
	}
}
