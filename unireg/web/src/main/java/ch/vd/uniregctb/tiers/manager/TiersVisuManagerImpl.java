package ch.vd.uniregctb.tiers.manager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesFiscalesHisto;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.StandardBatchIterator;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.decision.aci.DecisionAciViewComparator;
import ch.vd.uniregctb.di.view.DeclarationImpotListView;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.InterfaceDataException;
import ch.vd.uniregctb.mouvement.MouvementDossier;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.tiers.DecisionAciView;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.HistoFlags;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.view.AdresseCivilView;
import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.tiers.view.RapportsPrestationView;
import ch.vd.uniregctb.tiers.view.TiersView;
import ch.vd.uniregctb.tiers.view.TiersVisuView;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Service qui fournit les methodes pour visualiser un tiers
 *
 * @author xcifde
 */
public class TiersVisuManagerImpl extends TiersManager implements TiersVisuManager {

	private MouvementVisuManager mouvementVisuManager;

	private HibernateTemplate hibernateTemplate;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setMouvementVisuManager(MouvementVisuManager mouvementVisuManager) {
		this.mouvementVisuManager = mouvementVisuManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public TiersVisuView getView(Long numero, HistoFlags histoFlags,
	                             boolean modeImpression, boolean forsPrincipauxPagines, boolean forsSecondairesPagines, boolean autresForsPagines, WebParamPagination webParamPagination)
			throws AdresseException, ServiceInfrastructureException, DonneesCivilesException {

		final TiersVisuView tiersVisuView = new TiersVisuView(histoFlags);

		final Tiers tiers = getTiersDAO().get(numero);
		if (tiers == null) {
			throw new TiersNotFoundException(numero);
		}

		setTiersGeneralView(tiersVisuView, tiers);
		tiersVisuView.setComplement(buildComplement(tiers));

		if (tiers instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) tiers;
			if (pp.isConnuAuCivil()) {
				setHabitant(tiersVisuView, pp);
			}
			else {
				tiersVisuView.setTiers(pp);
			}
			tiersVisuView.setEtiquettes(getEtiquettes(tiers, tiersVisuView.isLabelsHisto()));
		}
		else if (tiers instanceof MenageCommun) {
			final MenageCommun menageCommun = (MenageCommun) tiers;
			setMenageCommun(tiersVisuView, menageCommun);
		}
		else if (tiers instanceof Entreprise) {
			final Entreprise entreprise = (Entreprise) tiers;
			setEntreprise(tiersVisuView, entreprise);
		}
		else if (tiers instanceof AutreCommunaute) {
			final AutreCommunaute autreCommunaute = (AutreCommunaute) tiers;
			tiersVisuView.setTiers(autreCommunaute);
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
			setDebiteurPrestationImposable(tiersVisuView, dpi, tiersVisuView.isRapportsPrestationHisto(), webParamPagination);
			setContribuablesAssocies(tiersVisuView, dpi, tiersVisuView.isCtbAssocieHisto());
			setForsFiscauxDebiteur(tiersVisuView, dpi);
			setPeriodicitesView(tiersVisuView, dpi);
			setLogicielView(tiersVisuView, dpi);
		}
		else if (tiers instanceof CollectiviteAdministrative) {
			tiersVisuView.setTiers(tiers);
		}
		else if (tiers instanceof Etablissement) {
			final Etablissement etb = (Etablissement) tiers;
			setEtablissement(tiersVisuView, etb);
		}

		if (tiersVisuView.getTiers() != null) {

			if (tiers instanceof Contribuable) {
				final Contribuable contribuable = (Contribuable) tiers;
				tiersVisuView.setDebiteurs(getDebiteurs(contribuable));
				tiersVisuView.setDis(new DeclarationImpotListView(contribuable, serviceInfrastructureService, messageSource).getDis());
				tiersVisuView.setMouvements(getMouvements(contribuable));
				setForsFiscaux(tiersVisuView, contribuable);
				setDecisionAciView(tiersVisuView,contribuable);
				setMandataires(tiersVisuView, contribuable);

				try {
					setSituationsFamille(tiersVisuView, contribuable);
				}
				catch (InterfaceDataException e) {
					LOGGER.warn(String.format("Exception lors de la récupération des situations de familles du contribuable %d", numero), e);
					tiersVisuView.setSituationsFamilleEnErreurMessage(e.getMessage());
				}
			}

			// adresses
			resolveAdressesHisto(new AdressesResolverCallback() {
				@Override
				public AdressesFiscalesHisto getAdresses(AdresseService service) throws AdresseException {
					final AdressesFiscalesHisto histo = service.getAdressesFiscalHisto(tiers, false);
					if (tiersVisuView.isAdressesHisto()) {
						return histo;
					}
					else {
						return histo.filter(TiersVisuManagerImpl::isAlwaysShown);
					}
				}

				@Override
				public void setAdressesView(List<AdresseView> adresses) {
					final List<AdresseView> adressesResultat = removeAdresseFromCivil(adresses);
					tiersVisuView.setHistoriqueAdresses(adressesResultat);
				}

				@Override
				public void onException(String message, List<AdresseView> adressesEnErreur) {
					tiersVisuView.setAdressesEnErreurMessage(message);
					tiersVisuView.setAdressesEnErreur(adressesEnErreur);
				}
			});

			if (tiers instanceof MenageCommun) {
				assignHistoriqueAddressesCiviles(tiersVisuView.getTiersPrincipal(),
				                                 tiersVisuView.isAdressesHistoCiviles(),
				                                 this::getAdressesHistoriquesCiviles,
				                                 tiersVisuView::setHistoriqueAdressesCiviles,
				                                 tiersVisuView::setExceptionAdresseCiviles);
				assignHistoriqueAddressesCiviles(tiersVisuView.getTiersConjoint(),
				                                 tiersVisuView.isAdressesHistoCivilesConjoint(),
				                                 this::getAdressesHistoriquesCiviles,
				                                 tiersVisuView::setHistoriqueAdressesCivilesConjoint,
				                                 tiersVisuView::setExceptionAdresseCivilesConjoint);
			}
			else if (tiers instanceof PersonnePhysique) {
				assignHistoriqueAddressesCiviles((PersonnePhysique) tiers,
				                                 tiersVisuView.isAdressesHistoCiviles(),
				                                 this::getAdressesHistoriquesCiviles,
				                                 tiersVisuView::setHistoriqueAdressesCiviles,
				                                 tiersVisuView::setExceptionAdresseCiviles);
			}
			else if (tiers instanceof Etablissement) {
				assignHistoriqueAddressesCiviles((Etablissement) tiers,
				                                 tiersVisuView.isAdressesHistoCiviles(),
				                                 this::getAdressesHistoriquesCiviles,
				                                 tiersVisuView::setHistoriqueAdressesCiviles,
				                                 tiersVisuView::setExceptionAdresseCiviles);
			}
			else if (tiers instanceof Entreprise) {
				assignHistoriqueAddressesCiviles((Entreprise) tiers,
				                                 tiersVisuView.isAdressesHistoCiviles(),
				                                 this::getAdressesHistoriquesCiviles,
				                                 tiersVisuView::setHistoriqueAdressesCiviles,
				                                 tiersVisuView::setExceptionAdresseCiviles);
			}
		}

		tiersVisuView.setNombreElementsTable(modeImpression ? 0 : 10);
		tiersVisuView.setForsPrincipauxPagines(forsPrincipauxPagines && !modeImpression);
		tiersVisuView.setForsSecondairesPagines(forsSecondairesPagines && !modeImpression);
		tiersVisuView.setAutresForsPagines(autresForsPagines && !modeImpression);
		return tiersVisuView;
	}

