package ch.vd.uniregctb.di.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.jms.JMSException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.DelaiDeclarationDAO;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionSommationDIHelperImpl;
import ch.vd.uniregctb.declaration.ordinaire.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.delai.DelaiDeclarationView;
import ch.vd.uniregctb.di.view.DeclarationImpotDetailComparator;
import ch.vd.uniregctb.di.view.DeclarationImpotDetailView;
import ch.vd.uniregctb.di.view.DeclarationImpotImpressionView;
import ch.vd.uniregctb.di.view.DeclarationImpotListView;
import ch.vd.uniregctb.di.view.ModeleDocumentView;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.DecompositionForsAnneeComplete;
import ch.vd.uniregctb.metier.assujettissement.HorsCanton;
import ch.vd.uniregctb.metier.assujettissement.HorsSuisse;
import ch.vd.uniregctb.metier.assujettissement.Indigent;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.VaudoisDepense;
import ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheCriteria;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Service offrant des methodes pour gérer le controller DeclarationImpotEditController
 *
 * @author xcifde
 *
 */
public class DeclarationImpotEditManagerImpl implements DeclarationImpotEditManager, MessageSourceAware {

	protected static final Logger LOGGER = Logger.getLogger(DeclarationImpotEditManagerImpl.class);

	private TiersGeneralManager tiersGeneralManager;

	private DeclarationImpotOrdinaireDAO diDAO;

	private PeriodeFiscaleDAO periodeFiscaleDAO;

	private DeclarationImpotService diService;

	private TiersDAO tiersDAO;

	private TiersService tiersService;

	private EvenementFiscalService evenementFiscalService;

	private ModeleDocumentDAO modeleDocumentDAO;

	private TacheDAO tacheDAO;

	private EditiqueCompositionService editiqueCompositionService;

	private EditiqueService editiqueService;

	private MessageSource messageSource;

	private DelaisService delaisService;

	private DelaiDeclarationDAO delaiDeclarationDAO;


	private ParametreAppService parametres;

	/**
	 * Contrôle que la DI existe
	 *
	 * @param id
	 */
	@Transactional(readOnly = true)
	public void controleDI(Long id) {
		DeclarationImpotOrdinaire di = diDAO.get(id);
		if (di == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.di.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}
	}

