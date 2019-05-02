package ch.vd.unireg.tiers.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseGenerique;
import ch.vd.unireg.adresse.AdresseGenerique.SourceType;
import ch.vd.unireg.adresse.AdresseMandataire;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.AdresseServiceImpl;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.adresse.AdresseTiersDAO;
import ch.vd.unireg.adresse.AdressesFiscalesHisto;
import ch.vd.unireg.adresse.AdressesResolutionException;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.DonneesCivilesException;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.NpaEtLocalite;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.RueEtNumero;
import ch.vd.unireg.common.pagination.WebParamPagination;
import ch.vd.unireg.decision.aci.DecisionAciViewComparator;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.declaration.view.QuestionnaireSNCView;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscal;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscalAvecSuivi;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscalView;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscalViewFactory;
import ch.vd.unireg.entreprise.EntrepriseService;
import ch.vd.unireg.etiquette.EtiquetteTiers;
import ch.vd.unireg.evenement.ide.ServiceIDEService;
import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.general.manager.TiersGeneralManager;
import ch.vd.unireg.general.view.TiersGeneralView;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.individu.IndividuView;
import ch.vd.unireg.individu.WebCivilService;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.entreprise.ServiceEntrepriseException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.EntiteOFS;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.model.AdressesCivilesHisto;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.ServiceEntreprise;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.lr.view.ListeRecapitulativeView;
import ch.vd.unireg.lr.view.ListesRecapitulativesView;
import ch.vd.unireg.mandataire.AccesMandatairesView;
import ch.vd.unireg.mandataire.ConfigurationMandataire;
import ch.vd.unireg.mandataire.MandataireCourrierView;
import ch.vd.unireg.mandataire.MandatairePerceptionView;
import ch.vd.unireg.mandataire.MandataireViewHelper;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.metier.bouclement.ExerciceCommercial;
import ch.vd.unireg.metier.bouclement.ExerciceCommercialHelper;
import ch.vd.unireg.rapport.RapportHelper;
import ch.vd.unireg.rapport.SensRapportEntreTiers;
import ch.vd.unireg.rapport.TypeRapportEntreTiersWeb;
import ch.vd.unireg.rapport.view.RapportView;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.registrefoncier.SituationRF;
import ch.vd.unireg.rt.view.RapportPrestationView;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.situationfamille.SituationFamilleService;
import ch.vd.unireg.situationfamille.VueSituationFamille;
import ch.vd.unireg.situationfamille.VueSituationFamilleMenageCommun;
import ch.vd.unireg.tiers.ActiviteEconomique;
import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.ContactImpotSource;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.DecisionAci;
import ch.vd.unireg.tiers.DecisionAciView;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.FlagEntreprise;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.IndividuNotFoundException;
import ch.vd.unireg.tiers.Mandat;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.RapportEntreTiersDAO;
import ch.vd.unireg.tiers.RapportEntreTiersKey;
import ch.vd.unireg.tiers.RapportPrestationImposable;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.tiers.RepresentationLegale;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.TiersWebHelper;
import ch.vd.unireg.tiers.view.AdresseCivilView;
import ch.vd.unireg.tiers.view.AdresseCivilViewComparator;
import ch.vd.unireg.tiers.view.AdresseView;
import ch.vd.unireg.tiers.view.AdresseViewComparator;
import ch.vd.unireg.tiers.view.AllegementFiscalView;
import ch.vd.unireg.tiers.view.CommuneView;
import ch.vd.unireg.tiers.view.ComplementView;
import ch.vd.unireg.tiers.view.DebiteurView;
import ch.vd.unireg.tiers.view.EtiquetteTiersView;
import ch.vd.unireg.tiers.view.FlagEntrepriseView;
import ch.vd.unireg.tiers.view.ForFiscalView;
import ch.vd.unireg.tiers.view.LogicielView;
import ch.vd.unireg.tiers.view.PeriodiciteView;
import ch.vd.unireg.tiers.view.PeriodiciteViewComparator;
import ch.vd.unireg.tiers.view.RegimeFiscalView;
import ch.vd.unireg.tiers.view.SituationFamilleView;
import ch.vd.unireg.tiers.view.TiersEditView;
import ch.vd.unireg.tiers.view.TiersView;
import ch.vd.unireg.tiers.view.TiersVisuView;
import ch.vd.unireg.type.PeriodeDecompte;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeMandat;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * Methodes annexes utilisées par TiersVisuManager et TiersEditManager
 *
 * @author xcifde
 */
