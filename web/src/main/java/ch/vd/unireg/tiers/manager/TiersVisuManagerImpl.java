package ch.vd.unireg.tiers.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.AdressesFiscalesHisto;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.common.pagination.WebParamPagination;
import ch.vd.unireg.decision.aci.DecisionAciViewComparator;
import ch.vd.unireg.di.view.DeclarationImpotListView;
import ch.vd.unireg.interfaces.InterfaceDataException;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.mouvement.MouvementDossier;
import ch.vd.unireg.mouvement.view.MouvementDetailView;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.DecisionAci;
import ch.vd.unireg.tiers.DecisionAciView;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.HistoFlag;
import ch.vd.unireg.tiers.HistoFlags;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.view.AdresseCivilView;
import ch.vd.unireg.tiers.view.AdresseView;
import ch.vd.unireg.tiers.view.TiersView;
import ch.vd.unireg.tiers.view.TiersVisuView;

/**
 * Service qui fournit les methodes pour visualiser un tiers
 *
 * @author xcifde
 */
public class TiersVisuManagerImpl extends TiersManager implements TiersVisuManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(TiersVisuManagerImpl.class);

	private MouvementVisuManager mouvementVisuManager;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setMouvementVisuManager(MouvementVisuManager mouvementVisuManager) {
		this.mouvementVisuManager = mouvementVisuManager;
	}

	@Override
	@Transactional(readOnly = true)
	public TiersVisuView getView(Long numero, HistoFlags histoFlags,
	                             boolean modeImpression, boolean forsPrincipauxPagines, boolean forsSecondairesPagines, boolean autresForsPagines, WebParamPagination webParamPagination)
			throws AdresseException, InfrastructureException {

		final TiersVisuView tiersVisuView = new TiersVisuView(histoFlags);

		final Tiers tiers = getTiersDAO().get(numero);
		if (tiers == null) {
			throw new TiersNotFoundException(numero);
		}

		setTiersGeneralView(tiersVisuView, tiers);
		tiersVisuView.setComplement(buildComplement(tiers, histoFlags.hasHistoFlag(HistoFlag.COORDONNEES_FINANCIERES)));

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
				tiersVisuView.setDis(new DeclarationImpotListView(contribuable, serviceInfrastructureService, messageHelper).getDis());
				tiersVisuView.setMouvements(getMouvements(contribuable));
				setForsFiscaux(tiersVisuView, contribuable);
				setDecisionAciView(tiersVisuView,contribuable);
				setMandataires(tiersVisuView, contribuable);

				// initialisation des collections pour éviter les lazy-init exceptions dans la JSP
				initLazyCollections(contribuable);

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

				// initialisation des collections pour éviter les lazy-init exceptions dans la JSP
				initLazyCollections(tiersVisuView.getTiersPrincipal());
				initLazyCollections(tiersVisuView.getTiersConjoint());

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

	/**
	 * Workaround temporaire pour initialiser les collections lazy d'hibernate référencées directement depuis les JSP. Normalement,
	 * les entités hibernate ne devraient jamais être exposées dans les JSP. La bonne manière de faire serait donc de ne plus exposer
	 * les tiers/tiersPrincipal/tiersConjoint dans la classe TiersVisuView.
	 *
	 * @param ctb un contribuable
	 */
	private static void initLazyCollections(@Nullable Contribuable ctb) {
		if (ctb != null) {
			ctb.getIdentificationsEntreprise().size();
		}
		if (ctb instanceof PersonnePhysique) {
			((PersonnePhysique) ctb).getIdentificationsPersonnes().size();
		}
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
	private List<MouvementDetailView> getMouvements(Contribuable contribuable) throws InfrastructureException {

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

	/**
	 * Compte le nombre de rapports prestation imposable pour un débiteur
	 */
	@Transactional(readOnly = true)
	public int countRapportsPrestationImposable(Long numeroDebiteur, boolean rapportsPrestationHisto) {
		return rapportEntreTiersDAO.countRapportsPrestationImposable(numeroDebiteur, !rapportsPrestationHisto);
	}
}
