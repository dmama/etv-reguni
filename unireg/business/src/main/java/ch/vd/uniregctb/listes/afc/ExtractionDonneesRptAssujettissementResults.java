package ch.vd.uniregctb.listes.afc;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.declaration.ordinaire.ForsList;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.DecompositionForsAnneeComplete;
import ch.vd.uniregctb.metier.assujettissement.HorsCanton;
import ch.vd.uniregctb.metier.assujettissement.HorsSuisse;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Classe de base des résultats qui présentent une ligne par période d'assujettissement (cas des sourciers purs, en particulier, pour lesquels
 * il n'existe pas de période d'imposition)
 */
public abstract class ExtractionDonneesRptAssujettissementResults extends ExtractionDonneesRptResults {

	private AssujettissementService assujettissementService;

	public ExtractionDonneesRptAssujettissementResults(RegDate dateTraitement, int periodeFiscale, int nbThreads, TiersService tiersService, ServiceInfrastructureService infraService,
	                                                   AssujettissementService assujettissementService) {
		super(dateTraitement, periodeFiscale, nbThreads, tiersService, infraService);
		this.assujettissementService = assujettissementService;
	}

	/**
	 * Implémentée par les classes dérivées pour oter les assujettissements qui ne doivent pas être pris en compte
	 * @param ctb contribuable assujetti
	 * @param listeAFiltrer liste à modifier en cas de filtrage effectif (en entrée, la liste n'est jamais vide)
	 * @return si la liste à filtrer ne contient plus d'éléments en sortie de la méthode, alors la valeur retournée doit être une description de la raison pour laquelle tous les assujettissements ont été filtrés (sinon, la valeur retournée sera ignorée de toute façon)
	 */
	protected abstract String filterAssujettissements(Contribuable ctb, List<Assujettissement> listeAFiltrer);

