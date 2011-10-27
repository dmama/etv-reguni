package ch.vd.uniregctb.tiers.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseCivileAdapter;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseGenerique.SourceType;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdresseServiceImpl;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.adresse.AdresseTiersDAO;
import ch.vd.uniregctb.adresse.AdressesFiscalesHisto;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.NpaEtLocalite;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.RueEtNumero;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.entreprise.EntrepriseView;
import ch.vd.uniregctb.entreprise.HostPersonneMoraleService;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.individu.HostCivilService;
import ch.vd.uniregctb.individu.IndividuView;
import ch.vd.uniregctb.interfaces.model.AdoptionReconnaissance;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesActives;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Logiciel;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;
import ch.vd.uniregctb.lr.view.ListeRecapDetailComparator;
import ch.vd.uniregctb.lr.view.ListeRecapDetailView;
import ch.vd.uniregctb.rapport.SensRapportEntreTiers;
import ch.vd.uniregctb.rapport.TypeRapportEntreTiersWeb;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.rt.view.RapportPrestationView;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.situationfamille.VueSituationFamille;
import ch.vd.uniregctb.situationfamille.VueSituationFamilleMenageCommun;
import ch.vd.uniregctb.tiers.AnnuleEtRemplace;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.ConseilLegal;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Curatelle;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportEntreTiersDAO;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.RepresentationLegale;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.Tutelle;
import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.tiers.view.AdresseViewComparator;
import ch.vd.uniregctb.tiers.view.ComplementView;
import ch.vd.uniregctb.tiers.view.DebiteurView;
import ch.vd.uniregctb.tiers.view.ForDebiteurViewComparator;
import ch.vd.uniregctb.tiers.view.ForFiscalView;
import ch.vd.uniregctb.tiers.view.ForFiscalViewComparator;
import ch.vd.uniregctb.tiers.view.LogicielView;
import ch.vd.uniregctb.tiers.view.PeriodiciteView;
import ch.vd.uniregctb.tiers.view.PeriodiciteViewComparator;
import ch.vd.uniregctb.tiers.view.SituationFamilleView;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.tiers.view.TiersView;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Methodes annexes utilisées par TiersVisuManager et TiersEditManager
 *
 * @author xcifde
 */
public class TiersManager implements MessageSourceAware {

	protected final Logger LOGGER = Logger.getLogger(TiersManager.class);

	private final List<TypeAdresseCivil> typesAdressesCiviles = Arrays.asList(TypeAdresseCivil.COURRIER, TypeAdresseCivil.PRINCIPALE, TypeAdresseCivil.SECONDAIRE, TypeAdresseCivil.TUTEUR);

	private HostCivilService hostCivilService;

	private HostPersonneMoraleService hostPersonneMoraleService;

	protected ServiceCivilService serviceCivilService;

	protected TiersDAO tiersDAO;

	private AdresseTiersDAO adresseTiersDAO;

	protected TiersService tiersService;

	protected ServiceInfrastructureService serviceInfrastructureService;

	private AdresseManager adresseManager;

	protected TiersGeneralManager tiersGeneralManager;

	protected AdresseService adresseService;

	protected MessageSource messageSource;

	protected SituationFamilleService situationFamilleService;

	protected RapportEntreTiersDAO rapportEntreTiersDAO;
	protected IbanValidator ibanValidator;
	private ServicePersonneMoraleService servicePM;
	private AutorisationManager autorisationManager;

	/**
	 * Recupere l'individu correspondant au tiers
	 *
	 * @param tiers
	 * @return
	 */
	protected IndividuView getIndividuView(PersonnePhysique habitant) {

		IndividuView individuView = null;
		Long noIndividu = habitant.getNumeroIndividu();
		if (noIndividu != null) {
			individuView = hostCivilService.getIndividu(noIndividu);
		}
		if (habitant.getDateDeces() != null && individuView != null) {//habitant décédé fiscalement
			individuView.setEtatCivil("DECEDE");
			individuView.setDateDernierChgtEtatCivil(RegDate.asJavaDate(habitant.getDateDeces()));
		}
		return individuView;
	}

	/**
	 * Recupere l'entreprise correspondant au tiers
	 *
	 * @param tiers
	 * @return
	 */
	protected EntrepriseView getEntrepriseView(Entreprise entreprise) {

		EntrepriseView entrepriseView = null;
		Long noEntreprise = entreprise.getNumero();
		if (noEntreprise != null) {
			entrepriseView = getHostPersonneMoraleService().get(noEntreprise);
		}
		return entrepriseView;
	}

	private RapportView createRapportViewPourFilliation(Individu reference, Individu autre, SensRapportEntreTiers sens) {
		final RapportView rapportView = new RapportView();
		rapportView.setSensRapportEntreTiers(sens);
		rapportView.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.FILIATION);

		try {
			final PersonnePhysique habitant = tiersDAO.getPPByNumeroIndividu(autre.getNoTechnique());
			if (habitant != null) {
				rapportView.setNumero(habitant.getNumero());
			}
		}
		catch (PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException e) {
			LOGGER.warn(String.format("Détermination impossible du tiers associé à l'individu %d : %s", autre.getNoTechnique(), e.getMessage()));
			rapportView.setMessageNumeroAbsent(String.format("Doublon détecté pour individu %d", autre.getNoTechnique()));
		}