	/**
	 * Annule une DI
	 *
	 * @param diEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerDI(DeclarationImpotDetailView diEditView) {
		DeclarationImpotOrdinaire di = diDAO.get(diEditView.getId());
		final Contribuable tiers = (Contribuable)di.getTiers();
		diService.annulationDI(tiers, di, RegDate.get());
		//Mise à jour de l'état de la tâche si il y en a une
		TacheCriteria criterion = new TacheCriteria();
		criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
		criterion.setAnnee(diEditView.getPeriodeFiscale());
		criterion.setEtatTache(TypeEtatTache.EN_INSTANCE);
		criterion.setContribuable(tiers);
		List<Tache> taches = tacheDAO.find(criterion);
		if (taches != null && taches.size() != 0) {
			for (Tache t : taches) {
				TacheAnnulationDeclarationImpot tache = (TacheAnnulationDeclarationImpot) t;
				tache.setEtat(TypeEtatTache.TRAITE);
			}
		}
	}

	/**
	 * Annule un delai
	 *
	 * @param diEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerDelai(DeclarationImpotDetailView diEditView, Long idDelai) {
		DeclarationImpotOrdinaire di = diDAO.get(diEditView.getId());
		if (di == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.di.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}

		Set<DelaiDeclaration> delais = di.getDelais();
		for (DelaiDeclaration delai : delais) {
			if (delai.getId().equals(idDelai)) {
				delai.setAnnule(true);
			}
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(readOnly = true)
	public void creerDI(Long numeroCtb, DateRange range, DeclarationImpotDetailView diEditView) {

		// on charge le tiers
		final Tiers tiers = tiersDAO.get(numeroCtb);
		if (tiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.tiers.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		ValidationException error = null;

		// Si la période fiscale de la di concerne l'année en cours, il s'agit d'une di ouverte
		diEditView.setOuverte(RegDate.get().year() == range.getDateDebut().year());
		PeriodeImposition periode = null;

		try {
			periode = checkRangeDi((Contribuable) tiers, range);
		}
		catch (ValidationException e) {
			error = e;
		}

		diEditView.setContribuable(tiersGeneralManager.get(tiers));
		if (error != null) {
			diEditView.setImprimable(false);
			diEditView.setErrorMessage(error.getMessage());
		}
		else {
			diEditView.setImprimable(true);
			diEditView.setPeriodeFiscale(periode.getDateDebut().year());
			diEditView.setDateDebutPeriodeImposition(periode.getDateDebut());
			diEditView.setDateFinPeriodeImposition(periode.getDateFin());
			diEditView.setTypeAdresseRetour(periode.getAdresseRetour());
			diEditView.setDelaiAccorde(delaisService.getDateFinDelaiRetourDeclarationImpotEmiseManuellement(RegDate.get()));

			//Par défaut le type de DI est celui de la dernière DI émise
			EtatDeclaration etatDiPrecedente = diDAO.findDerniereDiEnvoyee(numeroCtb);
			if (etatDiPrecedente != null) {
				DeclarationImpotOrdinaire diPrecedente = (DeclarationImpotOrdinaire) etatDiPrecedente.getDeclaration();
				if (diPrecedente.getTypeDeclaration().equals(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH)) {
					diEditView.setTypeDeclarationImpot(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL);
				}
				else {
					diEditView.setTypeDeclarationImpot(diPrecedente.getTypeDeclaration());
				}
			}
			else {
				diEditView.setTypeDeclarationImpot(TypeDocument.DECLARATION_IMPOT_VAUDTAX);
			}


			setDroitDI(diEditView, tiers);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(readOnly = true)
	public List<PeriodeImposition> calculateRangesProchainesDIs(Long numero) throws ValidationException {

		// on charge le tiers
		final Contribuable contribuable = (Contribuable) tiersDAO.get(numero);
		if (contribuable == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.tiers.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		return calculateRangesProchainesDIs(contribuable);
	}

	protected List<PeriodeImposition> calculateRangesProchainesDIs(final Contribuable contribuable) throws ValidationException {

		// le contribuable doit être valide
		final ValidationResults results = contribuable.validate();
		if (results.hasErrors()) {
			throw new ValidationException(contribuable, results.getErrors(), results.getWarnings());
		}

		// [UNIREG-879] on limite la plage de création des DIs online à la période 'première période fiscale' -> 'année fiscale courante'
		final int premiereAnnee = parametres.getPremierePeriodeFiscale();
		final int derniereAnnee = RegDate.get().year();

		final List<PeriodeImposition> ranges = new ArrayList<PeriodeImposition>();
		for (int annee = premiereAnnee; annee <= derniereAnnee; ++annee) {
			final List<PeriodeImposition> r = calculateRangesDIsPourAnnee(contribuable, annee);
			if (r != null) {
				ranges.addAll(r);
			}
		}

		// [UNIREG-1742][UNIREG-2051] dans certain cas, les déclarations sont remplacées par une note à l'administration fiscale de l'autre canton
		// [UNIREG-1742] les diplomates suisses ne reçoivent pas de déclaration
		CollectionUtils.filter(ranges, new Predicate() {
			public boolean evaluate(Object object) {
				final PeriodeImposition periode = (PeriodeImposition) object;
				return !periode.isRemplaceeParNote() && !periode.isDiplomateSuisse();
			}
		});
		
		if (ranges.isEmpty()) {
			return null;
		}
		else {
			return ranges;
		}
	}

	/**
	 * Calcul et retourne la liste des périodes d'imposition devant servir de base à la création des déclarations d'impôt.
	 *
	 * @param contribuable
	 *            le contribuable concerné
	 * @param annee
	 *            l'année concernée
	 * @return une liste de ranges (dans 95% des cas, un seul range), ou <b>null</b> s'il n'y a pas de déclarations à envoyer
	 * @throws ValidationException
	 */
	private List<PeriodeImposition> calculateRangesDIsPourAnnee(final Contribuable contribuable, int annee) throws ValidationException {

		// on calcul les périodes d'imposition du contribuable
		final List<PeriodeImposition> periodes;
		try {
			periodes = PeriodeImposition.determine(contribuable, annee);
		}
		catch (AssujettissementException e) {
			throw new ValidationException(contribuable, "Impossible de calculer l'assujettissement pour la raison suivante : " + e.getMessage());
		}

		// le contribuable n'est pas assujetti cette année-là
		if (periodes == null || periodes.isEmpty()) {
			return null;
		}

		final Set<Declaration> declarations = contribuable.getDeclarations();

		// On ne retourne que les périodes qui ne sont pas déjà associées avec une déclaration
		final List<PeriodeImposition> periodesNonAssociees = new ArrayList<PeriodeImposition>();
		for (PeriodeImposition a : periodes) {
			boolean match = false;
			if (declarations != null) {
				for (Declaration d : declarations) {
					if (!d.isAnnule() && DateRangeHelper.intersect(d, a)) {
						match = true;
						break;
					}
				}
			}
			if (!match) {
				periodesNonAssociees.add(a);
			}
		}

		return periodesNonAssociees;
	}

