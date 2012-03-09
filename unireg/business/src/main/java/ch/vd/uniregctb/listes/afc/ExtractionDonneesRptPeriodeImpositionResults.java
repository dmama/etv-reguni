package ch.vd.uniregctb.listes.afc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.DecompositionForsAnneeComplete;
import ch.vd.uniregctb.metier.assujettissement.HorsCanton;
import ch.vd.uniregctb.metier.assujettissement.HorsSuisse;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Classe de base des résultats qui présentent une ligne par période d'imposition (cas des contribuables au rôle ordinaire)
 */
public abstract class ExtractionDonneesRptPeriodeImpositionResults extends ExtractionDonneesRptResults {

	private static final String NON_ASSUJETTI_ROLE_ORDINAIRE = "Non-assujetti au rôle ordinaire";
	private final AssujettissementService assujettissementService;
	private final PeriodeImpositionService periodeImpositionService;

	public ExtractionDonneesRptPeriodeImpositionResults(RegDate dateTraitement, int periodeFiscale, int nbThreads, TiersService tiersService, ServiceInfrastructureService infraService,
	                                                    AssujettissementService assujettissementService, PeriodeImpositionService periodeImpositionService) {
		super(dateTraitement, periodeFiscale, nbThreads, tiersService, infraService);
		this.assujettissementService = assujettissementService;
		this.periodeImpositionService = periodeImpositionService;
	}

