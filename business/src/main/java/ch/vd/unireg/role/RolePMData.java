package ch.vd.unireg.role;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.FormeLegaleHisto;
import ch.vd.unireg.tiers.RaisonSocialeHisto;
import ch.vd.unireg.tiers.TiersService;

public class RolePMData extends RoleData {

	public final String noIDE;
	public final String raisonSociale;
	public final FormeLegale formeJuridique;

	public RolePMData(Entreprise entreprise, int ofsCommune, int annee, AdresseService adresseService, ServiceInfrastructureService infrastructureService, TiersService tiersService, AssujettissementService assujettissementService) throws
			CalculRoleException {
		super(entreprise, ofsCommune, annee, adresseService, infrastructureService, assujettissementService);
		this.noIDE = tiersService.getNumeroIDE(entreprise);
		this.raisonSociale = buildRaisonSociale(entreprise, annee, tiersService);
		this.formeJuridique = buildFormeJuridique(entreprise, annee, tiersService);
	}

	@Nullable
	protected static String buildRaisonSociale(Entreprise entreprise, int annee, TiersService tiersService) {
		final List<RaisonSocialeHisto> all = tiersService.getRaisonsSociales(entreprise, false);
		return buildLastHistoData(all, annee, RaisonSocialeHisto::getRaisonSociale);
	}

	@Nullable
	protected static FormeLegale buildFormeJuridique(Entreprise entreprise, int annee, TiersService tiersService) {
		final List<FormeLegaleHisto> all = tiersService.getFormesLegales(entreprise, false);
		return buildLastHistoData(all, annee, FormeLegaleHisto::getFormeLegale);
	}

	@Nullable
	private static <H extends DateRange, D> D buildLastHistoData(List<? extends H> histo, int annee, Function<? super H, ? extends D> dataExtractor) {
		return histo.stream()
				.filter(h -> h.getDateDebut().year() <= annee)
				.max(Comparator.comparing(DateRange::getDateDebut, NullDateBehavior.EARLIEST::compare))
				.map(dataExtractor)
				.orElse(null);
	}

	@Override
	protected boolean estSoumisImpot() {
		return Boolean.TRUE;
	}
}