	/**
	 * [UNIREG-832] Vérifie que les dates de début et de fin pour la création d'une déclaration d'impôt sont correctes.
	 *
	 * @param contribuable
	 *            le contribuable
	 * @param range
	 *            le range de de validité de la déclaration à créer.
	 * @throws ValidationException
	 *             si le contribuable ne valide pas, n'est pas du tout assujetti, si les dates ne correspondent pas à l'assujettissement
	 *             calculé ou s'il existe déjà une déclaration.
	 */
	protected PeriodeImposition checkRangeDi(Contribuable contribuable, DateRange range) {

		if (range.getDateDebut().year() != range.getDateFin().year()) {
			throw new ValidationException(contribuable, "La déclaration doit tenir dans une année complète.");
		}

		// le contribuable doit être valide
		final ValidationResults results = contribuable.validate();
		if (results.hasErrors()) {
			throw new ValidationException(contribuable, results.getErrors(), results.getWarnings());
		}

		final int annee = range.getDateDebut().year();
		final List<PeriodeImposition> ranges = calculateRangesDIsPourAnnee(contribuable, annee);

		if (ranges == null || ranges.isEmpty()) {
			throw new ValidationException(contribuable, "Le contribuable n'est pas assujetti du tout sur l'année " + annee + ", ou son assujettissement pour cette année est déjà couvert par les déclarations émises");
		}

		// on vérifie que la range spécifié correspond parfaitement avec l'assujettissement calculé

		DateRange elu = null;
		for (DateRange a : ranges) {
			if (DateRangeHelper.intersect(a, range)) {
				elu = a;
				break;
			}
		}

		if (elu == null) {
			throw new ValidationException(contribuable, "Le contribuable n'est pas assujetti sur la période spécifiée [" + range + "].");
		}

		if (!DateRangeHelper.equals(elu, range)) {
			throw new ValidationException(contribuable, "La période de déclaration spécifiée " + range
					+ " ne corresponds pas à la période d'imposition théorique " + DateRangeHelper.toString(elu) + ".");
		}

		// on vérifie qu'il n'existe pas déjà une déclaration

		Declaration declaration = null;

		final Set<Declaration> declarations = contribuable.getDeclarations();
		if (declarations != null) {
			for (Declaration d : declarations) {
				if (!d.isAnnule() && DateRangeHelper.intersect(d, range)) {
					declaration = d;
					break;
				}
			}
		}

		if (declaration != null) {
			throw new ValidationException(contribuable, "Le contribuable possède déjà une déclaration sur la période spécifiée [" + range
					+ "].");
		}

		return (PeriodeImposition) elu;
	}

	/**
	 * @return la plage de dates [debut; fin] couverte par la dernière déclaration (= la plus récente) existant sur le tiers spécifié; ou
	 *         <code>null</code> si le tiers ne possède aucune déclaration valide.
	 */
	public DateRange getDerniereDeclaration(Contribuable contribuable) {

		final List<Declaration> declarations = contribuable.getDeclarationsSorted();
		if (declarations == null || declarations.isEmpty()) {
			return null;
		}

		// recherche la dernière déclaration non-annulée
		Declaration derniere = null;
		for (int i = declarations.size() - 1; i >= 0; --i) {
			Declaration d = declarations.get(i);
			if (!d.isAnnule()) {
				derniere = d;
				break;
			}

		}

		return derniere;
	}

	@Transactional(readOnly = true)
	public RegDate getDateNewDi(Long numero){
		RegDate dateNewDi = null;

		//calcul de la date pour une nouvelle DI
		EtatDeclaration etatPeriode = diDAO.findDerniereDiEnvoyee(numero);
		if (etatPeriode != null) {
			dateNewDi = etatPeriode.getDeclaration().getDateDebut();
			dateNewDi = RegDate.get(dateNewDi.year() + 1, 1, 1);
		}
		else {
			dateNewDi = RegDate.get().addYears(-1);
		}

		//vérification que la période fiscale correspondant existe
		PeriodeFiscale periodFisc = periodeFiscaleDAO.getPeriodeFiscaleByYear(dateNewDi.year());
		if(periodFisc != null)
			return dateNewDi;
		else
			return null;
	}

	/**
	 * @param diImpressionView
	 * @throws DeclarationException
	 */
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocalDuplicataDI(DeclarationImpotImpressionView diImpressionView) throws DeclarationException {
		final DeclarationImpotOrdinaire declaration = diDAO.get(diImpressionView.getIdDI());

		if (tiersService.getAndSetOfficeImpot(declaration.getTiers()) == null) {
			throw new DeclarationException("Le contribuable ne possède pas de for de gestion");
		}
		final PeriodeFiscale periode = periodeFiscaleDAO.getPeriodeFiscaleByYear(declaration.getPeriode().getAnnee());
		final ModeleDocument modele = modeleDocumentDAO.getModelePourDeclarationImpotOrdinaire(periode, diImpressionView.getSelectedTypeDocument());
		declaration .setModeleDocument(modele);

		List<ch.vd.uniregctb.declaration.ordinaire.ModeleFeuilleDocumentEditique> annexes = null;

		final List<ModeleDocumentView> modeles = diImpressionView.getModelesDocumentView();
		for (ModeleDocumentView modeleView : modeles) {
			if (modeleView.getTypeDocument().equals(diImpressionView.getSelectedTypeDocument())) {
				annexes = modeleView.getModelesFeuilles();
				break;
			}
		}

		return diService.envoiDuplicataDIOnline(declaration, RegDate.get(), diImpressionView.getSelectedTypeDocument(), annexes);
	}