		final String nomBrut = tiersService.getNomPrenom(autre);
		final String nom;
		if (autre.getDateDeces() != null) {
			if (autre.isSexeMasculin()) {
				nom = String.format("%s, défunt", nomBrut);
			}
			else {
				nom = String.format("%s, défunte", nomBrut);
			}
		}
		else {
			nom = nomBrut;
		}
		rapportView.setNomCourrier1(nom);

		// le rapport est terminé au décès de l'un des membres
		if (reference.getDateDeces() != null || autre.getDateDeces() != null) {
			final RegDate dateFinRapport = RegDateHelper.minimum(reference.getDateDeces(), autre.getDateDeces(), NullDateBehavior.LATEST);
			rapportView.setDateFin(dateFinRapport);
		}

		// le rapport démarre à la naissance du dernier membre
		final RegDate dateDebutRapport = RegDateHelper.maximum(reference.getDateNaissance(), autre.getDateNaissance(), NullDateBehavior.EARLIEST);
		rapportView.setDateDebut(dateDebutRapport);

		return rapportView;
	}

	private AdoptionReconnaissance getAdoptionPourEnfant(Collection<AdoptionReconnaissance> adoptions, long noIndEnfant) {
		AdoptionReconnaissance a = null;
		if (adoptions != null && adoptions.size() > 0) {
			for (AdoptionReconnaissance candidat : adoptions) {
				if (candidat.getAdopteReconnu().getNoTechnique() == noIndEnfant) {
					a = candidat;
					break;
				}
			}
		}
		return a;
	}

	/**
	 * Recupère les rapports de filiation de type PARENT ou ENFANT
	 *
	 * @param numeroTiers
	 * @param sens
	 * @return
	 */
	protected List<RapportView> getRapportsFiliation(PersonnePhysique habitant) {
		final List<RapportView> rapportsView = new ArrayList<RapportView>();
		Assert.notNull(habitant.getNumeroIndividu(), "La personne physique n'a pas de numéro d'individu connu");

		final int year = RegDate.get().year();

		final AttributeIndividu[] enumValues = new AttributeIndividu[]{AttributeIndividu.ENFANTS, AttributeIndividu.PARENTS, AttributeIndividu.ADOPTIONS};
		final Individu ind = serviceCivilService.getIndividu(habitant.getNumeroIndividu(), year, enumValues);

		final String nomInd = tiersService.getNomPrenom(ind);

		// enfants biologiques
		final Collection<Individu> listFiliations = ind.getEnfants();
		for (Individu enfant : listFiliations) {
			final RapportView rapportView = createRapportViewPourFilliation(ind, enfant, SensRapportEntreTiers.OBJET);

			final boolean fermeOuAnnule = rapportView.isAnnule() || rapportView.getDateFin() != null;
			final String nomEnfant = tiersService.getNomPrenom(enfant);
			final String toolTipMessage = String.format("%s %s l'enfant de %s", nomEnfant, fermeOuAnnule ? "était" : "est", nomInd);
			rapportView.setToolTipMessage(toolTipMessage);

			rapportsView.add(rapportView);
		}

		// enfants adoptés / reconnus
		final Collection<AdoptionReconnaissance> adoptions = ind.getAdoptionsReconnaissances();
		if (adoptions != null) {
			for (AdoptionReconnaissance ar : adoptions) {
				final Individu enfant = ar.getAdopteReconnu();
				final RapportView rapportView = createRapportViewPourFilliation(ind, enfant, SensRapportEntreTiers.OBJET);
				final RegDate dateDebut = RegDateHelper.maximum(ar.getDateAdoption(), ar.getDateReconnaissance(), NullDateBehavior.EARLIEST);
				if (dateDebut != null) {
					rapportView.setDateDebut(dateDebut);
				}
				if (ar.getDateDesaveu() != null) {
					rapportView.setDateFin(ar.getDateDesaveu());
				}

				final boolean fermeOuAnnule = rapportView.isAnnule() || rapportView.getDateFin() != null;
				final String nomEnfant = tiersService.getNomPrenom(enfant);
				final String toolTipMessage = String.format("%s %s l'enfant de %s", nomEnfant, fermeOuAnnule ? "était" : "est", nomInd);
				rapportView.setToolTipMessage(toolTipMessage);

				rapportsView.add(rapportView);
			}
		}

		// parents
		final List<Individu> parents = ind.getParents();
		if (parents != null) {
			for (Individu parent : parents) {
				rapportsView.add(createRapportViewPourFilliation(ind, nomInd, parent, year));
			}
		}

		return rapportsView;
	}

	private RapportView createRapportViewPourFilliation(Individu individu, String nomIndividu, Individu parent, int year) {

		final RapportView rapportView = createRapportViewPourFilliation(individu, parent, SensRapportEntreTiers.SUJET);

		final Individu parentAvecAdoptions = serviceCivilService.getIndividu(parent.getNoTechnique(), year, AttributeIndividu.ADOPTIONS);
		final AdoptionReconnaissance ar = getAdoptionPourEnfant(parentAvecAdoptions.getAdoptionsReconnaissances(), individu.getNoTechnique());
		if (ar != null) {
			final RegDate dateDebut = RegDateHelper.maximum(ar.getDateAdoption(), ar.getDateReconnaissance(), NullDateBehavior.EARLIEST);
			if (dateDebut != null) {
				rapportView.setDateDebut(dateDebut);
			}
			if (ar.getDateDesaveu() != null && (rapportView.getRegDateFin() == null || ar.getDateDesaveu().isBefore(rapportView.getRegDateFin()))) {
				rapportView.setDateFin(ar.getDateDesaveu());
			}
		}

		final boolean fermeOuAnnule = rapportView.isAnnule() || rapportView.getDateFin() != null;
		final String nomParent = tiersService.getNomPrenom(parent);
		final String verbe = fermeOuAnnule ? "était" : "est";
		final String type = parent.isSexeMasculin() ? "le père" : "la mère";
		final String toolTipMessage = String.format("%s %s %s de %s", nomParent, verbe, type, nomIndividu);
		rapportView.setToolTipMessage(toolTipMessage);

		return rapportView;
	}

	/**
	 * Alimente Set<DebiteurView>
	 *
	 * @param contribuable
	 * @return
	 * @throws AdresseException
	 */
	protected Set<DebiteurView> getDebiteurs(Contribuable contribuable) throws AdresseException {

		final Set<DebiteurView> debiteursView = new HashSet<DebiteurView>();

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
					final List<String> nomCourrier = getAdresseService().getNomCourrier(dpi, null, false);
					debiteurView.setNomCourrier(nomCourrier);
					debiteursView.add(debiteurView);
				}
			}
		}


		return debiteursView;

	}

	/**
	 * Alimente List<RapportView>
	 *
	 * @param tiers
	 * @return
	 * @throws AdresseException
	 */
	protected List<RapportView> getRapports(Tiers tiers) throws AdresseException {
		final List<RapportView> rapportsView = new ArrayList<RapportView>();

		// Rapport entre tiers Objet
		for (RapportEntreTiers rapportEntreTiers : tiers.getRapportsObjet()) {
			if (rapportEntreTiers.getType() != TypeRapportEntreTiers.PRESTATION_IMPOSABLE) {
				final RapportView rapportView = new RapportView();
				rapportView.setId(rapportEntreTiers.getId());
				rapportView.setAnnule(rapportEntreTiers.isAnnule());
				rapportView.setSensRapportEntreTiers(SensRapportEntreTiers.OBJET);
				rapportView.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.fromCore(rapportEntreTiers.getType()));
				rapportView.setDateDebut(rapportEntreTiers.getDateDebut());
				rapportView.setDateFin(rapportEntreTiers.getDateFin());

				final Tiers tiersSujet = tiersDAO.get(rapportEntreTiers.getSujetId());
				rapportView.setNumero(tiersSujet.getNumero());

				List<String> nomSujet;
				try {
					nomSujet = adresseService.getNomCourrier(tiersSujet, null, false);
				}
				catch (Exception e) {
					nomSujet = new ArrayList<String>();
					nomSujet.add(e.getMessage());
				}
				rapportView.setNomCourrier(nomSujet);

				final String toolTipMessage = getRapportEntreTiersTooltips(rapportEntreTiers);
				rapportView.setToolTipMessage(toolTipMessage);
				if (rapportEntreTiers instanceof RepresentationLegale) {
					setNomAutoriteTutelaire(rapportEntreTiers, rapportView);

				}
				rapportsView.add(rapportView);
			}
		}

		// Rapport entre tiers Sujet
		for (RapportEntreTiers rapportEntreTiers : tiers.getRapportsSujet()) {
			if (rapportEntreTiers.getType() != TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE) {
				final RapportView rapportView = new RapportView();
				rapportView.setId(rapportEntreTiers.getId());
				rapportView.setAnnule(rapportEntreTiers.isAnnule());
				rapportView.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);
				rapportView.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.fromCore(rapportEntreTiers.getType()));
				rapportView.setDateDebut(rapportEntreTiers.getDateDebut());
				rapportView.setDateFin(rapportEntreTiers.getDateFin());

				final Tiers tiersObjet = tiersDAO.get(rapportEntreTiers.getObjetId());
				rapportView.setNumero(tiersObjet.getNumero());

				List<String> nomObjet;
				try {
					nomObjet = adresseService.getNomCourrier(tiersObjet, null, false);
				}
				catch (Exception e) {
					nomObjet = new ArrayList<String>();
					nomObjet.add(e.getMessage());
				}
				if (nomObjet != null && nomObjet.size() != 0) {
					rapportView.setNomCourrier(nomObjet);
				}

				final String toolTipMessage = getRapportEntreTiersTooltips(rapportEntreTiers);
				rapportView.setToolTipMessage(toolTipMessage);

				if (rapportEntreTiers instanceof RapportPrestationImposable) {
					final RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportEntreTiers;
					rapportView.setTypeActivite(rapportPrestationImposable.getTypeActivite());
					rapportView.setTauxActivite(rapportPrestationImposable.getTauxActivite());
				}
				else if (rapportEntreTiers instanceof RepresentationConventionnelle) {
					final RepresentationConventionnelle repres = (RepresentationConventionnelle) rapportEntreTiers;
					final Boolean b = repres.getExtensionExecutionForcee();
					rapportView.setExtensionExecutionForcee(b != null && b);
					final boolean isHorsSuisse = isHorsSuisse(rapportEntreTiers.getSujetId(), rapportEntreTiers);
					rapportView.setExtensionExecutionForceeAllowed(isHorsSuisse); // [UNIREG-2655]
				}
				else if (rapportEntreTiers instanceof RepresentationLegale) {
					setNomAutoriteTutelaire(rapportEntreTiers, rapportView);

				}
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
	 * Alimente List<RapportView>
	 *
	 * @param tiers
	 * @return
	 * @throws AdresseException
	 */
	protected void setContribuablesAssocies(TiersView tiersView, DebiteurPrestationImposable debiteur) throws AdresseException {
		final List<RapportView> rapportsView = new ArrayList<RapportView>();

		// Rapport entre tiers Objet
		final Set<RapportEntreTiers> rapports = debiteur.getRapportsObjet();
		for (RapportEntreTiers rapportEntreTiers : rapports) {
			if (rapportEntreTiers.getType() == TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE) {
				final RapportView rapportView = new RapportView();
				rapportView.setId(rapportEntreTiers.getId());
				rapportView.setAnnule(rapportEntreTiers.isAnnule());
				final Tiers tiersSujet = tiersDAO.get(rapportEntreTiers.getSujetId());
				rapportView.setNumero(tiersSujet.getNumero());
				final List<String> nomCourrier = getAdresseService().getNomCourrier(tiersSujet, null, false);
				rapportView.setNomCourrier(nomCourrier);
				final String toolTipMessage = getRapportEntreTiersTooltips(rapportEntreTiers);
				rapportView.setToolTipMessage(toolTipMessage);
				rapportsView.add(rapportView);
			}
		}
		tiersView.setContribuablesAssocies(rapportsView);
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
	 * Construit et retourne une string qui résume de manière compréhensible pour un humain un rapport entre deux tiers.
	 *
	 * @param rapport le rapport dont on veut obtenir un résumé
	 * @return un résumé du rapport; ou <b>null</b> s'il n'est pas possible de le créer pour une raison ou une autre.
	 */
	protected String getRapportEntreTiersTooltips(RapportEntreTiers rapport) {

		final Long sujetId = rapport.getSujetId();
		final Long objetId = rapport.getObjetId();
		final Tiers sujet = tiersDAO.get(sujetId);
		final Tiers objet = tiersDAO.get(objetId);

		final String nomSujet;
		final String nomObjet;
		try {
			nomSujet = getNomCourrierPlat(sujet);
			nomObjet = getNomCourrierPlat(objet);
		}
		catch (Exception e) {
			LOGGER.warn("Exception catchée pendant le calcul des tooltips", e);
			return null;
		}

		if (nomSujet == null || nomObjet == null) {
			return null;
		}

		final boolean fermeOuAnnule = rapport.isAnnule() || rapport.getDateFin() != null;

		if (rapport instanceof ContactImpotSource) {
			return String.format("%s %s le tiers référent de %s", nomSujet, fermeOuAnnule ? "était" : "est", nomObjet);
		}
		else if (rapport instanceof RepresentationConventionnelle) {
			return String.format("%s %s représenté(e) par %s", nomSujet, fermeOuAnnule ? "était" : "est", nomObjet);
		}
		else if (rapport instanceof Curatelle) {
			return String.format("%s %s le curateur de %s", nomObjet, fermeOuAnnule ? "était" : "est", nomSujet);
		}
		else if (rapport instanceof ConseilLegal) {
			return String.format("%s %s le conseiller légal de %s", nomObjet, fermeOuAnnule ? "était" : "est", nomSujet);
		}
		else if (rapport instanceof Tutelle) {
			return String.format("%s %s le tuteur de %s", nomObjet, fermeOuAnnule ? "était" : "est", nomSujet);
		}
		else if (rapport instanceof AnnuleEtRemplace) {
			return String.format("%s (n°%d) %s %s (n°%d)", nomObjet, objetId, fermeOuAnnule ? "remplaçait" : "remplace", nomSujet, sujetId);
		}
		else if (rapport instanceof AppartenanceMenage) {
			return String.format("%s %s au ménage %s", nomSujet, fermeOuAnnule ? "appartenait" : "appartient", nomObjet);
		}
		else if (rapport instanceof RapportPrestationImposable) {
			return String.format("%s %s employé(e) par %s", nomSujet, fermeOuAnnule ? "était" : "est", nomObjet);
		}
		else {
			throw new IllegalArgumentException("Type de rapport-entre-tiers inconnu = [" + rapport.getClass() + "]");
		}
	}

	protected String getNomCourrierPlat(Tiers tiers) throws AdresseException {

		final List<String> noms = adresseService.getNomCourrier(tiers, null, false);
		if (noms == null || noms.isEmpty()) {
			return null;
		}

		if (noms.size() == 1) {
			return noms.get(0);
		}
		else {
			final StringBuilder b = new StringBuilder(noms.get(0));
			for (int i = 1; i < noms.size(); ++i) {
				b.append(" / ").append(noms.get(i));
			}
			return b.toString();
		}
	}

	/**
	 * Alimente List<RapportPrestationView>
	 *
	 * @param dpi
	 * @param rapportsPrestationHisto
	 * @return
	 * @throws AdresseException
	 */
	protected List<RapportPrestationView> getRapportsPrestation(DebiteurPrestationImposable dpi, WebParamPagination pagination, boolean rapportsPrestationHisto) throws AdresseException {

		List<RapportPrestationView> rapportPrestationViews = new ArrayList<RapportPrestationView>();

		List<RapportPrestationImposable> rapports = rapportEntreTiersDAO.getRapportsPrestationImposable(dpi.getNumero(), pagination, !rapportsPrestationHisto);
		for (RapportPrestationImposable rapport : rapports) {
			RapportPrestationView rapportPrestationView = new RapportPrestationView();
			rapportPrestationView.setId(rapport.getId());
			rapportPrestationView.setAnnule(rapport.isAnnule());
			rapportPrestationView.setTypeActivite(rapport.getTypeActivite());
			rapportPrestationView.setTauxActivite(rapport.getTauxActivite());
			rapportPrestationView.setDateDebut(rapport.getDateDebut());
			rapportPrestationView.setDateFin(rapport.getDateFin());
			Tiers tiersObjet = tiersDAO.get(rapport.getSujetId());
			if (tiersObjet instanceof PersonnePhysique) {
				PersonnePhysique pp = (PersonnePhysique) tiersObjet;
				String nouveauNumeroAvs = tiersService.getNumeroAssureSocial(pp);
				if (nouveauNumeroAvs != null && !"".equals(nouveauNumeroAvs)) {
					rapportPrestationView.setNumeroAVS(FormatNumeroHelper.formatNumAVS(nouveauNumeroAvs));
				}
				else {
					String ancienNumeroAvs = tiersService.getAncienNumeroAssureSocial(pp);
					rapportPrestationView.setNumeroAVS(FormatNumeroHelper.formatAncienNumAVS(ancienNumeroAvs));
				}
			}
			rapportPrestationView.setNumero(tiersObjet.getNumero());
			List<String> nomCourrier = adresseService.getNomCourrier(tiersObjet, null, false);
			rapportPrestationView.setNomCourrier(nomCourrier);
			rapportPrestationViews.add(rapportPrestationView);
		}
		return rapportPrestationViews;
	}


	/**
	 * Alimente Set<ListeRecapitulativeView>
	 *
	 * @param debiteur
	 * @return
	 */
	private List<ListeRecapDetailView> getListesRecapitulatives(DebiteurPrestationImposable dpi) {

		List<ListeRecapDetailView> lrsView = new ArrayList<ListeRecapDetailView>();
		Set<Declaration> declarations = dpi.getDeclarations();
		for (Declaration declaration : declarations) {
			if (declaration instanceof DeclarationImpotSource) {
				DeclarationImpotSource lr = (DeclarationImpotSource) declaration;
				ListeRecapDetailView lrView = new ListeRecapDetailView();
				lrView.setId(lr.getId());
				final EtatDeclaration dernierEtat = lr.getDernierEtat();
				lrView.setEtat(dernierEtat == null ? null : dernierEtat.getEtat());
				lrView.setDateDebutPeriode(lr.getDateDebut());
				lrView.setDateFinPeriode(lr.getDateFin());
				lrView.setDateRetour(lr.getDateRetour());
				lrView.setAnnule(lr.isAnnule());
				Set<DelaiDeclaration> echeances = lr.getDelais();
				Iterator<DelaiDeclaration> itEcheance = echeances.iterator();
				RegDate delai = null;
				RegDate delaiMax = null;
				while (itEcheance.hasNext()) {
					DelaiDeclaration echeance = itEcheance.next();
					delai = echeance.getDelaiAccordeAu();
					if (delaiMax == null) {
						delaiMax = delai;
					}
					if (delai.isAfter(delaiMax)) {
						delaiMax = delai;
					}
				}
				lrView.setDelaiAccorde(delaiMax);
				lrsView.add(lrView);
			}
		}
		Collections.sort(lrsView, new ListeRecapDetailComparator());
		return lrsView;
	}

	/**
	 * Met a jour la vue en fonction de l'habitant
	 *
	 * @param habitant
	 * @param tiersView
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
	 *
	 * @param habitant
	 * @param tiersView
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

			if (tiersPrincipal.isHabitantVD()) {
				IndividuView individu = getIndividuView(tiersPrincipal);
				tiersView.setIndividu(individu);
			}
		}

		/* 2eme tiers */
		if (tiersConjoint != null) {
			tiersView.setTiersConjoint(tiersConjoint);
			tiersView.setNomPrenomConjoint(tiersService.getNomPrenom(tiersConjoint));

			if (tiersConjoint.isHabitantVD()) {
				IndividuView individu = getIndividuView(tiersConjoint);
				tiersView.setIndividuConjoint(individu);
			}
		}
	}

	/**
	 * Met a jour la vue en fonction de l'entreprise
	 *
	 * @param entreprise
	 * @param tiersView
	 */
	protected void setEntreprise(TiersView tiersView, Entreprise entreprise) {
		tiersView.setTiers(entreprise);
		EntrepriseView entrepriseView = getEntrepriseView(entreprise);
		tiersView.setEntreprise(entrepriseView);
	}

	/**
	 * Met a jour la vue en fonction du debiteur prestation imposable
	 *
	 * @param tiersView
	 * @param rapportsPrestationHisto
	 * @throws AdresseException
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
	 *
	 * @param tiersView
	 * @param contribuable
	 */
	protected void setForsFiscaux(TiersView tiersView, Contribuable contribuable) {
		final ForGestion forGestion = tiersService.getDernierForGestionConnu(contribuable, null);
		final ForFiscalPrincipal dernierForPrincipal = contribuable.getDernierForFiscalPrincipal();
		final ForFiscal forPrincipalActif = contribuable.getForFiscalPrincipalAt(null);
		final List<ForFiscal> forsFiscaux = contribuable.getForsFiscauxSorted();

		final List<ForFiscalView> forsFiscauxView = new ArrayList<ForFiscalView>();
		if (forsFiscaux != null) {
			ForFiscalView forPrincipalViewActif = null;
			for (ForFiscal forFiscal : forsFiscaux) {

				final boolean isForGestion = forGestion != null && forGestion.getSousjacent() == forFiscal;
				final boolean isDernierForPrincipal = (dernierForPrincipal == forFiscal);

				final ForFiscalView forFiscalView = new ForFiscalView(forFiscal, isForGestion, isDernierForPrincipal);

				if (forPrincipalActif == forFiscal) {
					forPrincipalViewActif = forFiscalView;
				}

				forsFiscauxView.add(forFiscalView);
			}
			Collections.sort(forsFiscauxView, new ForFiscalViewComparator());
			tiersView.setForsPrincipalActif(forPrincipalViewActif);
			tiersView.setForsFiscaux(forsFiscauxView);
		}
	}

	/**
	 * Met a jour les fors fiscaux pour le dpi
	 *
	 * @param tiersView
	 * @param dpi
	 */
	protected void setForsFiscauxDebiteur(TiersView tiersView, DebiteurPrestationImposable dpi) {
		final List<ForFiscalView> forsFiscauxView = new ArrayList<ForFiscalView>();
		final Set<ForFiscal> forsFiscaux = dpi.getForsFiscaux();
		if (forsFiscaux != null) {
			for (ForFiscal forFiscal : forsFiscaux) {
				final ForFiscalView forFiscalView = new ForFiscalView();
				forFiscalView.setId(forFiscal.getId());
				forFiscalView.setNumeroCtb(forFiscal.getTiers().getNumero());
				forFiscalView.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
				forFiscalView.setAnnule(forFiscal.isAnnule());
				forFiscalView.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				forFiscalView.setNumeroForFiscalCommune(forFiscal.getNumeroOfsAutoriteFiscale());
				forFiscalView.setDateOuverture(forFiscal.getDateDebut());
				forFiscalView.setDateFermeture(forFiscal.getDateFin());
				forFiscalView.setNatureForFiscal(forFiscal.getClass().getSimpleName());

				final boolean dernierFor = !forFiscal.isAnnule() && (forFiscal.getDateFin() == null || dpi.getForDebiteurPrestationImposableAfter(forFiscal.getDateFin()) == null);
				forFiscalView.setDernierForPrincipalOuDebiteur(dernierFor);

				forsFiscauxView.add(forFiscalView);
			}
			Collections.sort(forsFiscauxView, new ForDebiteurViewComparator());
			tiersView.setForsFiscaux(forsFiscauxView);
		}
	}

	/**
	 * Met a jour la vue periodicite avec la periodicites du debiteur
	 */
	protected void setPeriodicitesView(TiersView tiersView, DebiteurPrestationImposable dpi) {
		List<PeriodiciteView> listePeriodicitesView = new ArrayList<PeriodiciteView>();
		Set<Periodicite> setPeriodicites = dpi.getPeriodicites();
		if (setPeriodicites != null) {
			for (Periodicite periodicite : setPeriodicites) {
				PeriodiciteView periodiciteView = readFromPeriodicite(periodicite);
				listePeriodicitesView.add(periodiciteView);
			}
			Collections.sort(listePeriodicitesView, new PeriodiciteViewComparator());
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
			periodiciteCourante = new Periodicite(PeriodiciteDecompte.TRIMESTRIEL, PeriodeDecompte.M12, debutPeriodicite, null);

		}

		tiersView.setPeriodicite(readFromPeriodicite(periodiciteCourante));
	}

	/**
	 * Indique si l'on a le droit ou non de saisir une nouvelle situation de famille
	 *
	 * @param contribuable
	 * @return
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
	 *
	 * @param tiersView
	 * @param contribuable
	 * @throws AdresseException
	 */
	protected void setSituationsFamille(TiersView tiersView, Contribuable contribuable) throws AdresseException {

		final List<SituationFamilleView> situationsFamilleView = new ArrayList<SituationFamilleView>();
		final List<VueSituationFamille> situationsFamille = situationFamilleService.getVueHisto(contribuable);
		Collections.reverse(situationsFamille); // UNIREG-647

		for (VueSituationFamille situation : situationsFamille) {

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

		tiersView.setSituationsFamille(situationsFamilleView);
	}

	/**
	 * gestion des droits d'èdition d'un tiers
	 *
	 * @param tiers
	 * @param allowedOnglet
	 * @return true si l'utilisateur a le droit d'éditer le tiers
	 */
	protected boolean setDroitEdition(Tiers tiers, Map<String, Boolean> allowedOnglet) {

		final Map<String, Boolean> autorisations = autorisationManager.getAutorisations(tiers);
		allowedOnglet.putAll(autorisations);

		boolean isEditable = false;
		if (!(tiers instanceof Etablissement)) { // les établissements ne sont pas éditables pour l'instant
			for (Boolean b : autorisations.values()) {
				if (b!= null&& b) {
					isEditable = true;
					break;
				}
			}
		}
		return isEditable;
	}

	protected ComplementView buildComplement(Tiers tiers) {
		return new ComplementView(tiers, servicePM, serviceInfrastructureService, ibanValidator);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServicePM(ServicePersonneMoraleService servicePM) {
		this.servicePM = servicePM;
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
		Assert.notNull(tiers);

		boolean horsSuisse = false;

		for (ForFiscal ff : tiers.getForsFiscaux()) {
			if (!ff.isAnnule() && ff.isPrincipal() && DateRangeHelper.intersect(ff, range)) {
				horsSuisse |= ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS;
			}
		}

		return horsSuisse;
	}

	/**
	 * @param tiers
	 * @return true sur l'utilisateur connecté à les droits Ifosec et sécurité dossiers de modif le tiers retourne tjs false si le tiers n'est pas une PP ou un ménage
	 */
	protected boolean checkDroitEdit(Tiers tiers) {
		return autorisationManager.isEditAllowed(tiers);
	}

	/**
	 * Alimente la vue TiersGeneralView
	 *
	 * @param tiersView
	 * @throws AdresseException
	 */
	protected void setTiersGeneralView(TiersView tiersView, Tiers tiers) throws AdresseException {
		TiersGeneralView tiersGeneralView = tiersGeneralManager.getTiers(tiers, true);
		tiersView.setTiersGeneral(tiersGeneralView);
	}


	protected static interface AdressesResolverCallback {
		AdressesFiscalesHisto getAdresses(AdresseService service) throws AdresseException;

		void setAdressesView(List<AdresseView> adresses);

		void onException(String message, List<AdresseView> adressesEnErreur);
	}

	/**
	 * recuperation des adresses civiles historiques
	 *
	 * @param tiers                un tiers
	 * @param adressesHistoCiviles <b>vrai</b> si l'on vient l'historique complet des adresses; <b>faux</b> si l'on s'intéresse uniquement aux adresses actives aujourd'hui
	 * @return une liste d'adresses
	 * @throws ch.vd.uniregctb.common.DonneesCivilesException
	 *          si une adresse possède des données incohérentes (date de fin avant date de début, par exemple)
	 */
	public List<AdresseView> getAdressesHistoriquesCiviles(Tiers tiers, boolean adressesHistoCiviles) throws DonneesCivilesException {

		final Long noIndividu = tiersService.extractNumeroIndividuPrincipal(tiers);
		List<AdresseView> adresses = new ArrayList<AdresseView>();
		if (noIndividu != null) {
			if (adressesHistoCiviles) {
				final AdressesCivilesHistoriques adressesCivilesHisto = serviceCivilService.getAdressesHisto(noIndividu, false);
				if (adressesCivilesHisto != null) {
					// rempli tous les types d'adresse
					for (TypeAdresseCivil type : typesAdressesCiviles) {
						fillAdressesHistoCivilesView(adresses, adressesCivilesHisto, type);
					}
				}
			}
			else {
				final AdressesCivilesActives adressesCiviles = serviceCivilService.getAdresses(noIndividu, RegDate.get(), false);
				if (adressesCiviles != null) {
					// rempli tous les types d'adresse
					for (TypeAdresseCivil type : typesAdressesCiviles) {
						fillAdressesCivilesView(adresses, adressesCiviles, type);
					}
				}
			}
		}

		Collections.sort(adresses, new AdresseViewComparator());
		return adresses;
	}

	/**
	 * [UNIREG-3153] Résoud les adresses fiscales et met-à-disposition la liste des vues sur ces adresses. Cette méthode gère gracieusement les exceptions dans la résolution des adresses.
	 *
	 * @param callback un méthode de callback qui met-à-disposition les adresses et qui reçoit les vues des adresses en retour (ou le cas échéant, les messages d'erreur).
	 * @throws ServiceInfrastructureException en cas de problème sur le service infrastructure
	 */
	protected void resolveAdressesHisto(AdressesResolverCallback callback) throws ServiceInfrastructureException {

		try {
			List<AdresseView> adresses = new ArrayList<AdresseView>();

			final AdressesFiscalesHisto adressesFiscalesHisto = callback.getAdresses(adresseService);
			if (adressesFiscalesHisto != null) {
				// rempli tous les types d'adresse
				for (TypeAdresseTiers type : TypeAdresseTiers.values()) {
					fillAdressesView(adresses, adressesFiscalesHisto, type);
				}
				Collections.sort(adresses, new AdresseViewComparator());
			}

			callback.setAdressesView(adresses);
		}
		catch (Exception exception) {

			List<AdresseView> adresses = new ArrayList<AdresseView>();

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
	 * Renseigne la liste des adresses actives sur le form backing object. En cas d'erreur dans la résolution des adresses, les adresses en erreur et le message de l'erreur sont renseignés en lieu et
	 * place.
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

	public void fillAdressesHistoCivilesView(List<AdresseView> adressesView, AdressesCivilesHistoriques adressesCivilesHisto, TypeAdresseCivil type) throws DonneesCivilesException {
		final List<Adresse> adresses = adressesCivilesHisto.ofType(type);
		if (adresses == null) {
			// rien à faire
			return;
		}

		for (Adresse adresse : adresses) {
			AdresseView adresseView = createAdresseView(adresse, type);
			adressesView.add(adresseView);
		}
	}

	/**
	 * Remplir la collection des adressesView avec l'adresse civile du type spécifié.
	 */
	protected void fillAdressesCivilesView(List<AdresseView> adressesView, final AdressesCivilesActives adressesCiviles, TypeAdresseCivil type) throws DonneesCivilesException {

		if (TypeAdresseCivil.SECONDAIRE == type) {
			List<Adresse> addressesSecondaires = adressesCiviles.secondaires;
			if (addressesSecondaires == null) {
				// rien à faire
				return;
			}
			for (Adresse addressesSecondaire : addressesSecondaires) {
				final AdresseView adresseView = createAdresseView(addressesSecondaire, type);
				adressesView.add(adresseView);
			}

		}
		else {
			Adresse adresse = adressesCiviles.ofType(type);
			if (adresse == null) {
				// rien à faire
				return;
			}

			final AdresseView adresseView = createAdresseView(adresse, type);
			adressesView.add(adresseView);
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
		adresseView.setDateDebut(adresse.getDateDebut());
		adresseView.setDateFin(adresse.getDateFin());
		adresseView.setAnnule(adresse.isAnnule());
		adresseView.setId(adresse.getId());
		adresseView.setPermanente(adresse.isPermanente());

		final RueEtNumero rueEtNumero = AdresseServiceImpl.buildRueEtNumero(adresse);
		adresseView.setRue(rueEtNumero == null ? null : rueEtNumero.getRueEtNumero());

		final NpaEtLocalite npaEtLocalite = AdresseServiceImpl.buildNpaEtLocalite(adresse);
		adresseView.setLocalite(npaEtLocalite == null ? null : npaEtLocalite.toString());

		adresseView.setUsage(type);
		adresseView.setPaysOFS(adresse.getNoOfsPays());
		adresseView.setSource(adresse.getSource().getType());
		adresseView.setDefault(adresse.isDefault());
		adresseView.setComplements(adresse.getComplement());
		adresseView.setActive(adresse.isValidAt(RegDate.get()));
		adresseView.setSurVaud(estDansLeCanton(adresse));
		return adresseView;
	}

	/**
	 * Crée une adresse view à partir d'une adresse civile
	 *
	 * @param adresse une adresse civile
	 * @param type    le type d'adresse civile
	 * @return une adresse view
	 * @throws DonneesCivilesException si l'adresse civile est incohérente
	 */
	public AdresseView createAdresseView(Adresse adresse, TypeAdresseCivil type) throws DonneesCivilesException {
		AdresseGenerique adrGen = new AdresseCivileAdapter(adresse, (Tiers) null, false, getServiceInfrastructureService());
		AdresseView adresseView = createAdresseView(adrGen, null);
		adresseView.setUsageCivil(type);
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
		List<AdresseView> resultat = new ArrayList<AdresseView>();
		for (AdresseView view : adresses) {
			//UNIREG-1813 L'adresse domicile est retiré du bloc fiscal
			if (TypeAdresseTiers.DOMICILE != view.getUsage()) {
				if (view.getDateFin() == null || AdresseGenerique.SourceType.CIVILE != view.getSource()) {
					resultat.add(view);
				}

			}
		}
		return resultat;  //To change body of created methods use File | Settings | File Templates.
	}

	protected List<AdresseView> removeAdresseAnnulee(List<AdresseView> adresses) {
		List<AdresseView> resultat = new ArrayList<AdresseView>();
		for (AdresseView view : adresses) {
			if (!view.isAnnule()) {
				resultat.add(view);
			}
		}
		return resultat;  //To change body of created methods use File | Settings | File Templates.
	}

	/**
	 * Compte le nombre de rapports prestation imposable pour un débiteur
	 *
	 * @param numeroDebiteur
	 * @param rapportsPrestationHisto
	 * @return
	 */
	@Transactional(readOnly = true)
	public int countRapportsPrestationImposable(Long numeroDebiteur, boolean rapportsPrestationHisto) {
		return rapportEntreTiersDAO.countRapportsPrestationImposable(numeroDebiteur, !rapportsPrestationHisto);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHostCivilService(HostCivilService hostCivilService) {
		this.hostCivilService = hostCivilService;
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

	public HostPersonneMoraleService getHostPersonneMoraleService() {
		return hostPersonneMoraleService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHostPersonneMoraleService(HostPersonneMoraleService hostPersonneMoraleService) {
		this.hostPersonneMoraleService = hostPersonneMoraleService;
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

	/**
	 * Annule un tiers
	 *
	 * @param numero
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerTiers(Long numero) {
		final Tiers tiers = tiersService.getTiers(numero);
		if (tiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.tiers.inexistant", null, WebContextUtils.getDefaultLocale()));
		}
		tiersService.annuleTiers(tiers);
	}
}
