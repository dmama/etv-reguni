package ch.vd.unireg.di.manager;

import javax.jms.JMSException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
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
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.ActionException;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.DelaiDeclarationDAO;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationEmise;
import ch.vd.unireg.declaration.EtatDeclarationRetournee;
import ch.vd.unireg.declaration.EtatDeclarationSommee;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.ModeleDocumentDAO;
import ch.vd.unireg.declaration.ParametrePeriodeFiscaleEmolument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.editique.EditiqueCompositionService;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.ModeleFeuilleDocumentEditique;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.jms.BamMessageHelper;
import ch.vd.unireg.jms.BamMessageSender;
import ch.vd.unireg.metier.assujettissement.AssujettissementException;
import ch.vd.unireg.metier.assujettissement.PeriodeImposition;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionHelper;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionPersonnesMorales;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionPersonnesPhysiques;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.metier.bouclement.ExerciceCommercial;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForGestion;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheCriteria;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.TypeAdresseRetour;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeDocumentEmolument;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;
import ch.vd.unireg.utils.WebContextUtils;
import ch.vd.unireg.validation.ValidationService;

/**
 * Service offrant des methodes pour gérer le controller DeclarationImpotEditController
 *
 * @author xcifde
 */
public class DeclarationImpotEditManagerImpl implements DeclarationImpotEditManager, MessageSourceAware, InitializingBean {

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
	private DelaiDeclarationDAO delaiDocumentFiscalDAO;
	private ValidationService validationService;
	private ParametreAppService parametres;
	private BamMessageSender bamMessageSender;
	private PeriodeImpositionService periodeImpositionService;

	/**
	 * Interface pour l'implémentation spécifique à l'impression d'une DI PP ou PM
	 */
	private interface DeclarationPrinter {
		EditiqueResultat imprimeNouvelleDeclaration(Tiers tiers, RegDate dateDebut, RegDate dateFin, TypeDocument typeDocument,
		                                            TypeAdresseRetour adresseRetour, RegDate delaiAccorde, @Nullable RegDate dateRetour) throws DeclarationException, AssujettissementException;
		EditiqueResultat imprimeDuplicata(DeclarationImpotOrdinaire di, TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes) throws DeclarationException;
	}

	/**
	 * Interface pour les implémentations spécifique de génération d'une nouvelle déclaration PP ou PM (= uniquement la génération en base, on ne s'occupe pas de l'impression ici)
	 */
	private interface DeclarationImpotGenerator {
		DeclarationImpotOrdinaire creerDeclaration(Tiers tiers, RegDate dateDebut, RegDate dateFin,
		                                           @Nullable TypeDocument typeDocument, @Nullable TypeAdresseRetour typeAdresseRetour,
		                                           RegDate delaiAccorde, @Nullable RegDate dateRetour) throws DeclarationException, AssujettissementException;
	}

	/**
	 * Interface pour les implémentations spécifiques de génération d'un document de sommation pour les DI PP ou PM
	 */
	private interface DeclarationSummoner<T extends Declaration> {
		@Nullable
		Integer getMontantEmolument(T declaration);

		EditiqueResultat imprimeSommation(T declaration, RegDate date, @Nullable Integer emolument) throws DeclarationException, EditiqueException, JMSException;
	}

