package ch.vd.uniregctb.webservices.interfaces.impl;

import javax.jws.WebService;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.interfaces.fiscal.Fiscal;
import ch.vd.interfaces.fiscal.RechercherNoContribuable;
import ch.vd.interfaces.fiscal.RechercherNoContribuableResponse;
import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeTiers;
import ch.vd.uniregctb.tiers.TiersException;
import ch.vd.uniregctb.tiers.TiersService;

@WebService(targetNamespace = "http://www.vd.ch/interfaces/Fiscal/", serviceName = "FiscalService", portName = "FiscalServicePort", endpointInterface = "ch.vd.interfaces.fiscal.Fiscal")
public class FiscalWebServiceImpl implements Fiscal {

	private static final Logger LOGGER = Logger.getLogger(FiscalWebServiceImpl.class);

	private TiersService tiersService;
	private GlobalTiersSearcher tiersSearcher;

	@Override
	@Transactional(readOnly = true)
	public RechercherNoContribuableResponse rechercherNoContribuable(RechercherNoContribuable parameters) {

		List<TiersIndexedData> resultsUnireg = new ArrayList<TiersIndexedData>();
		TiersIndexedData tiersTrouve;
		int periode = parameters.getAnnee();
		long numeroCtbSeul = 0;

		TiersCriteria tiersCriteria;
		Contribuable contribuable;
		if (periode==0) {
			periode = RegDate.get().year();
		}

		String noAvs = String.valueOf(parameters.getNoAvs());
		if (noAvs != null && !noAvs.equals("0")) {

			if (noAvs.length() == 11) {
				noAvs = FormatNumeroHelper.formatAncienNumAVS(noAvs);
			}
			if (noAvs.length() == 13) {
				noAvs = FormatNumeroHelper.formatNumAVS(noAvs);

			}
			tiersCriteria = new TiersCriteria();
			tiersCriteria.setTypeTiers(TypeTiers.PERSONNE_PHYSIQUE);
			tiersCriteria.setNumeroAVS(noAvs);

			try {
				resultsUnireg = tiersSearcher.search(tiersCriteria);

			}
			catch (TooManyResultsIndexerException ee) {
				LOGGER.error("Exception dans l'indexer sur la recherche par noAvs: " + ee.getMessage(), ee);

			}
			catch (IndexerException e) {
				LOGGER.error("Exception dans l'indexer par noAvs: " + e.getMessage(), e);

			}
		}
		// Aucun individu trouvé ou plusieurs individu trouvé
		if (resultsUnireg.isEmpty() || resultsUnireg.size() > 1) {
			tiersCriteria = new TiersCriteria();
			tiersCriteria.setNomRaison(parameters.getPrenom() + ' ' + parameters.getNom());
			tiersCriteria.setTypeRechercheDuNom(TypeRecherche.CONTIENT);
			tiersCriteria.setTypeTiers(TypeTiers.PERSONNE_PHYSIQUE);


			try {
				resultsUnireg = tiersSearcher.search(tiersCriteria);

			}
			catch (TooManyResultsIndexerException ee) {
				LOGGER.error("Exception dans l'indexer sur la recherche par prenom nom: " + ee.getMessage(), ee);

			}
			catch (IndexerException e) {
				LOGGER.error("Exception dans l'indexer sur la recherche par prenom nom: " + e.getMessage(), e);

			}
		}

		RechercherNoContribuableResponse response = new RechercherNoContribuableResponse();
		if (resultsUnireg.isEmpty())
			response.setNbrOccurence(0);
		else {

			if (resultsUnireg.size() > 999) {
				response.setNbrOccurence(999);
			}
			else {
				response.setNbrOccurence(resultsUnireg.size());
			}

			if (resultsUnireg.size() == 1) {
				tiersTrouve = resultsUnireg.get(0);
				numeroCtbSeul = tiersTrouve.getNumero();
			}

			if (numeroCtbSeul > 0) {

				response.setNoContribuableSeul(numeroCtbSeul);
				contribuable = tiersService.getTiersDAO().getContribuableByNumero(numeroCtbSeul);
				if (contribuable != null) {

					// recherche du menage commun sur la periode
					Range rangePeriode = new Range(RegDate.get(periode, RegDate.JANVIER, 1), RegDate.get(periode, RegDate.DECEMBRE, 31));
					MenageCommun menage = null;
					try {
						menage = tiersService.getMenageCommunActifAt(contribuable, rangePeriode);
					}
					catch (Exception e) {

						LOGGER.error("Exception dans la recherche d'un menage : " + e.getMessage(), e);
					}
					if (menage != null) {
						List<Assujettissement> assujettissement = null;
						// On recherche un assujetissement du ménage sur la période
						try {
							assujettissement = Assujettissement.determine(menage, periode);
						}
						catch (AssujettissementException e1) {
							LOGGER.error("Exception dans la recherche d'assujettissement du ménage: période : " + periode + ' '
									+ e1.getMessage(), e1);
						}
						// Si la periode de recherche courrante ne donne rien, on recherche une période antérieur
						if (assujettissement == null) {
							if(contribuable.getDateDebutActivite()!=null){
							for (int i = parameters.getAnnee(); i >= contribuable.getDateDebutActivite().year(); i--) {
								periode = i;
								try {
									assujettissement = Assujettissement.determine(menage, periode);
								}
								catch (AssujettissementException e) {
									LOGGER.error("Exception dans la recherche d'assujettissement du ménage: période : " + i + ' '
											+ e.getMessage(), e);
								}

								if (assujettissement != null) {
									break;
								}
							}
							}

						}
						// Assujettissement trouvé dans l'une des deux phases de recherche précédente
						if (assujettissement != null) {
							response.setNoContribuableCouple(menage.getNumero());
						}
					}
					// Sourcier Pur ou pas
					PersonnePhysique pp = (PersonnePhysique) contribuable;
					RegDate finPeriode = RegDate.get(periode, RegDate.DECEMBRE, 31);
					if (pp.isActif(finPeriode.asJavaDate())) {
						response.setSourcierPur(false);
					}
					else {
						try {
							if (tiersService.isEtrangerSansPermisC(pp, null)) {
								response.setSourcierPur(true);
							}
							else {
								response.setSourcierPur(false);
							}
						}
						catch (TiersException e) {
							LOGGER.error("Impossible de déterminer si la PP est étrangère sans permis C : " + e.getMessage(), e);
						}
					}
				}
			}
		}

		return response;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersSearcher(GlobalTiersSearcher tiersSearcher) {
		this.tiersSearcher = tiersSearcher;
	}
}