	@Override
	protected List<InfoPeriodeImposition> buildInfoPeriodes(DecompositionForsAnneeComplete decomposition) throws ServiceInfrastructureException, CoupleInvalideException, AssujettissementException, ContribuableIgnoreException {

		final Contribuable ctb = decomposition.contribuable;
		final List<Assujettissement> assujettissements = assujettissementService.determine(ctb, decomposition.annee);
		if (assujettissements == null || assujettissements.isEmpty()) {
			throw new ContribuableIgnoreException(NON_ASSUJETTI);
		}

		final String raisonExclusion = filterAssujettissements(ctb, assujettissements);
		if (assujettissements.isEmpty()) {
			if (StringUtils.isBlank(raisonExclusion)) {
				throw new RuntimeException("Tous les assujettissements de la période fiscale " + periodeFiscale + " ont été filtrés sans explication");
			}
			throw new ContribuableIgnoreException(raisonExclusion);
		}

		final InfoIdentificationCtb identification = buildInfoIdentification(ctb, assujettissements.get(assujettissements.size() - 1).getDateFin());

		// on boucle ensuite sur les périodes d'assujettissement pour faire une ligne par période
		final List<InfoPeriodeImposition> liste = new ArrayList<InfoPeriodeImposition>(assujettissements.size());
		for (Assujettissement a : assujettissements) {

			final TypeAutoriteFiscale autoriteFiscaleForPrincipal;
			final ModeImposition modeImposition;
			final MotifRattachement motifRattachement;
			final Integer ofsCommuneForGestion;
			if (a instanceof SourcierPur) {
				final ForFiscalPrincipal ffp = extraireDernierForSource(a.getFors().principauxDansLaPeriode);
				modeImposition = ModeImposition.SOURCE;
				if (ffp == null) {
					// c'est toujours mieux que rien... cas du for SOURCE "inventé" par un for principal avec motif d'ouverture "CHGT_MODE_IMPOSITION"
					final ForFiscalPrincipal ffpNonSource = a.getFors().principal;
					motifRattachement = ffpNonSource.getMotifRattachement();
					autoriteFiscaleForPrincipal = ffpNonSource.getTypeAutoriteFiscale();
					ofsCommuneForGestion = getNumeroOfsCommuneVaudoise(ffpNonSource.getNumeroOfsAutoriteFiscale(), autoriteFiscaleForPrincipal, ffpNonSource.getDateDebut());
				}
				else {
					motifRattachement = ffp.getMotifRattachement();     // on n'a pas encore les fors secondaires sources...
					autoriteFiscaleForPrincipal = ffp.getTypeAutoriteFiscale();
					ofsCommuneForGestion = getNumeroOfsCommuneVaudoise(ffp.getNumeroOfsAutoriteFiscale(), autoriteFiscaleForPrincipal, ffp.getDateDebut());
				}
			}
			else {
				final ForGestion forGestion = tiersService.getDernierForGestionConnu(ctb, a.getDateFin());
				if (forGestion == null) {
					throw new RuntimeException("Assujettissement " + a + " non sourcier-pur sans for de gestion en fin d'assujettissement ?");
				}

				ofsCommuneForGestion = getNumeroOfsCommuneVaudoise(forGestion.getNoOfsCommune(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forGestion.getDateDebut());

				final ForFiscalRevenuFortune forRevenuFortune = forGestion.getSousjacent();
				motifRattachement = forRevenuFortune.getMotifRattachement();
				if (forRevenuFortune instanceof ForFiscalPrincipal) {
					autoriteFiscaleForPrincipal = TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
					modeImposition = ((ForFiscalPrincipal) forRevenuFortune).getModeImposition();
				}
				else {
					// les rattachements économiques sont au mode d'imposition ordinaire, enfin je crois...
					modeImposition = ModeImposition.ORDINAIRE;
					if (!(a instanceof HorsSuisse || a instanceof HorsCanton)) {
						throw new RuntimeException("Rattachement économique avec assujettissement différent de HS ou HC : " + a);
					}
					autoriteFiscaleForPrincipal = (a instanceof HorsSuisse ? TypeAutoriteFiscale.PAYS_HS : TypeAutoriteFiscale.COMMUNE_HC);
				}
			}

			final InfoPeriodeImposition info = buildInfoPeriodeImpositionFromAssujettissement(ctb, identification, modeImposition, motifRattachement, a, ofsCommuneForGestion,
			                                                                                  autoriteFiscaleForPrincipal);
			if (info != null) {
				liste.add(info);
			}
		}

		return liste;
	}

	protected InfoPeriodeImposition buildInfoPeriodeImpositionFromAssujettissement(Contribuable ctb, InfoIdentificationCtb identification,
	                                                                             ModeImposition modeImposition, MotifRattachement motifRattachement, Assujettissement a,
	                                                                             Integer ofsCommuneForGestion, TypeAutoriteFiscale autoriteFiscaleForPrincipal) {
		return buildInfoPeriodeImposition(ctb, identification, modeImposition, motifRattachement, a, a.getMotifFractDebut(), a.getMotifFractFin(),
		                                  ofsCommuneForGestion, autoriteFiscaleForPrincipal);
	}

	/**
	 * On se sert de cette méthode pour récupérer le dernier for SOURCE dans la collection des fors principaux actifs dans une période d'assujettissement
	 * (cette collection est extraite de la décomposition des fors attachée à l'assujettissement lui-même)... La raison en est l'arrondi qui est fait dans
	 * l'assujettissement entre la date de fin de for source et la date de fin d'assujettissement source (fin du mois) dans le cas d'un changement de mode
	 * d'imposition
	 * @param principauxDansLaPeriode les fors principaux triés valides dans une période
	 * @return le dernier for principal de la liste fournie avec un mode d'imposition SOURCE, ou <code>null</code> s'il n'y en a pas...
	 */
	private static ForFiscalPrincipal extraireDernierForSource(ForsList<ForFiscalPrincipal> principauxDansLaPeriode) {
		final ListIterator<ForFiscalPrincipal> iterator = principauxDansLaPeriode.listIterator(principauxDansLaPeriode.size());
		while (iterator.hasPrevious()) {
			final ForFiscalPrincipal ffp = iterator.previous();
			if (ffp.getModeImposition() == ModeImposition.SOURCE) {
				return ffp;
			}
		}
		return null;
	}
}
