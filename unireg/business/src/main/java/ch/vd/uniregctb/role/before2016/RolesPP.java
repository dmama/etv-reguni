package ch.vd.uniregctb.role.before2016;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.TiersService;

public class RolesPP extends Roles<InfoContribuablePP, InfoCommunePP, ContribuableImpositionPersonnesPhysiques> {

	@Override
	public void digestInfoFor(InfoFor infoFor, ContribuableImpositionPersonnesPhysiques ctb, Assujettissement assujettissement, RegDate dateFinAssujettissementPrecedent, int annee, int noOfsCommune, AdresseService adresseService, TiersService tiersService) {
		final InfoCommunePP infoCommune = getOrCreateInfoCommune(noOfsCommune);
		final InfoContribuablePP infoContribuable = infoCommune.getOrCreateInfoPourContribuable(ctb, annee, adresseService, tiersService);
		infoContribuable.addFor(infoFor);
	}

	@Override
	public List<DateRange> getPeriodesFiscales(ContribuableImpositionPersonnesPhysiques contribuable, TiersService tiersService) {
		final ForFiscalPrincipalPP premierForPrincipal = contribuable.getPremierForFiscalPrincipal();
		final ForFiscalPrincipalPP dernierForPrincipal = contribuable.getDernierForFiscalPrincipal();
		if (premierForPrincipal == null || dernierForPrincipal == null) {
			return Collections.emptyList();
		}

		// pour les personnes physiques, les périodes fiscales coincident avec les années civiles
		final RegDate dateDebut = premierForPrincipal.getDateDebut();
		final RegDate dateFin = RegDateHelper.minimum(dernierForPrincipal.getDateFin(), RegDate.get(RegDate.get().year(), 12, 31), NullDateBehavior.LATEST);
		final List<DateRange> pfs = new ArrayList<>(dateFin.year() - dateDebut.year() + 1);
		for (int year = dateDebut.year() ; year <= dateFin.year() ; ++ year) {
			pfs.add(new DateRangeHelper.Range(RegDate.get(year, 1, 1), RegDate.get(year, 12, 31)));
		}
		return pfs;
	}

	@Override
	protected InfoCommunePP createInfoCommune(int noOfsCommune) {
		return new InfoCommunePP(noOfsCommune);
	}

	@Override
	protected Long buildInfoContribuableKey(InfoContribuablePP infoContribuable) {
		return infoContribuable.noCtb;
	}
}
