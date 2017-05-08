package ch.vd.uniregctb.mandataire;

import ch.vd.uniregctb.adresse.AdresseMandataire;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.TiersService;

public class MandataireCourrierView extends MandataireView {

	private final Long idMandataire;            // seulement dans le cas du lien mandataire (en opposition à l'adresse mandataire simple)
	private final boolean withCopy;
	private final String libelleGenreImpot;     // seulement dans le cas du mandat spécial
	private final String codeGenreImpot;        // seulement dans le cas du mandat spécial

	public MandataireCourrierView(Mandat mandat, TiersService tiersService, ServiceInfrastructureService infraService) {
		super(mandat.getId(), mandat, mandat.getTypeMandat(), getNomRaisonSociale(mandat.getObjetId(), tiersService));
		this.idMandataire = mandat.getObjetId();
		this.withCopy = mandat.getWithCopy() != null && mandat.getWithCopy();
		this.libelleGenreImpot = mandat.getCodeGenreImpot() != null ? MandataireViewHelper.extractLibelleGenreImpot(mandat.getCodeGenreImpot(), infraService) : null;
		this.codeGenreImpot = mandat.getCodeGenreImpot();
	}

	public MandataireCourrierView(AdresseMandataire adresse, ServiceInfrastructureService infraService) {
		super(adresse.getId(), adresse, adresse.getTypeMandat(), adresse.getNomDestinataire());
		this.idMandataire = null;
		this.withCopy = adresse.isWithCopy();
		this.libelleGenreImpot = adresse.getCodeGenreImpot() != null ? MandataireViewHelper.extractLibelleGenreImpot(adresse.getCodeGenreImpot(), infraService) : null;
		this.codeGenreImpot = adresse.getCodeGenreImpot();
	}

	public Long getIdMandataire() {
		return idMandataire;
	}

	public boolean isWithCopy() {
		return withCopy;
	}

	public String getLibelleGenreImpot() {
		return libelleGenreImpot;
	}

	public String getCodeGenreImpot() {
		return codeGenreImpot;
	}
}