	/**
	 * Alimente la vue DeclarationImpotListView en fonction d'un contribuable
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	public void findByNumero(Long numero, DeclarationImpotListView diListView) {

		diListView.setContribuable(creerCtbDI(numero));
		List<DeclarationImpotDetailView> disView = new ArrayList<DeclarationImpotDetailView>();
		List<DeclarationImpotOrdinaire> dis = diDAO.findByNumero(numero);
		for (DeclarationImpotOrdinaire di : dis) {
			DeclarationImpotDetailView diView = new DeclarationImpotDetailView();
			diView.setId(di.getId());
			diView.setPeriodeFiscale(di.getPeriode().getAnnee());
			diView.setDateDebutPeriodeImposition(di.getDateDebut());
			diView.setDateFinPeriodeImposition(di.getDateFin());
			diView.setDelaiAccorde(di.getDelaiAccordeAu());
			diView.setDateRetour(di.getDateRetour());
			final EtatDeclaration etat = di.getDernierEtat();
			diView.setEtat(etat == null ? null : etat.getEtat());
			diView.setAnnule(di.isAnnule());
			disView.add(diView);
		}
		Collections.sort(disView, new DeclarationImpotDetailComparator());
		diListView.setDis(disView);
	}

	/**
	 * Alimente la vue DeclarationImpotEditView en fonction de l'ID de la DI
	 * @param id
	 * @return une vue DeclarationImpotEditView
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	public void get(Long id, DeclarationImpotDetailView diEditView) {

		DeclarationImpotOrdinaire di = diDAO.get(id);
		if (di == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.di.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}
		Contribuable ctb = (Contribuable) di.getTiers();
		TiersGeneralView tiersGeneralView = tiersGeneralManager.get(ctb);
		diEditView.setImprimable(true);
		diEditView.setContribuable(tiersGeneralView);
		diEditView.setId(id);

		final EtatDeclaration etatDI = di.getDernierEtat();
		diEditView.setEtat(etatDI == null ? null : etatDI.getEtat());
		diEditView.setPeriodeFiscale(di.getPeriode().getAnnee());
		diEditView.setTypeDeclarationImpot(di.getTypeDeclaration());
		diEditView.setDateDebutPeriodeImposition(di.getDateDebut());
		diEditView.setDateFinPeriodeImposition(di.getDateFin());
		diEditView.setDateRetour(di.getDateRetour());

		setDelais(diEditView, di);

		diEditView.setAnnule(di.isAnnule());
		setDroitDI(diEditView, di.getTiers());
		boolean isSommable = false;
		if (etatDI != null && etatDI.getEtat() == TypeEtatDeclaration.EMISE) {
			if (di.getDelaiAccordeAu() == null || RegDate.get().isAfter(di.getDelaiAccordeAu())) {
				isSommable = true;
			}
		}
		else {
			diEditView.setAllowedDelai(false);
		}

		boolean wasSommee = false;
		for (EtatDeclaration etat : di.getEtats()) {
			if (!etat.isAnnule() && etat.getEtat() == TypeEtatDeclaration.SOMMEE) {
				wasSommee = true;
				break;
			}
		}

		diEditView.setSommable(isSommable);
		diEditView.setWasSommee(wasSommee);
	}

	@Transactional(readOnly = true)
	public Long getTiersId(Long idDI) {
		final DeclarationImpotOrdinaire di = diDAO.get(idDI);
		if (di == null) {
			return null;
		}
		return di.getTiers().getNumero();
	}

	private void setDelais(DeclarationImpotDetailView diEditView, DeclarationImpotOrdinaire di) {
		List<DelaiDeclarationView> delaisView = new ArrayList<DelaiDeclarationView>();
		for (DelaiDeclaration delai : di.getDelais()) {
			DelaiDeclarationView delaiView = new DelaiDeclarationView();
			delaiView.setId(delai.getId());
			delaiView.setAnnule(delai.isAnnule());
			delaiView.setConfirmationEcrite(delai.getConfirmationEcrite());
			delaiView.setDateDemande(delai.getDateDemande());
			delaiView.setDateTraitement(delai.getDateTraitement());
			delaiView.setDelaiAccordeAu(delai.getDelaiAccordeAu());
			delaiView.setLogModifDate(delai.getLogModifDate());
			delaiView.setLogModifUser(delai.getLogModifUser());
			if (di.getPremierDelai() != null) {
				if (di.getPremierDelai().equals(delai.getDelaiAccordeAu())) {
					delaiView.setFirst(true);
				}
				else {
					delaiView.setFirst(false);
				}
			}
			delaisView.add(delaiView);
		}
		Collections.sort(delaisView);
		diEditView.setDelais(delaisView);
	}

	/**
	 * positionne les droits de l'utilisateur
	 * @param diEditView
	 */
	private void setDroitDI(DeclarationImpotDetailView diEditView, Tiers tiers) {

		final Niveau acces = SecurityProvider.getDroitAcces(tiers);
		if (acces == null || acces.equals(Niveau.LECTURE)) {
			diEditView.setAllowedSommation(false);
			diEditView.setAllowedDelai(false);
			diEditView.setAllowedDuplic(false);
			diEditView.setAllowedQuittancement(false);
		}
		else {
			//seuls les entreprise, les habitants, les non habitants et les ménages peuvent avoir une DI
			if(tiers instanceof Entreprise){
				//les entreprise ne sont pas gérée pour le moment, il est interdit de créer une DI pour une entreprise
				diEditView.setAllowedSommation(SecurityProvider.isGranted(Role.DI_SOM_PM));
				diEditView.setAllowedDelai(SecurityProvider.isGranted(Role.DI_DELAI_PM));
				diEditView.setAllowedDuplic(SecurityProvider.isGranted(Role.DI_DUPLIC_PM));
				diEditView.setAllowedQuittancement(SecurityProvider.isGranted(Role.DI_QUIT_PM));
			}
			else { //PP
				diEditView.setAllowedSommation(SecurityProvider.isGranted(Role.DI_SOM_PP));
				diEditView.setAllowedDelai(SecurityProvider.isGranted(Role.DI_DELAI_PP));
				diEditView.setAllowedDuplic(SecurityProvider.isGranted(Role.DI_DUPLIC_PP));
				diEditView.setAllowedQuittancement(SecurityProvider.isGranted(Role.DI_QUIT_PP));
			}
		}
	}

