package ch.vd.uniregctb.tiers.manager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.*;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.StandardBatchIterator;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.di.view.DeclarationImpotDetailComparator;
import ch.vd.uniregctb.di.view.DeclarationImpotDetailView;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.mouvement.MouvementDossier;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;
import ch.vd.uniregctb.rapport.TypeRapportEntreTiersWeb;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.tiers.view.AdresseViewComparator;
import ch.vd.uniregctb.tiers.view.RapportsPrestationView;
import ch.vd.uniregctb.tiers.view.TiersVisuView;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Service qui fournit les methodes pour visualiser un tiers
 *
 * @author xcifde
 */
public class TiersVisuManagerImpl extends TiersManager implements TiersVisuManager {

	private MouvementVisuManager mouvementVisuManager;

	private IbanValidator ibanValidator;



	private List<EnumTypeAdresse> typesAdressesCiviles = new ArrayList<EnumTypeAdresse>();

	public void setMouvementVisuManager(MouvementVisuManager mouvementVisuManager) {
		this.mouvementVisuManager = mouvementVisuManager;
	}

	public void setIbanValidator(IbanValidator ibanValidator) {
		this.ibanValidator = ibanValidator;
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(readOnly = true)
	public TiersVisuView getView(Long numero, boolean adressesHisto, boolean adressesHistoCiviles, boolean adressesHistoCivilesConjoint, boolean rapportsPrestationHisto,
	                             WebParamPagination webParamPagination) throws AdresseException, InfrastructureException {

		final TiersVisuView tiersVisuView = new TiersVisuView();
		tiersVisuView.setAdressesHisto(adressesHisto);
		tiersVisuView.setAdressesHistoCiviles(adressesHistoCiviles);
		tiersVisuView.setRapportsPrestationHisto(rapportsPrestationHisto);
		tiersVisuView.setAdressesHistoCivilesConjoint(adressesHistoCivilesConjoint);


		final Tiers tiers = getTiersDAO().get(numero);
		if (tiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.tiers.inexistant", null, WebContextUtils.getDefaultLocale()));
		}

		setTiersGeneralView(tiersVisuView, tiers);

		if (tiers instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) tiers;
			if (pp.isHabitantVD()) {
				setHabitant(tiersVisuView, pp);
			}
			else {
				tiersVisuView.setTiers(pp);
			}
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
			setDebiteurPrestationImposable(tiersVisuView, dpi, rapportsPrestationHisto, webParamPagination);
			setContribuablesAssocies(tiersVisuView, dpi);
			setForsFiscauxDebiteur(tiersVisuView, dpi);
			setPeriodicitesView(tiersVisuView,dpi);
		}
		else if (tiers instanceof CollectiviteAdministrative) {
			tiersVisuView.setTiers(tiers);
		}

		if (tiersVisuView.getTiers() != null) {
			if (tiers instanceof Contribuable) {
				final Contribuable contribuable = (Contribuable) tiers;
				tiersVisuView.setDebiteurs(getDebiteurs(contribuable));
				tiersVisuView.setDis(getDeclarationsImpotOrdinaire(contribuable));
				tiersVisuView.setMouvements(getMouvements(contribuable));
				setForsFiscaux(tiersVisuView, contribuable);
				setSituationsFamille(tiersVisuView, contribuable);
			}

			tiersVisuView.setHistoriqueAdresses(getAdressesHistoriques(tiers, adressesHisto));

			//Les entreprises et les etablissement ne sont pas pris en charge par l'adresseService
			if (!Tiers.NATURE_ENTREPRISE.equals(tiersVisuView.getNatureTiers()) &&
					!Tiers.NATURE_ETABLISSEMENT.equals(tiersVisuView.getNatureTiers())) {
				tiersVisuView.setHistoriqueAdressesCiviles(getAdressesHistoriquesCiviles(tiers, adressesHistoCiviles));
				final Tiers conjoint = tiersVisuView.getTiersConjoint();
				if (conjoint != null) {
					tiersVisuView.setHistoriqueAdressesCivilesConjoint(getAdressesHistoriquesCiviles(conjoint, adressesHistoCivilesConjoint));
				}
			}

			final List<RapportView> rapportsView = getRapports(tiers);

			// filtrer les rapports entre tiers si l'utilisateur a des droits en visu limitée
			if (SecurityProvider.isGranted(Role.VISU_LIMITE) && !SecurityProvider.isGranted(Role.VISU_ALL)) {
				final Iterator<RapportView> iter = rapportsView.iterator();
				while (iter.hasNext()) {
					final RapportView rv = iter.next();
					if (rv.getTypeRapportEntreTiers() != TypeRapportEntreTiersWeb.APPARTENANCE_MENAGE) {
						iter.remove();
					}
				}
			}
			if (tiers instanceof PersonnePhysique) {
				final PersonnePhysique pp = (PersonnePhysique) tiers;
				if (pp.getNumeroIndividu() != null && pp.getNumeroIndividu() != 0) {
					final List<RapportView> rapportsFiliationView = getRapportsFiliation(pp);
					rapportsView.addAll(rapportsFiliationView);
				}
			}
			tiersVisuView.setDossiersApparentes(rapportsView);

			final Map<String, Boolean> allowedOnglet = initAllowedModif();
			setDroitEdition(tiers, allowedOnglet);
			tiersVisuView.setAllowedOnglet(allowedOnglet);
		}
		//[UNIREG-2582]
		tiersVisuView.setIbanValidationMessage(verifierIban(tiers));
		return tiersVisuView;
	}

