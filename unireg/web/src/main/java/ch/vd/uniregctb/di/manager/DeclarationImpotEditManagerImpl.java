package ch.vd.uniregctb.di.manager;

import javax.jms.JMSException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.DelaiDeclarationDAO;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.ordinaire.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.jms.BamMessageHelper;
import ch.vd.uniregctb.jms.BamMessageSender;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionHelper;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionPersonnesPhysiques;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheCriteria;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpotPP;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;
import ch.vd.uniregctb.utils.WebContextUtils;
import ch.vd.uniregctb.validation.ValidationService;

/**
 * Service offrant des methodes pour gérer le controller DeclarationImpotEditController
 *
 * @author xcifde
 */
public class DeclarationImpotEditManagerImpl implements DeclarationImpotEditManager, MessageSourceAware {

	protected static final Logger LOGGER = LoggerFactory.getLogger(DeclarationImpotEditManagerImpl.class);

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
	private DelaiDeclarationDAO delaiDeclarationDAO;
	private ValidationService validationService;
	private ParametreAppService parametres;
	private BamMessageSender bamMessageSender;
	private PeriodeImpositionService periodeImpositionService;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<PeriodeImposition> calculateRangesProchainesDIs(Long numero) throws ValidationException {

		// on charge le tiers
		final ContribuableImpositionPersonnesPhysiques contribuable = (ContribuableImpositionPersonnesPhysiques) tiersDAO.get(numero);
		if (contribuable == null) {
			throw new TiersNotFoundException(numero);
		}

		return calculateRangesProchainesDIs(contribuable);
	}