	@FunctionalInterface
	private interface HistoriqueAdressesCivilesCalculator<T extends Contribuable> {
		List<AdresseCivilView> get(T ctb) throws Exception;
	}

	private static <T extends Contribuable> void assignHistoriqueAddressesCiviles(T ctb,
	                                                                              boolean showHisto,
	                                                                              HistoriqueAdressesCivilesCalculator<? super T> adressesCivilesGetter,
	                                                                              Consumer<List<AdresseCivilView>> viewSetter,
	                                                                              Consumer<String> exceptionSetter) {
		if (ctb != null) {
			try {
				final List<AdresseCivilView> views = adressesCivilesGetter.get(ctb);
				final List<AdresseCivilView> kept;
				if (showHisto || views.isEmpty()) {
					kept = views;
				}
				else {
					kept = new ArrayList<>(views.size());
					for (AdresseCivilView view : views) {
						if (isAlwaysShown(view)) {
							kept.add(view);
						}
					}
				}
				viewSetter.accept(kept);
			}
			catch (Exception e) {
				exceptionSetter.accept(e.getMessage());
			}
		}
		else {
			viewSetter.accept(null);
		}
	}

	protected void setDecisionAciView(TiersView tiersView,Contribuable contribuable){
		final List<DecisionAciView> decisionsView = new ArrayList<>();
		final Set<DecisionAci> decisions = contribuable.getDecisionsAci();
		if (decisions != null) {
			for (DecisionAci decision : decisions) {
				final DecisionAciView dView = new DecisionAciView(decision);
				decisionsView.add(dView);
			}
			decisionsView.sort(new DecisionAciViewComparator());
		}
		tiersView.setDecisionsAci(decisionsView);
		tiersView.setDecisionRecente(tiersService.isSousInfluenceDecisions(contribuable));
	}

