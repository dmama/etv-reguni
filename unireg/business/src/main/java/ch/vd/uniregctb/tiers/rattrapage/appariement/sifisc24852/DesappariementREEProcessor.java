package ch.vd.uniregctb.tiers.rattrapage.appariement.sifisc24852;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import ch.vd.rcent.unireg.unpairingree.OrganisationLocation;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * @author Raphaël Marmier, 2017-06-06, <raphael.marmier@vd.ch>
 */
public class DesappariementREEProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(DesappariementREEProcessor.class);

	private static final int BATCH_SIZE = 100;
	public static final double MIN_JARO_WINKLER_DISTANCE = 0.85;
	public static final String SIFISC_24852_USER = "JOB-SIFISC-24852";

	private final PlatformTransactionManager transactionManager;
	private final TiersService tiersService;
	private final DataEventService dataEventService;

	public DesappariementREEProcessor(PlatformTransactionManager transactionManager, TiersService tiersService, DataEventService dataEventService) {
		this.transactionManager = transactionManager;
		this.tiersService = tiersService;
		this.dataEventService = dataEventService;
	}

	public DesappariementREEResults run(final List<OrganisationLocation> aDesapparier, RegDate dateCharchementInitial, RegDate dateValeurDonneesCiviles, StatusManager s, boolean simulation) {

		final StatusManager status = (s != null ? s : new LoggingStatusManager(LOGGER));

		status.setMessage("Début du traitement...");

		final DesappariementREEResults rapportFinal = new DesappariementREEResults(aDesapparier, dateCharchementInitial, dateValeurDonneesCiviles, simulation);

		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<OrganisationLocation, DesappariementREEResults> template = new BatchTransactionTemplateWithResults<>(aDesapparier, BATCH_SIZE,
		                                                                                                                                               Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		template.execute(rapportFinal, new BatchWithResultsCallback<OrganisationLocation, DesappariementREEResults>() {

			@Override
			public void afterTransactionStart(TransactionStatus status) {
				if (simulation) {
					status.setRollbackOnly();
				}
			}

			@Override
			public DesappariementREEResults createSubRapport() {
				return new DesappariementREEResults(null, dateCharchementInitial, dateValeurDonneesCiviles, simulation);
			}

			@Override
			public boolean doInTransaction(List<OrganisationLocation> aDesapparier, DesappariementREEResults results) throws Exception {
				status.setMessage("Traitement du batch [" + aDesapparier.get(0).getCantonalId() + "; " + aDesapparier.get(aDesapparier.size() - 1).getCantonalId() + "] ...", progressMonitor.getProgressInPercent());
				traiterBatch(aDesapparier, results);
				return true;
			}
		}, progressMonitor);

		rapportFinal.setInterrompu(status.interrupted());
		rapportFinal.end();
		return rapportFinal;
	}

	private void traiterBatch(final List<OrganisationLocation> aDesapparier, DesappariementREEResults r) {

		for (final OrganisationLocation organisationLocation : aDesapparier) {
			desapparier(organisationLocation, r);
		}

	}

	private void desapparier(OrganisationLocation organisationLocation, DesappariementREEResults r) {

		DesappariementREEResults.TypeResultat resultat = DesappariementREEResults.TypeResultat.SUCCES;
		final List<String> msgs = new ArrayList<>();

		// Rechercher l'établissement correspondant, s'il existe
		final Etablissement etablissement = tiersService.getEtablissementByNumeroSite(organisationLocation.getCantonalId());
		if (etablissement == null) {
			DesappariementREEResults.Desappariement desappariement = new DesappariementREEResults.Desappariement(DesappariementREEResults.TypeResultat.ECHEC, organisationLocation, null, null, null);
			desappariement.setMessage("Etablissement introuvable!");
			r.addDesappariement(desappariement);
			throw new UnsupportedOperationException("Etablissement introuvable!");
			//return;
		}
		LOGGER.debug("Trouvé l'établissement n°" + etablissement.getNumero());

		final RegDate aujourdhui = RegDate.get();
		Entreprise entreprise = tiersService.getEntreprise(etablissement, aujourdhui);
		// Le rapport entre tiers peut avoir été fermé. Dans ce cas, on recherchera en fonction du dernier rapport entre tiers.
		if (entreprise == null) {
			final RapportEntreTiers rapportEntreTiers = etablissement.getDernierRapportObjet(TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE);
			if (rapportEntreTiers == null) {
				throw new RuntimeException(String.format("Impossible de trouver l'entreprise de l'établissement n°%s.", etablissement.getNumero()));
			}
			entreprise = (Entreprise) tiersService.getTiers(rapportEntreTiers.getSujetId());
		}
		final Etablissement etablissementPrincipal = tiersService.getEtablissementPrincipal(entreprise, aujourdhui);
		if (etablissement.getNumero().equals(etablissementPrincipal.getNumero())) {
			throw new UnsupportedOperationException("Le désappariement des établissements principaux n'est pas supporté à ce stade.");
		}

		/*
		 * Récupérer les données civiles Unireg et les rouvrir.
		 */

		// Raison sociale fiscale
		final String raisonSocialeFiscale = etablissement.getRaisonSociale();

		final double jaroWinklerDistance = StringUtils.getJaroWinklerDistance(raisonSocialeFiscale, organisationLocation.getName());
		if (jaroWinklerDistance < MIN_JARO_WINKLER_DISTANCE) {
			resultat = toVerifier(resultat);
			msgs.add("La raison sociale a peut-être changé.");
		}

		// Domicile fiscal
		final List<DomicileEtablissement> domicilesFiscaux = etablissement.getSortedDomiciles(false);

		if (domicilesFiscaux.isEmpty()) {
			resultat = toEchec(resultat);
			msgs.add("L'établissement n'a pas de domicile fiscal, impossible de le désapparier.");
		}
		final RegDate dateCharchementInitial = r.getDateCharchementInitial();
		final DomicileEtablissement domicileAtCutoff = DateRangeHelper.rangeAt(domicilesFiscaux, dateCharchementInitial.getOneDayBefore());
		final DomicileEtablissement lastDomicile = CollectionsUtils.getLastElement(domicilesFiscaux);
		if (domicileAtCutoff == null) {
			//resultat = toEchec(resultat);
			throw new IllegalArgumentException("Domicile fiscal introuvable pour la date de cutoff " + RegDateHelper.dateToDisplayString(dateCharchementInitial) + ".");
		} else {
			if (!domicileAtCutoff.getNumeroOfsAutoriteFiscale().equals(lastDomicile.getNumeroOfsAutoriteFiscale())) {
				resultat = toEchec(resultat);
				msgs.add("Le domicile fiscal a été changé après la date de l'appariement (Surcharge fiscale). Impossible de désapparier.");
			}
			else if (!domicileAtCutoff.getNumeroOfsAutoriteFiscale().equals(organisationLocation.getMunicipality())) {
				resultat = toVerifier(resultat);
				msgs.add("Le domicile a changé, il faudra déterminer quand le déménagement est survenu et faire le nécessaire.");
			}
		}

		// Désapparier l'établissement
		if (resultat != DesappariementREEResults.TypeResultat.ECHEC) {
			// Invalider le cache RCEnt
			dataEventService.onOrganisationChange(entreprise.getNumeroEntreprise());

			// Désapparier
			etablissement.setNumeroEtablissement(null);
			domicileAtCutoff.setAnnulationDate(aujourdhui.asJavaDate());
			domicileAtCutoff.setAnnulationUser(SIFISC_24852_USER);
			etablissement.addDomicile(new DomicileEtablissement(domicileAtCutoff.getDateDebut(), null, domicileAtCutoff.getTypeAutoriteFiscale(), domicileAtCutoff.getNumeroOfsAutoriteFiscale(), etablissement));

		}

		// Ajouter le résultat
		final DesappariementREEResults.Desappariement desappariement = new DesappariementREEResults.Desappariement(resultat, organisationLocation, null, null, etablissement.getNumero());
		desappariement.setRaisonSociale(raisonSocialeFiscale);
		desappariement.setRaisonsSocialesJaroWinker(jaroWinklerDistance);
		desappariement.setNoOFSCommune(domicileAtCutoff.getNumeroOfsAutoriteFiscale());
		desappariement.setMessage(StringUtils.join(msgs, " "));
		r.addDesappariement(desappariement);
	}

	private DesappariementREEResults.TypeResultat toVerifier(DesappariementREEResults.TypeResultat resultat) {
		if (resultat == DesappariementREEResults.TypeResultat.SUCCES) {
			return DesappariementREEResults.TypeResultat.VERIFIER;
		}
		return resultat;
	}

	private DesappariementREEResults.TypeResultat toEchec(DesappariementREEResults.TypeResultat resultat) {
		if (resultat == DesappariementREEResults.TypeResultat.SUCCES || resultat == DesappariementREEResults.TypeResultat.VERIFIER) {
			return DesappariementREEResults.TypeResultat.ECHEC;
		}
		return resultat;
	}

}