	@Override
	protected List<InfoPeriodeImposition> buildInfoPeriodes(DecompositionForsAnneeComplete decomposition) throws ServiceInfrastructureException, CoupleInvalideException, AssujettissementException, ContribuableIgnoreException {

		final Contribuable ctb = decomposition.contribuable;
		final List<Assujettissement> assujettissements = assujettissementService.determine(ctb, decomposition.annee);
		if (assujettissements == null || assujettissements.isEmpty()) {
			throw new ContribuableIgnoreException(NON_ASSUJETTI);
		}

		// les assujettissements sont triés, donc les périodes brutes aussi
		final Map<PeriodeImposition, List<Assujettissement>> mappingNonRegroupe = new HashMap<PeriodeImposition, List<Assujettissement>>(assujettissements.size());
		final List<PeriodeImposition> periodesBrutes = new ArrayList<PeriodeImposition>();
		for (Assujettissement a : assujettissements) {
			final PeriodeImposition periode = periodeImpositionService.determinePeriodeImposition(decomposition, a);
			if (periode != null) {
				periodesBrutes.add(periode);
				mappingNonRegroupe.put(periode, Arrays.asList(a));
			}
		}

		// s'il y a plusieurs périodes à ce niveau (cas normalement assez rare), elles vont peut-être être
		// rassemblées ensemble, donc il faut toujours être capable de retrouver tous les assujettissements
		// qui ont donné lieu à une période regroupée
		final List<PeriodeImposition> periodes;
		final Map<PeriodeImposition, List<Assujettissement>> mapping;
		if (periodesBrutes.size() > 1) {
			// [UNIREG-1118] On fusionne les périodes qui provoqueraient des déclarations identiques contiguës.
			periodes = DateRangeHelper.collate(periodesBrutes);
			mapping = new HashMap<PeriodeImposition, List<Assujettissement>>(periodes.size());
			for (PeriodeImposition p : periodesBrutes) {
				final RegDate dateDebut = p.getDateDebut();
				final PeriodeImposition periodeRegroupee = DateRangeHelper.rangeAt(periodes, dateDebut);
				List<Assujettissement> assujettissementsMappes = mapping.get(periodeRegroupee);
				if (assujettissementsMappes == null) {
					assujettissementsMappes = new ArrayList<Assujettissement>();
					mapping.put(periodeRegroupee, assujettissementsMappes);
				}
				assujettissementsMappes.addAll(mappingNonRegroupe.get(p));
			}
		}
		else {
			mapping = mappingNonRegroupe;
			periodes = periodesBrutes;
		}

		// s'il y a eu un assujettissement, mais pas de période d'imposition, on parle de non-assujetti
		// au rôle ordinaire
		if (periodes.isEmpty()) {
			throw new ContribuableIgnoreException(NON_ASSUJETTI_ROLE_ORDINAIRE);
		}

		// selon l'extraction voulue, certaines périodes d'impositions peuvent être exclues
		final String raisonExclusion = filterPeriodes(ctb, periodes);
		if (periodes.isEmpty()) {
			if (StringUtils.isBlank(raisonExclusion)) {
				throw new RuntimeException("Toutes les périodes d'imposition de la période fiscale " + periodeFiscale + " ont été filtrées sans explication");
			}
			throw new ContribuableIgnoreException(raisonExclusion);
		}

		final InfoIdentificationCtb identification = buildInfoIdentification(ctb, periodes.get(periodes.size() - 1).getDateFin());

		// une entité par période d'imposition restante
		final List<InfoPeriodeImposition> listeFinale = new ArrayList<InfoPeriodeImposition>(periodes.size());
		for (PeriodeImposition periode : periodes) {
			final List<Assujettissement> assujettissementsPourPeriode = mapping.get(periode);
			final Assujettissement premierAssujettissement = assujettissementsPourPeriode.get(0);
			final Assujettissement dernierAssujettissement = assujettissementsPourPeriode.get(assujettissementsPourPeriode.size() - 1);

			final TypeAutoriteFiscale autoriteFiscaleForPrincipal;
			final ModeImposition modeImposition;
			final MotifRattachement motifRattachement;
			final Integer ofsCommuneForGestion;

			final ForGestion forGestion = tiersService.getDernierForGestionConnu(ctb, periode.getDateFin());
			if (forGestion == null) {
				throw new RuntimeException("Periode d'imposition " + periode + " sans for de gestion en fin de période ?");
			}

			final Commune commune = infraService.getCommuneByNumeroOfsEtendu(forGestion.getNoOfsCommune(), forGestion.getDateDebut());
			if (commune.isFraction()) {
				final Commune communeFaitiere = infraService.getCommuneFaitiere(commune, forGestion.getDateDebut());
				ofsCommuneForGestion = communeFaitiere.getNoOFSEtendu();
			}
			else {
				ofsCommuneForGestion = commune.getNoOFSEtendu();
			}

			final ForFiscalRevenuFortune forRevenuFortune = forGestion.getSousjacent();
			motifRattachement = forRevenuFortune.getMotifRattachement();
			if (forRevenuFortune instanceof ForFiscalPrincipal) {
				autoriteFiscaleForPrincipal = TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
				modeImposition = ((ForFiscalPrincipal) forRevenuFortune).getModeImposition();
			}
			else {
				// les rattachements économiques sont au mode d'imposition ordinaire, enfin je crois...
				modeImposition = ModeImposition.ORDINAIRE;
				if (!(dernierAssujettissement instanceof HorsSuisse || dernierAssujettissement instanceof HorsCanton)) {
					throw new RuntimeException("Rattachement économique avec assujettissement différent de HS ou HC : " + dernierAssujettissement);
				}
				autoriteFiscaleForPrincipal = (dernierAssujettissement instanceof HorsSuisse ? TypeAutoriteFiscale.PAYS_HS : TypeAutoriteFiscale.COMMUNE_HC);
			}

			final InfoPeriodeImposition info = buildInfoPeriodeImpositionFromPeriodeImposition(ctb, identification, modeImposition, motifRattachement, periode,
			                                                                                   premierAssujettissement.getMotifFractDebut(), dernierAssujettissement.getMotifFractFin(),
			                                                                                   ofsCommuneForGestion, autoriteFiscaleForPrincipal);
			if (info != null) {
				listeFinale.add(info);
			}
		}

		return listeFinale;
	}

	protected InfoPeriodeImposition buildInfoPeriodeImpositionFromPeriodeImposition(Contribuable ctb, InfoIdentificationCtb identification,
																				    ModeImposition modeImposition, MotifRattachement motifRattachement, PeriodeImposition pi, MotifFor motifDebut,
																				    MotifFor motifFin, Integer ofsCommuneForGestion, TypeAutoriteFiscale autoriteFiscaleForPrincipal) {
		return buildInfoPeriodeImposition(ctb, identification, modeImposition, motifRattachement, pi, motifDebut, motifFin, ofsCommuneForGestion, autoriteFiscaleForPrincipal);
	}

	/**
	 * Filtrage des périodes d'imposition non utilisées pour l'extraction en cours
	 * @param ctb contribuable traité
	 * @param listeAFiltrer liste sur laquelle on doit appliquer le filtre si nécessaire
	 * @return si la liste à filtrer ne contient plus d'éléments en sortie de la méthode, alors la valeur retournée doit être une description de la raison pour laquelle toutes les périodes d'imposition ont été filtrées (sinon, la valeur retournée sera ignorée de toute façon)
	 */
	protected abstract String filterPeriodes(Contribuable ctb, List<PeriodeImposition> listeAFiltrer);
}