	/**
	 * Mise à jour de la vue MouvementDetailView
	 */
	private List<MouvementDetailView> getMouvements(Contribuable contribuable) throws ServiceInfrastructureException {

		final List<MouvementDetailView> mvtsView = new ArrayList<>();
		final Set<MouvementDossier> mvts = contribuable.getMouvementsDossier();
		for (MouvementDossier mvt : mvts) {
			if (mvt.getEtat().isTraite()) {
				final MouvementDetailView mvtView = mouvementVisuManager.getView(mvt, false);
				mvtsView.add(mvtView);
			}
		}
		Collections.sort(mvtsView);
		return mvtsView;
	}

	@Override
	@Transactional(readOnly = true)
	public void fillRapportsPrestationView(long noDebiteur, RapportsPrestationView view) {

		final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersDAO.get(noDebiteur);
		if (debiteur == null) {
			throw new TiersNotFoundException(noDebiteur);
		}

		final List<RapportsPrestationView.Rapport> rapports = new ArrayList<>();
		final Map<Long, List<RapportsPrestationView.Rapport>> rapportsByNumero = new HashMap<>();

		final long startRapports = System.nanoTime();

		final Set<RapportEntreTiers> list = debiteur.getRapportsObjet();

		// Rempli les informations de base

		for (RapportEntreTiers r : list) {
			if (r.getType() != TypeRapportEntreTiers.PRESTATION_IMPOSABLE) {
				continue;
			}

			final RapportPrestationImposable rpi = (RapportPrestationImposable) r;

			final RapportsPrestationView.Rapport rapport = new RapportsPrestationView.Rapport();
			rapport.id = r.getId();
			rapport.annule = r.isAnnule();
			rapport.noCTB = r.getSujetId();
			rapport.dateDebut = r.getDateDebut();
			rapport.dateFin = r.getDateFin();
			rapport.noCTB = rpi.getSujetId();
			rapports.add(rapport);

			ArrayList<RapportsPrestationView.Rapport> rl = (ArrayList<RapportsPrestationView.Rapport>) rapportsByNumero.get(rapport.noCTB);
			if (rl == null) {
				rl = new ArrayList<>();
				rapportsByNumero.put(rapport.noCTB, rl);
			}

			rl.add(rapport);
		}

		final long endRapports = System.nanoTime();
		LOGGER.debug("- chargement des rapports en " + ((endRapports - startRapports) / 1000000) + " ms");

		// Complète les noms, prénoms et nouveaux numéros AVS des non-habitants

		final long startNH = System.nanoTime();

		final Set<Long> pasDeNouveauNosAvs = new HashSet<>();

		final List infoNonHabitants = hibernateTemplate.find("select pp.numero, pp.prenomUsuel, pp.nom, pp.numeroAssureSocial from PersonnePhysique pp, RapportPrestationImposable rpi "
				                                                     + "where pp.habitant = false and pp.numero = rpi.sujetId and rpi.objetId =  " + noDebiteur, null);
		for (Object o : infoNonHabitants) {
			final Object line[] = (Object[]) o;
			final Long numero = (Long) line[0];
			final String prenom = (String) line[1];
			final String nom = (String) line[2];
			final String noAVS = (String) line[3];

			if (StringUtils.isBlank(noAVS)) {
				pasDeNouveauNosAvs.add(numero);
			}

			final List<RapportsPrestationView.Rapport> rl = rapportsByNumero.get(numero);
			Assert.notNull(rl);

			for (RapportsPrestationView.Rapport r : rl) {
				r.nomCourrier = Collections.singletonList(getNomPrenom(prenom, nom));
				r.noAVS = FormatNumeroHelper.formatNumAVS(noAVS);
			}
		}

		// Complète les anciens numéros AVS des non-habitants qui n'en possède pas des nouveaux

		if (!pasDeNouveauNosAvs.isEmpty()) {
			final StandardBatchIterator<Long> it = new StandardBatchIterator<>(pasDeNouveauNosAvs, 500);
			while (it.hasNext()) {
				final List<Long> ids = it.next();

				final List<Object[]> ancienNosAvs = hibernateTemplate.execute(new HibernateCallback<List<Object[]>>() {
					@Override
					public List<Object[]> doInHibernate(Session session) throws HibernateException, SQLException {
						final Query query = session.createQuery(
								"select ip.personnePhysique.id, ip.identifiant from IdentificationPersonne ip where ip.categorieIdentifiant = 'CH_AHV_AVS' and ip.personnePhysique.id in (:ids)");
						query.setParameterList("ids", ids);
						//noinspection unchecked
						return query.list();
					}
				});

				for (Object[] line : ancienNosAvs) {
					final Long numero = (Long) line[0];
					final String noAVS = (String) line[1];

					final List<RapportsPrestationView.Rapport> rl = rapportsByNumero.get(numero);
					Assert.notNull(rl);

					for (RapportsPrestationView.Rapport r : rl) {
						r.noAVS = FormatNumeroHelper.formatAncienNumAVS(noAVS);
					}
				}
			}
		}

		final long endNH = System.nanoTime();
		LOGGER.debug("- chargement des non-habitants en " + ((endNH - startNH) / 1000000) + " ms");

		// Complète les noms, prénoms et numéros AVS des habitants

		final long startH = System.nanoTime();

		final Map<Long, List<RapportsPrestationView.Rapport>> rapportsByNumeroIndividu = new HashMap<>();

		final List infoHabitants = hibernateTemplate.find("select pp.numero, pp.numeroIndividu from PersonnePhysique pp, RapportPrestationImposable rpi "
				                                                  + "where pp.habitant = true and pp.numero = rpi.sujetId and rpi.objetId =  " + noDebiteur, null);
		for (Object o : infoHabitants) {
			final Object line[] = (Object[]) o;
			final Long numero = (Long) line[0];
			final Long numeroIndividu = (Long) line[1];

			ArrayList<RapportsPrestationView.Rapport> rl = (ArrayList<RapportsPrestationView.Rapport>) rapportsByNumeroIndividu.get(numeroIndividu);
			if (rl == null) {
				rl = new ArrayList<>();
				rapportsByNumeroIndividu.put(numeroIndividu, rl);
			}

			rl.addAll(rapportsByNumero.get(numero));
		}

		final Set<Long> numerosIndividus = rapportsByNumeroIndividu.keySet();
		final StandardBatchIterator<Long> iterator = new StandardBatchIterator<>(numerosIndividus, 500);
		while (iterator.hasNext()) {
			final List<Long> batch = iterator.next();
			try {
				final List<Individu> individus = serviceCivilService.getIndividus(batch, null);
				for (Individu ind : individus) {
					final List<RapportsPrestationView.Rapport> rl = rapportsByNumeroIndividu.get(ind.getNoTechnique());
					Assert.notNull(rl);
					for (RapportsPrestationView.Rapport rapport : rl) {
						rapport.nomCourrier = Collections.singletonList(serviceCivilService.getNomPrenom(ind));
						rapport.noAVS = getNumeroAvs(ind);
					}
				}
			}
			catch (ServiceCivilException e) {
				LOGGER.debug("Impossible de charger le lot d'individus [" + batch + "], on continue un-par-un. L'erreur est : " + e.getMessage());
				// on recommence, un-par-un
				for (Long numero : batch) {
					try {
						Individu ind = serviceCivilService.getIndividu(numero, null);
						if (ind != null) {
							final List<RapportsPrestationView.Rapport> rl = rapportsByNumeroIndividu.get(ind.getNoTechnique());
							Assert.notNull(rl);
							for (RapportsPrestationView.Rapport rapport : rl) {
								rapport.nomCourrier = Collections.singletonList(serviceCivilService.getNomPrenom(ind));
								rapport.noAVS = getNumeroAvs(ind);
							}
						}
					}
					catch (ServiceCivilException ex) {
						LOGGER.warn("Impossible de charger l'individu [" + numero + "]. L'erreur est : " + ex.getMessage(), ex);
						// on affiche le message d'erreur directement dans la page, pour éviter qu'il soit perdu
						final List<RapportsPrestationView.Rapport> rl = rapportsByNumeroIndividu.get(numero);
						Assert.notNull(rl);
						for (RapportsPrestationView.Rapport rapport : rl) {
							rapport.nomCourrier = Collections.singletonList("##erreur## : " + ex.getMessage());
							rapport.noAVS = "##erreur##";
						}
					}
				}
			}
		}

		final long endH = System.nanoTime();
		LOGGER.debug("- chargement des habitants en " + ((endH - startH) / 1000000) + " ms");

		view.idDpi = noDebiteur;
		view.tiersGeneral = tiersGeneralManager.getDebiteur(debiteur, true);
		view.editionAllowed = SecurityHelper.isGranted(securityProvider, Role.RT);
		view.rapports = rapports;
	}

	private String getNumeroAvs(Individu ind) {
		final String noAVS;
		if (StringUtils.isBlank(ind.getNouveauNoAVS())) {
			noAVS = FormatNumeroHelper.formatAncienNumAVS(ind.getNoAVS11());
		}
		else {
			noAVS = FormatNumeroHelper.formatNumAVS(ind.getNouveauNoAVS());
		}
		return noAVS;
	}

	private String getNomPrenom(String prenom, String nom) {
		final String nomPrenom;
		if (nom != null && prenom != null) {
			nomPrenom = String.format("%s %s", prenom, nom);
		}
		else if (nom != null) {
			nomPrenom = nom;
		}
		else if (prenom != null) {
			nomPrenom = prenom;
		}
		else {
			nomPrenom = "";
		}
		return nomPrenom;
	}

	/**
	 * Compte le nombre de rapports prestation imposable pour un débiteur
	 */
	@Transactional(readOnly = true)
	public int countRapportsPrestationImposable(Long numeroDebiteur, boolean rapportsPrestationHisto) {
		return rapportEntreTiersDAO.countRapportsPrestationImposable(numeroDebiteur, !rapportsPrestationHisto);
	}
}
