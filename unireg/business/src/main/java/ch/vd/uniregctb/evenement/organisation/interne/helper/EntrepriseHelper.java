package ch.vd.uniregctb.evenement.organisation.interne.helper;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.type.TypeRegimeFiscal;

/**
 * @author Raphaël Marmier, 2015-09-28
 */
public class EntrepriseHelper {

	@NotNull
	public static DomicileEtablissement createDomicileEtablissement(Etablissement etablissement, Siege siege, RegDate date, EvenementOrganisationContext context) {
		final DomicileEtablissement domicile = new DomicileEtablissement(date , null, siege.getTypeAutoriteFiscale(), siege.getNoOfs(), etablissement);
		return context.getTiersDAO().addAndSave(etablissement, domicile);
	}

	@NotNull
	public static Etablissement createEtablissement(SiteOrganisation site, boolean principal, EvenementOrganisationContext context) {
		Audit.info(String.format("Création d'un établissement %s no site %s", principal ? "principal" : "secondaire", site.getNumeroSite()));
		final Etablissement etablissement = new Etablissement();
		etablissement.setNumeroEtablissement(site.getNumeroSite());
		etablissement.setPrincipal(true);
		return (Etablissement) context.getTiersDAO().save(etablissement);
	}

	@NotNull
	public static Entreprise createEntreprise(Long noOrganisation, RegDate date, EvenementOrganisationContext context) {
		Audit.info(String.format("Création d'une entreprise pour l'organisation %s", noOrganisation));
		final Entreprise entreprise = new Entreprise();
		// Le numéro
		entreprise.setNumeroEntreprise(noOrganisation);
		// Le régime fiscal VD + CH
		entreprise.addRegimeFiscal(new RegimeFiscal(date, null, RegimeFiscal.Portee.CH, TypeRegimeFiscal.ORDINAIRE));
		entreprise.addRegimeFiscal(new RegimeFiscal(date, null, RegimeFiscal.Portee.VD, TypeRegimeFiscal.ORDINAIRE));
		return (Entreprise) context.getTiersDAO().save(entreprise);
	}

	public static void createAddBouclement(Entreprise entreprise, RegDate creationDate, EvenementOrganisationContext context) {
		final Bouclement bouclement = BouclementHelper.createBouclementSelonSemestre(creationDate);
		bouclement.setEntreprise(entreprise);
		context.getTiersDAO().addAndSave(entreprise, bouclement);
	}
}
