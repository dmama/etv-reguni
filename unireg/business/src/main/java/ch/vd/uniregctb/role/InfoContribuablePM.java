package ch.vd.uniregctb.role;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.FormeLegaleHisto;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Informations sur un contribuable PM dans le fichier des rôles pour une periode fiscale (= un bouclement)
 */
public class InfoContribuablePM extends InfoContribuable<InfoContribuablePM> {

	private final String noIde;
	private final String raisonSociale;
	private final FormeLegale formeJuridique;
	private final RegDate dateBouclement;
	private final TypeAutoriteFiscale tafForPrincipal;
	private final Integer noOfsForPrincipal;

	public InfoContribuablePM(Entreprise ctb, RegDate dateBouclement, AdresseService adresseService, TiersService tiersService) {
		super(ctb, dateBouclement.year(), adresseService);
		this.noIde = tiersService.getNumeroIDE(ctb);
		this.raisonSociale = tiersService.getDerniereRaisonSociale(ctb);
		this.formeJuridique = extractFormeJuridique(ctb, dateBouclement, tiersService);
		this.dateBouclement = dateBouclement;

		final ForFiscalPrincipal ffp = ctb.getDernierForFiscalPrincipalAvant(dateBouclement);
		if (ffp == null) {
			tafForPrincipal = null;
			noOfsForPrincipal = null;
		}
		else {
			tafForPrincipal = ffp.getTypeAutoriteFiscale();
			noOfsForPrincipal = ffp.getNumeroOfsAutoriteFiscale();
		}
	}

	private InfoContribuablePM(InfoContribuablePM original) {
		super(original);
		this.noIde = original.noIde;
		this.raisonSociale = original.raisonSociale;
		this.formeJuridique = original.formeJuridique;
		this.dateBouclement = original.dateBouclement;
		this.tafForPrincipal = original.tafForPrincipal;
		this.noOfsForPrincipal = original.noOfsForPrincipal;
	}

	public String getNoIde() {
		return noIde;
	}

	public String getRaisonSociale() {
		return raisonSociale;
	}

	public FormeLegale getFormeJuridique() {
		return formeJuridique;
	}

	public RegDate getDateBouclement() {
		return dateBouclement;
	}

	public TypeAutoriteFiscale getTafForPrincipal() {
		return tafForPrincipal;
	}

	public Integer getNoOfsForPrincipal() {
		return noOfsForPrincipal;
	}

	@Override
	public InfoContribuablePM duplicate() {
		return new InfoContribuablePM(this);
	}

	@Nullable
	private static FormeLegale extractFormeJuridique(Entreprise entreprise, RegDate dateBouclement, TiersService tiersService) {
		final List<FormeLegaleHisto> formesJuridiques = tiersService.getFormesLegales(entreprise, false);
		final DateRange atOrBeforeRange = new DateRangeHelper.Range(null, dateBouclement);
		final List<DateRange> atOrBefore = DateRangeHelper.intersections(atOrBeforeRange, formesJuridiques);
		if (atOrBefore != null && !atOrBefore.isEmpty()) {
			final RegDate dateReference = CollectionsUtils.getLastElement(atOrBefore).getDateFin();
			return DateRangeHelper.rangeAt(formesJuridiques, dateReference).getFormeLegale();
		}
		else {
			return null;
		}
	}
}
