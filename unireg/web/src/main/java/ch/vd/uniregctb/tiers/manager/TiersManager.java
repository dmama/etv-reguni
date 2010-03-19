package ch.vd.uniregctb.tiers.manager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.vd.uniregctb.tiers.*;
import org.apache.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdresseSupplementaire;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.adresse.AdresseTiersDAO;
import ch.vd.uniregctb.adresse.AdressesFiscales;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.adresse.AdresseGenerique.Source;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.entreprise.EntrepriseView;
import ch.vd.uniregctb.entreprise.HostPersonneMoraleService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.individu.HostCivilService;
import ch.vd.uniregctb.individu.IndividuView;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.lr.view.ListeRecapDetailComparator;
import ch.vd.uniregctb.lr.view.ListeRecapDetailView;
import ch.vd.uniregctb.rapport.TypeRapportEntreTiersWeb;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.rt.view.RapportPrestationView;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.situationfamille.VueSituationFamille;
import ch.vd.uniregctb.situationfamille.VueSituationFamilleMenageCommun;
import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.tiers.view.AdresseViewComparator;
import ch.vd.uniregctb.tiers.view.DebiteurView;
import ch.vd.uniregctb.tiers.view.ForDebiteurViewComparator;
import ch.vd.uniregctb.tiers.view.ForFiscalView;
import ch.vd.uniregctb.tiers.view.ForFiscalViewComparator;
import ch.vd.uniregctb.tiers.view.SituationFamilleView;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.tiers.view.TiersView;
import ch.vd.uniregctb.tiers.view.TiersVisuView;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.SensRapportEntreTiers;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Methodes annexes utilisées par TiersVisuManager et TiersEditManager
 *
 * @author xcifde
 *
 */
public class TiersManager implements MessageSourceAware {

	protected final Logger LOGGER = Logger.getLogger(TiersManager.class);

	private HostCivilService hostCivilService;

	private HostPersonneMoraleService hostPersonneMoraleService;

	private ServiceCivilService serviceCivilService;

	private final Calendar calToday = new GregorianCalendar();

	protected TiersDAO tiersDAO;

	private AdresseTiersDAO adresseTiersDAO;

	protected TiersService tiersService;

	private ServiceInfrastructureService serviceInfrastructureService;

	private AdresseManager adresseManager;

	protected TiersGeneralManager tiersGeneralManager;

	protected AdresseService adresseService;

	protected MessageSource messageSource;

	protected SituationFamilleService situationFamilleService;