	protected List<PeriodeImposition> calculateRangesProchainesDIs(final ContribuableImpositionPersonnesPhysiques contribuable) throws ValidationException {

		// le contribuable doit être valide
		final ValidationResults results = validationService.validate(contribuable);
		if (results.hasErrors()) {
			throw new ValidationException(contribuable, results.getErrors(), results.getWarnings());
		}

		// [UNIREG-879] on limite la plage de création des DIs online à la période 'première période fiscale' -> 'année fiscale courante'
		final int premiereAnnee = parametres.getPremierePeriodeFiscalePersonnesPhysiques();
		final int derniereAnnee = RegDate.get().year();

		final List<PeriodeImposition> ranges = new ArrayList<>();
		for (int annee = premiereAnnee; annee <= derniereAnnee; ++annee) {
			final List<PeriodeImposition> r = calculateRangesDIsPourAnnee(contribuable, annee);
			if (r != null) {
				ranges.addAll(r);
			}
		}

		// [UNIREG-1742][UNIREG-2051] dans certain cas, les déclarations sont remplacées par une note à l'administration fiscale de l'autre canton
		// [UNIREG-1742] les diplomates suisses ne reçoivent pas de déclaration
		// [SIFISC-8094] si la déclaration a été jugée optionnelle, il faut pouvoir l'émettre manuellement
		CollectionUtils.filter(ranges, new Predicate<PeriodeImposition>() {
			@Override
			public boolean evaluate(PeriodeImposition periode) {
				return (!periode.isDeclarationRemplaceeParNote() && !periode.isDiplomateSuisseSansImmeuble()) || periode.isDeclarationOptionnelle();
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
	 * @param contribuable le contribuable concerné
	 * @param annee        l'année concernée
	 * @return une liste de ranges (dans 95% des cas, un seul range), ou <b>null</b> s'il n'y a pas de déclarations à envoyer
	 * @throws ValidationException
	 */
	private List<PeriodeImposition> calculateRangesDIsPourAnnee(final ContribuableImpositionPersonnesPhysiques contribuable, int annee) throws ValidationException {

		// on calcul les périodes d'imposition du contribuable
		final List<PeriodeImposition> periodes;
		try {
			periodes = periodeImpositionService.determine(contribuable, annee);
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
		final List<PeriodeImposition> periodesNonAssociees = new ArrayList<>();
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
				periodesNonAssociees.add((PeriodeImpositionPersonnesPhysiques) a);
			}
		}

		return periodesNonAssociees;
	}

	@Override
	public PeriodeImpositionPersonnesPhysiques checkRangeDi(ContribuableImpositionPersonnesPhysiques contribuable, DateRange range) throws ValidationException {

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
			throw new ValidationException(contribuable,
					"Le contribuable n'est pas assujetti du tout sur l'année " + annee + ", ou son assujettissement pour cette année est déjà couvert par les déclarations émises");
		}

		// on vérifie que la range spécifié correspond parfaitement avec l'assujettissement calculé

		PeriodeImposition elu = null;
		for (PeriodeImposition a : ranges) {
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
					+ " ne corresponds pas à la période d'imposition théorique " + DateRangeHelper.toString(elu) + '.');
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

		return (PeriodeImpositionPersonnesPhysiques) elu;
	}
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocalDuplicataDI(Long id, TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes, boolean saveModele) throws DeclarationException {
		final DeclarationImpotOrdinairePP declaration = (DeclarationImpotOrdinairePP) diDAO.get(id);

		if (tiersService.getOfficeImpotId(declaration.getTiers()) == null) {
			throw new DeclarationException("Le contribuable ne possède pas de for de gestion");
		}

		final PeriodeFiscale periode = periodeFiscaleDAO.getPeriodeFiscaleByYear(declaration.getPeriode().getAnnee());
		final ModeleDocument modele = modeleDocumentDAO.getModelePourDeclarationImpotOrdinaire(periode, typeDocument);
		//SIFISC-8176 On ne sauve le nouveau type de document sur la DI qu'à la demande explicite de l'utilisateur
		if (saveModele) {

			declaration.setModeleDocument(modele);

		}


		 String messageInfoImpression = String.format("Impression (%s/%s) d'un duplicata de DI (%s) pour le contribuable %d et la période [%s ; %s]",
				AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOIDSigle(), modele.getTypeDocument(), declaration.getTiers().getNumero(),
				RegDateHelper.dateToDashString(declaration.getDateDebut()), RegDateHelper.dateToDashString(declaration.getDateFin()));

		if (saveModele) {
			messageInfoImpression = String.format("%s. Sauvegarde du nouveau type de document sur la DI", messageInfoImpression);
		}
		Audit.info(messageInfoImpression);

		return diService.envoiDuplicataDIOnline(declaration, typeDocument, annexes);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocaleDI(Long ctbId, @Nullable Long id, RegDate dateDebut, RegDate dateFin, TypeDocument typeDocument,
	                                                 TypeAdresseRetour adresseRetour, RegDate delaiAccorde, @Nullable RegDate dateRetour) throws Exception {
		final Tiers tiers = tiersDAO.get(ctbId);
		if (tiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.contribuable.inexistant", null, WebContextUtils.getDefaultLocale()));
		}
		if (!(tiers instanceof ContribuableImpositionPersonnesPhysiques)) {
			throw new DeclarationException("Le tiers n'est pas soumis au régime des personnes physiques");
		}

		final ContribuableImpositionPersonnesPhysiques ctb = (ContribuableImpositionPersonnesPhysiques) tiers;
		if (tiersService.getOfficeImpotId(ctb) == null) {
			throw new DeclarationException("Le contribuable ne possède pas de for de gestion");
		}

		final DeclarationImpotOrdinairePP di = creerNouvelleDI(ctb, dateDebut, dateFin, typeDocument, adresseRetour, delaiAccorde, dateRetour);
		//Envoi du flux xml à l'éditique + envoi d'un événement fiscal
		return diService.envoiDIOnline(di, RegDate.get());
	}

	@Override
	public DeclarationImpotOrdinaire quittancerDI(long id, TypeDocument typeDocument, RegDate dateRetour) {

		final DeclarationImpotOrdinaire di = diDAO.get(id);

		// UNIREG-1437 : on peut aussi changer le type de document
		if (di.getTypeDeclaration() != typeDocument) {

			// les types qui peuvent revenir de la view sont COMPLETE_LOCAL et VAUDTAX
			// on ne va pas remplacer un COMPLETE_BATCH par un COMPLETE_LOCAL, cela ne sert à rien
			if (di.getTypeDeclaration() != TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH || typeDocument != TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL) {
				assigneModeleDocument(di, typeDocument);
			}
		}

		// quittancement de la DI avec envoi d'événement fiscal
		diService.quittancementDI((Contribuable) di.getTiers(), di, dateRetour, EtatDeclarationRetournee.SOURCE_WEB, true);

		// Envoi du message de quittance au BAM
		sendQuittancementToBam(di, dateRetour);

		return di;
	}

	protected DeclarationImpotOrdinairePP creerNouvelleDI(ContribuableImpositionPersonnesPhysiques ctb, RegDate dateDebut, RegDate dateFin, TypeDocument typeDocument, TypeAdresseRetour typeAdresseRetour,
	                                                    RegDate delaiAccorde, @Nullable RegDate dateRetour) throws AssujettissementException {
		DeclarationImpotOrdinairePP di = new DeclarationImpotOrdinairePP();

		// [SIFISC-1227] ce numéro de séquence ne doit pas être assigné, ainsi il sera re-calculé dans la méthode {@link TiersService#addAndSave(Tiers, Declaration)}
		// di.setNumero(1);

		di.setDateDebut(dateDebut);
		di.setDateFin(dateFin);

		final TypeContribuable typeContribuable;
		final boolean diLibre;
		final List<PeriodeImposition> periodesImposition = periodeImpositionService.determine(ctb, dateDebut.year());
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

		// [SIFISC-4923] il faut prendre le dernier for de gestion connu (car il arrive qu'il n'y en ait plus de connu
		// à la date précise demandée, par exemple dans le cas du ctb HC qui vend son dernier immeuble dans l'année).
		final ForGestion forGestion = tiersService.getDernierForGestionConnu(ctb, di.getDateFin());
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
		assigneModeleDocument(di, typeDocument);

		final ch.vd.uniregctb.tiers.CollectiviteAdministrative collectiviteAdministrative;
		if (typeAdresseRetour == TypeAdresseRetour.ACI) {
			collectiviteAdministrative = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noACI);
		}
		else if (typeAdresseRetour == TypeAdresseRetour.CEDI) {
			collectiviteAdministrative = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
		}
		else {
			final Integer officeImpot = tiersService.getOfficeImpotId(ctb);
			if (officeImpot == null) {
				throw new ActionException("le contribuable ne possède pas de for de gestion");
			}
			collectiviteAdministrative = tiersService.getCollectiviteAdministrative(officeImpot);
		}
		di.setRetourCollectiviteAdministrativeId(collectiviteAdministrative.getId());

		final Integer codeSegment = PeriodeImpositionHelper.determineCodeSegment(ctb, di.getDateFin().year());
		if (codeSegment == null && periode.getAnnee() >= DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE) {
			di.setCodeSegment(DeclarationImpotService.VALEUR_DEFAUT_CODE_SEGMENT);
		}
		else {
			di.setCodeSegment(codeSegment);
		}


		final EtatDeclaration emission = new EtatDeclarationEmise(RegDate.get());
		di.addEtat(emission);

		// [UNIREG-2705] Création d'une DI déjà retournée
		if (dateRetour != null) {
			if (!RegDateHelper.isAfterOrEqual(dateRetour, RegDate.get(), NullDateBehavior.LATEST)) {
				throw new ActionException("La date de retour d'une DI émise aujourd'hui ne peut pas être dans le passé");
			}

			diService.quittancementDI(ctb, di, dateRetour, EtatDeclarationRetournee.SOURCE_WEB, false);
		}

		final DelaiDeclaration delai = new DelaiDeclaration();
		delai.setDelaiAccordeAu(delaiAccorde);
		delai.setDateDemande(RegDate.get());
		delai.setDateTraitement(RegDate.get());
		di.addDelai(delai);

		// persistence du lien entre le contribuable et la nouvelle DI
		di = (DeclarationImpotOrdinairePP) tiersDAO.addAndSave(ctb, di);

		//Mise à jour de l'état de la tâche si il y en a une
		final TacheCriteria criterion = new TacheCriteria();
		criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpotPP);
		criterion.setAnnee(dateDebut.year());
		criterion.setEtatTache(TypeEtatTache.EN_INSTANCE);
		criterion.setContribuable(ctb);
		final List<Tache> taches = tacheDAO.find(criterion);
		if (taches != null && !taches.isEmpty()) {
			for (Tache t : taches) {
				final TacheEnvoiDeclarationImpotPP tache = (TacheEnvoiDeclarationImpotPP) t;
				if (tache.getDateDebut().equals(di.getDateDebut()) && tache.getDateFin().equals(di.getDateFin())) {
					tache.setEtat(TypeEtatTache.TRAITE);
				}
			}
		}
		return di;
	}

	private void sendQuittancementToBam(DeclarationImpotOrdinaire di, RegDate dateQuittancement) {
		final long ctbId = di.getTiers().getNumero();
		final int annee = di.getPeriode().getAnnee();
		final int noSequence = di.getNumero();
		try {
			final Map<String, String> bamHeaders = BamMessageHelper.buildCustomBamHeadersForQuittancementDeclaration(di, dateQuittancement, null);
			final String businessId = String.format("%d-%d-%d-%s", ctbId, annee, noSequence, new SimpleDateFormat("yyyyMMddHHmmssSSS").format(DateHelper.getCurrentDate()));
			final String processDefinitionId = BamMessageHelper.PROCESS_DEFINITION_ID_PAPIER;       // nous allons assimiler les quittancements IHM à des quittancements "papier"
			final String processInstanceId = BamMessageHelper.buildProcessInstanceId(di);
			bamMessageSender.sendBamMessageQuittancementDi(processDefinitionId, processInstanceId, businessId, ctbId, annee, bamHeaders);
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(String.format("Erreur à la notification au BAM du quittancement de la DI %d (%d) du contribuable %d", annee, noSequence, ctbId), e);
		}
	}

	/**
	 * Assigne le modèle de document à la DI en fonction du type de document trouvé dans la view
	 *
	 * @param di         DI à laquelle le modèle de document sera assigné
	 * @param typeDeclarationImpot le type de déclaration souhaitée
	 */
	private void assigneModeleDocument(DeclarationImpotOrdinaire di, TypeDocument typeDeclarationImpot) {
		final PeriodeFiscale periode = di.getPeriode();
		final ModeleDocument modeleDocument = modeleDocumentDAO.getModelePourDeclarationImpotOrdinaire(periode, typeDeclarationImpot);
		if (modeleDocument == null) {
			throw new ActionException(String.format("Le modèle de document %s pour l'année %d n'existe pas.", typeDeclarationImpot, periode.getAnnee()));
		}
		di.setModeleDocument(modeleDocument);
	}

	/**
	 * Persiste en base le delai
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public Long saveDelai(Long idDeclaration, RegDate dateDemande, RegDate delaiAccordeAu, boolean confirmationEcrite) {
		final DeclarationImpotOrdinaire di = diDAO.get(idDeclaration);
		DelaiDeclaration delai = new DelaiDeclaration();
		delai.setDateTraitement(RegDate.get());
		delai.setConfirmationEcrite(confirmationEcrite);
		delai.setDateDemande(dateDemande);
		delai.setDelaiAccordeAu(delaiAccordeAu);
		delai = diService.addAndSave(di, delai);
		return delai.getId();
	}

	/**
	 * Sommer une Declaration Impot
	 *
	 * @throws EditiqueException
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocalSommationDI(Long id) throws EditiqueException {

		final RegDate dateDuJour = RegDate.get();
		final DeclarationImpotOrdinairePP di = (DeclarationImpotOrdinairePP) diDAO.get(id);
		final EtatDeclarationSommee etat = new EtatDeclarationSommee(dateDuJour, dateDuJour);
		di.addEtat(etat);
		diDAO.save(di);

		try {
			final EditiqueResultat resultat = editiqueCompositionService.imprimeSommationDIOnline(di, dateDuJour);
			evenementFiscalService.publierEvenementFiscalSommationDeclarationImpot(di, etat.getDateObtention());
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
	public EditiqueResultat envoieImpressionLocalConfirmationDelai(Long idDI, Long idDelai) throws EditiqueException {
		try {
			final DelaiDeclaration delai = delaiDeclarationDAO.get(idDelai);
			final DeclarationImpotOrdinairePP di = (DeclarationImpotOrdinairePP) diDAO.get(idDI);
			return editiqueCompositionService.imprimeConfirmationDelaiOnline(di, delai);
		}
		catch (JMSException e) {
			throw new EditiqueException(e);
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDiDAO(DeclarationImpotOrdinaireDAO diDAO) {
		this.diDAO = diDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDiService(DeclarationImpotService diService) {
		this.diService = diService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setModeleDocumentDAO(ModeleDocumentDAO modeleDocumentDAO) {
		this.modeleDocumentDAO = modeleDocumentDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEditiqueCompositionService(EditiqueCompositionService editiqueCompositionService) {
		this.editiqueCompositionService = editiqueCompositionService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setParametres(ParametreAppService parametres) {
		this.parametres = parametres;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDelaiDeclarationDAO(DelaiDeclarationDAO delaiDeclarationDAO) {
		this.delaiDeclarationDAO = delaiDeclarationDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setBamMessageSender(BamMessageSender bamMessageSender) {
		this.bamMessageSender = bamMessageSender;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPeriodeImpositionService(PeriodeImpositionService periodeImpositionService) {
		this.periodeImpositionService = periodeImpositionService;
	}
}
