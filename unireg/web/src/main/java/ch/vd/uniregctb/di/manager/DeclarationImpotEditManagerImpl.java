package ch.vd.uniregctb.di.manager;

import javax.jms.JMSException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotCriteria;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.DelaiDeclarationDAO;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
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
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
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
import ch.vd.uniregctb.validation.ValidationService;

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

	private MessageSource messageSource;

	private DelaisService delaisService;

	private DelaiDeclarationDAO delaiDeclarationDAO;

	private ValidationService validationService;

	private ParametreAppService parametres;

	/**
	 * Contrôle que la DI existe
	 *
	 * @param id
	 */
	@Override
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
	@Override
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
	@Override
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
	@Override
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

		diEditView.setContribuable(tiersGeneralManager.getTiers(tiers, true));
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
				if (diPrecedente.getTypeDeclaration() == TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH) {
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

			// [UNIREG-2705] s'il existe une DI retournée annulée pour le même contribuable et la même
			// période, alors on propose de marquer cette nouvelle DI comme déjà retournée
			if (diEditView.isAllowedQuittancement()) {
				final DeclarationImpotCriteria criteres = new DeclarationImpotCriteria();
				criteres.setAnnee(range.getDateDebut().year());
				criteres.setContribuable(numeroCtb);
				final List<DeclarationImpotOrdinaire> dis = diDAO.find(criteres);
				if (dis != null && dis.size() > 0) {
					for (DeclarationImpotOrdinaire di : dis) {
						if (di.isAnnule() && DateRangeHelper.equals(di, periode)) {
							final EtatDeclaration etat = di.getDernierEtat();
							if (etat != null && etat.getEtat() == TypeEtatDeclaration.RETOURNEE) {
								diEditView.setDateRetour(RegDate.get());
								diEditView.setDateRetourProposeeCarDeclarationRetourneeAnnuleeExiste(true);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
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
		final ValidationResults results = validationService.validate(contribuable);
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
			@Override
			public boolean evaluate(Object object) {
				final PeriodeImposition periode = (PeriodeImposition) object;
				return !periode.isRemplaceeParNote() && !periode.isDiplomateSuisseSansImmeuble();
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
		final ValidationResults results = validationService.validate(contribuable);
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

	@Override
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
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocalDuplicataDI(DeclarationImpotImpressionView diImpressionView) throws DeclarationException {
		final DeclarationImpotOrdinaire declaration = diDAO.get(diImpressionView.getIdDI());

		if (tiersService.getOfficeImpotId(declaration.getTiers()) == null) {
			throw new DeclarationException("Le contribuable ne possède pas de for de gestion");
		}
		final PeriodeFiscale periode = periodeFiscaleDAO.getPeriodeFiscaleByYear(declaration.getPeriode().getAnnee());
		final TypeDocument selectedTypeDocument = diImpressionView.getSelectedTypeDocument();
		final ModeleDocument modele = modeleDocumentDAO.getModelePourDeclarationImpotOrdinaire(periode, selectedTypeDocument);
		declaration.setModeleDocument(modele);

		List<ch.vd.uniregctb.declaration.ordinaire.ModeleFeuilleDocumentEditique> annexes = null;

		final List<ModeleDocumentView> modeles = diImpressionView.getModelesDocumentView();
		for (ModeleDocumentView modeleView : modeles) {
			if (modeleView.getTypeDocument() == selectedTypeDocument) {
				annexes = modeleView.getModelesFeuilles();
				break;
			}
		}

		Audit.info(String.format("Impression d'un duplicata de DI pour le contribuable %d et la période [%s ; %s]",
				declaration.getTiers().getNumero(),
				RegDateHelper.dateToDashString(declaration.getDateDebut()),
				RegDateHelper.dateToDashString(declaration.getDateFin())));

		return diService.envoiDuplicataDIOnline(declaration, RegDate.get(), selectedTypeDocument, annexes);
	}

	/**
	 * Alimente la vue DeclarationImpotListView en fonction d'un contribuable
	 * @throws AdressesResolutionException
	 */
	@Override
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
	@Override
	@Transactional(readOnly = true)
	public void get(Long id, DeclarationImpotDetailView diEditView) {

		DeclarationImpotOrdinaire di = diDAO.get(id);
		if (di == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.di.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}
		Contribuable ctb = (Contribuable) di.getTiers();
		TiersGeneralView tiersGeneralView = tiersGeneralManager.getTiers(ctb, true);
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

	@Override
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
			DelaiDeclarationView delaiView = new DelaiDeclarationView(delai);
			delaiView.setFirst(di.getPremierDelai() == delai.getDelaiAccordeAu());
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
		if (acces == null || acces == Niveau.LECTURE) {
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
	@Override
	@Transactional(readOnly = true)
	public DeclarationImpotDetailView refresh(DeclarationImpotDetailView diEditView) {
		DeclarationImpotOrdinaire di = diDAO.get(diEditView.getId());
		if (di == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.di.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}

		Contribuable ctb = (Contribuable) di.getTiers();
		TiersGeneralView tiersGeneralView = tiersGeneralManager.getTiers(ctb, true);
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
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocalDI(DeclarationImpotDetailView diEditView) throws Exception {
		//Sauvegarde de la DI
		DeclarationImpotOrdinaire declaration = save(diEditView);
		if (tiersService.getOfficeImpotId(declaration.getTiers()) == null) {
			throw new DeclarationException("Le contribuable ne possède pas de for de gestion");
		}
		//Envoi du flux xml à l'éditique + envoi d'un événement fiscal
		return diService.envoiDIOnline(declaration, RegDate.get());
	}

	/**
	 * Persiste en base et indexe le tiers modifie
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public DeclarationImpotOrdinaire save(DeclarationImpotDetailView diEditView) throws Exception {
		final Contribuable ctb = (Contribuable) tiersDAO.get(diEditView.getContribuable().getNumero());
		DeclarationImpotOrdinaire di;
		if (ctb == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.contribuable.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		if (diEditView.getId() == null)	{
			di = new DeclarationImpotOrdinaire();

			final RegDate dateDebut = diEditView.getRegDateDebutPeriodeImposition();
			di.setNumero(1);
			di.setDateDebut(dateDebut);
			di.setDateFin(RegDate.get(diEditView.getDateFinPeriodeImposition()));

			final TypeContribuable typeContribuable;
			final boolean diLibre;
			final List<PeriodeImposition> periodesImposition = PeriodeImposition.determine(ctb, dateDebut.year());
			if (periodesImposition != null) {
				final PeriodeImposition dernierePeriode = periodesImposition.get(periodesImposition.size() - 1);
				typeContribuable = dernierePeriode.getTypeContribuable();

				// une DI est libre si je ne trouve aucune période d'assujettissement qui colle avec les dates de la DI elle-même
				// (il faut quand-même accessoirement qu'elle soit créée sur la période fiscale courante)
				if (dateDebut.year() == RegDate.get().year()) {
					boolean trouveMatch = false;
					for (PeriodeImposition p : periodesImposition) {
						trouveMatch = DateRangeHelper.equals(p, di);
						if (trouveMatch) {
							break;
						}
					}
					diLibre = !trouveMatch;
				}
				else {
					diLibre = false;
				}
			}
			else {
				// pas d'assujettissement + di = di libre, forcément...
				diLibre = true;
				typeContribuable = null;
			}

			di.setTypeContribuable(typeContribuable);
			di.setLibre(diLibre);

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

			// assigne le modèle de document à la DI en fonction de ce que contient la vue
			assigneModeleDocument(diEditView, di);

			final ch.vd.uniregctb.tiers.CollectiviteAdministrative collectiviteAdministrative;
			if (diEditView.getTypeAdresseRetour() == TypeAdresseRetour.ACI) {
				collectiviteAdministrative = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noACI);
			}
			else if (diEditView.getTypeAdresseRetour() == TypeAdresseRetour.CEDI) {
				collectiviteAdministrative = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
			}
			else {
				final Integer officeImpot = tiersService.getOfficeImpotId(ctb);
				if (officeImpot == null) {
					throw new ActionException("le contribuable ne possède pas de for de gestion");
				}
				collectiviteAdministrative = tiersService.getOrCreateCollectiviteAdministrative(officeImpot);
			}
			di.setRetourCollectiviteAdministrativeId(collectiviteAdministrative.getId());

			final Qualification derniereQualification = PeriodeImposition.determineQualification(ctb, diEditView.getRegDateFinPeriodeImposition().year());
			di.setQualification(derniereQualification);

			final EtatDeclaration emission = new EtatDeclarationEmise(RegDate.get());
			di.addEtat(emission);

			// [UNIREG-2705] Création d'une DI déjà retournée
			if (diEditView.getDateRetour() != null) {
				if (!RegDateHelper.isAfterOrEqual(diEditView.getRegDateRetour(), RegDate.get(), NullDateBehavior.LATEST)) {
					throw new ActionException("La date de retour d'une DI émise aujourd'hui ne peut pas être dans le passé");
				}

				final EtatDeclaration retour = new EtatDeclarationRetournee(diEditView.getRegDateRetour());
				di.addEtat(retour);
			}

			final DelaiDeclaration delai = new DelaiDeclaration();
			delai.setDelaiAccordeAu(diEditView.getRegDelaiAccorde());
			delai.setDateDemande(RegDate.get());
			delai.setDateTraitement(RegDate.get());
			di.addDelai(delai);

			di.setTiers(ctb);
			di = diDAO.save(di);

			ctb.addDeclaration(di);
			tiersDAO.save(ctb);

			//Mise à jour de l'état de la tâche si il y en a une
			final TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
			criterion.setAnnee(dateDebut.year());
			criterion.setEtatTache(TypeEtatTache.EN_INSTANCE);
			criterion.setContribuable(ctb);
			final List<Tache> taches = tacheDAO.find(criterion);
			if (taches != null && taches.size() != 0) {
				for (Tache t : taches) {
					final TacheEnvoiDeclarationImpot tache = (TacheEnvoiDeclarationImpot)t;
					if (tache.getDateDebut().equals(di.getDateDebut()) && tache.getDateFin().equals(di.getDateFin())) {
						tache.setEtat(TypeEtatTache.TRAITE);
					}
				}
			}
		} else {
			di = diDAO.get(diEditView.getId());
			if (diEditView.getRegDateRetour() != null) {
				if (!diEditView.getRegDateRetour().equals(di.getDateRetour())) {
					if (di.getDateRetour() != null){
						final EtatDeclaration etatRetournePrecedent = di.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE);
						etatRetournePrecedent.setAnnule(true);
					}
					final EtatDeclaration etat = new EtatDeclarationRetournee();
					etat.setDateObtention(RegDate.get(diEditView.getDateRetour()));
					di.addEtat(etat);
					evenementFiscalService.publierEvenementFiscalRetourDI((Contribuable) di.getTiers(), di, RegDate.get(diEditView.getDateRetour()));
				}
			}
			else {
				final EtatDeclaration etatRetournePrecedent = di.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE);
				if (etatRetournePrecedent != null) {
					etatRetournePrecedent.setAnnule(true);
				}
			}

			// UNIREG-1437 : on peut aussi changer le type de document
			if (di.getTypeDeclaration() != diEditView.getTypeDeclarationImpot()) {

				// les types qui peuvent revenir de la view sont COMPLETE_LOCAL et VAUDTAX
				// on ne va pas remplacer un COMPLETE_BATCH par un COMPLETE_LOCAL, cela ne sert à rien
				if (di.getTypeDeclaration() != TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH || diEditView.getTypeDeclarationImpot() != TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL) {
					assigneModeleDocument(diEditView, di);
				}
			}
		}

		return di;
	}

	/**
	 * Assigne le modèle de document à la DI en fonction du type de document trouvé dans la view
	 * @param diEditView view utilisée comme source du type de document ({@link ch.vd.uniregctb.di.view.DeclarationImpotDetailView#getTypeDeclarationImpot()}
	 * @param di DI à laquelle le modèle de document sera assigné
	 */
	private void assigneModeleDocument(DeclarationImpotDetailView diEditView, DeclarationImpotOrdinaire di) {
		final PeriodeFiscale periode = di.getPeriode();
		final ModeleDocument modeleDocument = modeleDocumentDAO.getModelePourDeclarationImpotOrdinaire(periode, diEditView.getTypeDeclarationImpot());
		if (modeleDocument == null) {
			throw new ActionException(String.format("Le modèle de document %s pour l'année %d n'existe pas.", diEditView.getTypeDeclarationImpot(), periode.getAnnee()));
		}
		di.setModeleDocument(modeleDocument);
	}

	/**
	 * Cree une vue pour le delai d'une declaration
	 *
	 * @param idDeclaration
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public DelaiDeclarationView creerDelai(Long idDeclaration) {
		DelaiDeclarationView  delaiView = new DelaiDeclarationView();
		delaiView.setIdDeclaration(idDeclaration);
		DeclarationImpotOrdinaire di = diDAO.get(idDeclaration);
		if (di == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.di.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}
		delaiView.setTiersId(di.getTiers().getId());
		delaiView.setDeclarationPeriode(di.getPeriode().getAnnee());
		delaiView.setDeclarationRange(new DateRangeHelper.Range(di.getDateDebut(), di.getDateFin()));
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
	@Override
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
	@Override
	@Transactional(readOnly = true)
	public TiersGeneralView creerCtbDI(Long numero) {

		Tiers tiers = tiersDAO.get(numero);
		if (tiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.tiers.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}
		TiersGeneralView tiersGeneralView = tiersGeneralManager.getTiers(tiers, true);

		return tiersGeneralView;
	}

	/**
	 * Sommer une Declaration Impot
	 *
	 * @throws EditiqueException
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocalSommationDI(DeclarationImpotDetailView bean) throws EditiqueException {

		final RegDate dateDuJour = RegDate.get();
		final DeclarationImpotOrdinaire di = diDAO.get(bean.getId());
		final EtatDeclarationSommee etat = new EtatDeclarationSommee(dateDuJour,dateDuJour);
		di.addEtat(etat);
		diDAO.save(di);

		try {
			final EditiqueResultat resultat = editiqueCompositionService.imprimeSommationDIOnline(di, dateDuJour);
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
	@Override
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
	 * Renvoie le modèle de document du type recherché présent dans la liste, ou <code>null</code> s'il n'y en a pas
	 * @param modeles les modèles à fouiller
	 * @param type le type recherché
	 * @return le premier modèle trouvé dans la liste correspondant au type recherché
	 */
	private static ModeleDocument findModeleOfType(Collection<ModeleDocument> modeles, TypeDocument type) {
		for (ModeleDocument modele : modeles) {
			if (modele.getTypeDocument() == type) {
				return modele;
			}
		}
		return null;
	}

	/**
	 * Alimente la vue du controller DeclarationImpotImpressionController
	 *
	 * @param id
	 * @param typeDocument
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public DeclarationImpotImpressionView getView(Long id, String typeDocument) {

		final DeclarationImpotOrdinaire di = diDAO.get(id);
		final TypeDocument enumTypeDocument;
		if (typeDocument == null) {
			if (di.getTypeDeclaration() == TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH) {
				enumTypeDocument = TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL;
			}
			else {
				enumTypeDocument = di.getTypeDeclaration();
			}
		}
		else {
			enumTypeDocument = TypeDocument.valueOf(typeDocument);
		}

		final List<ModeleDocument> modelesDocument = modeleDocumentDAO.getByPeriodeFiscale(di.getPeriode());

		// [UNIREG-2001] si une annexe est dans la partie "LOCAL" mais pas dans la partie "BATCH", on ne la demande pas par défaut
		// (on stocke ici donc tous les formulaires existants dans la partie "BATCH" pour un contrôle rapide)
		final ModeleDocument modeleDocumentCompleteBatch = findModeleOfType(modelesDocument, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
		final Set<String> numerosFormulairesBatch = new HashSet<String>();
		if (modeleDocumentCompleteBatch != null) {
			for (ModeleFeuilleDocument modeleFeuille : modeleDocumentCompleteBatch.getModelesFeuilleDocument()) {
				numerosFormulairesBatch.add(modeleFeuille.getNumeroFormulaire());
			}
		}

		final List<ModeleDocumentView> modelesDocumentView = new ArrayList<ModeleDocumentView>();
		for (ModeleDocument modele : modelesDocument) {
			if (modele.getTypeDocument() != TypeDocument.LISTE_RECAPITULATIVE && modele.getTypeDocument() != TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH) {

				final List<ModeleFeuilleDocumentEditique> modelesFeuilleDocumentView = new ArrayList<ModeleFeuilleDocumentEditique>();
				for (ModeleFeuilleDocument modeleFeuilleDocument : modele.getModelesFeuilleDocument()) {

					// [UNIREG-2001] si une annexe est dans la partie "LOCAL" mais pas dans la partie "BATCH", on ne la demande pas par défaut
					final int nbreFeuilles;
					if (numerosFormulairesBatch.size() > 0 && modele.getTypeDocument() == TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL) {
						if (numerosFormulairesBatch.contains(modeleFeuilleDocument.getNumeroFormulaire())) {
							nbreFeuilles = 1;
						}
						else {
							nbreFeuilles = 0;
						}
					}
					else {
						nbreFeuilles = 1;
					}

					final ModeleFeuilleDocumentEditique modeleFeuilleDocumentView = new ModeleFeuilleDocumentEditique();
					modeleFeuilleDocumentView.setIntituleFeuille(modeleFeuilleDocument.getIntituleFeuille());
					modeleFeuilleDocumentView.setNumeroFormulaire(modeleFeuilleDocument.getNumeroFormulaire());
					modeleFeuilleDocumentView.setNbreIntituleFeuille(nbreFeuilles);
					modelesFeuilleDocumentView.add(modeleFeuilleDocumentView);
				}
				final ModeleDocumentView modeleView = new ModeleDocumentView();
				modeleView.setTypeDocument(modele.getTypeDocument());
				Collections.sort(modelesFeuilleDocumentView);
				modeleView.setModelesFeuilles(modelesFeuilleDocumentView);
				modelesDocumentView.add(modeleView);
			}
		}

		final DeclarationImpotImpressionView declarationImpotImpressionView = new DeclarationImpotImpressionView();
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

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setEditiqueCompositionService(EditiqueCompositionService editiqueCompositionService) {
		this.editiqueCompositionService = editiqueCompositionService;
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

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocalTaxationOffice(DeclarationImpotDetailView bean) throws EditiqueException {
		final DeclarationImpotOrdinaire di = diDAO.get(bean.getId());
		di.setDateImpressionChemiseTaxationOffice(DateHelper.getCurrentDate());

		try {
			final EditiqueResultat resulat = editiqueCompositionService.imprimeTaxationOfficeOnline(di);
			return resulat;
		}
		catch (JMSException e) {
			throw new EditiqueException(e);
		}
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void maintenirDI(Long idTache) {
		Tache tache = tacheDAO.get(idTache);
		tache.setEtat(TypeEtatTache.TRAITE);

	}

}