	/**
	 * Reactualise la vue
	 *
	 * @param diEditView
	 * @return
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	public DeclarationImpotDetailView refresh(DeclarationImpotDetailView diEditView) {
		DeclarationImpotOrdinaire di = diDAO.get(diEditView.getId());
		if (di == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.di.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}

		Contribuable ctb = (Contribuable) di.getTiers();
		TiersGeneralView tiersGeneralView = tiersGeneralManager.get(ctb);
		diEditView.setContribuable(tiersGeneralView);
		diEditView.setPeriodeFiscale(di.getPeriode().getAnnee());
		diEditView.setDateDebutPeriodeImposition(di.getDateDebut());
		diEditView.setDateFinPeriodeImposition(di.getDateFin());

		setDelais(diEditView, di);

		diEditView.setAnnule(di.isAnnule());
		return diEditView;
	}


	/**
	 * Imprime une DI vierge
	 * Partie envoie
	 * @param diEditView
	 * @throws Exception
	 */
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocalDI(DeclarationImpotDetailView diEditView) throws Exception {
		//Sauvegarde de la DI
		DeclarationImpotOrdinaire declaration = save(diEditView);
		if (tiersService.getAndSetOfficeImpot(declaration.getTiers()) == null) {
			throw new DeclarationException("Le contribuable ne possède pas de for de gestion");
		}
		//Envoi du flux xml à l'éditique + envoi d'un événement fiscal
		return diService.envoiDIOnline(declaration, RegDate.get());
	}