	private RapportEntreTiersDAO rapportEntreTiersDAO;

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
			individuView = getHostCivilService().getIndividu(noIndividu);
		}
		if (habitant.getDateDeces() != null) {//habitant décédé fiscalement
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
		Long noEntreprise = entreprise.getNumeroEntreprise();
		if (noEntreprise != null) {
			entrepriseView = getHostPersonneMoraleService().get(noEntreprise);
		}
		return entrepriseView;
	}

	/**
	 * Recupère les rapports de filiation de type PARENT ou ENFANT
	 *
	 * @param numeroTiers
	 * @param sens
	 * @return
	 */
	protected List<RapportView> getRapportsFiliation(PersonnePhysique habitant) {
		List<RapportView> rapportsView = new ArrayList<RapportView>();
		Assert.notNull(habitant.getNumeroIndividu(), "La personne physique n'a pas de numéro d'individu connu");

		int year = getCalToday().get(Calendar.YEAR);

		EnumAttributeIndividu[] enumValues = new EnumAttributeIndividu[] {
			EnumAttributeIndividu.ENFANTS, EnumAttributeIndividu.PARENTS
		};
		Individu ind = getServiceCivilService().getIndividu(habitant.getNumeroIndividu(), year, enumValues);
		Collection<Individu> listFiliations = ind.getEnfants();
		Iterator<Individu> it = listFiliations.iterator();
		for (int i = 0; i < listFiliations.size(); i++) {
			Individu individu = it.next();
			PersonnePhysique habFils  = tiersDAO.getPPByNumeroIndividu(individu.getNoTechnique());
			if (habFils != null) {
				RapportView rapportView = new RapportView();
				rapportView.setSensRapportEntreTiers(SensRapportEntreTiers.OBJET);
				rapportView.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.FILIATION);
				rapportView.setNumero(habFils.getNumero());
				rapportView.setNomCourrier1(tiersService.getNomPrenom(individu));
				rapportView.setDateDebut(RegDate.asJavaDate(individu.getDateNaissance()));
				rapportsView.add(rapportView);
			}
		}

		Individu mere = ind.getMere();
		if (mere != null) {
			PersonnePhysique habMere  = tiersDAO.getPPByNumeroIndividu(mere.getNoTechnique());
			if (habMere != null) {
				RapportView rapportMereView = new RapportView();
				rapportMereView.setNumero(habitant.getNumero());
				rapportMereView.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);
				rapportMereView.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.FILIATION);
				rapportMereView.setNumero(habMere.getNumero());
				rapportMereView.setNomCourrier1(tiersService.getNomPrenom(mere));
				rapportMereView.setDateDebut(RegDate.asJavaDate(ind.getDateNaissance()));
				rapportsView.add(rapportMereView);
			}
		}
		Individu pere = ind.getPere();
		if (pere != null) {
			PersonnePhysique habPere  = tiersDAO.getPPByNumeroIndividu(pere.getNoTechnique());
			if (habPere != null) {
				RapportView rapportPereView = new RapportView();
				rapportPereView.setNumero(habitant.getNumero());
				rapportPereView.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);
				rapportPereView.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.FILIATION);
				rapportPereView.setNumero(habPere.getNumero());
				rapportPereView.setNomCourrier1(tiersService.getNomPrenom(pere));
				rapportPereView.setDateDebut(RegDate.asJavaDate(ind.getDateNaissance()));
				rapportsView.add(rapportPereView);
			}
		}

		return rapportsView;
	}

	/**
	 * Copie les informations de l'individu Individu dans la vue IndividuView
	 *
	 * @param indImpl
	 * @return
	 */
	protected IndividuView createIndividuView(Individu indImpl) {

		IndividuView individu = new IndividuView();

		individu.setNumeroIndividu(indImpl.getNoTechnique());
		individu.setNom(indImpl.getDernierHistoriqueIndividu().getNom());
		individu.setPrenom(indImpl.getDernierHistoriqueIndividu().getPrenom());
		individu.setDateNaissance(RegDate.asJavaDate(indImpl.getDateNaissance()));
		if (indImpl.isSexeMasculin()) {
			individu.setSexe(Sexe.MASCULIN);
		}
		else {
			individu.setSexe(Sexe.FEMININ);
		}
		return individu;
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
					final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) r.getObjet();
					final DebiteurView debiteurView = new DebiteurView();
					debiteurView.setAnnule(r.isAnnule());
					debiteurView.setId(r.getId());
					debiteurView.setNumero(dpi.getNumero());
					debiteurView.setCategorieImpotSource(dpi.getCategorieImpotSource());
					debiteurView.setPersonneContact(dpi.getPersonneContact());
					final List<String> nomCourrier = getAdresseService().getNomCourrier(dpi, null, false);
					debiteurView.setNomCourrier(nomCourrier);
					debiteurView.setComplementNom(dpi.getComplementNom());
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
	protected List<RapportView> getRapports(Tiers tiers) throws AdresseException{
		List<RapportView> rapportsView = new ArrayList<RapportView>();

		// Rapport entre tiers Objet
		for (RapportEntreTiers rapportEntreTiers : tiers.getRapportsObjet()) {
			if (!rapportEntreTiers.getType().equals(TypeRapportEntreTiers.PRESTATION_IMPOSABLE)) {
				final RapportView rapportView = new RapportView();
				rapportView.setId(rapportEntreTiers.getId());
				rapportView.setAnnule(rapportEntreTiers.isAnnule());
				rapportView.setSensRapportEntreTiers(SensRapportEntreTiers.OBJET);
				rapportView.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.fromCore(rapportEntreTiers.getType()));
				rapportView.setDateDebut(rapportEntreTiers.getDateDebut());
				rapportView.setDateFin(rapportEntreTiers.getDateFin());

				final Tiers tiersSujet = rapportEntreTiers.getSujet();
				rapportView.setNumero(tiersSujet.getNumero());

				final List<String> nomCourrier = getAdresseService().getNomCourrier(tiersSujet, null, false);
				rapportView.setNomCourrier(nomCourrier);
				rapportsView.add(rapportView);
			}
		}

		// Rapport entre tiers Sujet
		for (RapportEntreTiers rapportEntreTiers : tiers.getRapportsSujet()) {
			if (!rapportEntreTiers.getType().equals(TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE)) {
				final RapportView rapportView = new RapportView();
				rapportView.setId(rapportEntreTiers.getId());
				rapportView.setAnnule(rapportEntreTiers.isAnnule());
				rapportView.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);
				rapportView.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.fromCore(rapportEntreTiers.getType()));
				rapportView.setDateDebut(rapportEntreTiers.getDateDebut());
				rapportView.setDateFin(rapportEntreTiers.getDateFin());

				final Tiers tiersObjet = rapportEntreTiers.getObjet();
				rapportView.setNumero(tiersObjet.getNumero());

				final List<String> nomCourrier = getAdresseService().getNomCourrier(tiersObjet, null, false);
				if ( nomCourrier != null && nomCourrier.size() != 0) {
					rapportView.setNomCourrier(nomCourrier);
				}
				if (rapportEntreTiers instanceof RapportPrestationImposable) {
					final RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportEntreTiers;
					rapportView.setTypeActivite(rapportPrestationImposable.getTypeActivite());
					rapportView.setTauxActivite(rapportPrestationImposable.getTauxActivite());
				}
				else if (rapportEntreTiers instanceof RepresentationConventionnelle) {
					final RepresentationConventionnelle repres = (RepresentationConventionnelle) rapportEntreTiers;
					final Boolean b = repres.getExtensionExecutionForcee();
					rapportView.setExtensionExecutionForcee(b != null && b.booleanValue());
				}
				rapportsView.add(rapportView);
			}
		}
		Collections.sort(rapportsView);
		return rapportsView;
	}


	/**
	 * Alimente List<RapportView>
	 *
	 * @param tiers
	 * @return
	 * @throws AdresseException
	 */
	protected void setContribuablesAssocies(TiersView tiersView, DebiteurPrestationImposable debiteur) throws AdresseException{
		List<RapportView> rapportsView = new ArrayList<RapportView>();

		// Rapport entre tiers Objet
		Set<RapportEntreTiers> rapports = debiteur.getRapportsObjet();
		for (RapportEntreTiers rapportEntreTiers : rapports) {
			if (rapportEntreTiers.getType().equals(TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE)) {
				RapportView rapportView = new RapportView();
				rapportView.setId(rapportEntreTiers.getId());
				rapportView.setAnnule(rapportEntreTiers.isAnnule());
				Tiers tiersSujet = rapportEntreTiers.getSujet();
				rapportView.setNumero(tiersSujet.getNumero());
				List<String> nomCourrier = getAdresseService().getNomCourrier(tiersSujet, null, false);
				rapportView.setNomCourrier(nomCourrier);
				rapportsView.add(rapportView);
			}
		}
		tiersView.setContribuablesAssocies(rapportsView);
	}

	/**
	 * Alimente List<RapportPrestationView>
	 *
	 * @param dpi
	 * @return
	 * @throws AdresseException
	 */
	protected List<RapportPrestationView> getRapportsPrestation(DebiteurPrestationImposable dpi, WebParamPagination pagination) throws AdresseException{
		List<RapportPrestationView>  rapportPrestationViews = new ArrayList<RapportPrestationView>();
		List<RapportPrestationImposable> rapports = getRapportEntreTiersDAO().getRapportsPrestationImposable(dpi.getNumero(), pagination);
		for (RapportPrestationImposable rapport : rapports) {
			RapportPrestationImposable rapportPrestationImposable = rapport;
			RapportPrestationView rapportPrestationView = new RapportPrestationView();
			rapportPrestationView.setId(rapportPrestationImposable.getId());
			rapportPrestationView.setAnnule(rapportPrestationImposable.isAnnule());
			rapportPrestationView.setTypeActivite(rapportPrestationImposable.getTypeActivite());
			rapportPrestationView.setTauxActivite(rapportPrestationImposable.getTauxActivite());
			rapportPrestationView.setDateDebut(rapportPrestationImposable.getDateDebut());
			rapportPrestationView.setDateFin(rapportPrestationImposable.getDateFin());
			Tiers tiersObjet = rapportPrestationImposable.getSujet();
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
			List<String> nomCourrier = getAdresseService().getNomCourrier(tiersObjet, null, false);
			rapportPrestationView.setNomCourrier(nomCourrier);
			rapportPrestationViews.add(rapportPrestationView);
		}
		return rapportPrestationViews;
	}


	/**
	 * Alimente List<RapportPrestationView>
	 *
	 * @param dpi
	 * @return
	 * @throws AdresseException
	 */
	protected List<RapportPrestationView> getRapportsPrestation(DebiteurPrestationImposable dpi) throws AdresseException{
		List<RapportPrestationView>  rapportPrestationViews = new ArrayList<RapportPrestationView>();
		Set<RapportEntreTiers> rapports = dpi.getRapportsObjet();
		for (RapportEntreTiers rapport : rapports) {
			if (rapport instanceof RapportPrestationImposable) {
				RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapport;
				RapportPrestationView rapportPrestationView = new RapportPrestationView();
				rapportPrestationView.setId(rapportPrestationImposable.getId());
				rapportPrestationView.setAnnule(rapportPrestationImposable.isAnnule());
				rapportPrestationView.setTypeActivite(rapportPrestationImposable.getTypeActivite());
				rapportPrestationView.setTauxActivite(rapportPrestationImposable.getTauxActivite());
				rapportPrestationView.setDateDebut(rapportPrestationImposable.getDateDebut());
				rapportPrestationView.setDateFin(rapportPrestationImposable.getDateFin());
				Tiers tiersObjet = rapportPrestationImposable.getSujet();
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
				List<String> nomCourrier = getAdresseService().getNomCourrier(tiersObjet, null, false);
				rapportPrestationView.setNomCourrier(nomCourrier);
				rapportPrestationViews.add(rapportPrestationView);
			}
		}
		Collections.sort(rapportPrestationViews);
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
				lrView.setEtat(lr.getDernierEtat().getEtat());
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
	 * @param noLocalite
	 *            le numéro OFS de la localité.
	 * @return la localité correspondante, ou <b>null</b> si elle n'existe pas.
	 */
	protected Localite getLocaliteByONRP(final Integer noLocalite) {
		try {
			return getServiceInfrastructureService().getLocaliteByONRP(noLocalite);
		}
		catch (InfrastructureException e) {
			LOGGER.error("Impossible de trouver la localité avec le numéro OFS = " + noLocalite, e);
			return null;
		}
	}

	/**
	 * @param numeroRue
	 *            le numéro technique de la rue.
	 * @return la rue correspondante, ou <b>null</b> si elle n'existe pas.
	 */
	protected Rue getRueByNumero(Integer numeroRue) {
		try {
			return getServiceInfrastructureService().getRueByNumero(numeroRue.intValue());
		}
		catch (InfrastructureException e) {
			LOGGER.error("Impossible de trouver la rue avec le numéro = " + numeroRue, e);
			return null;
		}
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

			if (tiersPrincipal.isHabitant()) {
				IndividuView individu = getIndividuView(tiersPrincipal);
				tiersView.setIndividu(individu);
			}
		}

		/* 2eme tiers */
		if (tiersConjoint != null) {
			tiersView.setTiersConjoint(tiersConjoint);

			if (tiersConjoint.isHabitant()) {
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
	 * @param entreprise
	 * @param tiersView
	 * @throws AdresseException
	 */
	protected void setDebiteurPrestationImposable(TiersView tiersView, DebiteurPrestationImposable dpi, WebParamPagination webParamPagination) throws AdresseException{
		tiersView.setTiers(dpi);
		tiersView.setRapportsPrestation(getRapportsPrestation(dpi, webParamPagination));
		tiersView.setLrs(getListesRecapitulatives(dpi));
		if (dpi.getContribuable() == null) {
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
		ForGestion forGestion = tiersService.getDernierForGestionConnu(tiersView.getTiers(), null);
		List<ForFiscalView> forsFiscauxView = new ArrayList<ForFiscalView> ();
		Set<ForFiscal> forsFiscaux = contribuable.getForsFiscaux();
		if (forsFiscaux != null) {
			for (ForFiscal forFiscal : forsFiscaux) {
				ForFiscalView forFiscalView = new ForFiscalView();
				if (contribuable.getDernierForFiscalPrincipal() != null) {
					if (contribuable.getDernierForFiscalPrincipal().getId().longValue() == forFiscal.getId().longValue()) {
						forFiscalView.setDernierForPrincipal(true);
					}
					else {
						forFiscalView.setDernierForPrincipal(false);
					}
				}
				forFiscalView.setId(forFiscal.getId());
				forFiscalView.setNumeroCtb(forFiscal.getTiers().getNumero());
				forFiscalView.setGenreImpot(forFiscal.getGenreImpot());
				forFiscalView.setAnnule(forFiscal.isAnnule());
				if (forFiscal instanceof ForFiscalAutreImpot) {
					forFiscalView.setDateEvenement(forFiscal.getDateDebut());
				}
				else {
					forFiscalView.setDateOuverture(forFiscal.getDateDebut());
					forFiscalView.setDateFermeture(forFiscal.getDateFin());
				}
				TypeAutoriteFiscale typeForFiscal = forFiscal.getTypeAutoriteFiscale();
				forFiscalView.setTypeAutoriteFiscale(typeForFiscal);
				switch (typeForFiscal) {
				case COMMUNE_OU_FRACTION_VD:
					forFiscalView.setNumeroForFiscalCommune(forFiscal.getNumeroOfsAutoriteFiscale());
					break;
				case COMMUNE_HC:
					forFiscalView.setNumeroForFiscalCommuneHorsCanton(forFiscal.getNumeroOfsAutoriteFiscale());
					break;
				case PAYS_HS:
					forFiscalView.setNumeroForFiscalPays(forFiscal.getNumeroOfsAutoriteFiscale());
					break;
				default:
					break;
				}
				if (forFiscal instanceof ForFiscalRevenuFortune) {
					ForFiscalRevenuFortune forFiscalRevenuFortune = (ForFiscalRevenuFortune) forFiscal;
					forFiscalView.setMotifOuverture(forFiscalRevenuFortune.getMotifOuverture());
					forFiscalView.setMotifFermeture(forFiscalRevenuFortune.getMotifFermeture());
					forFiscalView.setMotifRattachement(forFiscalRevenuFortune.getMotifRattachement());
					if (forGestion != null) {
						long idForGestion = forGestion.getSousjacent().getId();
						long idFor = forFiscalView.getId();
						forFiscalView.setForGestion(idForGestion == idFor);
					}
					else {
						forFiscalView.setForGestion(Boolean.FALSE);
					}
				}
				if (forFiscal instanceof ForFiscalPrincipal) {
					ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) forFiscal;
					forFiscalView.setModeImposition(forFiscalPrincipal.getModeImposition());
				}

				forFiscalView.setNatureForFiscal(forFiscal.getClass().getSimpleName());
				forsFiscauxView.add(forFiscalView);
			}
			Collections.sort(forsFiscauxView, new ForFiscalViewComparator());
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
		List<ForFiscalView> forsFiscauxView = new ArrayList<ForFiscalView> ();
		Set<ForFiscal> forsFiscaux = dpi.getForsFiscaux();
		if (forsFiscaux != null) {
			for (ForFiscal forFiscal : forsFiscaux) {
				ForFiscalView forFiscalView = new ForFiscalView();
				forFiscalView.setId(forFiscal.getId());
				forFiscalView.setNumeroCtb(forFiscal.getTiers().getNumero());
				forFiscalView.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
				forFiscalView.setAnnule(forFiscal.isAnnule());
				forFiscalView.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				forFiscalView.setNumeroForFiscalCommune(forFiscal.getNumeroOfsAutoriteFiscale());
				forFiscalView.setDateOuverture(forFiscal.getDateDebut());
				forFiscalView.setDateFermeture(forFiscal.getDateFin());
				forFiscalView.setNatureForFiscal(forFiscal.getClass().getSimpleName());
				forsFiscauxView.add(forFiscalView);
			}
			Collections.sort(forsFiscauxView, new ForDebiteurViewComparator());
			tiersView.setForsFiscaux(forsFiscauxView);
		}
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
	 * @param tiersView
	 * @param contribuable
	 * @throws AdresseException
	 */
	protected void setSituationsFamille(TiersView tiersView, Contribuable contribuable) throws AdresseException {

		List<SituationFamilleView> situationsFamilleView = new ArrayList<SituationFamilleView>();
		List<VueSituationFamille> situationsFamille = situationFamilleService.getVueHisto(contribuable);
		Collections.reverse(situationsFamille); // UNIREG-647

		for (VueSituationFamille situation : situationsFamille) {

			SituationFamilleView view = new SituationFamilleView();
			view.setAnnule(situation.isAnnule());
			view.setId(situation.getId());
			view.setDateDebut(situation.getDateDebut());
			view.setDateFin(situation.getDateFin());
			view.setNombreEnfants(situation.getNombreEnfants());
			view.setEtatCivil(situation.getEtatCivil());
			view.setAllowed(situation.getSource()==VueSituationFamille.Source.FISCALE_TIERS);

			if (situation instanceof VueSituationFamilleMenageCommun) {

				final VueSituationFamilleMenageCommun situationMC = (VueSituationFamilleMenageCommun) situation;
				view.setTarifImpotSource(situationMC.getTarifApplicable());
				view.setNatureSituationFamille(situationMC.getClass().getSimpleName());

				final Long numeroContribuablePrincipal = situationMC.getNumeroContribuablePrincipal();
				if (numeroContribuablePrincipal != null) {
					final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(numeroContribuablePrincipal.longValue());
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
	 * @param tiers
	 * @param allowedOnglet
	 * @return true si l'utilisateur a le droit d'éditer le tiers
	 */
	protected boolean setDroitEdition(Tiers tiers, Map<String, Boolean> allowedOnglet){
		boolean isEditable = false;

		final Niveau acces = SecurityProvider.getDroitAcces(tiers);
		if (acces == null || acces.equals(Niveau.LECTURE)) {
			allowedOnglet.put(TiersVisuView.MODIF_FISCAL, Boolean.FALSE);
			allowedOnglet.put(TiersVisuView.MODIF_CIVIL, Boolean.FALSE);
			allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.FALSE);
			allowedOnglet.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.FALSE);
			allowedOnglet.put(TiersVisuView.MODIF_RAPPORT, Boolean.FALSE);
			allowedOnglet.put(TiersVisuView.MODIF_DOSSIER, Boolean.FALSE);
			allowedOnglet.put(TiersVisuView.MODIF_DEBITEUR, Boolean.FALSE);
			allowedOnglet.put(TiersVisuView.MODIF_MOUVEMENT, Boolean.FALSE);
			allowedOnglet.put(TiersEditView.FISCAL_FOR_PRINC, Boolean.FALSE);
			allowedOnglet.put(TiersEditView.FISCAL_FOR_SEC, Boolean.FALSE);
			allowedOnglet.put(TiersEditView.FISCAL_FOR_AUTRE, Boolean.FALSE);
			allowedOnglet.put(TiersEditView.FISCAL_SIT_FAMILLLE, Boolean.FALSE);
			allowedOnglet.put(TiersEditView.ADR_D, Boolean.FALSE);
			allowedOnglet.put(TiersEditView.ADR_C, Boolean.FALSE);
			allowedOnglet.put(TiersEditView.ADR_B, Boolean.FALSE);
			allowedOnglet.put(TiersEditView.ADR_P, Boolean.FALSE);
			allowedOnglet.put(TiersEditView.COMPLEMENT_COMMUNICATION, Boolean.FALSE);
			allowedOnglet.put(TiersEditView.COMPLEMENT_COOR_FIN, Boolean.FALSE);
			allowedOnglet.put(TiersEditView.DOSSIER_TRAVAIL, Boolean.FALSE);
			allowedOnglet.put(TiersEditView.DOSSIER_NO_TRAVAIL, Boolean.FALSE);
			allowedOnglet.put(TiersVisuView.MODIF_DI, Boolean.FALSE);
			return false;
		}

		if (SecurityProvider.isGranted(Role.COOR_FIN)) {
			allowedOnglet.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.TRUE);
			allowedOnglet.put(TiersEditView.COMPLEMENT_COOR_FIN, Boolean.TRUE);
			isEditable = true;
		}

		if (SecurityProvider.isGranted(Role.SUIVI_DOSS)) {
			allowedOnglet.put(TiersVisuView.MODIF_MOUVEMENT, Boolean.TRUE);
			isEditable = true;
		}
		if (tiers.isAnnule()) {
			//droit pour un tiers annulé
			if (SecurityProvider.isGranted(Role.MODIF_NONHAB_INACTIF)) {
				allowedOnglet.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.TRUE);
				allowedOnglet.put(TiersEditView.COMPLEMENT_COMMUNICATION, Boolean.TRUE);
				allowedOnglet.put(TiersVisuView.MODIF_DOSSIER, Boolean.FALSE);
				allowedOnglet.put(TiersVisuView.MODIF_FISCAL, Boolean.FALSE);
				allowedOnglet.put(TiersVisuView.MODIF_DI, Boolean.FALSE);
				if (SecurityProvider.isGranted(Role.ADR_PP_D)) {
					allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					allowedOnglet.put(TiersEditView.ADR_D, Boolean.TRUE);
				}
				if (SecurityProvider.isGranted(Role.ADR_PP_B)) {
					allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					allowedOnglet.put(TiersEditView.ADR_B, Boolean.TRUE);
				}
				if (SecurityProvider.isGranted(Role.ADR_PP_C)) {
					allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					allowedOnglet.put(TiersEditView.ADR_C, Boolean.TRUE);
				}
				if (SecurityProvider.isGranted(Role.ADR_P)) {
						allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
						allowedOnglet.put(TiersEditView.ADR_P, Boolean.TRUE);
				}
				isEditable = true;
			}
			return isEditable;
		}

		if (tiers instanceof Contribuable) {
			if (SecurityProvider.isGranted(Role.CREATE_DPI)) {
				allowedOnglet.put(TiersVisuView.MODIF_DEBITEUR, Boolean.TRUE);
				isEditable = true;
			}
			if ((tiers instanceof PersonnePhysique || tiers instanceof MenageCommun)) {
				if (SecurityProvider.isGranted(Role.SIT_FAM)) {
					Contribuable contribuable = (Contribuable) tiers;
					boolean isSitFamActive = isSituationFamilleActive(contribuable);
					boolean civilOK = true;
					if (tiers instanceof PersonnePhysique) {
						PersonnePhysique pp = (PersonnePhysique)tiers;
						if (pp.isHabitant()) {
						Individu ind = serviceCivilService.getIndividu(pp.getNumeroIndividu(), RegDate.get().year());
							for (EtatCivil etatCivil : ind.getEtatsCivils()) {
								if (etatCivil.getDateDebutValidite() == null){
									civilOK = false;
								}
							}
						}
					}
					if (civilOK && (isSitFamActive || !contribuable.getSituationsFamille().isEmpty())) {
						allowedOnglet.put(TiersVisuView.MODIF_FISCAL, Boolean.TRUE);
						allowedOnglet.put(TiersEditView.FISCAL_SIT_FAMILLLE, Boolean.TRUE);
						isEditable = true;
					}
				}
				if (SecurityProvider.isGranted(Role.DI_EMIS_PP) || SecurityProvider.isGranted(Role.DI_DELAI_PM) ||
					SecurityProvider.isGranted(Role.DI_DUPLIC_PP) || SecurityProvider.isGranted(Role.DI_QUIT_PP) ||
					SecurityProvider.isGranted(Role.DI_SOM_PP)) {
					allowedOnglet.put(TiersVisuView.MODIF_DI, Boolean.TRUE);
					isEditable = true;
				}
			}
		}

		if (tiers instanceof PersonnePhysique) {
			PersonnePhysique pp = (PersonnePhysique)tiers;
			if (pp.isHabitant()) {
				isEditable = setDroitHabitant(tiers, allowedOnglet) || isEditable;
			}
			else {
				isEditable = setDroitNonHabitant(tiers, allowedOnglet) || isEditable;
			}
		}
		else if (tiers instanceof MenageCommun) {
			//les ménages n'ont jamais les onglets civil et rapport prestation
			MenageCommun menageCommun = (MenageCommun) tiers;
			boolean isHabitant = false;
			for (PersonnePhysique pp : menageCommun.getPersonnesPhysiques()) {
				if (pp.isHabitant()) {
					isHabitant = true;
					break;
				}
			}
			if (isHabitant) {
				isEditable = setDroitHabitant(tiers, allowedOnglet) || isEditable;
			}
			else {
				isEditable = setDroitNonHabitant(tiers, allowedOnglet) || isEditable;
			}
		}
		else if (tiers instanceof AutreCommunaute) {
			//les autres communautés n'ont jamais les onglets fiscal, rapport prestation et dossier apparenté
			if (SecurityProvider.isGranted(Role.MODIF_AC)) {
				allowedOnglet.put(TiersVisuView.MODIF_CIVIL, Boolean.TRUE);
				allowedOnglet.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.TRUE);
				allowedOnglet.put(TiersEditView.COMPLEMENT_COMMUNICATION, Boolean.TRUE);
				if (SecurityProvider.isGranted(Role.ADR_PM_D)) {
					allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					allowedOnglet.put(TiersEditView.ADR_D, Boolean.TRUE);
				}
				if (SecurityProvider.isGranted(Role.ADR_PM_B)) {
					allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					allowedOnglet.put(TiersEditView.ADR_B, Boolean.TRUE);
				}
				if (SecurityProvider.isGranted(Role.ADR_PM_C)) {
					allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					allowedOnglet.put(TiersEditView.ADR_C, Boolean.TRUE);
				}
				if (SecurityProvider.isGranted(Role.ADR_P)) {
						allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
						allowedOnglet.put(TiersEditView.ADR_P, Boolean.TRUE);
				}
				isEditable = true;
			}
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			//les DPI n'ont jamais les onglets civil, dossier apparenté et débiteur IS
			if (SecurityProvider.isGranted(Role.CREATE_DPI)) {
				allowedOnglet.put(TiersVisuView.MODIF_FISCAL, Boolean.TRUE);
				allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
				allowedOnglet.put(TiersEditView.ADR_B, Boolean.TRUE);
				allowedOnglet.put(TiersEditView.ADR_C, Boolean.TRUE);
				allowedOnglet.put(TiersEditView.ADR_D, Boolean.TRUE);
				allowedOnglet.put(TiersEditView.ADR_P, Boolean.TRUE);
				allowedOnglet.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.TRUE);
				allowedOnglet.put(TiersEditView.COMPLEMENT_COMMUNICATION, Boolean.TRUE);
				isEditable = true;
			}
			if (SecurityProvider.isGranted(Role.RT)) {
				allowedOnglet.put(TiersVisuView.MODIF_RAPPORT, Boolean.TRUE);
				isEditable = true;
			}
		}
		else {//Entreprise, Etablissement ou CollectiviteAdministrative non éditables pour le moment
			isEditable = false;
		}
		return isEditable;
	}

	private static enum TypeImposition {
		AUCUN_FOR_ACTIF,
		ORDINAIRE_DEPENSE,
		SOURCIER;

		public boolean isOrdinaireDepenseOuNonActif() {
			return this == AUCUN_FOR_ACTIF || this == ORDINAIRE_DEPENSE;
		}

		public boolean isSourcierOuNonActif() {
			return this == AUCUN_FOR_ACTIF || this == SOURCIER;
		}
	}

	private static TypeImposition calculeTypeImposition(Tiers tiers) {
		final TypeImposition type;
		final ForFiscalPrincipal forFiscalPrincipal = tiers.getForFiscalPrincipalAt(null);
		if (forFiscalPrincipal != null) {
			final ModeImposition modeImposition = tiers.getForFiscalPrincipalAt(null).getModeImposition();
			switch (modeImposition) {
				case SOURCE :
				case MIXTE_137_1 :
				case MIXTE_137_2 :
					type = TypeImposition.SOURCIER;
					break;
				default :
					type = TypeImposition.ORDINAIRE_DEPENSE;
					break;
			}
		}
		else {
			type = TypeImposition.AUCUN_FOR_ACTIF;
		}
		return type;
	}

	/**
	 * Le type d'autorité fiscale est null en cas d'absence de for fiscal principal actif
	 * @param tiers
	 * @return
	 */
	private static Pair<TypeImposition, TypeAutoriteFiscale> calculeTypeImpositionEtAutoriteFiscale(Tiers tiers) {
		final TypeImposition typeImposition = calculeTypeImposition(tiers);
		final TypeAutoriteFiscale typeAutoriteFiscale;
		final ForFiscalPrincipal forFiscalPrincipal = tiers.getForFiscalPrincipalAt(null);
		if (forFiscalPrincipal != null) {
			typeAutoriteFiscale = forFiscalPrincipal.getTypeAutoriteFiscale();
		}
		else {
			typeAutoriteFiscale = null;
		}
		return new Pair<TypeImposition, TypeAutoriteFiscale>(typeImposition, typeAutoriteFiscale);
	}

	/**
	 * enrichi la map de droit d'édition des onglets pour un habitant ou un ménage commun considéré habitant
	 * @param tiers
	 * @param allowedOnglet
	 */
	private boolean setDroitHabitant(Tiers tiers, Map<String, Boolean> allowedOnglet) {

		Assert.isTrue(tiers instanceof PersonnePhysique || tiers instanceof MenageCommun, "Le tiers " + tiers.getNumero() + " n'est ni une personne physique ni un ménage commun");

		//les habitants n'ont jamais les onglets civil et rapport prestation
		boolean isEditable = codeFactorise1(tiers, allowedOnglet);
		if (checkDroitEditPP(tiers)) {
			codeFactorise2(allowedOnglet);
			isEditable = true;
		}
		isEditable = codeFactorise3(tiers, allowedOnglet, isEditable);

		final boolean isPersonnePhysique = tiers instanceof PersonnePhysique;
		TypeImposition typeImposition = calculeTypeImposition(tiers);
		if (typeImposition == TypeImposition.AUCUN_FOR_ACTIF && isPersonnePhysique) {
			// [UNIREG-1736] un sourcier est un individu qui a un for source ou dont le
			// ménage commun actif a un for source...
			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple((PersonnePhysique) tiers, null);
			if (ensemble != null) {
				final MenageCommun menage = ensemble.getMenage();
				typeImposition = calculeTypeImposition(menage);
			}
		}

		if ((typeImposition.isOrdinaireDepenseOuNonActif() && SecurityProvider.isGranted(Role.FOR_PRINC_ORDDEP_HAB)) ||
			(typeImposition.isSourcierOuNonActif() && SecurityProvider.isGranted(Role.FOR_PRINC_SOURC_HAB))) {
			allowedOnglet.put(TiersVisuView.MODIF_FISCAL, Boolean.TRUE);
			allowedOnglet.put(TiersEditView.FISCAL_FOR_PRINC, Boolean.TRUE);
			isEditable = true;
		}
		if (isPersonnePhysique && typeImposition == TypeImposition.SOURCIER && SecurityProvider.isGranted(Role.RT)) {
			allowedOnglet.put(TiersVisuView.MODIF_DOSSIER, Boolean.TRUE);
			allowedOnglet.put(TiersEditView.DOSSIER_TRAVAIL, Boolean.TRUE);
			isEditable = true;
		}
		return isEditable;
	}

	/**
	 * enrichi la map de droit d'édition des onglets pour un non habitant ou un ménage commun considéré non habitant
	 * @param tiers
	 * @param allowedOnglet
	 */
	private boolean setDroitNonHabitant(Tiers tiers, Map<String, Boolean> allowedOnglet){

		//les non habitants n'ont jamais l'onglet rapport prestation
		//les ménage commun n'ont jamais les onglets civil et rapport prestation

		Assert.isTrue(tiers instanceof PersonnePhysique || tiers instanceof MenageCommun, "Le tiers " + tiers.getNumero() + " n'est ni une personne physique ni un ménage commun");

		final boolean isPersonnePhysique = tiers instanceof PersonnePhysique;

		boolean isEditable = codeFactorise1(tiers, allowedOnglet);
		if (tiers.isDebiteurInactif()) {//I107
			if (SecurityProvider.isGranted(Role.MODIF_NONHAB_INACTIF)) {
				if (isPersonnePhysique) {
					allowedOnglet.put(TiersVisuView.MODIF_CIVIL, Boolean.TRUE);
				}
				allowedOnglet.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.TRUE);
				allowedOnglet.put(TiersEditView.COMPLEMENT_COMMUNICATION, Boolean.TRUE);
				allowedOnglet.put(TiersVisuView.MODIF_DOSSIER, Boolean.FALSE);
				allowedOnglet.put(TiersVisuView.MODIF_FISCAL, Boolean.FALSE);
				if (SecurityProvider.isGranted(Role.ADR_PP_D)) {
					allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					allowedOnglet.put(TiersEditView.ADR_D, Boolean.TRUE);
				}
				if (SecurityProvider.isGranted(Role.ADR_PP_B)) {
					allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					allowedOnglet.put(TiersEditView.ADR_B, Boolean.TRUE);
				}
				if (SecurityProvider.isGranted(Role.ADR_PP_C)) {
					allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					allowedOnglet.put(TiersEditView.ADR_C, Boolean.TRUE);
				}
				isEditable = true;
			}
		}
		else {
			if (checkDroitEditPP(tiers)) {
				if (isPersonnePhysique) {
					allowedOnglet.put(TiersVisuView.MODIF_CIVIL, Boolean.TRUE);
				}
				codeFactorise2(allowedOnglet);
				isEditable = true;
			}

			isEditable = codeFactorise3(tiers, allowedOnglet, isEditable);

			Pair<TypeImposition, TypeAutoriteFiscale> types = calculeTypeImpositionEtAutoriteFiscale(tiers);
			if (types.getFirst() == TypeImposition.AUCUN_FOR_ACTIF && isPersonnePhysique) {
				// [UNIREG-1736] un sourcier est un individu qui a un for source ou dont le
				// ménage commun actif a un for source...
				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple((PersonnePhysique) tiers, null);
				if (ensemble != null) {
					final MenageCommun menage = ensemble.getMenage();
					types = calculeTypeImpositionEtAutoriteFiscale(menage);
				}
			}

			final TypeImposition typeImposition = types.getFirst();
			final TypeAutoriteFiscale typeAutoriteFiscale = types.getSecond();
			if (isPersonnePhysique && typeImposition == TypeImposition.SOURCIER && SecurityProvider.isGranted(Role.RT)) {
				allowedOnglet.put(TiersVisuView.MODIF_DOSSIER, Boolean.TRUE);
				allowedOnglet.put(TiersEditView.DOSSIER_TRAVAIL, Boolean.TRUE);
				isEditable = true;
			}
			final boolean autoriteFiscaleVaudoiseOuIndeterminee = typeAutoriteFiscale == null || typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
			final boolean autoriteFiscaleNonVaudoiseOuIndeterminee = typeAutoriteFiscale != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
			if ((typeImposition.isOrdinaireDepenseOuNonActif() && autoriteFiscaleNonVaudoiseOuIndeterminee && SecurityProvider.isGranted(Role.FOR_PRINC_ORDDEP_HCHS)) ||
					(typeImposition.isOrdinaireDepenseOuNonActif() && autoriteFiscaleVaudoiseOuIndeterminee && SecurityProvider.isGranted(Role.FOR_PRINC_ORDDEP_GRIS)) ||
					(typeImposition.isSourcierOuNonActif() && autoriteFiscaleNonVaudoiseOuIndeterminee && SecurityProvider.isGranted(Role.FOR_PRINC_SOURC_HCHS)) ||
					(typeImposition.isSourcierOuNonActif() && autoriteFiscaleVaudoiseOuIndeterminee && SecurityProvider.isGranted(Role.FOR_PRINC_SOURC_GRIS))) {
				allowedOnglet.put(TiersVisuView.MODIF_FISCAL, Boolean.TRUE);
				allowedOnglet.put(TiersEditView.FISCAL_FOR_PRINC, Boolean.TRUE);
				isEditable = true;
			}
		}
		return isEditable;
	}

	/**
	 * Code commun pour les méthodes setDroitNonHabitant et setDroitHabitant
	 *
	 * @param tiers
	 * @param allowedOnglet
	 * @return
	 */
	private boolean codeFactorise1(Tiers tiers, Map<String, Boolean> allowedOnglet ) {
		boolean isEditable = false;
		if (SecurityProvider.isGranted(Role.ADR_P)) {
			allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
			allowedOnglet.put(TiersEditView.ADR_P, Boolean.TRUE);
			isEditable = true;
		}

		if (SecurityProvider.isGranted(Role.ADR_PP_C_DCD) && tiers instanceof PersonnePhysique) {
			PersonnePhysique pp = (PersonnePhysique) tiers;
			if (tiersService.isDecede(pp)) {
				allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
				allowedOnglet.put(TiersEditView.ADR_C, Boolean.TRUE);
				isEditable = true;
			}
		} else if (SecurityProvider.isGranted(Role.ADR_PP_C_DCD) && tiers instanceof MenageCommun) {
			MenageCommun mc = (MenageCommun) tiers;
			for (PersonnePhysique pp : mc.getPersonnesPhysiques()) {
				if (tiersService.isDecede(pp)) {
					allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					allowedOnglet.put(TiersEditView.ADR_C, Boolean.TRUE);
					isEditable = true;
					break;
				}
			}
		}
		return isEditable;
	}

	/**
	 * Code commun pour les méthodes setDroitNonHabitant et setDroitHabitant
	 *
	 * @param allowedOnglet
	 * @return
	 */
	private void codeFactorise2(Map<String, Boolean> allowedOnglet) {
		allowedOnglet.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.TRUE);
		allowedOnglet.put(TiersEditView.COMPLEMENT_COMMUNICATION, Boolean.TRUE);
		allowedOnglet.put(TiersVisuView.MODIF_DOSSIER, Boolean.TRUE);
		allowedOnglet.put(TiersEditView.DOSSIER_NO_TRAVAIL, Boolean.TRUE);
		if (SecurityProvider.isGranted(Role.ADR_PP_D)) {
			allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
			allowedOnglet.put(TiersEditView.ADR_D, Boolean.TRUE);
		}
		if (SecurityProvider.isGranted(Role.ADR_PP_B)) {
			allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
			allowedOnglet.put(TiersEditView.ADR_B, Boolean.TRUE);
		}
		if (SecurityProvider.isGranted(Role.ADR_PP_C)) {
			allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
			allowedOnglet.put(TiersEditView.ADR_C, Boolean.TRUE);
		}
	}

	/**
	 * Code commun pour les méthodes setDroitNonHabitant et setDroitHabitant
	 *
	 * @param tiers
	 * @param allowedOnglet
	 * @param isEditable
	 * @return
	 */
	private boolean codeFactorise3(Tiers tiers, Map<String, Boolean> allowedOnglet,
			boolean isEditable) {
		if (!tiers.getForsFiscauxPrincipauxActifsSorted().isEmpty() && SecurityProvider.isGranted(Role.FOR_SECOND_PP)) {
			allowedOnglet.put(TiersVisuView.MODIF_FISCAL, Boolean.TRUE);
			allowedOnglet.put(TiersEditView.FISCAL_FOR_SEC, Boolean.TRUE);
			isEditable = true;
		}
		if (SecurityProvider.isGranted(Role.FOR_AUTRE)) {
			allowedOnglet.put(TiersVisuView.MODIF_FISCAL, Boolean.TRUE);
			allowedOnglet.put(TiersEditView.FISCAL_FOR_AUTRE, Boolean.TRUE);
			isEditable = true;
		}
		return isEditable;
	}

	/**
	 * @param tiers (uniquement PP ou ménage)
	 * @return true sur l'utilisateur connecté à les droits Ifosec de modif le tiers
	 */
	private boolean checkDroitEditPP(Tiers tiers) {
		boolean isHabitant = false;
		Tiers tiersAssujetti = null;
		if (tiers instanceof PersonnePhysique) {
			PersonnePhysique pp = (PersonnePhysique)tiers;
			MenageCommun menage = tiersService.findMenageCommun(pp, null);
			if (menage != null){
				tiersAssujetti = menage;
			}
			else tiersAssujetti = tiers;
			if (pp.isHabitant()) {
				isHabitant = true;
			}
		}
		else if (tiers instanceof MenageCommun) {
			Assert.isTrue(tiers instanceof MenageCommun, "checkDroitEditPP : le tiers fourni n'est ni une PP ni un couple");
			tiersAssujetti = tiers;
			//les ménages n'ont jamais les onglets civil et rapport prestation
			MenageCommun menageCommun = (MenageCommun) tiers;
			for (PersonnePhysique pp : menageCommun.getPersonnesPhysiques()) {
				if (pp.isHabitant()) {
					isHabitant = true;
					break;
				}
			}
		}

		int typeCtb = 0; //0 non assujetti, 1 HC/HS, 2 VD ordinaire, 3 VD sourcier pur, 4 VD sourcier mixte
		ForFiscalPrincipal forpCtbAssu = tiersAssujetti.getForFiscalPrincipalAt(null);
		if (forpCtbAssu != null) {
			TypeAutoriteFiscale typeFor = forpCtbAssu.getTypeAutoriteFiscale();
			ModeImposition modeImp = forpCtbAssu.getModeImposition();
			switch (typeFor) {
				case COMMUNE_OU_FRACTION_VD :
					switch (modeImp) {
					case SOURCE :
						typeCtb = 3;
						break;
					case MIXTE_137_1 :
					case MIXTE_137_2 :
						typeCtb = 4;
						break;
					default :
						typeCtb = 3;
				}
				break;
				default :
					typeCtb = 1;
			}
		}

		if ((typeCtb == 0 && SecurityProvider.isGranted(Role.MODIF_NONHAB_DEBPUR) && !isHabitant) ||
				(typeCtb == 0 && SecurityProvider.isGranted(Role.MODIF_HAB_DEBPUR) && isHabitant) ||
				(typeCtb == 1 && SecurityProvider.isGranted(Role.MODIF_HC_HS)) ||
				((typeCtb == 2 || typeCtb == 4) && SecurityProvider.isGranted(Role.MODIF_VD_ORD)) ||
				(typeCtb > 2 && SecurityProvider.isGranted(Role.MODIF_VD_SOURC))) {

				return true;
		}
		return false;
	}

	/**
	 * @param collectivite)
	 * @return true sur l'utilisateur connecté à les droits Ifosec de modif le tiers
	 */
	private boolean checkDroitEditCA(CollectiviteAdministrative collectivite) {
		return SecurityProvider.isGranted(Role.CREATE_CA) || SecurityProvider.isGranted(Role.MODIF_CA);
	}

	/**
	 * @param tiers
	 * @return true sur l'utilisateur connecté à les droits Ifosec et sécurité dossiers de modif le tiers
	 * retourne tjs false si le tiers n'est pas une PP ou un ménage
	 */
	protected boolean checkDroitEdit(Tiers tiers){

		final Niveau acces = SecurityProvider.getDroitAcces(tiers);
		if (acces == null || acces.equals(Niveau.LECTURE)) {
			return false;
		}

		if (tiers instanceof PersonnePhysique || tiers instanceof MenageCommun) {
			return checkDroitEditPP(tiers);
		}
		else if (tiers instanceof CollectiviteAdministrative) {
			CollectiviteAdministrative collectivite = (CollectiviteAdministrative) tiers;
			return checkDroitEditCA(collectivite);
		}
		else {
			return false;
		}
	}

	/**
	 * Alimente la vue TiersGeneralView
	 *
	 * @param tiersView
	 * @throws AdresseException
	 */
	protected void setTiersGeneralView(TiersView tiersView, Tiers tiers) throws AdresseException  {
		TiersGeneralView tiersGeneralView = tiersGeneralManager.get(tiers);
		tiersView.setTiersGeneral(tiersGeneralView);
	}

	/**
	 * Renseigne la liste des adresses actives sur le form backing object. En cas d'erreur dans la résolution des adresses, les adresses en
	 * erreur et le message de l'erreur sont renseignés en lieu et place.
	 */
	protected void setAdressesActives(TiersEditView tiersEditView, final Tiers tiers) {

		try {
			List<AdresseView> adresses = new ArrayList<AdresseView>();

			final AdressesFiscales adressesFiscales = getAdresseService().getAdressesFiscales(tiers, null, false);
			if (adressesFiscales != null) {
				// rempli tous les types d'adresse
				for (TypeAdresseTiers type : TypeAdresseTiers.values()) {
					fillAdressesView(adresses, adressesFiscales, type, tiers);
				}
				Collections.sort(adresses, new AdresseViewComparator());
			}

			tiersEditView.setAdressesActives(adresses);
		}
		catch (AdresseException exception) {

			if (exception instanceof AdressesResolutionException) {
				final AdressesResolutionException are = (AdressesResolutionException) exception;
				/*
				 * En cas d'erreur dans la résolution des adresses, on récupère les adresses qui ont provoqué l'erreur et on affiche un écran
				 * spécial pour permettre à l'utilisateur de résoudre le problème
				 */
				List<AdresseView> adresses = new ArrayList<AdresseView>();
				for (AdresseTiers a : are.getAdresse()) {
					AdresseView view = getAdresseManager().getAdresseView(a.getId());
					view.setUsage(a.getUsage());
					view.setSource(Source.FISCALE);
					adresses.add(view);
				}
				tiersEditView.setAdressesEnErreur(adresses);
			}

			// Dans tous les cas, on affiche le message d'erreur
			tiersEditView.setAdressesEnErreurMessage(exception.getMessage());
		}
	}

	/**
	 * Remplir la collection des adressesView avec l'adresse fiscale du type spécifié.
	 */
	protected void fillAdressesView(List<AdresseView> adressesView, final AdressesFiscales adressesFiscales, TypeAdresseTiers type,
			Tiers tiers) {
		AdresseGenerique adresse = adressesFiscales.ofType(type);
		if (adresse != null) {
			AdresseView adresseView = createAdresseView(adresse, type, tiers);
			adressesView.add(adresseView);
		}
	}

	/**
	 * Methode annexe de creation d'adresse view pour un type donne
	 *
	 * @param addGen
	 * @param type
	 * @return
	 * @throws InfrastructureException
	 *
	 */
	protected AdresseView createAdresseView(AdresseGenerique addGen, TypeAdresseTiers type, Tiers tiers) {
		AdresseView adresseView = new AdresseView();
		adresseView.setDateDebut(addGen.getDateDebut());
		adresseView.setDateFin(addGen.getDateFin());
		adresseView.setAnnule(addGen.isAnnule());
		if (addGen.getId() != null) {
			AdresseTiers adresseTiers = adresseTiersDAO.get(addGen.getId());
			if (adresseTiers instanceof AdresseSupplementaire) {
				AdresseSupplementaire adresseSupplementaire = (AdresseSupplementaire) adresseTiers;
				adresseView.setPermanente(adresseSupplementaire.isPermanente());
			}
			else {
				adresseView.setPermanente(false);
			}
		}
		else {
			adresseView.setPermanente(false);
		}
		Integer numeroRue = addGen.getNumeroRue();
		String rueFull = null;
		if ((numeroRue != null) && (numeroRue.intValue() != 0)) {
			Rue rue = getRueByNumero(numeroRue);
			if (rue != null) {
				if ((rue.getDesignationCourrier() != null) && (addGen.getCasePostale() == null)) {
					rueFull = rue.getDesignationCourrier() ;
					if (addGen.getNumero() != null) {
						rueFull = rueFull + " " + addGen.getNumero();
					}
				}
				if ((rue.getDesignationCourrier() == null) && (addGen.getCasePostale() != null)) {
					rueFull = addGen.getCasePostale();
				}
				if ((rue.getDesignationCourrier() != null) && (addGen.getCasePostale() != null)) {

					rueFull = rue.getDesignationCourrier();

					if (addGen.getNumero() != null) {
						rueFull = rueFull + " " + addGen.getNumero();
					}
					rueFull = rueFull + " / " + addGen.getCasePostale();
				}
				Localite localite = getLocaliteByONRP(rue.getNoLocalite());
				if (localite != null) {
					String localiteStr = "";
					if (localite.getNPA() != null) {
						localiteStr = localite.getNPA().toString() + " ";
					}
					localiteStr += localite.getNomAbregeMinuscule();
					adresseView.setLocalite(localiteStr);
				}
			}
		}
		else {
			if ((addGen.getRue() != null) && (addGen.getCasePostale() == null)) {
				rueFull = addGen.getRue();
				if (addGen.getNumero() != null) {
					rueFull = rueFull + " " + addGen.getNumero();
				}
			}
			if ((addGen.getRue() == null) && (addGen.getCasePostale() != null)) {
				rueFull = addGen.getCasePostale();
			}
			if ((addGen.getRue() != null) && (addGen.getCasePostale() != null)) {
				rueFull = addGen.getRue();
				if (addGen.getNumero() != null) {
					rueFull = rueFull + " " + addGen.getNumero();
				}
				rueFull = rueFull + " / " + addGen.getCasePostale();
			}
			String localiteStr = "";
			if (StringUtils.hasText(addGen.getNumeroPostal())) {
				localiteStr = addGen.getNumeroPostal() + " ";
			}
			localiteStr += addGen.getLocalite();
			adresseView.setLocalite(localiteStr);
		}

		adresseView.setRue(rueFull);
		adresseView.setUsage(type);
		adresseView.setPaysOFS(addGen.getNoOfsPays());
		adresseView.setSource(addGen.getSource());
		adresseView.setDefault(addGen.isDefault());

		adresseView.setComplements(addGen.getComplement());

		AdresseTiers adresseTiers = tiers.getAdresseActive(type, addGen.getDateDebut());
		adresseView.setId(adresseTiers == null ? null : adresseTiers.getId());

		return adresseView;
	}

	/**
	 * Initialise les champs avec les valeurs des paramètres
	 *
	 * @param tiersCriteriaView
	 * @param numero
	 * @param nomRaison
	 * @param localiteOuPays
	 * @param noOfsFor
	 * @param dateNaissance
	 * @param numeroAssureSocial
	 */
	public void initFieldsWithParams(	TiersCriteriaView tiersCriteriaView,
										Long numero,
										String nomRaison,
										String localiteOuPays,
										Long noOfsFor,
										RegDate dateNaissance,
										String numeroAssureSocial) {

	}

	/**
	 * Compte le nombre de rapports prestation imposable pour un débiteur
	 * @param numeroDebiteur
	 * @return
	 */
	@Transactional(readOnly = true)
	public int countRapportsPrestationImposable(Long numeroDebiteur){
		return rapportEntreTiersDAO.countRapportsPrestationImposable(numeroDebiteur);
	}

	public HostCivilService getHostCivilService() {
		return hostCivilService;
	}

	public void setHostCivilService(HostCivilService hostCivilService) {
		this.hostCivilService = hostCivilService;
	}

	public TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public ServiceCivilService getServiceCivilService() {
		return serviceCivilService;
	}

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

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	public HostPersonneMoraleService getHostPersonneMoraleService() {
		return hostPersonneMoraleService;
	}

	public void setHostPersonneMoraleService(HostPersonneMoraleService hostPersonneMoraleService) {
		this.hostPersonneMoraleService = hostPersonneMoraleService;
	}

	public Calendar getCalToday() {
		return calToday;
	}

	public AdresseManager getAdresseManager() {
		return adresseManager;
	}

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

	public SituationFamilleService getSituationFamilleService() {
		return situationFamilleService;
	}

	public void setSituationFamilleService(SituationFamilleService situationFamilleService) {
		this.situationFamilleService = situationFamilleService;
	}

	public AdresseTiersDAO getAdresseTiersDAO() {
		return adresseTiersDAO;
	}

	public void setAdresseTiersDAO(AdresseTiersDAO adresseTiersDAO) {
		this.adresseTiersDAO = adresseTiersDAO;
	}

	public RapportEntreTiersDAO getRapportEntreTiersDAO() {
		return rapportEntreTiersDAO;
	}

	public void setRapportEntreTiersDAO(RapportEntreTiersDAO rapportEntreTiersDAO) {
		this.rapportEntreTiersDAO = rapportEntreTiersDAO;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public MessageSource getMessageSource() {
		return messageSource;
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
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.tiers.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}
		tiersService.annuleTiers(tiers);
	}
}
