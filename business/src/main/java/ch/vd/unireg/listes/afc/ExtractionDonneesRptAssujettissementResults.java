package ch.vd.unireg.listes.afc;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.declaration.ForsList;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.metier.assujettissement.AssujettissementException;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.DecompositionFors;
import ch.vd.unireg.metier.assujettissement.DecompositionForsAnneeComplete;
import ch.vd.unireg.metier.assujettissement.HorsCanton;
import ch.vd.unireg.metier.assujettissement.HorsSuisse;
import ch.vd.unireg.metier.assujettissement.SourcierPur;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForFiscalRevenuFortune;
import ch.vd.unireg.tiers.ForGestion;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Classe de base des résultats qui présentent une ligne par période d'assujettissement (cas des sourciers purs, en particulier, pour lesquels
 * il n'existe pas de période d'imposition)
 */
public abstract class ExtractionDonneesRptAssujettissementResults extends ExtractionDonneesRptResults {

	private final AssujettissementService assujettissementService;

	public ExtractionDonneesRptAssujettissementResults(RegDate dateTraitement, int periodeFiscale, int nbThreads, TiersService tiersService, ServiceInfrastructureService infraService,
	                                                   AssujettissementService assujettissementService, AdresseService adresseService) {
		super(dateTraitement, periodeFiscale, nbThreads, tiersService, infraService, adresseService);
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
	protected List<InfoPeriodeImposition> buildInfoPeriodes(DecompositionForsAnneeComplete decomposition) throws InfrastructureException, CoupleInvalideException, AssujettissementException, ContribuableIgnoreException {

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

		final InfoIdentificationCtb identification = buildInfoIdentification(ctb, decomposition.annee);

		// on boucle ensuite sur les périodes d'assujettissement pour faire une ligne par période
		final List<InfoPeriodeImposition> liste = new ArrayList<>(assujettissements.size());
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
					// [SIFISC-10312] également le cas d'un sourcier mixte2 qui se marie dans l'année
					final ForFiscalPrincipal ffpNonSource = extractForPrincipalDernierRecours(a.getFors());
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
				if (forRevenuFortune instanceof ForFiscalPrincipalPP) {
					autoriteFiscaleForPrincipal = TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
					modeImposition = ((ForFiscalPrincipalPP) forRevenuFortune).getModeImposition();
				}
				else if (forRevenuFortune.isPrincipal()) {
					// TODO [SIPM] on n'a pas encore traité le cas des fors principaux de PM
					throw new NotImplementedException("L'assujettissement des PM n'existe pas encore...");
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
	 * On se sert de cette méthode pour récupérer un for principal sur une période quand toutes les autres méthodes ont échoué (= selon le principe
	 * du "c'est mieux que rien...")
	 * @param decomposition décomposition de base
	 * @return le for principal trouvé, ou <code>null</code> s'il n'y en a vraiment pas (mais comment peut-il alors y avoir assujettissement ?)
	 */
	private static ForFiscalPrincipal extractForPrincipalDernierRecours(DecompositionFors decomposition) {
		if (decomposition.principal != null) {
			return decomposition.principal;
		}

		if (!decomposition.principauxDansLaPeriode.isEmpty()) {
			return decomposition.principauxDansLaPeriode.last();
		}

		return null;
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
		for (ForFiscalPrincipal ffp : CollectionsUtils.revertedOrder(principauxDansLaPeriode)) {
			if (ffp instanceof ForFiscalPrincipalPP && ((ForFiscalPrincipalPP) ffp).getModeImposition() == ModeImposition.SOURCE) {
				return ffp;
			}
		}
		return null;
	}
}