	/**
	 * Persiste en base et indexe le tiers modifie
	 */
	@Transactional(rollbackFor = Throwable.class)
	public DeclarationImpotOrdinaire save(DeclarationImpotDetailView diEditView) throws Exception {
		Contribuable ctb = (Contribuable) tiersDAO.get(diEditView.getContribuable().getNumero());
		DeclarationImpotOrdinaire di = null;
		if (ctb == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.contribuable.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		if (diEditView.getId() == null)	{
			di = new DeclarationImpotOrdinaire();

			RegDate dateDebut = diEditView.getRegDateDebutPeriodeImposition();
			di.setNumero(Integer.valueOf(1));
			di.setDateDebut(dateDebut);
			di.setDateFin(RegDate.get(diEditView.getDateFinPeriodeImposition()));

			TypeContribuable typeContribuable = null;
			final List<Assujettissement> assujettissements = Assujettissement.determine(ctb, dateDebut.year());
			if (assujettissements != null) {
				final Assujettissement assujettissement = assujettissements.get(assujettissements.size() - 1);
				if ((assujettissement instanceof VaudoisOrdinaire) || (assujettissement instanceof Indigent)) {
					typeContribuable = TypeContribuable.VAUDOIS_ORDINAIRE;
				}
				else if (assujettissement instanceof HorsSuisse) {
					typeContribuable = TypeContribuable.HORS_SUISSE;
				}
				else if (assujettissement instanceof HorsCanton) {
					typeContribuable = TypeContribuable.HORS_CANTON;
				}
				else if (assujettissement instanceof VaudoisDepense) {
					typeContribuable = TypeContribuable.VAUDOIS_DEPENSE;
				}
			}
			di.setTypeContribuable(typeContribuable);

			final ForGestion forGestion = tiersService.getForGestionActif(ctb, di.getDateFin());
			if (forGestion != null) {
				di.setNumeroOfsForGestion(forGestion.getNoOfsCommune());
			}
			else {
				throw new ActionException("le contribuable ne possède pas de for de gestion au " + di.getDateFin());
			}

			final PeriodeFiscale periode = periodeFiscaleDAO.getPeriodeFiscaleByYear(dateDebut.year());
			if (periode == null) {
				throw new ActionException("la période fiscale pour l'année " + dateDebut.year() + " n'existe pas.");
			}
			di.setPeriode(periode);

			ModeleDocument modeleDocument = modeleDocumentDAO.getModelePourDeclarationImpotOrdinaire(periode, diEditView
					.getTypeDeclarationImpot());
			if (modeleDocument == null) {
				throw new ActionException("le modèle de document " + diEditView.getTypeDeclarationImpot() + " pour l'année "
						+ dateDebut.year() + " n'existe pas.");
			}
			di.setModeleDocument(modeleDocument);

			ch.vd.uniregctb.tiers.CollectiviteAdministrative collectiviteAdministrative = null;
			if (diEditView.getTypeAdresseRetour().equals(TypeAdresseRetour.ACI)) {
				collectiviteAdministrative = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noACI);
			}
			else if (diEditView.getTypeAdresseRetour().equals(TypeAdresseRetour.CEDI)) {
				collectiviteAdministrative = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
			}
			else {
				Integer officeImpot = tiersService.getAndSetOfficeImpot(ctb);
				if (officeImpot == null) {
					throw new ActionException("le contribuable ne possède pas de for de gestion");
				}
				collectiviteAdministrative = tiersService.getOrCreateCollectiviteAdministrative(officeImpot.intValue());
			}
			di.setRetourCollectiviteAdministrative(collectiviteAdministrative);

			Qualification derniereQualification = PeriodeImposition.determineQualification(ctb, diEditView.getRegDateFinPeriodeImposition().year());
			di.setQualification(derniereQualification);

			EtatDeclaration etat = new EtatDeclaration();
			etat.setEtat(TypeEtatDeclaration.EMISE);
			etat.setDateObtention(RegDate.get());
			di.addEtat(etat);

			DelaiDeclaration delai = new DelaiDeclaration();
			delai.setDelaiAccordeAu(diEditView.getRegDelaiAccorde());
			delai.setDateDemande(RegDate.get());
			delai.setDateTraitement(RegDate.get());
			di.addDelai(delai);

			di.setTiers(ctb);
			di = diDAO.save(di);

			ctb.addDeclaration(di);
			tiersDAO.save(ctb);

			//Mise à jour de l'état de la tâche si il y en a une
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
			criterion.setAnnee(new Integer(dateDebut.year()));
			criterion.setEtatTache(TypeEtatTache.EN_INSTANCE);
			criterion.setContribuable(ctb);
			List<Tache> taches = tacheDAO.find(criterion);
			if (taches != null && taches.size() != 0) {
				for (Tache t : taches) {
					TacheEnvoiDeclarationImpot tache = (TacheEnvoiDeclarationImpot)t;
					if (tache.getDateDebut().equals(di.getDateDebut()) &&
							tache.getDateFin().equals(di.getDateFin())) {
						tache.setEtat(TypeEtatTache.TRAITE);
					}
				}
			}
		} else {
			di = diDAO.get(diEditView.getId());
			if (diEditView.getRegDateRetour() != null) {
				if (!diEditView.getRegDateRetour().equals(di.getDateRetour())) {
					if (di.getDateRetour() != null){
						EtatDeclaration etatRetournePrecedent = di.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE);
						etatRetournePrecedent.setAnnule(true);
					}
					EtatDeclaration etat = new EtatDeclaration();
					etat.setEtat(TypeEtatDeclaration.RETOURNEE);
					etat.setDateObtention(RegDate.get(diEditView.getDateRetour()));
					di.addEtat(etat);
					evenementFiscalService.publierEvenementFiscalRetourDI((Contribuable) di.getTiers(), di, RegDate.get(diEditView.getDateRetour()));
				}
			}
			else {
				EtatDeclaration etatRetournePrecedent = di.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE);
				if (etatRetournePrecedent != null) {
					etatRetournePrecedent.setAnnule(true);
				}
			}
		}

		return di;
	}

	/**
	 * Cree une vue pour le delai d'une declaration
	 *
	 * @param idDeclaration
	 * @return
	 */
	@Transactional(rollbackFor = Throwable.class)
	public DelaiDeclarationView creerDelai(Long idDeclaration) {
		DelaiDeclarationView  delaiView = new DelaiDeclarationView();
		delaiView.setIdDeclaration(idDeclaration);
		DeclarationImpotOrdinaire di = diDAO.get(idDeclaration);
		if (di == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.di.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}
		delaiView.setDateExpedition(di.getDateExpedition());
		delaiView.setOldDelaiAccorde(di.getDelaiAccordeAu());
		delaiView.setAnnule(false);
		// [UNIREG-1119] ajout de valeurs par défaut pour le délai
		delaiView.setDateDemande(RegDate.get());
		delaiView.setDelaiAccordeAu(delaisService.getDateFinDelaiRetourDeclarationImpotEmiseManuellement(delaiView.getDateDemande()));

		return delaiView;
	}

	/**
	 * Persiste en base le delai
	 *
	 * @param delaiView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void saveDelai(DelaiDeclarationView delaiView) {
		DeclarationImpotOrdinaire di = diDAO.get(delaiView.getIdDeclaration());
		DelaiDeclaration delai = new DelaiDeclaration();
		delai.setDateTraitement(RegDate.get());
		delai.setAnnule(delaiView.isAnnule());
		delai.setConfirmationEcrite(delaiView.getConfirmationEcrite());
		delai.setDateDemande(delaiView.getDateDemande());
		delai.setDelaiAccordeAu(delaiView.getDelaiAccordeAu());
		di.addDelai(delai);
	}

	/**
	 * Alimente la vue contribuable pour la DI
	 *
	 * @param numero
	 * @return
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	public TiersGeneralView creerCtbDI(Long numero) {

		Tiers tiers = tiersDAO.get(numero);
		if (tiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.tiers.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}
		TiersGeneralView tiersGeneralView = tiersGeneralManager.get(tiers);

		return tiersGeneralView;
	}

	/**
	 * Sommer une Declaration Impot
	 *
	 * @throws EditiqueException
	 */
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocalSommationDI(DeclarationImpotDetailView bean) throws EditiqueException {
		final DeclarationImpotOrdinaire di = diDAO.get(bean.getId());
		final EtatDeclaration etat = new EtatDeclaration();
		etat.setEtat(TypeEtatDeclaration.SOMMEE);
		etat.setDateObtention(RegDate.get());
		di.addEtat(etat);
		diDAO.save(di);

		try {
			final EditiqueResultat resultat = editiqueCompositionService.imprimeSommationDIOnline(di, RegDate.get());
			evenementFiscalService.publierEvenementFiscalSommationDI((Contribuable)di.getTiers(), di, etat.getDateObtention());
			return resultat;
		}
		catch (JMSException e) {
			throw new EditiqueException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocalConfirmationDelai(DeclarationImpotDetailView diEditView, Long idDelai) throws EditiqueException {
		try {
			final DelaiDeclaration delai = delaiDeclarationDAO.get(idDelai);
			final DeclarationImpotOrdinaire di = diDAO.get(diEditView.getId());
			return editiqueCompositionService.imprimeConfirmationDelaiOnline(di, delai);
		}
		catch (JMSException e) {
			throw new EditiqueException(e);
		}
	}

	/**
	 * Imprimer une taxation d'office
	 *
	 * @param diEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void imprimerTO(DeclarationImpotDetailView diEditView) {
		DeclarationImpotOrdinaire di = diDAO.get(diEditView.getId());
		evenementFiscalService.publierEvenementFiscalTaxationOffice((Contribuable) di.getTiers(), di, RegDate.get());
	}


	/**
	 * Alimente la vue du controller DeclarationImpotImpressionController
	 *
	 * @param id
	 * @param typeDocument
	 * @return
	 */
	@Transactional(readOnly = true)
	public DeclarationImpotImpressionView getView(Long id, String typeDocument) {

		DeclarationImpotOrdinaire di = diDAO.get(id);
		TypeDocument enumTypeDocument = null;
		if (typeDocument == null) {
			enumTypeDocument = di.getTypeDeclaration();
			if (enumTypeDocument.equals(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH)) {
				enumTypeDocument = TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL;
			}
		} else {
			enumTypeDocument = TypeDocument.valueOf(typeDocument);
		}

		List<ModeleDocument> modelesDocument = modeleDocumentDAO.getByPeriodeFiscale(di.getPeriode());
		DeclarationImpotImpressionView declarationImpotImpressionView = new DeclarationImpotImpressionView();
		List<ModeleDocumentView> modelesDocumentView = new ArrayList<ModeleDocumentView>();
		for (ModeleDocument modele : modelesDocument) {
			if (!modele.getTypeDocument().equals(TypeDocument.LISTE_RECAPITULATIVE)
					&& !modele.getTypeDocument().equals(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH)) {
				List<ModeleFeuilleDocumentEditique> modelesFeuilleDocumentView = new ArrayList<ModeleFeuilleDocumentEditique>();
				for (ModeleFeuilleDocument modeleFeuilleDocument : modele.getModelesFeuilleDocument()) {
					ModeleFeuilleDocumentEditique modeleFeuilleDocumentView = new ModeleFeuilleDocumentEditique();
					modeleFeuilleDocumentView.setIntituleFeuille(modeleFeuilleDocument.getIntituleFeuille());
					modeleFeuilleDocumentView.setNumeroFormulaire(modeleFeuilleDocument.getNumeroFormulaire());
					if (enumTypeDocument.equals(modele.getTypeDocument())) {
						modeleFeuilleDocumentView.setNbreIntituleFeuille(Integer.valueOf(1));
					}
					modelesFeuilleDocumentView.add(modeleFeuilleDocumentView);
				}
				ModeleDocumentView modeleView = new ModeleDocumentView();
				modeleView.setTypeDocument(modele.getTypeDocument());
				Collections.sort(modelesFeuilleDocumentView);
				modeleView.setModelesFeuilles(modelesFeuilleDocumentView);
				modelesDocumentView.add(modeleView);
			}
		}

		declarationImpotImpressionView.setIdDI(id);
		declarationImpotImpressionView.setSelectedTypeDocument(enumTypeDocument);

		declarationImpotImpressionView.setModelesDocumentView(modelesDocumentView);
		return declarationImpotImpressionView;
	}

	public void setDiDAO(DeclarationImpotOrdinaireDAO diDAO) {
		this.diDAO = diDAO;
	}

	public void setDiService(DeclarationImpotService diService) {
		this.diService = diService;
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}

	public void setModeleDocumentDAO(ModeleDocumentDAO modeleDocumentDAO) {
		this.modeleDocumentDAO = modeleDocumentDAO;
	}

	public void setTacheDAO(TacheDAO tacheDAO) {
		this.tacheDAO = tacheDAO;
	}

	/**
	 * @return the messageSource
	 */
	protected MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setEditiqueCompositionService(EditiqueCompositionService editiqueCompositionService) {
		this.editiqueCompositionService = editiqueCompositionService;
	}

	public void setEditiqueService(EditiqueService editiqueService) {
		this.editiqueService = editiqueService;
	}

	public void setDelaisService(DelaisService delaisService) {
		this.delaisService = delaisService;
	}

	public void setParametres(ParametreAppService parametres) {
		this.parametres = parametres;
	}

	public void setDelaiDeclarationDAO(DelaiDeclarationDAO delaiDeclarationDAO) {
		this.delaiDeclarationDAO = delaiDeclarationDAO;
	}

	@Transactional(rollbackFor = Throwable.class)
	public byte[] getCopieConformeSommation(DeclarationImpotDetailView diEditView) throws EditiqueException {
		final DeclarationImpotOrdinaire di = diDAO.get(diEditView.getId());
		String nomDocument = diService.construitIdArchivageSommationDI(di);
		byte[] pdf = editiqueService.getPDFDeDocumentDepuisArchive(di.getTiers().getNumero(), ImpressionSommationDIHelperImpl.TYPE_DOCUMENT_SOMMATION_DI, nomDocument);
		if (pdf == null) {
			nomDocument = diService.construitAncienIdArchivageSommationDI(di);
			pdf = editiqueService.getPDFDeDocumentDepuisArchive(di.getTiers().getNumero(), ImpressionSommationDIHelperImpl.TYPE_DOCUMENT_SOMMATION_DI, nomDocument);
		}
		if (pdf == null) {
			nomDocument = diService.construitAncienIdArchivageSommationDIPourOnLine(di);
			pdf = editiqueService.getPDFDeDocumentDepuisArchive(di.getTiers().getNumero(), ImpressionSommationDIHelperImpl.TYPE_DOCUMENT_SOMMATION_DI, nomDocument);
		}
		return pdf;
	}

	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocalTaxationOffice(DeclarationImpotDetailView bean) throws EditiqueException {
		final DeclarationImpotOrdinaire di = diDAO.get(bean.getId());
		di.setDateImpressionChemiseTaxationOffice(new Date());

		try {
			final EditiqueResultat resulat = editiqueCompositionService.imprimeTaxationOfficeOnline(di);
			return resulat;
		}
		catch (JMSException e) {
			throw new EditiqueException(e);
		}
	}

	@Transactional(rollbackFor = Throwable.class)
	public void maintenirDI(Long idTache) {
		Tache tache = tacheDAO.get(idTache);
		tache.setEtat(TypeEtatTache.TRAITE);

	}

}