	/**
	 * Permet renseigner la view sur le fait que l'iban du tiers associé est valide ou pas
	 * @param tiers le tiers dont l'IBAN doit être vérifié
	 * @return <code>null</code> si l'IBAN est valide, explication textuelle de l'erreur sinon
	 */
	private String verifierIban(Tiers tiers) {
		if (tiers != null) {
			final String iban = tiers.getNumeroCompteBancaire();
			if (iban != null) {
				return ibanValidator.getIbanValidationError(iban);
			}
		}
		return null;
	}

	/**
	 * initialise les droits d'édition des onglets du tiers
	 *
	 * @return la map de droit d'édition des onglets
	 */
	private Map<String, Boolean> initAllowedModif() {
		Map<String, Boolean> allowedModif = new HashMap<String, Boolean>();
		allowedModif.put(TiersVisuView.MODIF_FISCAL, Boolean.FALSE);
		allowedModif.put(TiersVisuView.MODIF_ADRESSE, Boolean.FALSE);
		allowedModif.put(TiersVisuView.MODIF_CIVIL, Boolean.FALSE);
		allowedModif.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.FALSE);
		allowedModif.put(TiersVisuView.MODIF_DEBITEUR, Boolean.FALSE);
		allowedModif.put(TiersVisuView.MODIF_DOSSIER, Boolean.FALSE);
		allowedModif.put(TiersVisuView.MODIF_RAPPORT, Boolean.FALSE);
		allowedModif.put(TiersVisuView.MODIF_MOUVEMENT, Boolean.FALSE);