public class TiersManager implements MessageSourceAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(TiersManager.class);

	private static final List<TypeAdresseCivil> TYPES_ADRESSES_CIVILES = Arrays.asList(TypeAdresseCivil.COURRIER, TypeAdresseCivil.PRINCIPALE, TypeAdresseCivil.SECONDAIRE, TypeAdresseCivil.TUTEUR);

	private WebCivilService webCivilService;

	private EntrepriseService entrepriseService;

	protected ServiceCivilService serviceCivilService;

	protected ServiceEntreprise serviceEntreprise;

	protected TiersDAO tiersDAO;

	private AdresseTiersDAO adresseTiersDAO;

	protected TiersService tiersService;

	protected ServiceInfrastructureService serviceInfrastructureService;

	private AdresseManager adresseManager;

	protected TiersGeneralManager tiersGeneralManager;

	protected AdresseService adresseService;

	protected ConfigurationMandataire configurationMandataire;

	protected MessageSource messageSource;

	protected SituationFamilleService situationFamilleService;

	protected RapportEntreTiersDAO rapportEntreTiersDAO;
	protected IbanValidator ibanValidator;
	private AutorisationManager autorisationManager;
	protected SecurityProviderInterface securityProvider;

	protected ExerciceCommercialHelper exerciceCommercialHelper;

	protected ServiceIDEService serviceIDEService;

	protected RegistreFoncierService registreFoncierService;
	protected MessageHelper messageHelper;

	/**
	 * Recupere l'individu correspondant au tiers
	 */
	protected IndividuView getIndividuView(PersonnePhysique habitant) {

		IndividuView individuView = null;
		Long noIndividu = habitant.getNumeroIndividu();
		if (noIndividu != null) {
			individuView = webCivilService.getIndividu(noIndividu);
		}
		if (habitant.getDateDeces() != null && individuView != null) {//habitant décédé fiscalement
			individuView.setEtatCivil("DECEDE");
			individuView.setDateDernierChgtEtatCivil(habitant.getDateDeces());
		}
		return individuView;
	}

	/**
	 * Alimente List&lt;EtiquetteTiersView&gt;
	 */
	protected List<EtiquetteTiersView> getEtiquettes(Tiers tiers, boolean histo) {
		final List<EtiquetteTiersView> views;
		final Set<EtiquetteTiers> db = tiers.getEtiquettes();
		if (db == null || db.isEmpty()) {
			views = Collections.emptyList();
		}
		else {
			views = db.stream()
					.filter(e -> histo || isAlwaysShown(e))
					.map(EtiquetteTiersView::new)
					.sorted(new AnnulableHelper.AnnulesApresWrappingComparator<>(Comparator.comparing(EtiquetteTiersView::getDateDebut).reversed()))
					.collect(Collectors.toList());
		}
		return views;
	}

	/**
	 * Alimente Set&lt;DebiteurView&gt;
	 */
	protected Set<DebiteurView> getDebiteurs(Contribuable contribuable) throws AdresseException {

		final Set<DebiteurView> debiteursView = new HashSet<>();

		final Set<RapportEntreTiers> rapports = contribuable.getRapportsSujet();
		if (rapports != null) {
			for (RapportEntreTiers r : rapports) {
				if (r instanceof ContactImpotSource) {
					final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(r.getObjetId());
					final DebiteurView debiteurView = new DebiteurView();
					debiteurView.setAnnule(r.isAnnule());
					debiteurView.setId(r.getId());
					debiteurView.setNumero(dpi.getNumero());
					debiteurView.setCategorieImpotSource(dpi.getCategorieImpotSource());
					debiteurView.setPersonneContact(dpi.getPersonneContact());
					debiteurView.setSensRapportEntreTiers(SensRapportEntreTiers.OBJET);
					try {
						final List<String> nomCourrier = getAdresseService().getNomCourrier(dpi, null, false);
						debiteurView.setNomCourrier(nomCourrier);
					}
					catch (ServiceEntrepriseException e) {
						debiteurView.setNomCourrier(Collections.singletonList("<erreur lors de l'accès au service civil>"));
					}
					debiteursView.add(debiteurView);
				}
			}
		}
		return debiteursView;
	}

	/**
	 * Alimente Set&lt;DecisionAciView&gt;
	 */
	protected void setDecisionAciView(TiersView tiersView, Contribuable contribuable) {
		final List<DecisionAciView> decisionsView = new ArrayList<>();
		final Set<DecisionAci> decisions = contribuable.getDecisionsAci();
		if (decisions != null) {
			for (DecisionAci decision : decisions) {
				final DecisionAciView dView = new DecisionAciView(decision);
				decisionsView.add(dView);
			}
			decisionsView.sort(new DecisionAciViewComparator());
			tiersView.setDecisionsAci(decisionsView);
			tiersView.setDecisionRecente(tiersService.isSousInfluenceDecisions(contribuable));
		}

	}

	/**
	 * Alimente List&lt;RapportView&gt;
	 */
	protected List<RapportView> getRapportsEtablissements(Tiers tiers) throws AdresseException {
		final List<RapportView> rapportsView = new ArrayList<>();

		// Rapport entre tiers Sujet
		for (RapportEntreTiers rapportEntreTiers : tiers.getRapportsSujet()) {
			if (rapportEntreTiers.getType() == TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE) {
				final RapportView rapportView = new RapportView();
				rapportView.setId(rapportEntreTiers.getId());
				rapportView.setAnnule(rapportEntreTiers.isAnnule());
				rapportView.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);
				rapportView.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.fromCore(rapportEntreTiers.getType()));
				rapportView.setDateDebut(rapportEntreTiers.getDateDebut());
				rapportView.setDateFin(rapportEntreTiers.getDateFin());

				if (((ActiviteEconomique) rapportEntreTiers).isPrincipal()) {
					rapportView.setActiviteEconomiquePrincipale(true);
				}

				final Tiers tiersObjet = tiersDAO.get(rapportEntreTiers.getObjetId());
				rapportView.setNumero(tiersObjet.getNumero());

				if (tiersObjet instanceof Etablissement && !rapportView.isActiviteEconomiquePrincipale()
						&& !rapportView.isAnnule() && ((Etablissement) tiersObjet).getNumeroEtablissement() == null) {
					rapportView.setEtablissementAnnulable(true);
				}

				List<String> nomObjet;
				try {
					nomObjet = adresseService.getNomCourrier(tiersObjet, null, false);
				}
				catch (Exception e) {
					nomObjet = new ArrayList<>();
					nomObjet.add(e.getMessage());
				}
				rapportView.setNomCourrier(nomObjet);

				final String toolTipMessage = TiersWebHelper.getRapportEntreTiersTooltips(rapportEntreTiers, adresseService, tiersService, messageHelper);
				rapportView.setToolTipMessage(toolTipMessage);

				rapportsView.add(rapportView);
			}
		}
		Collections.sort(rapportsView);
		return rapportsView;
	}

	/**
	 * Alimente List&lt;RapportView&gt;
	 */
	protected List<RapportView> getRapports(Tiers tiers) throws AdresseException {
		final List<RapportView> rapportsView = new ArrayList<>();

		// Rapport entre tiers Objet
		for (RapportEntreTiers rapportEntreTiers : tiers.getRapportsObjet()) {
			final RapportEntreTiersKey key = new RapportEntreTiersKey(rapportEntreTiers.getType(), RapportEntreTiersKey.Source.OBJET);
			if (RapportHelper.ALLOWED_VISU_COMPLETE.contains(key)) {
				rapportsView.add(new RapportView(rapportEntreTiers, SensRapportEntreTiers.OBJET, tiersService, adresseService, messageHelper));
			}
		}

		// Rapport entre tiers Sujet
		for (RapportEntreTiers rapportEntreTiers : tiers.getRapportsSujet()) {
			final RapportEntreTiersKey key = new RapportEntreTiersKey(rapportEntreTiers.getType(), RapportEntreTiersKey.Source.SUJET);
			if (RapportHelper.ALLOWED_VISU_COMPLETE.contains(key)) {
				final RapportView rapportView = new RapportView(rapportEntreTiers, SensRapportEntreTiers.SUJET, tiersService, adresseService, messageHelper);
				rapportsView.add(rapportView);
			}
		}
		Collections.sort(rapportsView);
		return rapportsView;
	}

	protected void setNomAutoriteTutelaire(RapportEntreTiers rapportEntreTiers, RapportView rapportView) {
		final RepresentationLegale representationLegale = (RepresentationLegale) rapportEntreTiers;
		Long numeroTiersAutoriteTutelaire = representationLegale.getAutoriteTutelaireId();
		if (numeroTiersAutoriteTutelaire != null) {
			CollectiviteAdministrative autoriteTutelaire = (CollectiviteAdministrative) tiersDAO.get(numeroTiersAutoriteTutelaire);
			if (autoriteTutelaire != null) {
				String nom = tiersService.getNomCollectiviteAdministrative(autoriteTutelaire);
				rapportView.setNomAutoriteTutelaire(nom);
			}
		}
	}


	/**
	 * Alimente List&lt;RapportView&gt;
	 */
	protected void setContribuablesAssocies(TiersView tiersView, DebiteurPrestationImposable debiteur, boolean ctbAssocieHisto) throws AdresseException {
		final List<RapportView> rapportsView = new ArrayList<>();

		// Rapport entre tiers Objet
		final Set<RapportEntreTiers> rapports = debiteur.getRapportsObjet();
		for (RapportEntreTiers rapportEntreTiers : rapports) {
			if (rapportEntreTiers.getType() == TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE) {
				final RapportView rapportView = new RapportView();
				rapportView.setId(rapportEntreTiers.getId());
				rapportView.setAnnule(rapportEntreTiers.isAnnule());
				rapportView.setDateDebut(rapportEntreTiers.getDateDebut());
				rapportView.setDateFin(rapportEntreTiers.getDateFin());
				final Tiers tiersSujet = tiersDAO.get(rapportEntreTiers.getSujetId());
				rapportView.setNumero(tiersSujet.getNumero());
				final List<String> nomCourrier = getAdresseService().getNomCourrier(tiersSujet, null, false);
				rapportView.setNomCourrier(nomCourrier);
				final String toolTipMessage = TiersWebHelper.getRapportEntreTiersTooltips(rapportEntreTiers, adresseService, tiersService, messageHelper);
				rapportView.setToolTipMessage(toolTipMessage);
				boolean accepterRapportHistorique = isRapportHistorique(rapportEntreTiers) && ctbAssocieHisto;
				if (accepterRapportHistorique || !isRapportHistorique(rapportEntreTiers)) {
					rapportsView.add(rapportView);
				}

			}
		}
		tiersView.setContribuablesAssocies(rapportsView);
	}

	private boolean isRapportHistorique(RapportEntreTiers r) {
		return r.getDateFin() != null || r.isAnnule();
	}


	protected void setLogicielView(TiersView tiersView, DebiteurPrestationImposable debiteur) {
		final Long logicielId = debiteur.getLogicielId();
		if (logicielId != null) {
			final Logiciel logiciel = serviceInfrastructureService.getLogiciel(logicielId);
			if (logiciel != null) {
				tiersView.setLogiciel(new LogicielView(logiciel));
			}
		}
	}

	/**
	 * Alimente List&lt;RapportPrestationView&gt;
	 */
	protected List<RapportPrestationView> getRapportsPrestation(DebiteurPrestationImposable dpi, WebParamPagination pagination, boolean rapportsPrestationHisto) throws AdresseException {

		final List<RapportPrestationView> rapportPrestationViews = new ArrayList<>();

		final List<RapportPrestationImposable> rapports = rapportEntreTiersDAO.getRapportsPrestationImposable(dpi.getNumero(), pagination, !rapportsPrestationHisto);
		for (RapportPrestationImposable rapport : rapports) {
			final RapportPrestationView rapportPrestationView = new RapportPrestationView();
			rapportPrestationView.setId(rapport.getId());
			rapportPrestationView.setAnnule(rapport.isAnnule());
			rapportPrestationView.setDateDebut(rapport.getDateDebut());
			rapportPrestationView.setDateFin(rapport.getDateFin());
			final Tiers ctb = tiersDAO.get(rapport.getSujetId());
			try {
				if (ctb instanceof PersonnePhysique) {
					final PersonnePhysique pp = (PersonnePhysique) ctb;
					final String nouveauNumeroAvs = tiersService.getNumeroAssureSocial(pp);
					if (StringUtils.isNotBlank(nouveauNumeroAvs)) {
						rapportPrestationView.setNumeroAVS(FormatNumeroHelper.formatNumAVS(nouveauNumeroAvs));
					}
					else {
						final String ancienNumeroAvs = tiersService.getAncienNumeroAssureSocial(pp);
						rapportPrestationView.setNumeroAVS(FormatNumeroHelper.formatAncienNumAVS(ancienNumeroAvs));
					}
				}
				final List<String> nomCourrier = adresseService.getNomCourrier(ctb, null, false);
				rapportPrestationView.setNomCourrier(nomCourrier);
			}
			catch (IndividuNotFoundException | ServiceCivilException e) {
				LOGGER.error("Impossible d'obtenir les informations civiles du contribuable " + ctb.getNumero(), e);
			}
			rapportPrestationView.setNumero(ctb.getNumero());
			rapportPrestationViews.add(rapportPrestationView);
		}
		return rapportPrestationViews;
	}


	/**
	 * Alimente Set&lt;ListeRecapitulativeView&gt;
	 */
	private List<ListeRecapitulativeView> getListesRecapitulatives(DebiteurPrestationImposable dpi) {
		final ListesRecapitulativesView globalView = new ListesRecapitulativesView(dpi);
		return globalView.getLrs();
	}

	/**
	 * Met a jour la vue en fonction de l'habitant
	 */
	protected void setHabitant(TiersView tiersView, PersonnePhysique habitant) {
		IndividuView individu = getIndividuView(habitant);
		if (individu != null) {
			tiersView.setTiers(habitant);
			tiersView.setIndividu(individu);
		}
	}

	/**
	 * Met a jour la vue en fonction du menage commun
	 */
	protected void setMenageCommun(TiersView tiersView, MenageCommun menageCommun) {
		tiersView.setTiers(menageCommun);

		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menageCommun, null);
		final PersonnePhysique tiersPrincipal = ensemble.getPrincipal();
		final PersonnePhysique tiersConjoint = ensemble.getConjoint();

		/* 1er tiers */
		if (tiersPrincipal != null) {
			tiersView.setTiersPrincipal(tiersPrincipal);
			tiersView.setNomPrenomPrincipal(tiersService.getNomPrenom(tiersPrincipal));

			if (tiersPrincipal.isConnuAuCivil()) {
				IndividuView individu = getIndividuView(tiersPrincipal);
				tiersView.setIndividu(individu);
			}

			// étiquettes (seulement en visualisation)
			if (tiersView instanceof TiersVisuView) {
				tiersView.setEtiquettes(getEtiquettes(tiersPrincipal, ((TiersVisuView) tiersView).isLabelsHisto()));
			}
		}

		/* 2eme tiers */
		if (tiersConjoint != null) {
			tiersView.setTiersConjoint(tiersConjoint);
			tiersView.setNomPrenomConjoint(tiersService.getNomPrenom(tiersConjoint));

			if (tiersConjoint.isConnuAuCivil()) {
				IndividuView individu = getIndividuView(tiersConjoint);
				tiersView.setIndividuConjoint(individu);
			}

			// étiquettes (seulement en visualisation)
			if (tiersView instanceof TiersVisuView) {
				tiersView.setEtiquettesConjoint(getEtiquettes(tiersConjoint, ((TiersVisuView) tiersView).isLabelsConjointHisto()));
			}
		}
	}

	protected void setMandataires(TiersVisuView tiersView, Contribuable contribuable) {

		final List<MandataireCourrierView> mandatairesCourrier = new LinkedList<>();
		final List<MandatairePerceptionView> mandatairesPerception = new LinkedList<>();

		final AccesMandatairesView accesMandataire = new AccesMandatairesView(contribuable, configurationMandataire, serviceInfrastructureService);
		if (accesMandataire.hasAnything()) {

			// les liens vers les mandataires
			final Set<RapportEntreTiers> rapportsSujet = contribuable.getRapportsSujet();
			if (rapportsSujet != null && !rapportsSujet.isEmpty()) {
				for (RapportEntreTiers ret : rapportsSujet) {
					if (ret instanceof Mandat) {
						final Mandat mandat = (Mandat) ret;
						switch (mandat.getTypeMandat()) {
						case TIERS:
							if (accesMandataire.hasTiersPerception() && (tiersView.isMandatairesPerceptionHisto() || isAlwaysShown(mandat))) {
								mandatairesPerception.add(new MandatairePerceptionView(mandat, tiersService));
							}
							break;
						case GENERAL:
							if (accesMandataire.hasGeneral() && (tiersView.isMandatairesCourrierHisto() || isAlwaysShown(mandat))) {
								mandatairesCourrier.add(new MandataireCourrierView(mandat, tiersService, serviceInfrastructureService));
							}
							break;
						case SPECIAL:
							if (accesMandataire.hasSpecial(mandat.getCodeGenreImpot()) && (tiersView.isMandatairesCourrierHisto() || isAlwaysShown(mandat))) {
								mandatairesCourrier.add(new MandataireCourrierView(mandat, tiersService, serviceInfrastructureService));
							}
							break;
						}
					}
				}
			}

			// les adresses mandataires
			final Set<AdresseMandataire> adressesMandataires = contribuable.getAdressesMandataires();
			if (adressesMandataires != null && !adressesMandataires.isEmpty()) {
				for (AdresseMandataire adresse : adressesMandataires) {
					if ((adresse.getTypeMandat() == TypeMandat.GENERAL && accesMandataire.hasGeneral()) || (adresse.getTypeMandat() == TypeMandat.SPECIAL && accesMandataire.hasSpecial(adresse.getCodeGenreImpot()))) {
						if (tiersView.isMandatairesCourrierHisto() || isAlwaysShown(adresse)) {
							mandatairesCourrier.add(new MandataireCourrierView(adresse, serviceInfrastructureService));
						}
					}
				}
			}

			// tri et transfert dans la vue globale
			mandatairesCourrier.sort(MandataireViewHelper.COURRIER_COMPARATOR);
			mandatairesPerception.sort(MandataireViewHelper.BASIC_COMPARATOR);
		}

		tiersView.setMandatairesCourrier(mandatairesCourrier);
		tiersView.setMandatairesPerception(mandatairesPerception);
		tiersView.setAccesMandataire(accesMandataire);
	}

	/**
	 * Détermine si un élément annulable et localisable dans le temps est visible toujours (= true) ou seulement si l'historique est activé (= false)
	 *
	 * @param data élément à tester
	 * @param <T>  type de cet élément
	 * @return <code>true</code> si l'élément doit être toujours affiché, <code>false</code> s'il ne doit l'être
	 */
	protected static <T extends Annulable & DateRange> boolean isAlwaysShown(T data) {
		return !data.isAnnule() && RegDateHelper.isAfterOrEqual(data.getDateFin(), RegDate.get(), NullDateBehavior.LATEST);
	}

	/**
	 * Met a jour la vue en fonction de l'entreprise
	 */
	protected void setEntreprise(TiersVisuView tiersView, Entreprise entreprise) {
		tiersView.setTiers(entreprise);

		// map des régimes fiscaux existants indexés par code
		final List<TypeRegimeFiscal> typesRegime = serviceInfrastructureService.getRegimesFiscaux();
		final Map<String, TypeRegimeFiscal> mapRegimesParCode = new HashMap<>(typesRegime.size());
		for (TypeRegimeFiscal type : typesRegime) {
			mapRegimesParCode.put(type.getCode(), type);
		}

		// les régimes fiscaux
		final Set<RegimeFiscal> regimes = entreprise.getRegimesFiscaux();
		if (regimes != null) {
			final List<RegimeFiscalView> vd = new ArrayList<>(regimes.size());
			final List<RegimeFiscalView> ch = new ArrayList<>(regimes.size());
			for (RegimeFiscal regime : regimes) {
				final RegimeFiscalView rfView = new RegimeFiscalView(regime.getId(), regime.isAnnule(), regime.getDateDebut(), regime.getDateFin(), mapRegimesParCode.get(regime.getCode()));
				if (regime.getPortee() == RegimeFiscal.Portee.VD) {
					if (tiersView.isRegimesFiscauxVDHisto() || isAlwaysShown(regime)) {
						vd.add(rfView);
					}
				}
				else if (regime.getPortee() == RegimeFiscal.Portee.CH) {
					if (tiersView.isRegimesFiscauxCHHisto() || isAlwaysShown(regime)) {
						ch.add(rfView);
					}
				}
				else {
					throw new IllegalArgumentException("Portée inconnue sur un régime fiscal : " + regime.getPortee());
				}
			}

			final Comparator<RegimeFiscalView> comparator = new AnnulableHelper.AnnulableDateRangeComparator<>(true);
			vd.sort(comparator);
			ch.sort(comparator);

			tiersView.setRegimesFiscauxVD(vd);
			tiersView.setRegimesFiscauxCH(ch);
		}

		// les allègements fiscaux
		final Set<AllegementFiscal> allegements = entreprise.getAllegementsFiscaux();
		if (allegements != null) {
			final List<AllegementFiscalView> views = new ArrayList<>(allegements.size());
			for (AllegementFiscal af : allegements) {
				if (tiersView.isAllegementsFiscauxHisto() || isAlwaysShown(af)) {
					final AllegementFiscalView afView = new AllegementFiscalView(af);
					views.add(afView);
				}
			}

			views.sort(AllegementFiscalView.DEFAULT_COMPARATOR);
			tiersView.setAllegementsFiscaux(views);
		}

		// les exercices commerciaux
		final List<ExerciceCommercial> exercices = exerciceCommercialHelper.getExercicesCommerciauxExposables(entreprise);
		tiersView.setExercicesCommerciaux(exercices);
		//Informations de bouclement et de d'exercice commercial renseigné
		tiersView.setBouclementsRenseignes(entreprise.hasBouclements());
		tiersView.setDateDebutPremierExerciceCommercial(entreprise.getDateDebutPremierExerciceCommercial());

		// les flags d'entreprise
		final Set<FlagEntreprise> flags = entreprise.getFlags();
		if (flags != null) {
			final List<FlagEntrepriseView> views = new ArrayList<>(flags.size());
			for (FlagEntreprise flag : flags) {
				if (tiersView.isFlagsEntrepriseHisto(flag.getType().getGroupe()) || isAlwaysShown(flag)) {
					final FlagEntrepriseView view = new FlagEntrepriseView(flag);
					views.add(view);
				}
			}

			views.sort(new AnnulableHelper.AnnulableDateRangeComparator<>(true));
			tiersView.setFlags(views);
		}

		// les questionnaires SNC
		final List<QuestionnaireSNC> qsnc = entreprise.getDeclarationsTriees(QuestionnaireSNC.class, true);
		if (!qsnc.isEmpty()) {
			final List<QuestionnaireSNCView> views = new ArrayList<>(qsnc.size());
			for (QuestionnaireSNC q : qsnc) {
				views.add(new QuestionnaireSNCView(q, serviceInfrastructureService, messageHelper));
			}
			views.sort(Comparator.comparing(QuestionnaireSNCView::getDateDebut, NullDateBehavior.EARLIEST::compare).reversed());
			tiersView.setQuestionnairesSNC(views);
		}

		// les autres documents fiscaux
		final Set<AutreDocumentFiscal> autresDocuments = entreprise.getAutresDocumentsFiscaux();
		if (autresDocuments != null && !autresDocuments.isEmpty()) {
			final List<AutreDocumentFiscalView> avecSuiviViews = new ArrayList<>(autresDocuments.size());
			final List<AutreDocumentFiscalView> sansSuiviViews = new ArrayList<>(autresDocuments.size());
			for (AutreDocumentFiscal document : autresDocuments) {

				// les demandes de dégrèvements ne sont pas affichées ici, mais dans un onglet spécifique
				if (document instanceof DemandeDegrevementICI) {
					continue;
				}

				final AutreDocumentFiscalView view = AutreDocumentFiscalViewFactory.buildView(document, serviceInfrastructureService, messageHelper);
				if (document instanceof AutreDocumentFiscalAvecSuivi) {
					avecSuiviViews.add(view);
				}
				else {
					sansSuiviViews.add(view);
				}
			}

			final Comparator<AutreDocumentFiscalView> comparator = Comparator.comparing(AutreDocumentFiscalView::getDateEnvoi, NullDateBehavior.EARLIEST::compare)
					.thenComparing(AutreDocumentFiscalView::getId)
					.reversed();
			avecSuiviViews.sort(comparator);
			sansSuiviViews.sort(comparator);

			tiersView.setAutresDocumentsFiscauxSuivis(avecSuiviViews);
			tiersView.setAutresDocumentsFiscauxNonSuivis(sansSuiviViews);
		}

		// L'ACI est-elle actuellement maîtresse des données civiles pour cette entreprise?
		try {
			tiersView.setEntreprise(getEntrepriseService().getEntreprise(entreprise)); // EntrepriseView

			final boolean serviceIDEObligEtendues = serviceIDEService.isServiceIDEObligEtendues(entreprise, null);
			tiersView.setCivilSousControleACI(serviceIDEObligEtendues);
		}
		catch (ServiceEntrepriseException e) {
			tiersView.setCivilSousControleACI(false);
			tiersView.setExceptionDonneesCiviles(e.getMessage());
		}

		tiersView.setCommunesImmeubles(getCommunesImmeubles(entreprise));
	}

	private List<CommuneView> getCommunesImmeubles(Contribuable ctb) {

		// récupération de toutes les communes vaudoises
		final Map<Integer, EntiteOFS> communesVaudoises = serviceInfrastructureService.getCommunesDeVaud().stream()
				.sorted(DateRangeComparator::compareRanges)
				.collect(Collectors.toMap(Commune::getNoOFS,
				                          Function.identity(),
				                          (c1, c2) -> c2));

		// les communes sur lesquelles l'entreprise a des immeubles
		final Map<Integer, CommuneView> communes = registreFoncierService.getDroitsForCtb(ctb, true, false, false).stream()
				.filter(AnnulableHelper::nonAnnule)
				.flatMap(d -> d.getImmeubleList().stream())
				.filter(AnnulableHelper::nonAnnule)
				.map(ImmeubleRF::getSituations)
				.flatMap(Set::stream)
				.filter(AnnulableHelper::nonAnnule)
				.map(situation -> resolveCommune(situation, communesVaudoises))
				.map(commune -> new CommuneView(commune.getNoOFS(), commune.getNomOfficiel()))
				.collect(Collectors.toMap(CommuneView::getNoOfs,
				                          Function.identity(),
				                          (c1, c2) -> c2));

		// si on a plusieurs communes avec un numéro OFS différent mais le même nom, on faut les distinguer
		final Map<String, List<CommuneView>> parNom = communes.values().stream()
				.collect(Collectors.toMap(CommuneView::getNom,
				                          Collections::singletonList,
				                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));
		for (List<CommuneView> lists : parNom.values()) {
			if (lists.size() > 1) {
				lists.forEach(view -> {
					final String nouveauNom = String.format("%s (%d)", view.getNom(), view.getNoOfs());
					communes.put(view.getNoOfs(), new CommuneView(view.getNoOfs(), nouveauNom));
				});
			}
		}

		return communes.values().stream()
				.sorted(Comparator.comparing(CommuneView::getNom))
				.collect(Collectors.toList());
	}

	/**
	 * Extrait la commune de la situation et complète la map des communes à la volée.
	 *
	 * @param situation         une situation d'immeuble
	 * @param communesVaudoises une map des communes
	 * @return la commune correspondant à la situation spécifiée
	 */
	private static EntiteOFS resolveCommune(@NotNull SituationRF situation, @NotNull Map<Integer, EntiteOFS> communesVaudoises) {

		final Integer communeSurchargee = situation.getNoOfsCommuneSurchargee();
		if (communeSurchargee != null) {
			// [SIFISC-24367] on expose que le numéro Ofs de la commune surchargée
			final EntiteOFS commune = communesVaudoises.get(communeSurchargee);
			if (commune == null) {
				throw new ObjectNotFoundException("La commune avec le numéro Ofs=" + communeSurchargee + " n'existe pas.");
			}
			return commune;
		}

		// on complète - si nécessaire - la liste des communes vaudoises avec la commune venant du RF
		final int noOfs = situation.getCommune().getNoOfs();
		return communesVaudoises.computeIfAbsent(noOfs, ofs -> buildCommuneInconnue(situation.getCommune()));
	}

	private static EntiteOFS buildCommuneInconnue(CommuneRF communeRF) {
		return new EntiteOFS() {
			@Override
			public String getNomCourt() {
				return communeRF.getNomRf();
			}

			@Override
			public String getNomOfficiel() {
				return communeRF.getNomRf();
			}

			@Override
			public int getNoOFS() {
				return communeRF.getNoOfs();
			}

			@Override
			public String getSigleOFS() {
				return null;
			}
		};
	}

	/**
	 * Mise à jour en fonction des données de l'établissement
	 */
	protected void setEtablissement(TiersView tiersView, Etablissement etb) {
		tiersView.setTiers(Objects.requireNonNull(etb));
		try {
			tiersView.setEtablissement(entrepriseService.getEtablissement(etb));
		}
		catch (ServiceEntrepriseException e) {
			tiersView.setExceptionDonneesCiviles(e.getMessage());
		}
	}

	/**
	 * Met a jour la vue en fonction du debiteur prestation imposable
	 */
	protected void setDebiteurPrestationImposable(TiersView tiersView, DebiteurPrestationImposable dpi, boolean rapportsPrestationHisto, WebParamPagination webParamPagination) throws
			AdresseException {
		tiersView.setTiers(dpi);
		tiersView.setRapportsPrestation(getRapportsPrestation(dpi, webParamPagination, rapportsPrestationHisto));
		tiersView.setLrs(getListesRecapitulatives(dpi));
		if (dpi.getContribuableId() == null) {
			tiersView.setAddContactISAllowed(true);
		}
		else {
			tiersView.setAddContactISAllowed(false);
		}
	}

	/**
	 * Met a jour les fors fiscaux
	 */
	protected void setForsFiscaux(TiersView tiersView, Contribuable contribuable) {
		final List<ForFiscalView> forsFiscauxView = ForFiscalView.getList(contribuable, tiersService::getDernierForGestionConnu);
		tiersView.setForsPrincipalActif(forsFiscauxView.stream()
				                                .filter(ForFiscalView::isPrincipalActif)
				                                .findFirst()
				                                .orElse(null));
		tiersView.setForsFiscaux(forsFiscauxView);
	}

	/**
	 * Met a jour les fors fiscaux pour le dpi
	 */
	protected void setForsFiscauxDebiteur(TiersView tiersView, DebiteurPrestationImposable dpi) {
		final List<ForFiscalView> forsFiscauxView = ForFiscalView.getList(dpi, tiersService::getDernierForGestionConnu);
		tiersView.setForsPrincipalActif(forsFiscauxView.stream()
				                                .filter(ForFiscalView::isPrincipalActif)
				                                .findFirst()
				                                .orElse(null));
		tiersView.setForsFiscaux(forsFiscauxView);
	}

	/**
	 * Met a jour la vue periodicite avec la periodicites du debiteur
	 */
	protected void setPeriodicitesView(TiersVisuView tiersView, DebiteurPrestationImposable dpi) {
		final List<PeriodiciteView> listePeriodicitesView = new ArrayList<>();
		final Set<Periodicite> setPeriodicites = dpi.getPeriodicites();
		if (setPeriodicites != null) {
			for (Periodicite periodicite : setPeriodicites) {
				if (tiersView.isPeriodicitesHisto() || isAlwaysShown(periodicite)) {
					final PeriodiciteView periodiciteView = readFromPeriodicite(periodicite);
					listePeriodicitesView.add(periodiciteView);
				}
			}
			listePeriodicitesView.sort(new PeriodiciteViewComparator());
			tiersView.setPeriodicites(listePeriodicitesView);
		}
	}

	protected PeriodiciteView readFromPeriodicite(Periodicite periodicite) {
		PeriodiciteView periodiciteView = new PeriodiciteView();
		periodiciteView.setDateDebut(periodicite.getDateDebut());
		periodiciteView.setDateFin(periodicite.getDateFin());
		final DebiteurPrestationImposable debiteurPrestationImposable = periodicite.getDebiteur();
		if (debiteurPrestationImposable != null) {
			periodiciteView.setDebiteurId(debiteurPrestationImposable.getNumero());
		}
		periodiciteView.setId(periodicite.getId());
		periodiciteView.setAnnule(periodicite.isAnnule());
		periodiciteView.setPeriodiciteDecompte(periodicite.getPeriodiciteDecompte());
		periodiciteView.setPeriodeDecompte(periodicite.getPeriodeDecompte());
		if (periodicite.isValidAt(RegDate.get()) && !periodicite.isAnnule()) {
			periodiciteView.setActive(true);
		}
		else {
			periodiciteView.setActive(false);
		}
		return periodiciteView;
	}

	protected void setPeriodiciteCourante(TiersView tiersView, DebiteurPrestationImposable dpi) {
		Periodicite periodiciteCourante = dpi.getDernierePeriodicite();

		if (periodiciteCourante == null) {
			// Périodicité par défaut à enregistrer dans la vue
			final RegDate debutPeriodicite = RegDate.get(RegDate.get().year(), 1, 1);
			periodiciteCourante = new Periodicite(PeriodiciteDecompte.MENSUEL, PeriodeDecompte.M12, debutPeriodicite, null);

		}

		tiersView.setPeriodicite(readFromPeriodicite(periodiciteCourante));
	}

	/**
	 * Indique si l'on a le droit ou non de saisir une nouvelle situation de famille
	 */
	protected boolean isSituationFamilleActive(Contribuable contribuable) {
		Set<ForFiscal> forsFiscaux = contribuable.getForsFiscaux();
		for (ForFiscal forFiscal : forsFiscaux) {
			if (forFiscal instanceof ForFiscalPrincipal) {
				ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) forFiscal;
				//[UNIREG-1278] il doit être possible de créer une situation de famille même si le contribuable est hors canton
				if (forFiscalPrincipal.getDateFin() == null) {
					return true;
				}
			}
			if (forFiscal instanceof ForFiscalSecondaire) {
				ForFiscalSecondaire forFiscalSecondaire = (ForFiscalSecondaire) forFiscal;
				if (forFiscalSecondaire.getDateFin() == null) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Met à jour TiersView en fonction de Contribuable
	 */
	protected void setSituationsFamille(TiersView tiersView, Contribuable contribuable) throws AdresseException {

		final List<VueSituationFamille> situationsFamille = situationFamilleService.getVueHisto(contribuable);
		Collections.reverse(situationsFamille); // UNIREG-647
		final List<SituationFamilleView> situationsFamilleView = new ArrayList<>(situationsFamille.size());

		for (VueSituationFamille situation : situationsFamille) {
			if (tiersView instanceof TiersVisuView && !((TiersVisuView) tiersView).isSituationsFamilleHisto() && !isAlwaysShown(situation)) {
				continue;
			}

			final SituationFamilleView view = new SituationFamilleView();
			view.setAnnule(situation.isAnnule());
			view.setId(situation.getId());
			view.setDateDebut(situation.getDateDebut());
			view.setDateFin(situation.getDateFin());
			view.setNombreEnfants(situation.getNombreEnfants());
			view.setEtatCivil(situation.getEtatCivil());

			final VueSituationFamille.Source source = situation.getSource();
			view.setEditable(source == VueSituationFamille.Source.FISCALE_TIERS);
			view.setSource(source != null ? source.name() : null);

			if (situation instanceof VueSituationFamilleMenageCommun) {

				final VueSituationFamilleMenageCommun situationMC = (VueSituationFamilleMenageCommun) situation;
				view.setTarifImpotSource(situationMC.getTarifApplicable());
				view.setNatureSituationFamille(situationMC.getClass().getSimpleName());

				final Long numeroContribuablePrincipal = situationMC.getNumeroContribuablePrincipal();
				if (numeroContribuablePrincipal != null) {
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(numeroContribuablePrincipal);
					final List<String> nomCourrier = getAdresseService().getNomCourrier(pp, null, false);
					view.setNomCourrier1TiersRevenuPlusEleve(nomCourrier.get(0));
					view.setNumeroTiersRevenuPlusEleve(numeroContribuablePrincipal);
				}
			}

			situationsFamilleView.add(view);
		}

		tiersView.setWithSituationsFamille(!situationsFamille.isEmpty());       // certaines de ces situations de familles ne se retrouvent pas dans la vue si l'historique n'est pas demandé
		tiersView.setSituationsFamille(situationsFamilleView);
	}

	protected ComplementView buildComplement(Tiers tiers, boolean coordonneesHisto) {
		return new ComplementView(tiers, coordonneesHisto, ibanValidator);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIbanValidator(IbanValidator ibanValidator) {
		this.ibanValidator = ibanValidator;
	}

	/**
	 * [UNIREG-2655] Détermine si un tiers possède un for fiscal principal hors-Suisse durant une certaine période
	 *
	 * @param tiersId l'id d'un iters
	 * @param range   une période temporelle
	 * @return <b>vrai</b> si le tiers possède au moins un fors fiscal principal hors-Suisse pendant la période considérée.
	 */
	protected boolean isHorsSuisse(Long tiersId, DateRange range) {
		final Tiers tiers = tiersDAO.get(tiersId);
		if (tiers == null) {
			throw new IllegalArgumentException();
		}

		boolean horsSuisse = false;

		for (ForFiscal ff : tiers.getForsFiscaux()) {
			if (!ff.isAnnule() && ff.isPrincipal() && DateRangeHelper.intersect(ff, range)) {
				horsSuisse |= ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS;
			}
		}

		return horsSuisse;
	}

	/**
	 * @return true sur l'utilisateur connecté à les droits d'édition et sécurité dossiers de modif le tiers retourne tjs false si le tiers n'est pas une PP ou un ménage
	 */
	protected boolean checkDroitEdit(Tiers tiers) {
		return autorisationManager.isEditAllowed(tiers);
	}

	/**
	 * Alimente la vue TiersGeneralView
	 */
	protected void setTiersGeneralView(TiersView tiersView, Tiers tiers) throws AdresseException {
		TiersGeneralView tiersGeneralView = tiersGeneralManager.getTiers(tiers, true);
		tiersView.setTiersGeneral(tiersGeneralView);
	}

	public void setMessageHelper(MessageHelper messageHelper) {
		this.messageHelper = messageHelper;
	}


	protected interface AdressesResolverCallback {
		AdressesFiscalesHisto getAdresses(AdresseService service) throws AdresseException;

		void setAdressesView(List<AdresseView> adresses);

		void onException(String message, List<AdresseView> adressesEnErreur);
	}

	/**
	 * recuperation des adresses civiles historiques
	 *
	 * @param pp une personne physique
	 * @return une liste d'adresses
	 * @throws ch.vd.unireg.common.DonneesCivilesException si une adresse possède des données incohérentes (date de fin avant date de début, par exemple)
	 */
	public List<AdresseCivilView> getAdressesHistoriquesCiviles(PersonnePhysique pp) throws DonneesCivilesException {
		final Long noIndividu = pp.getNumeroIndividu();
		final List<AdresseCivilView> adresses = new ArrayList<>();
		if (noIndividu != null) {
			final AdressesCivilesHisto adressesCivilesHisto = serviceCivilService.getAdressesHisto(noIndividu, false);
			fillAdressesCivilesViews(adresses, adressesCivilesHisto);
		}
		return adresses;
	}

	@NotNull
	public List<AdresseCivilView> getAdressesHistoriquesCiviles(Entreprise entreprise) throws DonneesCivilesException {
		if (!entreprise.isConnueAuCivil()) {
			return Collections.emptyList();
		}
		final AdressesCivilesHisto histo = serviceEntreprise.getAdressesEntrepriseHisto(entreprise.getNumeroEntreprise());
		if (histo == null) {
			return Collections.emptyList();
		}
		// [SIFISC-24996] on affiche toutes les adresses civiles des entreprises (y compris les adresses 'cases postales')
		return histo.getAll().stream()
				.map(AdresseCivilView::new)
				.sorted(new AdresseCivilViewComparator())
				.collect(Collectors.toList());
	}

	public List<AdresseCivilView> getAdressesHistoriquesCiviles(Etablissement etb) throws DonneesCivilesException {
		if (!etb.isConnuAuCivil()) {
			return Collections.emptyList();
		}
		final AdressesCivilesHisto histo = serviceEntreprise.getAdressesEtablissementCivilHisto(etb.getNumeroEtablissement());
		if (histo == null) {
			return Collections.emptyList();
		}
		// [SIFISC-28037] on affiche toutes les adresses civiles des établissements (y compris les adresses 'cases postales')
		return histo.getAll().stream()
				.map(AdresseCivilView::new)
				.sorted(new AdresseCivilViewComparator())
				.collect(Collectors.toList());
	}

	private void fillAdressesCivilesViews(List<AdresseCivilView> dest, AdressesCivilesHisto histo) throws DonneesCivilesException {
		if (histo != null) {
			// on remplit tous les types d'adresse
			for (TypeAdresseCivil type : TYPES_ADRESSES_CIVILES) {
				fillAdressesHistoCivilesView(dest, histo, type);
			}
			dest.sort(new AdresseCivilViewComparator());
		}
	}

	/**
	 * [UNIREG-3153] Résoud les adresses fiscales et met-à-disposition la liste des vues sur ces adresses. Cette méthode gère gracieusement les exceptions dans la résolution des adresses.
	 *
	 * @param callback un méthode de callback qui met-à-disposition les adresses et qui reçoit les vues des adresses en retour (ou le cas échéant, les messages d'erreur).
	 * @throws ServiceInfrastructureException en cas de problème sur le service infrastructure
	 */
	protected void resolveAdressesHisto(AdressesResolverCallback callback) throws ServiceInfrastructureException {

		try {
			List<AdresseView> adresses = new ArrayList<>();

			final AdressesFiscalesHisto adressesFiscalesHisto = callback.getAdresses(adresseService);
			if (adressesFiscalesHisto != null) {
				// rempli tous les types d'adresse
				for (TypeAdresseTiers type : TypeAdresseTiers.values()) {
					fillAdressesView(adresses, adressesFiscalesHisto, type);
				}
				adresses.sort(new AdresseViewComparator());
			}

			callback.setAdressesView(adresses);
		}
		catch (Exception exception) {
			LOGGER.warn(exception.getMessage(), exception);

			List<AdresseView> adresses = new ArrayList<>();

			if (exception instanceof AdressesResolutionException) {
				final AdressesResolutionException are = (AdressesResolutionException) exception;
				/*
				 * En cas d'erreur dans la résolution des adresses, on récupère les adresses qui ont provoqué l'erreur et on affiche un écran
				 * spécial pour permettre à l'utilisateur de résoudre le problème
				 */
				for (AdresseTiers a : are.getAdresse()) {
					AdresseView view = adresseManager.getAdresseView(a.getId());
					view.setUsage(a.getUsage());
					view.setSource(SourceType.FISCALE);
					adresses.add(view);
				}
			}

			// Dans tous les cas, on affiche le message d'erreur
			callback.onException(exception.getMessage(), adresses);
		}
	}

	/**
	 * Renseigne la liste des adresses actives sur le form backing object. En cas d'erreur dans la résolution des adresses, les adresses en erreur et le message de l'erreur sont renseignés en lieu et place.
	 */
	protected void setAdressesActives(final TiersEditView tiersEditView, final Tiers tiers) throws ServiceInfrastructureException {

		resolveAdressesHisto(new AdressesResolverCallback() {
			@Override
			public AdressesFiscalesHisto getAdresses(AdresseService service) throws AdresseException {
				return service.getAdressesFiscalHisto(tiers, false);
			}

			@Override
			public void setAdressesView(List<AdresseView> adresses) {
				adresses = removeAdresseFromCivil(adresses);
				//[UNIREG2717] les adresses fiscales retournées doivent aussi contenir les adresses fermées afin que celles ci puissent être annulées
				adresses = removeAdresseAnnulee(adresses);
				tiersEditView.setAdressesActives(adresses);
			}

			@Override
			public void onException(String message, List<AdresseView> adressesEnErreur) {
				tiersEditView.setAdressesEnErreurMessage(message);
				tiersEditView.setAdressesEnErreur(adressesEnErreur);
			}
		});
	}

	/**
	 * Renseigne la liste des adresses fiscales Non calculees modifiables sur le form backing object.
	 */
	protected void setAdressesFiscalesModifiables(final TiersEditView tiersEditView, final Tiers tiers) throws ServiceInfrastructureException {

		resolveAdressesHisto(new AdressesResolverCallback() {
			@Override
			public AdressesFiscalesHisto getAdresses(AdresseService service) throws AdresseException {
				return service.getAdressesTiers(tiers);
			}

			@Override
			public void setAdressesView(List<AdresseView> adresses) {
				adresses = removeAdresseAnnulee(adresses);
				tiersEditView.setAdressesFiscalesModifiables(adresses);
			}

			@Override
			public void onException(String message, List<AdresseView> adressesEnErreur) {
				tiersEditView.setAdressesEnErreurMessage(message);
				tiersEditView.setAdressesEnErreur(adressesEnErreur);
			}
		});
	}

	/**
	 * Rempli la collection des adressesView avec les adresses fiscales historiques du type spécifié.
	 */
	protected void fillAdressesView(List<AdresseView> adressesView, final AdressesFiscalesHisto adressesFiscalHisto, TypeAdresseTiers type) throws ServiceInfrastructureException {

		final Collection<AdresseGenerique> adresses = adressesFiscalHisto.ofType(type);
		if (adresses == null) {
			// rien à faire
			return;
		}

		for (AdresseGenerique adresse : adresses) {
			AdresseView adresseView = createAdresseView(adresse, type);
			adressesView.add(adresseView);
		}
	}

	public void fillAdressesHistoCivilesView(List<AdresseCivilView> adressesView, AdressesCivilesHisto adressesCivilesHisto, TypeAdresseCivil type) throws DonneesCivilesException {
		final List<Adresse> adresses = adressesCivilesHisto.ofType(type);
		if (adresses == null) {
			// rien à faire
			return;
		}

		for (Adresse adresse : adresses) {
			adressesView.add(new AdresseCivilView(adresse, type));
		}
	}

	private void fillAdresseView(AdresseView tofill, AdresseGenerique source, TypeAdresseTiers type) {
		tofill.setDateDebut(source.getDateDebut());
		tofill.setDateFin(source.getDateFin());
		tofill.setAnnule(source.isAnnule());
		tofill.setId(source.getId());
		tofill.setPermanente(source.isPermanente());
		tofill.setEgid(source.getEgid());
		tofill.setEwid(source.getEwid());

		final RueEtNumero rueEtNumero = AdresseServiceImpl.buildRueEtNumero(source);
		tofill.setRue(rueEtNumero == null ? null : rueEtNumero.getRueEtNumero());

		final NpaEtLocalite npaEtLocalite = AdresseServiceImpl.buildNpaEtLocalite(source);
		tofill.setLocalite(npaEtLocalite == null ? null : npaEtLocalite.toString());

		tofill.setUsage(type);
		tofill.setPaysOFS(source.getNoOfsPays());
		tofill.setSource(source.getSource().getType());
		tofill.setDefault(source.isDefault());
		tofill.setComplements(source.getComplement());
		tofill.setActive(source.isValidAt(RegDate.get()));
		tofill.setSurVaud(estDansLeCanton(source));

		if (source.getCasePostale() != null) {
			tofill.setTexteCasePostale(source.getCasePostale().getType());
			tofill.setNumeroCasePostale(source.getCasePostale().getNumero());
			tofill.setNpaCasePostale(source.getCasePostale().getNpa());
		}
	}

	/**
	 * Crée une adresse view à partir d'une adresse générique.
	 *
	 * @param adresse une adresse générique
	 * @param type    un type d'adresse
	 * @return une adresse view
	 */
	public AdresseView createAdresseView(AdresseGenerique adresse, TypeAdresseTiers type) {
		final AdresseView adresseView = new AdresseView();
		fillAdresseView(adresseView, adresse, type);
		return adresseView;
	}

	private boolean estDansLeCanton(AdresseGenerique adresse) {
		try {
			return serviceInfrastructureService.estDansLeCanton(adresse);
		}
		catch (ServiceInfrastructureException e) {
			LOGGER.error("Impossible de déterminer le canton de l'adresse : " + adresse, e);
			return false;
		}
	}

	protected List<AdresseView> removeAdresseFromCivil(List<AdresseView> adresses) {
		List<AdresseView> resultat = new ArrayList<>();
		for (AdresseView view : adresses) {
			//UNIREG-1813 L'adresse domicile est retiré du bloc fiscal
			if (TypeAdresseTiers.DOMICILE != view.getUsage()) {
				if (view.getDateFin() == null || AdresseGenerique.SourceType.CIVILE_PERS != view.getSource()) {
					resultat.add(view);
				}

			}
		}
		return resultat;  //To change body of created methods use File | Settings | File Templates.
	}

	protected List<AdresseView> removeAdresseAnnulee(List<AdresseView> adresses) {
		List<AdresseView> resultat = new ArrayList<>();
		for (AdresseView view : adresses) {
			if (!view.isAnnule()) {
				resultat.add(view);
			}
		}
		return resultat;  //To change body of created methods use File | Settings | File Templates.
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setWebCivilService(WebCivilService webCivilService) {
		this.webCivilService = webCivilService;
	}

	public TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public ServiceInfrastructureService getServiceInfrastructureService() {
		return serviceInfrastructureService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	public EntrepriseService getEntrepriseService() {
		return entrepriseService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEntrepriseService(EntrepriseService entrepriseService) {
		this.entrepriseService = entrepriseService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAdresseManager(AdresseManager adresseManager) {
		this.adresseManager = adresseManager;
	}

	public TiersGeneralManager getTiersGeneralManager() {
		return tiersGeneralManager;
	}

	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}

	public AdresseService getAdresseService() {
		return adresseService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setConfigurationMandataire(ConfigurationMandataire configurationMandataire) {
		this.configurationMandataire = configurationMandataire;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setSituationFamilleService(SituationFamilleService situationFamilleService) {
		this.situationFamilleService = situationFamilleService;
	}

	public AdresseTiersDAO getAdresseTiersDAO() {
		return adresseTiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAdresseTiersDAO(AdresseTiersDAO adresseTiersDAO) {
		this.adresseTiersDAO = adresseTiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRapportEntreTiersDAO(RapportEntreTiersDAO rapportEntreTiersDAO) {
		this.rapportEntreTiersDAO = rapportEntreTiersDAO;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public MessageSource getMessageSource() {
		return messageSource;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAutorisationManager(AutorisationManager autorisationManager) {
		this.autorisationManager = autorisationManager;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setServiceEntreprise(ServiceEntreprise serviceEntreprise) {
		this.serviceEntreprise = serviceEntreprise;
	}

	public void setExerciceCommercialHelper(ExerciceCommercialHelper exerciceCommercialHelper) {
		this.exerciceCommercialHelper = exerciceCommercialHelper;
	}

	public void setServiceIDEService(ServiceIDEService serviceIDEService) {
		this.serviceIDEService = serviceIDEService;
	}

	public void setRegistreFoncierService(RegistreFoncierService registreFoncierService) {
		this.registreFoncierService = registreFoncierService;
	}
}