	private Map<TypeDocument, DeclarationPrinter> printers;
	private Map<Class<? extends Tiers>, DeclarationImpotGenerator> diGenerators;
	private Map<Class<? extends DeclarationImpotOrdinaire>, DeclarationSummoner<?>> summoners;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<PeriodeImposition> calculateRangesProchainesDIs(Long numero) throws ValidationException {

		// on charge le tiers
		final Contribuable contribuable = (Contribuable) tiersDAO.get(numero);
		if (contribuable == null) {
			throw new TiersNotFoundException(numero);
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
		final int premiereAnnee;
		if (contribuable instanceof ContribuableImpositionPersonnesPhysiques) {
			premiereAnnee = parametres.getPremierePeriodeFiscalePersonnesPhysiques();
		}
		else if (contribuable instanceof ContribuableImpositionPersonnesMorales) {
			premiereAnnee = parametres.getPremierePeriodeFiscalePersonnesMorales();
		}
		else {
			return Collections.emptyList();
		}
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
	private List<PeriodeImposition> calculateRangesDIsPourAnnee(final Contribuable contribuable, int annee) throws ValidationException {

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

		final List<DeclarationImpotOrdinaire> declarations = contribuable.getDeclarationsTriees(DeclarationImpotOrdinaire.class, false);

		// On ne retourne que les périodes qui ne sont pas déjà associées avec une déclaration
		final List<PeriodeImposition> periodesNonAssociees = new ArrayList<>();
		for (PeriodeImposition a : periodes) {
			boolean match = false;
			for (DeclarationImpotOrdinaire d : declarations) {
				if (DateRangeHelper.intersect(d, a)) {
					match = true;
					break;
				}
			}
			if (!match) {
				periodesNonAssociees.add(a);
			}
		}

		return periodesNonAssociees;
	}

	@Override
	public PeriodeImposition checkRangeDi(Contribuable contribuable, DateRange range) throws ValidationException {
		if (contribuable instanceof ContribuableImpositionPersonnesPhysiques) {
			return checkRangeDi((ContribuableImpositionPersonnesPhysiques) contribuable, range);
		}
		else if (contribuable instanceof ContribuableImpositionPersonnesMorales) {
			return checkRangeDi((ContribuableImpositionPersonnesMorales) contribuable, range);
		}
		else {
			throw new ValidationException(contribuable, "Aucune période d'imposition supportée pour ce contribuable.");
		}
	}

	@Override
	public PeriodeImpositionPersonnesPhysiques checkRangeDi(ContribuableImpositionPersonnesPhysiques contribuable, DateRange range) throws ValidationException {
		if (range.getDateDebut().year() != range.getDateFin().year()) {
			throw new ValidationException(contribuable, "La déclaration doit tenir dans une année complète.");
		}
		return checkRangeDi(contribuable, PeriodeImpositionPersonnesPhysiques.class, range);
	}

	@Override
	public PeriodeImpositionPersonnesMorales checkRangeDi(ContribuableImpositionPersonnesMorales contribuable, DateRange range) throws ValidationException {
		return checkRangeDi(contribuable, PeriodeImpositionPersonnesMorales.class, range);
	}

	private <P extends PeriodeImposition> P checkRangeDi(Contribuable contribuable, Class<P> clazz, DateRange range) throws ValidationException {
		// le contribuable doit être valide
		final ValidationResults results = validationService.validate(contribuable);
		if (results.hasErrors()) {
			throw new ValidationException(contribuable, results.getErrors(), results.getWarnings());
		}

		final int annee = range.getDateFin().year();
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

		DeclarationImpotOrdinaire declaration = null;
		final List<DeclarationImpotOrdinaire> declarations = contribuable.getDeclarationsTriees(DeclarationImpotOrdinaire.class, false);
		for (DeclarationImpotOrdinaire d : declarations) {
			if (DateRangeHelper.intersect(d, range)) {
				declaration = d;
				break;
			}
		}

		if (declaration != null) {
			throw new ValidationException(contribuable, "Le contribuable possède déjà une déclaration sur la période spécifiée [" + range + "].");
		}
		if (!clazz.isInstance(elu)) {
			throw new IllegalArgumentException("Mauvais type de période d'imposition calculée sur le contribuable " + contribuable.getNumero() + " : " + elu.getClass().getSimpleName() + " (" + clazz.getSimpleName() + " attendu)");
		}
		return clazz.cast(elu);
	}

	@NotNull
	private DeclarationPrinter getPrinter(TypeDocument typeDocument) throws DeclarationException {
		final DeclarationPrinter printer = printers.get(typeDocument);
		if (printer == null) {
			throw new DeclarationException("Impression d'une déclaration de type " + typeDocument + " non-supportée.");
		}
		return printer;
	}

	@NotNull
	private DeclarationImpotGenerator getDeclarationImpotGenerator(@NotNull Tiers tiers) throws DeclarationException {
		final DeclarationImpotGenerator generator = diGenerators.get(tiers.getClass());
		if (generator == null) {
			throw new DeclarationException("Génération d'une déclaration d'impôt pour un tiers de type " + tiers.getClass().getSimpleName() + " non-supportée.");
		}
		return generator;
	}

	@NotNull
	private <T extends DeclarationImpotOrdinaire> DeclarationSummoner<T> getDeclarationSummoner(@NotNull T declaration) throws DeclarationException {
		//noinspection unchecked
		final DeclarationSummoner<T> summoner = (DeclarationSummoner<T>) summoners.get(declaration.getClass());
		if (summoner == null) {
			throw new DeclarationException("Sommation d'une déclaration de type " + declaration.getClass().getSimpleName() + " non-supportée.");
		}
		return summoner;
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocalDuplicataDI(Long id, TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes, boolean saveModele) throws DeclarationException {

		final DeclarationImpotOrdinaire declaration = diDAO.get(id);

		final PeriodeFiscale periode = periodeFiscaleDAO.getPeriodeFiscaleByYear(declaration.getPeriode().getAnnee());
		final ModeleDocument modele = modeleDocumentDAO.getModelePourDeclarationImpotOrdinaire(periode, typeDocument);

		// SIFISC-8176 On ne sauve le nouveau type de document sur la DI qu'à la demande explicite de l'utilisateur
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

		return getPrinter(typeDocument).imprimeDuplicata(declaration, typeDocument, annexes);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocaleDI(Long ctbId, RegDate dateDebut, RegDate dateFin, TypeDocument typeDocument,
	                                                 TypeAdresseRetour adresseRetour, RegDate delaiAccorde, @Nullable RegDate dateRetour) throws Exception {
		final Tiers tiers = tiersDAO.get(ctbId);
		if (tiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.contribuable.inexistant", null, WebContextUtils.getDefaultLocale()));
		}

		return getPrinter(typeDocument).imprimeNouvelleDeclaration(tiers, dateDebut, dateFin, typeDocument, adresseRetour, delaiAccorde, dateRetour);
	}

	@Override
	public void genererDISansImpression(Long ctbId, RegDate dateDebut, RegDate dateFin, RegDate delaiAccorde, @Nullable RegDate dateRetour) throws Exception {
		final Tiers tiers = tiersDAO.get(ctbId);
		if (tiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.contribuable.inexistant", null, WebContextUtils.getDefaultLocale()));
		}

		getDeclarationImpotGenerator(tiers).creerDeclaration(tiers, dateDebut, dateFin, null, null, delaiAccorde, dateRetour);
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
		diService.quittancementDI(di.getTiers(), di, dateRetour, EtatDeclarationRetournee.SOURCE_WEB, true);

		// Envoi du message de quittance au BAM
		sendQuittancementToBam(di, dateRetour);

		return di;
	}

	private <T extends DeclarationImpotOrdinaire> T creerNouvelleDI(Contribuable ctb, T di, RegDate dateDebut, RegDate dateFin,
	                                                                @Nullable TypeDocument typeDocument, @Nullable TypeAdresseRetour typeAdresseRetour,
	                                                                RegDate delaiAccorde, @Nullable RegDate dateRetour, TypeTache typeTacheEnvoi) throws AssujettissementException {

		// [SIFISC-1227] le numéro de séquence ne doit pas être assigné, ainsi il sera re-calculé dans la méthode {@link TiersService#addAndSave(Tiers, Declaration)}
		// di.setNumero(1);

		di.setDateDebut(dateDebut);
		di.setDateFin(dateFin);

		final TypeContribuable typeContribuable;
		final boolean diLibre;
		final List<PeriodeImposition> periodesImposition = periodeImpositionService.determine(ctb, dateFin.year());
		if (periodesImposition != null) {
			final PeriodeImposition dernierePeriode = periodesImposition.get(periodesImposition.size() - 1);
			typeContribuable = dernierePeriode.getTypeContribuable();

			// une DI est libre si je ne trouve aucune période d'assujettissement qui colle avec les dates de la DI elle-même
			// (il faut quand-même accessoirement qu'elle soit créée sur la période fiscale courante)
			if (dateFin.year() == RegDate.get().year()) {
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

		final PeriodeFiscale periode = periodeFiscaleDAO.getPeriodeFiscaleByYear(dateFin.year());
		if (periode == null) {
			throw new ActionException("la période fiscale pour l'année " + dateFin.year() + " n'existe pas.");
		}
		di.setPeriode(periode);

		// assigne le modèle de document à la DI en fonction de ce que contient la vue
		if (typeDocument != null) {
			assigneModeleDocument(di, typeDocument);
		}

		final ch.vd.unireg.tiers.CollectiviteAdministrative collectiviteAdministrative;
		if (typeAdresseRetour == TypeAdresseRetour.ACI) {
			collectiviteAdministrative = tiersService.getOfficeImpot(ServiceInfrastructureService.noACI);
		}
		else if (typeAdresseRetour == TypeAdresseRetour.CEDI) {
			collectiviteAdministrative = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noCEDI, true);
		}
		else {
			final Integer officeImpot = tiersService.getOfficeImpotId(ctb);
			if (officeImpot == null) {
				throw new ActionException("le contribuable ne possède pas de for de gestion");
			}
			collectiviteAdministrative = tiersService.getOfficeImpot(officeImpot);
		}
		di.setRetourCollectiviteAdministrativeId(collectiviteAdministrative.getId());

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
		delai.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		delai.setDelaiAccordeAu(delaiAccorde);
		delai.setDateDemande(RegDate.get());
		delai.setDateTraitement(RegDate.get());
		di.addDelai(delai);
		di.setDelaiRetourImprime(delaiAccorde);     // pour les DI envoyées manuellement, il y a égalité des délais imprimés et effectifs

		// persistence du lien entre le contribuable et la nouvelle DI
		di = tiersDAO.addAndSave(ctb, di);

		//Mise à jour de l'état de la tâche si il y en a une
		final TacheCriteria criterion = new TacheCriteria();
		criterion.setTypeTache(typeTacheEnvoi);
		criterion.setAnnee(dateDebut.year());
		criterion.setEtatTache(TypeEtatTache.EN_INSTANCE);
		criterion.setContribuable(ctb);
		final List<Tache> taches = tacheDAO.find(criterion);
		if (taches != null && !taches.isEmpty()) {
			for (Tache t : taches) {
				final TacheEnvoiDeclarationImpot tache = (TacheEnvoiDeclarationImpot) t;
				if (tache.getDateDebut().equals(di.getDateDebut()) && tache.getDateFin().equals(di.getDateFin())) {
					tache.setEtat(TypeEtatTache.TRAITE);
				}
			}
		}
		return di;
	}

	protected DeclarationImpotOrdinairePM creerNouvelleDI(ContribuableImpositionPersonnesMorales ctb, RegDate dateDebut, RegDate dateFin,
	                                                      RegDate dateDebutExercice, RegDate dateFinExercice,
	                                                      @Nullable TypeDocument typeDocument, @Nullable TypeAdresseRetour typeAdresseRetour,
	                                                      RegDate delaiAccorde, @Nullable RegDate dateRetour) throws AssujettissementException, DeclarationException {

		final DeclarationImpotOrdinairePM di = new DeclarationImpotOrdinairePM();
		di.setDateDebutExerciceCommercial(dateDebutExercice);
		di.setDateFinExerciceCommercial(dateFinExercice);

		// assignation du code segment
		if (ctb instanceof Entreprise && typeDocument != null) {
			di.setCodeSegment(diService.computeCodeSegment((Entreprise) ctb, dateFin, typeDocument));
		}

		return creerNouvelleDI(ctb, di, dateDebut, dateFin, typeDocument, typeAdresseRetour, delaiAccorde, dateRetour, TypeTache.TacheEnvoiDeclarationImpotPM);
	}

	protected DeclarationImpotOrdinairePP creerNouvelleDI(ContribuableImpositionPersonnesPhysiques ctb, RegDate dateDebut, RegDate dateFin, @Nullable TypeDocument typeDocument, @Nullable TypeAdresseRetour typeAdresseRetour,
	                                                    RegDate delaiAccorde, @Nullable RegDate dateRetour) throws AssujettissementException {

		final DeclarationImpotOrdinairePP di = new DeclarationImpotOrdinairePP();

		// [SIFISC-4923] il faut prendre le dernier for de gestion connu (car il arrive qu'il n'y en ait plus de connu
		// à la date précise demandée, par exemple dans le cas du ctb HC qui vend son dernier immeuble dans l'année).
		final ForGestion forGestion = tiersService.getDernierForGestionConnu(ctb, dateFin);
		if (forGestion != null) {
			di.setNumeroOfsForGestion(forGestion.getNoOfsCommune());
		}
		else {
			throw new ActionException("le contribuable ne possède pas de for de gestion au " + dateFin);
		}

		final Integer codeSegment = PeriodeImpositionHelper.determineCodeSegment(ctb, dateFin.year());
		if (codeSegment == null && dateFin.year() >= DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE) {
			di.setCodeSegment(DeclarationImpotService.VALEUR_DEFAUT_CODE_SEGMENT);
		}
		else {
			di.setCodeSegment(codeSegment);
		}

		return creerNouvelleDI(ctb, di, dateDebut, dateFin, typeDocument, typeAdresseRetour, delaiAccorde, dateRetour, TypeTache.TacheEnvoiDeclarationImpotPP);
	}

	private void sendQuittancementToBam(DeclarationImpotOrdinaire di, RegDate dateQuittancement) {
		final long ctbId = di.getTiers().getNumero();
		final int annee = di.getPeriode().getAnnee();
		final int noSequence = di.getNumero();
		try {
			final Map<String, String> bamHeaders = BamMessageHelper.buildCustomBamHeadersForQuittancementDeclaration(di, dateQuittancement, null);
			final String businessId = String.format("%d-%d-%d-%s", ctbId, annee, noSequence, new SimpleDateFormat("yyyyMMddHHmmssSSS").format(DateHelper.getCurrentDate()));
			final String processDefinitionId = di instanceof DeclarationImpotOrdinairePM ? BamMessageHelper.PROCESS_DEFINITION_ID_PAPIER_PM : BamMessageHelper.PROCESS_DEFINITION_ID_PAPIER_PP;       // nous allons assimiler les quittancements IHM à des quittancements "papier"
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
	public Long saveNouveauDelai(Long idDeclaration, RegDate dateDemande, RegDate delaiAccordeAu, EtatDelaiDocumentFiscal etat, boolean sursis) {
		final DeclarationImpotOrdinaire di = diDAO.get(idDeclaration);
		DelaiDeclaration delai = new DelaiDeclaration();
		delai.setDateTraitement(RegDate.get());
		delai.setDateDemande(dateDemande);
		delai.setEtat(etat);
		delai.setDelaiAccordeAu(delaiAccordeAu);
		delai.setSursis(sursis);
		delai = diService.addAndSave(di, delai);
		return delai.getId();
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void saveDelai(Long idDelai, EtatDelaiDocumentFiscal etat, RegDate delaiAccordeAu) {
		final DelaiDeclaration delai = delaiDocumentFiscalDAO.get(idDelai);
		delai.setDateTraitement(RegDate.get());
		delai.setEtat(etat);
		delai.setDelaiAccordeAu(delaiAccordeAu);
	}

	/**
	 * Sommer une Declaration Impot
	 *
	 * @throws EditiqueException
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocalSommationDI(Long id) throws EditiqueException, DeclarationException {
		final DeclarationImpotOrdinaire di = diDAO.get(id);
		return envoieImpressionLocalSommationDI(di);
	}

	private <T extends DeclarationImpotOrdinaire> EditiqueResultat envoieImpressionLocalSommationDI(T di) throws EditiqueException, DeclarationException {
		final DeclarationSummoner<T> summoner = getDeclarationSummoner(di);
		final Integer emolument = summoner.getMontantEmolument(di);
		final RegDate today = RegDate.get();
		final EtatDeclarationSommee etat = new EtatDeclarationSommee(today, today, emolument);
		final EtatDeclarationSommee saved = diService.addAndSave(di, etat);

		try {
			final EditiqueResultat resultat = summoner.imprimeSommation(di, today, emolument);
			evenementFiscalService.publierEvenementFiscalSommationDeclarationImpot(di, saved.getDateObtention());
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
	public EditiqueResultat envoieImpressionLocalConfirmationDelaiPP(Long idDI, Long idDelai) throws EditiqueException {
		try {
			final DelaiDeclaration delai = delaiDocumentFiscalDAO.get(idDelai);
			final DeclarationImpotOrdinairePP di = (DeclarationImpotOrdinairePP) diDAO.get(idDI);
			final Pair<EditiqueResultat, String> resultat = editiqueCompositionService.imprimeConfirmationDelaiOnline(di, delai);
			delai.setCleArchivageCourrier(resultat.getRight());
			return resultat.getLeft();
		}
		catch (JMSException e) {
			throw new EditiqueException(e);
		}
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocaleLettreDecisionDelaiPM(Long idDelai) throws EditiqueException {
		try {
			final DelaiDeclaration delai = delaiDocumentFiscalDAO.get(idDelai);
			final DeclarationImpotOrdinairePM di = (DeclarationImpotOrdinairePM) delai.getDeclaration();
			final Pair<EditiqueResultat, String> resultat;
			switch (delai.getEtat()) {
			case ACCORDE:
			case REFUSE:
				resultat = editiqueCompositionService.imprimeLettreDecisionDelaiOnline(di, delai);
				break;
			default:
				throw new EditiqueException("Impossible d'imprimer un courrier pour un délai qui n'est ni accordé, ni refusé.");
			}
			delai.setCleArchivageCourrier(resultat.getRight());
			return resultat.getLeft();
		}
		catch (JMSException e) {
			throw new EditiqueException(e);
		}
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void envoieImpressionBatchLettreDecisionDelaiPM(Long idDelai) throws EditiqueException {
		try {
			final DelaiDeclaration delai = delaiDocumentFiscalDAO.get(idDelai);
			final DeclarationImpotOrdinairePM di = (DeclarationImpotOrdinairePM) delai.getDeclaration();
			final String cle;
			switch (delai.getEtat()) {
			case ACCORDE:
			case REFUSE:
				cle = editiqueCompositionService.imprimeLettreDecisionDelaiForBatch(di, delai);
				break;
			default:
				throw new EditiqueException("Impossible d'imprimer un courrier pour un délai qui n'est ni accordé, ni refusé.");
			}
			delai.setCleArchivageCourrier(cle);
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
	public void setDelaiDocumentFiscalDAO(DelaiDeclarationDAO delaiDocumentFiscalDAO) {
		this.delaiDocumentFiscalDAO = delaiDocumentFiscalDAO;
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

	/**
	 * Génération d'une déclaration d'impôt PP
	 */
	private class DeclarationImpotPersonnesPhysiquesGenerator implements DeclarationImpotGenerator {
		@Override
		public DeclarationImpotOrdinairePP creerDeclaration(Tiers tiers, RegDate dateDebut, RegDate dateFin,
		                                                    @Nullable TypeDocument typeDocument, @Nullable TypeAdresseRetour adresseRetour,
		                                                    RegDate delaiAccorde, @Nullable RegDate dateRetour) throws DeclarationException, AssujettissementException {

			if (!(tiers instanceof ContribuableImpositionPersonnesPhysiques)) {
				throw new DeclarationException("Le tiers n'est pas soumis au régime des personnes physiques");
			}

			return creerNouvelleDI((ContribuableImpositionPersonnesPhysiques) tiers, dateDebut, dateFin, typeDocument, adresseRetour, delaiAccorde, dateRetour);
		}
	}

	/**
	 * Génération d'une déclaration d'impôt PM
	 */
	private class DeclarationImpotPersonnesMoralesGenerator implements DeclarationImpotGenerator {
		@Override
		public DeclarationImpotOrdinairePM creerDeclaration(Tiers tiers, RegDate dateDebut, RegDate dateFin,
		                                                    @Nullable TypeDocument typeDocument, @Nullable TypeAdresseRetour adresseRetour,
		                                                    RegDate delaiAccorde, @Nullable RegDate dateRetour) throws DeclarationException, AssujettissementException {

			if (!(tiers instanceof ContribuableImpositionPersonnesMorales)) {
				throw new DeclarationException("Le tiers n'est pas soumis au régime des personnes morales");
			}

			final List<ExerciceCommercial> exercices = tiersService.getExercicesCommerciaux((Entreprise) tiers);
			final ExerciceCommercial exercice = DateRangeHelper.rangeAt(exercices, dateFin);
			final RegDate dateDebutExercice = exercice != null ? exercice.getDateDebut() : null;
			final RegDate dateFinExercice = exercice != null ? exercice.getDateFin() : null;
			return creerNouvelleDI((ContribuableImpositionPersonnesMorales) tiers, dateDebut, dateFin, dateDebutExercice, dateFinExercice, typeDocument, adresseRetour, delaiAccorde, dateRetour);
		}
	}

	/**
	 * Impression des déclarations d'impôt PP
	 */
	private class DeclarationImpotPersonnesPhysiquesPrinter implements DeclarationPrinter {

		private final DeclarationImpotPersonnesPhysiquesGenerator diGenerator;

		public DeclarationImpotPersonnesPhysiquesPrinter(DeclarationImpotPersonnesPhysiquesGenerator diGenerator) {
			this.diGenerator = diGenerator;
		}

		@Override
		public EditiqueResultat imprimeNouvelleDeclaration(Tiers tiers, RegDate dateDebut, RegDate dateFin, TypeDocument typeDocument, TypeAdresseRetour adresseRetour, RegDate delaiAccorde, @Nullable RegDate dateRetour) throws DeclarationException, AssujettissementException {
			if (tiersService.getOfficeImpotId(tiers) == null) {
				throw new DeclarationException("Le contribuable ne possède pas de for de gestion");
			}

			final DeclarationImpotOrdinairePP di = diGenerator.creerDeclaration(tiers, dateDebut, dateFin, typeDocument, adresseRetour, delaiAccorde, dateRetour);
			//Envoi du flux xml à l'éditique + envoi d'un événement fiscal
			return diService.envoiDIOnline(di, RegDate.get());
		}

		@Override
		public EditiqueResultat imprimeDuplicata(DeclarationImpotOrdinaire declaration, TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes) throws DeclarationException {
			if (!(declaration instanceof DeclarationImpotOrdinairePP)) {
				throw new DeclarationException("La déclaration n'est pas une déclaration associée aux personnes physiques");
			}

			if (tiersService.getOfficeImpotId(declaration.getTiers()) == null) {
				throw new DeclarationException("Le contribuable ne possède pas de for de gestion");
			}

			return diService.envoiDuplicataDIOnline((DeclarationImpotOrdinairePP) declaration, typeDocument, annexes);
		}
	}

	/**
	 * Impression des déclarations d'impôt PM
	 */
	private class DeclarationImpotPersonnesMoralesPrinter implements DeclarationPrinter {

		private final DeclarationImpotPersonnesMoralesGenerator diGenerator;

		public DeclarationImpotPersonnesMoralesPrinter(DeclarationImpotPersonnesMoralesGenerator diGenerator) {
			this.diGenerator = diGenerator;
		}

		@Override
		public EditiqueResultat imprimeNouvelleDeclaration(Tiers tiers, RegDate dateDebut, RegDate dateFin, TypeDocument typeDocument, TypeAdresseRetour adresseRetour, RegDate delaiAccorde, @Nullable RegDate dateRetour) throws DeclarationException, AssujettissementException {
			final DeclarationImpotOrdinairePM di = diGenerator.creerDeclaration(tiers, dateDebut, dateFin, typeDocument, adresseRetour, delaiAccorde, dateRetour);
			//Envoi du flux xml à l'éditique + envoi d'un événement fiscal
			return diService.envoiDIOnline(di, RegDate.get());
		}

		@Override
		public EditiqueResultat imprimeDuplicata(DeclarationImpotOrdinaire declaration, TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes) throws DeclarationException {
			if (!(declaration instanceof DeclarationImpotOrdinairePM)) {
				throw new DeclarationException("La déclaration n'est pas une déclaration associée aux personnes morales");
			}
			return diService.envoiDuplicataDIOnline((DeclarationImpotOrdinairePM) declaration, annexes);
		}
	}

	/**
	 * Sommation des déclarations d'impôt PP
	 */
	private class DeclarationImpotPersonnesPhysiquesSummoner implements DeclarationSummoner<DeclarationImpotOrdinairePP> {
		@Nullable
		@Override
		public Integer getMontantEmolument(DeclarationImpotOrdinairePP declaration) {
			final ParametrePeriodeFiscaleEmolument paramEmolument = declaration.getPeriode().getParametrePeriodeFiscaleEmolument(TypeDocumentEmolument.SOMMATION_DI_PP);
			return paramEmolument != null ? paramEmolument.getMontant() : null;
		}

		@Override
		public EditiqueResultat imprimeSommation(DeclarationImpotOrdinairePP declaration, RegDate date, @Nullable Integer emolument) throws DeclarationException, EditiqueException, JMSException {
			return editiqueCompositionService.imprimeSommationDIOnline(declaration, date, emolument);
		}
	}

	/**
	 * Sommation des déclaration d'impôt PM
	 */
	private class DeclarationImpotPersonnesMoralesSummoner implements DeclarationSummoner<DeclarationImpotOrdinairePM> {
		@Nullable
		@Override
		public Integer getMontantEmolument(DeclarationImpotOrdinairePM declaration) {
			// pas d'émolument pour les sommations de DI PM
			return null;
		}

		@Override
		public EditiqueResultat imprimeSommation(DeclarationImpotOrdinairePM declaration, RegDate date, @Nullable Integer emolument) throws DeclarationException, EditiqueException, JMSException {
			if (emolument != null) {    // si ça pête, c'est qu'on a oublié de mettre en cohérence le résultat de la méthode getMontantEmolument avec celle-ci...
				throw new IllegalArgumentException();
			}
			return editiqueCompositionService.imprimeSommationDIOnline(declaration, date, date);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		final DeclarationImpotPersonnesPhysiquesGenerator ppGenerator = new DeclarationImpotPersonnesPhysiquesGenerator();
		final DeclarationImpotPersonnesMoralesGenerator pmGenerator = new DeclarationImpotPersonnesMoralesGenerator();

		final DeclarationPrinter ppPrinter = new DeclarationImpotPersonnesPhysiquesPrinter(ppGenerator);
		final DeclarationPrinter pmPrinter = new DeclarationImpotPersonnesMoralesPrinter(pmGenerator);

		this.printers = new EnumMap<>(TypeDocument.class);
		this.printers.put(TypeDocument.DECLARATION_IMPOT_APM_BATCH, pmPrinter);
		this.printers.put(TypeDocument.DECLARATION_IMPOT_APM_LOCAL, pmPrinter);
		this.printers.put(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ppPrinter);
		this.printers.put(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, ppPrinter);
		this.printers.put(TypeDocument.DECLARATION_IMPOT_DEPENSE, ppPrinter);
		this.printers.put(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, ppPrinter);
		this.printers.put(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pmPrinter);
		this.printers.put(TypeDocument.DECLARATION_IMPOT_PM_LOCAL, pmPrinter);
		this.printers.put(TypeDocument.DECLARATION_IMPOT_VAUDTAX, ppPrinter);

		this.diGenerators = new HashMap<>(3);
		this.diGenerators.put(PersonnePhysique.class, ppGenerator);
		this.diGenerators.put(MenageCommun.class, ppGenerator);
		this.diGenerators.put(Entreprise.class, pmGenerator);

		this.summoners = new HashMap<>(2);
		registerSummoner(this.summoners, DeclarationImpotOrdinairePP.class, new DeclarationImpotPersonnesPhysiquesSummoner());
		registerSummoner(this.summoners, DeclarationImpotOrdinairePM.class, new DeclarationImpotPersonnesMoralesSummoner());
	}

	/**
	 * Enregistrement d'une factory de sommation pour une classe de déclaration d'impôt ordinaire (on passe par une méthode ad'hoc
	 * histoire de garantir un peu de validation sur les types des déclarations et des factories associées)
	 * @param map map destination des mappings
	 * @param clazz class de la déclaration d'impôt concernée
	 * @param summoner factory de sommation pour ce genre de déclaration d'impôt
	 * @param <T> type de la déclaration d'impôt
	 */
	private static <T extends DeclarationImpotOrdinaire> void registerSummoner(Map<Class<? extends DeclarationImpotOrdinaire>, DeclarationSummoner<?>> map,
	                                                                           Class<T> clazz, DeclarationSummoner<T> summoner) {
		map.put(clazz, summoner);
	}
}