		return allowedModif;
	}


	/**
	 * Recuperation des adresses historiques
	 *
	 * @param tiers
	 * @param adresseActive
	 * @return List<AdresseView>
	 * @throws AdressesResolutionException
	 */
	private List<AdresseView> getAdressesHistoriques(Tiers tiers, boolean adresseHisto) throws AdresseException, InfrastructureException {

		List<AdresseView> adresses = new ArrayList<AdresseView>();

		if (adresseHisto) {
			final AdressesFiscalesHisto adressesFiscalHisto = adresseService.getAdressesFiscalHisto(tiers, false);
			if (adressesFiscalHisto != null) {
				// rempli tous les types d'adresse
				for (TypeAdresseTiers type : TypeAdresseTiers.values()) {
					fillAdressesView(adresses, adressesFiscalHisto, type, tiers);
				}
			}
		}
		else {
			final AdressesFiscales adressesFiscales = adresseService.getAdressesFiscales(tiers, null, false);
			if (adressesFiscales != null) {
				// rempli tous les types d'adresse
				for (TypeAdresseTiers type : TypeAdresseTiers.values()) {
					fillAdressesView(adresses, adressesFiscales, type, tiers);
				}
			}
		}

		Collections.sort(adresses, new AdresseViewComparator());

		List<AdresseView> adressesResultat = removeAdresseFromCivil(adresses);
		return adressesResultat;
	}


	/**
	 * recuperation des adresses civiles historiques
	 *
	 * @param tiers
	 * @param adressesHistoCiviles
	 * @return
	 */
	private List<AdresseView> getAdressesHistoriquesCiviles(Tiers tiers, boolean adressesHistoCiviles) throws AdresseException, InfrastructureException {
		List<AdresseView> adresses = new ArrayList<AdresseView>();
		List<EnumTypeAdresse> listeTypeCivil = getTypesAdressesCiviles();
		if (adressesHistoCiviles) {
			final AdressesCivilesHisto adressesCivilesHisto = adresseService.getAdressesCivilesHisto(tiers, false);

			if (adressesCivilesHisto != null) {
				// rempli tous les types d'adresse
				for (EnumTypeAdresse type : listeTypeCivil) {
					fillAdressesHistoCivilesView(adresses, adressesCivilesHisto, type, tiers);
				}
			}
		}
		else {
			final AdressesCiviles adressesCiviles = adresseService.getAdressesCiviles(tiers, null, false);
			if (adressesCiviles != null) {
				// rempli tous les types d'adresse
				for (EnumTypeAdresse type : listeTypeCivil) {
					fillAdressesCivilesView(adresses, adressesCiviles, type, tiers);
				}
			}
		}

		Collections.sort(adresses, new AdresseViewComparator());

		return adresses;
	}


	/**
	 * Rempli la collection des adressesView avec les adresses fiscales historiques du type spécifié.
	 */
	private void fillAdressesView(List<AdresseView> adressesView, final AdressesFiscalesHisto adressesFiscalHisto, TypeAdresseTiers type,
	                              Tiers tiers) throws InfrastructureException {

		final Collection<AdresseGenerique> adresses = adressesFiscalHisto.ofType(type);
		if (adresses == null) {
			// rien à faire
			return;
		}

		for (AdresseGenerique adresse : adresses) {
			AdresseView adresseView = createVisuAdresseView(adresse, type, tiers);
			adressesView.add(adresseView);
		}
	}

	private void fillAdressesHistoCivilesView(List<AdresseView> adressesView, AdressesCivilesHisto adressesCivilesHisto, EnumTypeAdresse type, Tiers tiers) throws AdresseDataException,
			InfrastructureException {
		final List<Adresse> adresses = adressesCivilesHisto.ofType(type);
		if (adresses == null) {
			// rien à faire
			return;
		}

		for (Adresse adresse : adresses) {

			AdaptAdresseCivileToAdresseView(adressesView, type, tiers, adresse);

		}
	}

	private void AdaptAdresseCivileToAdresseView(List<AdresseView> adressesView, EnumTypeAdresse type, Tiers tiers, Adresse adresse) throws AdresseDataException, InfrastructureException {
		try {
			AdresseGenerique adrGen = new AdresseCivileAdapter(adresse, false, getServiceInfrastructureService());
			AdresseView adresseView = createVisuAdresseView(adrGen, null, tiers);
			adresseView.setUsageCivil(type.getName());
			adressesView.add(adresseView);

		}
		catch (DonneesCivilesException e) {
			throw new AdresseDataException(e.getMessage());
		}
	}

	/**
	 * Remplir la collection des adressesView avec l'adresse civile du type spécifié.
	 */
	protected void fillAdressesCivilesView(List<AdresseView> adressesView, final AdressesCiviles adressesCiviles, EnumTypeAdresse type,
	                                       Tiers tiers) throws AdresseDataException, InfrastructureException {
		Adresse adresse = adressesCiviles.ofType(type);
		if (adresse == null) {
			// rien à faire
			return;
		}
		AdaptAdresseCivileToAdresseView(adressesView, type, tiers, adresse);
	}

	/**
	 * Methode annexe de creation d'adresse view pour un type donne
	 *
	 * @param addProf
	 * @param type
	 * @return
	 * @throws InfrastructureException
	 */
	private AdresseView createVisuAdresseView(AdresseGenerique adr, TypeAdresseTiers type,
	                                          Tiers tiers) throws InfrastructureException {
		AdresseView adresseView = createAdresseView(adr, type, tiers);

		RegDate dateJour = RegDate.get();
		if (((adr.getDateDebut() == null) || (adr.getDateDebut().isBeforeOrEqual(dateJour)))
				&& ((adr.getDateFin() == null) || (adr.getDateFin().isAfterOrEqual(dateJour)))) {
			adresseView.setActive(true);
		}
		else {
			adresseView.setActive(false);
		}

		adresseView.setSurVaud(getServiceInfrastructureService().estDansLeCanton(adr));


		return adresseView;
	}

	/**
	 * Mise à jour de la vue Declaration Impot Ordinaire
	 *
	 * @param contribuable
	 * @return
	 */
	private List<DeclarationImpotDetailView> getDeclarationsImpotOrdinaire(Contribuable contribable) {

		List<DeclarationImpotDetailView> disView = new ArrayList<DeclarationImpotDetailView>();
		Set<Declaration> declarations = contribable.getDeclarations();
		for (Declaration declaration : declarations) {
			if (declaration instanceof DeclarationImpotOrdinaire) {
				DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) declaration;
				DeclarationImpotDetailView diView = new DeclarationImpotDetailView();
				diView.setId(di.getId());
				diView.setDateDebutPeriodeImposition(di.getDateDebut());
				diView.setDateFinPeriodeImposition(di.getDateFin());
				diView.setPeriodeFiscale(di.getPeriode() != null ? di.getPeriode().getAnnee() : null);
				diView.setAnnule(di.isAnnule());
				final EtatDeclaration dernierEtat = di.getDernierEtat();
				diView.setEtat(dernierEtat == null ? null : dernierEtat.getEtat());
				diView.setDelaiAccorde(di.getDelaiAccordeAu());
				diView.setDateRetour(di.getDateRetour());
				disView.add(diView);
			}
		}
		Collections.sort(disView, new DeclarationImpotDetailComparator());
		return disView;
	}

	/**
	 * Mise à jour de la vue MouvementDetailView
	 *
	 * @param contribuable
	 * @return
	 */
	private List<MouvementDetailView> getMouvements(Contribuable contribuable) throws InfrastructureException {

		final List<MouvementDetailView> mvtsView = new ArrayList<MouvementDetailView>();
		final Set<MouvementDossier> mvts = contribuable.getMouvementsDossier();
		for (MouvementDossier mvt : mvts) {
			if (mvt.getEtat().isTraite()) {
				final MouvementDetailView mvtView = mouvementVisuManager.getView(mvt);
				mvtsView.add(mvtView);
			}
		}
		Collections.sort(mvtsView);
		return mvtsView;
	}

	@Transactional(readOnly = true)
	public void fillRapportsPrestationView(long noDebiteur, RapportsPrestationView view) {

		final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersDAO.get(noDebiteur);
		if (debiteur == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.tiers.inexistant", null, WebContextUtils.getDefaultLocale()));
		}

		final List<RapportsPrestationView.Rapport> rapports = new ArrayList<RapportsPrestationView.Rapport>();
		final Map<Long, List<RapportsPrestationView.Rapport>> rapportsByNumero = new HashMap<Long, List<RapportsPrestationView.Rapport>>();

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
			rapport.typeActivite = rpi.getTypeActivite();
			rapport.tauxActivite = rpi.getTauxActivite();
			rapport.noCTB = rpi.getSujetId();
			rapports.add(rapport);

			ArrayList<RapportsPrestationView.Rapport> rl = (ArrayList<RapportsPrestationView.Rapport>) rapportsByNumero.get(rapport.noCTB);
			if (rl == null) {
				rl = new ArrayList<RapportsPrestationView.Rapport>();
				rapportsByNumero.put(rapport.noCTB, rl);
			}

			rl.add(rapport);
		}

		final long endRapports = System.nanoTime();
		LOGGER.debug("- chargement des rapports en " + ((endRapports - startRapports) / 1000000) + " ms");

		// Complète les noms, prénoms et nouveaux numéros AVS des non-habitants

		final long startNH = System.nanoTime();

		final Set<Long> pasDeNouveauNosAvs = new HashSet<Long>();

		final List infoNonHabitants = tiersDAO.getHibernateTemplate()
				.find("select pp.numero, pp.prenom, pp.nom, pp.numeroAssureSocial from PersonnePhysique pp, RapportPrestationImposable rpi " +
						"where pp.habitant = false and pp.numero = rpi.sujetId and rpi.objetId =  " + noDebiteur);
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
				r.nomCourrier1 = getNomPrenom(prenom, nom);
				r.noAVS = FormatNumeroHelper.formatNumAVS(noAVS);
			}
		}

		// Complète les anciens numéros AVS des non-habitants qui n'en possède pas des nouveaux

		if (!pasDeNouveauNosAvs.isEmpty()) {
			final StandardBatchIterator<Long> it = new StandardBatchIterator<Long>(pasDeNouveauNosAvs, 500);
			while (it.hasNext()) {
				final List<Long> ids = asList(it.next());

				final List ancienNosAvs = (List) tiersDAO.getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
					public Object doInHibernate(Session session) throws HibernateException, SQLException {
						final Query query = session.createQuery(
								"select ip.personnePhysique.id, ip.identifiant from IdentificationPersonne ip where ip.categorieIdentifiant = 'CH_AHV_AVS' and ip.personnePhysique.id in (:ids)");
						query.setParameterList("ids", ids);
						return query.list();
					}
				});

				for (Object o : ancienNosAvs) {
					final Object line[] = (Object[]) o;
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

		final Map<Long, List<RapportsPrestationView.Rapport>> rapportsByNumeroIndividu = new HashMap<Long, List<RapportsPrestationView.Rapport>>();

		final List infoHabitants = tiersDAO.getHibernateTemplate().find("select pp.numero, pp.numeroIndividu from PersonnePhysique pp, RapportPrestationImposable rpi " +
				"where pp.habitant = true and pp.numero = rpi.sujetId and rpi.objetId =  " + noDebiteur);
		for (Object o : infoHabitants) {
			final Object line[] = (Object[]) o;
			final Long numero = (Long) line[0];
			final Long numeroIndividu = (Long) line[1];

			ArrayList<RapportsPrestationView.Rapport> rl = (ArrayList<RapportsPrestationView.Rapport>) rapportsByNumeroIndividu.get(numeroIndividu);
			if (rl == null) {
				rl = new ArrayList<RapportsPrestationView.Rapport>();
				rapportsByNumeroIndividu.put(numeroIndividu, rl);
			}

			rl.addAll(rapportsByNumero.get(numero));
		}

		final Set<Long> numerosIndividus = rapportsByNumeroIndividu.keySet();
		final StandardBatchIterator<Long> iterator = new StandardBatchIterator<Long>(numerosIndividus, 500);
		while (iterator.hasNext()) {
			final List<Long> batch = asList(iterator.next());
			final List<Individu> individus = serviceCivilService.getIndividus(batch, null);
			for (Individu ind : individus) {
				final List<RapportsPrestationView.Rapport> rl = rapportsByNumeroIndividu.get(ind.getNoTechnique());
				Assert.notNull(rl);
				for (RapportsPrestationView.Rapport rapport : rl) {
					rapport.nomCourrier1 = serviceCivilService.getNomPrenom(ind);
					rapport.noAVS = getNumeroAvs(ind);
				}
			}
		}

		final long endH = System.nanoTime();
		LOGGER.debug("- chargement des habitants en " + ((endH - startH) / 1000000) + " ms");

		view.idDpi = noDebiteur;
		view.tiersGeneral = tiersGeneralManager.get(debiteur);
		view.editionAllowed = SecurityProvider.isGranted(Role.RT);
		view.rapports = rapports;
	}

	private String getNumeroAvs(Individu ind) {
		final String noAVS;
		if (StringUtils.isBlank(ind.getNouveauNoAVS())) {
			noAVS = FormatNumeroHelper.formatAncienNumAVS(ind.getDernierHistoriqueIndividu().getNoAVS());
		}
		else {
			noAVS = FormatNumeroHelper.formatNumAVS(ind.getNouveauNoAVS());
		}
		return noAVS;
	}

	private List<Long> asList(Iterator<Long> i) {
		List<Long> b = new ArrayList<Long>();
		while (i.hasNext()) {
			b.add(i.next());
		}
		return b;
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

	public List<EnumTypeAdresse> getTypesAdressesCiviles() {
		if (typesAdressesCiviles.isEmpty()) {
			typesAdressesCiviles.add(EnumTypeAdresse.COURRIER);
			typesAdressesCiviles.add(EnumTypeAdresse.PRINCIPALE);
			typesAdressesCiviles.add(EnumTypeAdresse.SECONDAIRE);
			typesAdressesCiviles.add(EnumTypeAdresse.TUTELLE);

		}

		return typesAdressesCiviles;
	}
}
