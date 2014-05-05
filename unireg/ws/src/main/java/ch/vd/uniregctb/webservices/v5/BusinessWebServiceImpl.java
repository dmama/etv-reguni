package ch.vd.uniregctb.webservices.v5;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallback;
import ch.vd.registre.base.tx.TxCallbackException;
import ch.vd.shared.batchtemplate.BatchResults;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.unireg.avatars.AvatarService;
import ch.vd.unireg.avatars.ImageData;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.ws.ack.v1.AckStatus;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckResult;
import ch.vd.unireg.ws.deadline.v1.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v1.DeadlineResponse;
import ch.vd.unireg.ws.deadline.v1.DeadlineStatus;
import ch.vd.unireg.ws.modifiedtaxpayers.v1.PartyNumberList;
import ch.vd.unireg.ws.parties.v1.Entry;
import ch.vd.unireg.ws.parties.v1.Parties;
import ch.vd.unireg.ws.security.v1.SecurityResponse;
import ch.vd.unireg.ws.taxoffices.v1.TaxOffice;
import ch.vd.unireg.ws.taxoffices.v1.TaxOffices;
import ch.vd.unireg.xml.error.v1.ErrorType;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.party.corporation.v3.CorporationEvent;
import ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclarationKey;
import ch.vd.unireg.xml.party.v3.Party;
import ch.vd.unireg.xml.party.v3.PartyInfo;
import ch.vd.unireg.xml.party.v3.PartyPart;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BatchIterator;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.StandardBatchIterator;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.source.ListeRecapService;
import ch.vd.uniregctb.efacture.EFactureService;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.indexer.EmptySearchCriteriaException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.EvenementPM;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;
import ch.vd.uniregctb.jms.BamMessageHelper;
import ch.vd.uniregctb.jms.BamMessageSender;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.metier.piis.PeriodeImpositionImpotSourceService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.webservices.common.AccessDeniedException;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.common.WebServiceHelper;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.ServiceException;
import ch.vd.uniregctb.xml.party.v3.PartyBuilder;

public class BusinessWebServiceImpl implements BusinessWebService {

	private static final Logger LOGGER = Logger.getLogger(BusinessWebServiceImpl.class);

	private static final int DECLARATION_ACK_BATCH_SIZE = 50;
	private static final int PARTIES_BATCH_SIZE = 20;

	protected static final int MAX_BATCH_SIZE = 100;

	private static final Set<CategorieImpotSource> CIS_SUPPORTEES = EnumHelper.getCategoriesImpotSourceAutorisees();

	private static final Map<Class<? extends Tiers>, PartyFactory<?>> PARTY_FACTORIES = buildPartyFactoryMap();

	private final Context context = new Context();
	private GlobalTiersSearcher tiersSearcher;
	private ExecutorService threadPool;
	private AvatarService avatarService;

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.context.securityProvider = securityProvider;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.context.transactionManager = transactionManager;
	}

	public void setTiersService(TiersService tiersService) {
		this.context.tiersService = tiersService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.context.tiersDAO = tiersDAO;
	}

	public void setDiService(DeclarationImpotService diService) {
		this.context.diService = diService;
	}

	public void setBamSender(BamMessageSender bamSender) {
		this.context.bamSender = bamSender;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.context.infraService = infraService;
	}

	public void setLrService(ListeRecapService lrService) {
		this.context.lrService = lrService;
	}

	public void setTiersSearcher(GlobalTiersSearcher tiersSearcher) {
		this.tiersSearcher = tiersSearcher;
	}

	public void setPersonneMoraleService(ServicePersonneMoraleService personneMoraleService) {
		this.context.servicePM = personneMoraleService;
	}

	public void setAssujettissementService(AssujettissementService service) {
		context.assujettissementService = service;
	}

	public void setPeriodeImpositionService(PeriodeImpositionService service) {
		context.periodeImpositionService = service;
	}

	public void setPeriodeImpositionImpotSourceService(PeriodeImpositionImpotSourceService service) {
		context.periodeImpositionImpotSourceService = service;
	}

	public void setServiceCivil(ServiceCivilService service) {
		context.serviceCivilService = service;
	}

	public void setHibernateTemplate(HibernateTemplate template) {
		context.hibernateTemplate = template;
	}

	public void setSituationService(SituationFamilleService situationService) {
		context.situationService = situationService;
	}

	public void setAdresseService(AdresseService adresseService) {
		context.adresseService = adresseService;
	}

	public void setIbanValidator(IbanValidator ibanValidator) {
		context.ibanValidator = ibanValidator;
	}

	public void setParametreService(ParametreAppService parametreService) {
		context.parametreService = parametreService;
	}

	public void setEFactureService(EFactureService eFactureService) {
		context.eFactureService = eFactureService;
	}

	public void setThreadPool(ExecutorService threadPool) {
		this.threadPool = threadPool;
	}

	public void setAvatarService(AvatarService avatarService) {
		this.avatarService = avatarService;
	}

	private <T> T doInTransaction(boolean readonly, TransactionCallback<T> callback) {
		final TransactionTemplate template = new TransactionTemplate(context.transactionManager);
		template.setReadOnly(readonly);
		return template.execute(callback);
	}

	@Override
	public SecurityResponse getSecurityOnParty(String user, int partyNo) {
		final Niveau niveau = context.securityProvider.getDroitAcces(user, partyNo);
		return new SecurityResponse(user, partyNo, EnumHelper.toXml(niveau));
	}

	@Override
	public void setAutomaticRepaymentBlockingFlag(final int partyNo, UserLogin user, final boolean blocked) throws AccessDeniedException {
		doInTransaction(false, new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Tiers tiers = context.tiersService.getTiers(partyNo);
				if (tiers == null) {
					throw new TiersNotFoundException(partyNo);
				}
				tiers.setBlocageRemboursementAutomatique(blocked);
			}
		});
	}

	@Override
	public boolean getAutomaticRepaymentBlockingFlag(final int partyNo, UserLogin user) throws AccessDeniedException {
		return doInTransaction(true, new TransactionCallback<Boolean>() {
			@Override
			public Boolean doInTransaction(TransactionStatus status) {
				final Tiers tiers = context.tiersService.getTiers(partyNo);
				if (tiers == null) {
					throw new TiersNotFoundException(partyNo);
				}
				return tiers.getBlocageRemboursementAutomatique() != null && tiers.getBlocageRemboursementAutomatique() ? Boolean.TRUE : Boolean.FALSE;
			}
		});
	}

	@Override
	public OrdinaryTaxDeclarationAckResponse ackOrdinaryTaxDeclarations(final UserLogin user, OrdinaryTaxDeclarationAckRequest request) throws AccessDeniedException {
		final RegDate dateRetour = ch.vd.uniregctb.xml.DataHelper.xmlToCore(request.getDate());
		final String source = request.getSource();

		final OrdinaryTaxDeclarationAckBatchResult result = new OrdinaryTaxDeclarationAckBatchResult();
		final BatchTransactionTemplateWithResults<TaxDeclarationKey, OrdinaryTaxDeclarationAckBatchResult> template = new BatchTransactionTemplateWithResults<>(request.getDeclaration(),
		                                                                                                                                                                DECLARATION_ACK_BATCH_SIZE,
		                                                                                                                                                                Behavior.REPRISE_AUTOMATIQUE,
		                                                                                                                                                                context.transactionManager, null);
		template.execute(result, new BatchWithResultsCallback<TaxDeclarationKey, OrdinaryTaxDeclarationAckBatchResult>() {
			@Override
			public boolean doInTransaction(List<TaxDeclarationKey> keys, OrdinaryTaxDeclarationAckBatchResult result) throws Exception {
				for (TaxDeclarationKey key : keys) {
					quittancerDeclaration(user, key, source, dateRetour, result);
				}
				return true;
			}

			@Override
			public OrdinaryTaxDeclarationAckBatchResult createSubRapport() {
				return new OrdinaryTaxDeclarationAckBatchResult();
			}
		}, null);

		return new OrdinaryTaxDeclarationAckResponse(result.getList());
	}

	/**
	 * Classe de résultats pour l'utilisation du {@link ch.vd.uniregctb.common.BatchTransactionTemplateWithResults} dans le quittancement de DI
	 */
	private static final class OrdinaryTaxDeclarationAckBatchResult implements BatchResults<TaxDeclarationKey, OrdinaryTaxDeclarationAckBatchResult> {

		private final List<OrdinaryTaxDeclarationAckResult> list = new LinkedList<>();

		@Override
		public void addErrorException(TaxDeclarationKey key, Exception e) {
			list.add(new OrdinaryTaxDeclarationAckResult(key, AckStatus.UNEXPECTED_ERROR, WebServiceHelper.buildExceptionMessage(e), 0, null));
		}

		public void addCasTraite(TaxDeclarationKey key, AckStatus status, String message) {
			list.add(new OrdinaryTaxDeclarationAckResult(key, status, message, 0, null));
		}

		@Override
		public void addAll(OrdinaryTaxDeclarationAckBatchResult ordinaryTaxDeclarationAckBatchResult) {
			list.addAll(ordinaryTaxDeclarationAckBatchResult.getList());
		}

		public List<OrdinaryTaxDeclarationAckResult> getList() {
			return list;
		}
	}

	private void quittancerDeclaration(UserLogin userLogin, TaxDeclarationKey key, String source, RegDate dateRetour, OrdinaryTaxDeclarationAckBatchResult result) {
		try {
			final int partyNo = key.getTaxpayerNumber();
			final int pf = key.getTaxPeriod();
			final int noSeq = key.getSequenceNumber();

			// TODO JDE : faut-il faire ce test ? Il n'était pas fait dans les versions précédentes du service...
			WebServiceHelper.checkPartyReadWriteAccess(context.securityProvider, userLogin, partyNo);

			final Tiers tiers = context.tiersService.getTiers(partyNo);
			if (tiers == null) {
				result.addCasTraite(key, AckStatus.ERROR_UNKNOWN_PARTY, null);
			}
			else if (tiers.isDebiteurInactif()) {
				result.addCasTraite(key, AckStatus.ERROR_INACTIVE_DEBTOR, null);
			}
			else if (tiers.getDernierForFiscalPrincipal() == null) {
				result.addCasTraite(key, AckStatus.ERROR_TAX_LIABILITY, "Aucun for principal.");
			}
			else if (tiers instanceof Contribuable) {
				final Contribuable ctb = (Contribuable) tiers;
				final DeclarationImpotOrdinaire di = findDeclaration(ctb, pf, noSeq);
				if (di == null) {
					result.addCasTraite(key, AckStatus.ERROR_UNKNOWN_TAX_DECLARATION, null);
				}
				else if (di.isAnnule()) {
					result.addCasTraite(key, AckStatus.ERROR_CANCELED_TAX_DECLARATION, null);
				}
				else if (RegDateHelper.isBeforeOrEqual(dateRetour, di.getDateExpedition(), NullDateBehavior.EARLIEST)) {
					result.addCasTraite(key, AckStatus.ERROR_INVALID_ACK_DATE, "Date donnée antérieure à la date d'émission de la déclaration.");
				}
				else if (RegDateHelper.isAfter(dateRetour, RegDate.get(), NullDateBehavior.LATEST)) {
					result.addCasTraite(key, AckStatus.ERROR_INVALID_ACK_DATE, "Date donnée dans le futur de la date du jour.");
				}
				else {
					// envoie le quittancement au BAM
					sendQuittancementToBam(di, dateRetour);

					// procéde au quittancement
					context.diService.quittancementDI(ctb, di, dateRetour, source, true);

					// tout est bon...
					result.addCasTraite(key, AckStatus.OK, null);
				}
			}
			else {
				result.addCasTraite(key, AckStatus.ERROR_WRONG_PARTY_TYPE, "Le tiers donné n'est pas un contribuable.");
			}
		}
		catch (AccessDeniedException e) {
			result.addCasTraite(key, AckStatus.ERROR_ACCESS_DENIED, WebServiceHelper.buildExceptionMessage(e));
		}
		catch (ObjectNotFoundException e) {
			result.addCasTraite(key, AckStatus.ERROR_UNKNOWN_PARTY, WebServiceHelper.buildExceptionMessage(e));
		}
		catch (Throwable e) {
			result.addCasTraite(key, AckStatus.UNEXPECTED_ERROR, WebServiceHelper.buildExceptionMessage(e));
		}
	}


	/**
	 * Envoi du quittancement vers le BAM
	 * @param di la DI quittancée
	 * @param dateQuittancement date de quittancement de la DI
	 */
	private void sendQuittancementToBam(DeclarationImpotOrdinaire di, RegDate dateQuittancement) {
		final long ctbId = di.getTiers().getNumero();
		final int annee = di.getPeriode().getAnnee();
		final int noSequence = di.getNumero();
		try {
			final Map<String, String> bamHeaders = BamMessageHelper.buildCustomBamHeadersForQuittancementDeclaration(di, dateQuittancement, null);
			final String businessId = String.format("%d-%d-%d-%s", ctbId, annee, noSequence, new SimpleDateFormat("yyyyMMddHHmmssSSS").format(DateHelper.getCurrentDate()));
			final String processDefinitionId = BamMessageHelper.PROCESS_DEFINITION_ID_PAPIER;       // pour le moment, tous les quittancements par le WS concenent les DI "papier"
			final String processInstanceId = BamMessageHelper.buildProcessInstanceId(di);
			context.bamSender.sendBamMessageQuittancementDi(processDefinitionId, processInstanceId, businessId, ctbId, annee, bamHeaders);
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(String.format("Erreur à la notification au BAM du quittancement de la DI %d (%d) du contribuable %d", annee, noSequence, ctbId), e);
		}
	}

	/**
	 * Recherche la declaration pour une année et un numéro de déclaration dans l'année
	 *
	 * @param contribuable     un contribuable
	 * @param annee            une période fiscale complète (ex. 2010)
	 * @param numeroSequenceDI le numéro de séquence de la déclaration pour le contribuable et la période considérés
	 * @return une déclaration d'impôt ordinaire, ou <b>null</b> si aucune déclaration correspondant aux critère n'est trouvée.
	 */
	private static DeclarationImpotOrdinaire findDeclaration(Contribuable contribuable, int annee, int numeroSequenceDI) {

		// [SIFISC-1227] Nous avons des cas où le numéro de séquence a été ré-utilisé après annulation d'une DI précédente
		// -> on essaie toujours de renvoyer la déclaration non-annulée qui correspond et, s'il n'y en a pas, de renvoyer
		// la dernière déclaration annulée trouvée

		DeclarationImpotOrdinaire declaration = null;
		DeclarationImpotOrdinaire declarationAnnuleeTrouvee = null;
		final List<Declaration> declarations = contribuable.getDeclarationsForPeriode(annee, true);
		if (declarations != null && !declarations.isEmpty()) {
			for (Declaration d : declarations) {
				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) d;
				if (numeroSequenceDI == 0) {
					// Dans le cas où le numero dans l'année n'est pas spécifié on prend la dernière DI trouvée sur la période
					declaration = di;
				}
				else if (di.getNumero() == numeroSequenceDI) {
					if (di.isAnnule()) {
						declarationAnnuleeTrouvee = di;
					}
					else {
						declaration = di;
						break;
					}
				}
			}
		}

		return declaration != null ? declaration : declarationAnnuleeTrouvee;
	}

	@Override
	public DeadlineResponse newOrdinaryTaxDeclarationDeadline(final int partyNo, final int pf, final int seqNo, UserLogin user, DeadlineRequest request) throws AccessDeniedException {
		final RegDate nouveauDelai = ch.vd.uniregctb.xml.DataHelper.xmlToCore(request.getNewDeadline());
		final RegDate dateObtention = ch.vd.uniregctb.xml.DataHelper.xmlToCore(request.getGrantedOn());
		final RegDate today = RegDate.get();

		return doInTransaction(false, new TransactionCallback<DeadlineResponse>() {
			@Override
			public DeadlineResponse doInTransaction(TransactionStatus status) {

				final Tiers tiers = context.tiersService.getTiers(partyNo);
				if (tiers == null) {
					throw new TiersNotFoundException(partyNo);
				}
				else if (tiers instanceof Contribuable) {
					final Contribuable ctb = (Contribuable) tiers;
					final DeclarationImpotOrdinaire di = findDeclaration(ctb, pf, seqNo);
					if (di == null) {
						throw new ObjectNotFoundException("Déclaration d'impôt inexistante.");
					}
					else {
						final DeadlineResponse response;
						if (di.isAnnule()) {
							response = new DeadlineResponse(DeadlineStatus.ERROR_CANCELLED_DECLARATION, null);
						}
						else if (di.getDernierEtat().getEtat() != TypeEtatDeclaration.EMISE) {
							response = new DeadlineResponse(DeadlineStatus.ERROR_BAD_DECLARATION_STATUS, "La déclaration n'est pas dans l'état 'EMISE'.");
						}
						else if (RegDateHelper.isAfter(dateObtention, today, NullDateBehavior.LATEST)) {
							response = new DeadlineResponse(DeadlineStatus.ERROR_INVALID_GRANTED_ON, "La date d'obtention du délai ne peut pas être dans le futur de la date du jour.");
						}
						else if (RegDateHelper.isBefore(nouveauDelai, today, NullDateBehavior.LATEST)) {
							response = new DeadlineResponse(DeadlineStatus.ERROR_INVALID_DEADLINE, "Un nouveau délai ne peut pas être demandé dans le passé de la date du jour.");
						}
						else {
							final RegDate delaiActuel = di.getDernierDelais().getDelaiAccordeAu();
							if (RegDateHelper.isBeforeOrEqual(nouveauDelai, delaiActuel, NullDateBehavior.LATEST)) {
								response = new DeadlineResponse(DeadlineStatus.ERROR_INVALID_DEADLINE, "Un délai plus lointain existe déjà.");
							}
							else {
								final DelaiDeclaration delai = new DelaiDeclaration();
								delai.setDateTraitement(RegDate.get());
								delai.setConfirmationEcrite(false);
								delai.setDateDemande(dateObtention);
								delai.setDelaiAccordeAu(nouveauDelai);
								di.addDelai(delai);

								response = new DeadlineResponse(DeadlineStatus.OK, null);
							}
						}

						return response;
					}
				}
				else {
					throw new ObjectNotFoundException("Le tiers donné n'est pas un contribuable.");
				}
			}
		});
	}

	@Override
	public TaxOffices getTaxOffices(int municipalityId, @Nullable RegDate date) {

		final Commune commune = context.infraService.getCommuneByNumeroOfs(municipalityId, date);
		if (commune == null || !commune.isVaudoise()) {
			throw new ObjectNotFoundException(String.format("Commune %d inconnue dans le canton de Vaud.", municipalityId));
		}

		final Integer codeRegion = commune.getCodeRegion();
		final Integer codeDistrict = commune.getCodeDistrict();
		if (codeRegion == null || codeDistrict == null) {
			throw new ObjectNotFoundException("Code(s) région et/ou district inconnu(s) pour la commune.");
		}

		return doInTransaction(true, new TransactionCallback<TaxOffices>() {
			@Override
			public TaxOffices doInTransaction(TransactionStatus status) {
				final CollectiviteAdministrative oid = context.tiersDAO.getCollectiviteAdministrativeForDistrict(codeDistrict);
				final CollectiviteAdministrative oir = context.tiersDAO.getCollectiviteAdministrativeForRegion(codeRegion);
				return new TaxOffices(new TaxOffice(oid.getNumero().intValue(), oid.getNumeroCollectiviteAdministrative()),
				                      new TaxOffice(oir.getNumero().intValue(), oir.getNumeroCollectiviteAdministrative()),
				                      null);
			}
		});
	}

	@Override
	public PartyNumberList getModifiedTaxPayers(UserLogin user, final Date since, final Date until) throws AccessDeniedException {
		return doInTransaction(true, new TransactionCallback<PartyNumberList>() {
			@Override
			public PartyNumberList doInTransaction(TransactionStatus status) {
				final List<Long> longList = context.tiersDAO.getListeCtbModifies(since, until);
				final List<Integer> intList = new ArrayList<>(longList.size());
				for (Long id : longList) {
					intList.add(id.intValue());
				}
				return new PartyNumberList(intList);
			}
		});
	}

	private static <T extends DateRange> List<T> extractIntersecting(List<T> src, DateRange periode) {
		if (src == null || src.isEmpty()) {
			return Collections.emptyList();
		}
		final List<T> res = new ArrayList<>(src.size());
		for (T range : src) {
			if (DateRangeHelper.intersect(range, periode)) {
				res.add(range);
			}
		}
		return res;
	}

	@Override
	public DebtorInfo getDebtorInfo(UserLogin user, final int debtorNo, final int pf) throws AccessDeniedException {
		return doInTransaction(true, new TransactionCallback<DebtorInfo>() {
			@Override
			public DebtorInfo doInTransaction(TransactionStatus status) {
				final Tiers tiers = context.tiersDAO.get(debtorNo, false);
				if (tiers == null || !(tiers instanceof DebiteurPrestationImposable)) {
					throw new ObjectNotFoundException("Pas de débiteur de prestation imposable avec le numéro " + debtorNo);
				}

				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
				final List<? extends DateRange> lrEmises = dpi.getDeclarationsForPeriode(pf, false);
				final List<DateRange> lrManquantes = context.lrService.findLRsManquantes(dpi, RegDate.get(pf, 12, 31), new ArrayList<DateRange>());
				final List<DateRange> lrManquantesInPf = extractIntersecting(lrManquantes, new DateRangeHelper.Range(RegDate.get(pf, 1, 1), RegDate.get(pf, 12, 31)));
				return new DebtorInfo(debtorNo, pf, lrManquantesInPf.size() + lrEmises.size(), lrEmises.size(), null);
			}
		});
	}

	@Override
	public List<PartyInfo> searchParty(UserLogin user, @Nullable String partyNo, @Nullable String name, SearchMode nameSearchMode, @Nullable String townOrCountry,
	                                   @Nullable RegDate dateOfBirth, @Nullable String socialInsuranceNumber, @Nullable String uidNumber, @Nullable Integer taxResidenceFSOId,
	                                   boolean onlyActiveMainTaxResidence, @Nullable Set<PartySearchType> partyTypes, @Nullable DebtorCategory debtorCategory,
	                                   @Nullable Boolean activeParty, @Nullable Long oldWithholdingNumber) throws AccessDeniedException, IndexerException {
		final TiersCriteria criteria = new TiersCriteria();
		if (partyNo != null && StringUtils.isNotBlank(partyNo)) {
			// tous les autres critères sont ignorés si le numéro est renseigné
			final String pureNo = StringUtils.trimToNull(partyNo.replaceAll("[^0-9]", StringUtils.EMPTY));
			if (pureNo != null) {
				criteria.setNumero(Long.parseLong(pureNo));
			}
		}
		else {
			criteria.setNomRaison(StringUtils.trimToNull(name));
			criteria.setTypeRechercheDuNom(EnumHelper.toCore(nameSearchMode));
			criteria.setLocaliteOuPays(StringUtils.trimToNull(townOrCountry));
			criteria.setDateNaissance(dateOfBirth);
			criteria.setNumeroAVS(StringUtils.trimToNull(socialInsuranceNumber));
			if (taxResidenceFSOId != null) {
				criteria.setNoOfsFor(Integer.toString(taxResidenceFSOId));
				criteria.setForPrincipalActif(onlyActiveMainTaxResidence);
			}
			criteria.setTypesTiers(EnumHelper.toCore(partyTypes));
			criteria.setCategorieDebiteurIs(ch.vd.uniregctb.xml.EnumHelper.xmlToCore(debtorCategory));
			criteria.setTiersActif(activeParty);
			criteria.setAncienNumeroSourcier(oldWithholdingNumber);
			criteria.setNumeroIDE(uidNumber);
		}

		if (criteria.isEmpty()) {
			throw new EmptySearchCriteriaException("Les critères de recherche sont vides");
		}

		final List<TiersIndexedData> coreResult = tiersSearcher.search(criteria);
		final List<PartyInfo> result = new ArrayList<>(coreResult.size());
		for (TiersIndexedData data : coreResult) {
			if (data != null && (data.getCategorieImpotSource() == null || CIS_SUPPORTEES.contains(data.getCategorieImpotSource()))) {
				final PartyInfo info = ch.vd.uniregctb.xml.DataHelper.coreToXMLv3(data);
				result.add(info);
			}
		}
		return result;
	}

	@Override
	public List<CorporationEvent> searchCorporationEvent(UserLogin user, @Nullable Integer corporationId, @Nullable String eventCode,
	                                                     @Nullable RegDate startDate, @Nullable RegDate endDate) throws AccessDeniedException, EmptySearchCriteriaException {
		if (corporationId == null && StringUtils.isBlank(eventCode) && startDate == null && endDate == null) {
			throw new EmptySearchCriteriaException("Les critères de recherche sont vides.");
		}
		final Long corpNr = corporationId != null ? Long.valueOf(corporationId) : null;
		final List<EvenementPM> list = context.servicePM.findEvenements(corpNr, eventCode, startDate, endDate);
		return DataHelper.coreToXML(list);
	}

	private static interface PartyFactory<T extends Tiers> {
		Party buildParty(T tiers, @Nullable Set<PartyPart> parts, Context context) throws ServiceException;
	}

	private static <T extends Tiers> void addToPartyFactorMap(Map<Class<? extends Tiers>, PartyFactory<?>> map, Class<T> clazz, PartyFactory<T> factory) {
		map.put(clazz, factory);
	}

	private static Map<Class<? extends Tiers>, PartyFactory<?>> buildPartyFactoryMap() {
		final Map<Class<? extends Tiers>, PartyFactory<?>> map = new HashMap<>();
		addToPartyFactorMap(map, PersonnePhysique.class, new NaturalPersonPartyFactory());
		addToPartyFactorMap(map, MenageCommun.class, new CommonHouseholdPartyFactory());
		addToPartyFactorMap(map, DebiteurPrestationImposable.class, new DebtorPartyFactory());
		addToPartyFactorMap(map, Entreprise.class, new CorporationPartyFactory());
		addToPartyFactorMap(map, CollectiviteAdministrative.class, new AdministrativeAuthorityPartyFactory());
		addToPartyFactorMap(map, AutreCommunaute.class, new OtherCommunityPartyFactory());
		return map;
	}

	private static final class NaturalPersonPartyFactory implements PartyFactory<PersonnePhysique> {
		@Override
		public Party buildParty(PersonnePhysique pp, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
			return PartyBuilder.newNaturalPerson(pp, parts, context);
		}
	}

	private static final class CommonHouseholdPartyFactory implements PartyFactory<MenageCommun> {
		@Override
		public Party buildParty(MenageCommun mc, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
			return PartyBuilder.newCommonHousehold(mc, parts, context);
		}
	}

	private static final class DebtorPartyFactory implements PartyFactory<DebiteurPrestationImposable> {
		@Override
		public Party buildParty(DebiteurPrestationImposable dpi, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
			return PartyBuilder.newDebtor(dpi, parts, context);
		}
	}

	private static final class CorporationPartyFactory implements PartyFactory<Entreprise> {
		@Override
		public Party buildParty(Entreprise pm, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
			return PartyBuilder.newCorporation(pm, parts, context);
		}
	}

	private static final class AdministrativeAuthorityPartyFactory implements PartyFactory<CollectiviteAdministrative> {
		@Override
		public Party buildParty(CollectiviteAdministrative ca, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
			return PartyBuilder.newAdministrativeAuthority(ca, parts, context);
		}
	}

	private static final class OtherCommunityPartyFactory implements PartyFactory<AutreCommunaute> {
		@Override
		public Party buildParty(AutreCommunaute ac, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
			return PartyBuilder.newOtherCommunity(ac, parts, context);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends Tiers> Party buildParty(Tiers tiers, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
		if (tiers == null) {
			return null;
		}
		final PartyFactory<T> factory = (PartyFactory<T>) PARTY_FACTORIES.get(tiers.getClass());
		if (factory == null) {
			LOGGER.warn(String.format("Parties of core class %s cannot be externalized (no factory found)", tiers.getClass().getName()));
			return null;
		}

		return factory.buildParty((T) tiers, parts, context);
	}

	@Override
	public Party getParty(UserLogin user, final int partyNo, @Nullable final Set<PartyPart> parts) throws AccessDeniedException, ServiceException {
		try {
			return doInTransaction(true, new TxCallback<Party>() {
				@Override
				public Party execute(TransactionStatus status) throws ServiceException {
					final Tiers tiers = context.tiersService.getTiers(partyNo);
					return buildParty(tiers, parts, context);
				}
			});
		}
		catch (TxCallbackException e) {
			final Throwable cause = e.getCause();
			if (cause instanceof ServiceException) {
				throw (ServiceException) cause;
			}
			else {
				throw e;
			}
		}
	}

	private static Set<TiersDAO.Parts> toCoreAvecForsFiscaux(Set<PartyPart> parts) {
		Set<TiersDAO.Parts> set = ch.vd.uniregctb.xml.DataHelper.xmlToCoreV3(parts);
		if (set == null) {
			set = EnumSet.noneOf(TiersDAO.Parts.class);
		}
		// les fors fiscaux sont nécessaires pour déterminer les dates de début et de fin d'activité.
		set.add(TiersDAO.Parts.FORS_FISCAUX);
		return set;
	}

	private static class PartiesTask implements Callable<Parties> {

		private final Set<Integer> partyNos;
		private final Set<PartyPart> parts;
		private final UserLogin user;
		private final Context context;

		private PartiesTask(List<Integer> partyNos, Set<PartyPart> parts, UserLogin user, Context context) {
			this.partyNos = new HashSet<>(partyNos);
			this.parts = parts;
			this.user = user;
			this.context = context;
		}

		@Override
		public Parties call() throws ServiceException {
			// mode read-only..
			final TransactionTemplate template = new TransactionTemplate(context.transactionManager);
			template.setReadOnly(true);
			try {
				return template.execute(new TxCallback<Parties>() {
					@Override
					public Parties execute(TransactionStatus status) throws Exception {
						// on ne veut vraiment pas modifier la base
						return context.hibernateTemplate.execute(FlushMode.MANUAL, new HibernateCallback<Parties>() {
							@Override
							public Parties doInHibernate(Session session) throws HibernateException, SQLException {
								try {
									return doExtract();
								}
								catch (ServiceException e) {
									throw new TxCallbackException(e);
								}
							}
						});
					}
				});
			}
			catch (TxCallbackException e) {
				final Throwable cause = e.getCause();
				if (cause instanceof ServiceException) {
					throw (ServiceException) cause;
				}
				else {
					throw e;
				}
			}
		}

		private Parties doExtract() throws ServiceException {

			final Parties parties = new Parties();
			final List<Entry> entries = parties.getEntries();

			// vérification des droits d'accès et de l'existence des tiers
			final Iterator<Integer> idIterator = partyNos.iterator();
			final Set<Long> idLongSet = new HashSet<>(partyNos.size());
			while (idIterator.hasNext()) {
				final int id = idIterator.next();
				try {
					WebServiceHelper.checkPartyReadAccess(context.securityProvider, user, id);
					idLongSet.add((long) id);
				}
				catch (AccessDeniedException e) {
					entries.add(new Entry(id, null, new ch.vd.unireg.xml.error.v1.Error(ErrorType.ACCESS, e.getMessage())));
					idIterator.remove();
				}
				catch (TiersNotFoundException e) {
					entries.add(new Entry(id, null, new ch.vd.unireg.xml.error.v1.Error(ErrorType.BUSINESS, e.getMessage())));
					idIterator.remove();
				}
			}

			// récupération des données autorisées
			final List<Tiers> batchResult = context.tiersDAO.getBatch(idLongSet, toCoreAvecForsFiscaux(parts));
			for (Tiers t : batchResult) {
				if (t != null) {
					try {
						final Party party = buildParty(t, parts, context);
						if (party == null) {
							entries.add(new Entry(t.getNumero().intValue(), null, new ch.vd.unireg.xml.error.v1.Error(ErrorType.BUSINESS, "Tiers non exposé.")));
						}
						else {
							entries.add(new Entry(t.getNumero().intValue(), party, null));
						}
					}
					catch (Exception e) {
						final ErrorType errorType;
						if (e instanceof ServiceException) {
							final ServiceException se = (ServiceException) e;
							if (se.getInfo() instanceof BusinessExceptionInfo) {
								errorType = ErrorType.BUSINESS;
							}
							else if (se.getInfo() instanceof AccessDeniedExceptionInfo) {
								errorType = ErrorType.ACCESS;
							}
							else {
								errorType = ErrorType.TECHNICAL;
							}
						}
						else {
							errorType = ErrorType.TECHNICAL;
						}

						LOGGER.error("Exception au mapping xml du tiers " + t.getNumero(), e);
						entries.add(new Entry(t.getNumero().intValue(), null, new ch.vd.unireg.xml.error.v1.Error(errorType, e.getMessage())));
					}
					idLongSet.remove(t.getNumero());
				}
			}

			// le reliquat sont des erreurs techniques (= bugs ?)
			for (long id : idLongSet) {
				entries.add(new Entry((int) id, null, new ch.vd.unireg.xml.error.v1.Error(ErrorType.TECHNICAL, "Erreur inattendue.")));
			}

			return parties;
		}
	}

	@Override
	public Parties getParties(UserLogin user, List<Integer> partyNos, @Nullable Set<PartyPart> parts) throws AccessDeniedException, ServiceException {

		// on enlève les doublons sur les numéros de tiers (et les éventuels <i>null</i>)
		final Set<Integer> nos = new HashSet<>(partyNos);
		nos.remove(null);

		if (nos.size() > MAX_BATCH_SIZE) {
			throw new BadRequestException("Le nombre de tiers demandés ne peut dépasser " + MAX_BATCH_SIZE);
		}

		// on envoie la sauce sur plusieurs threads
		final ExecutorCompletionService<Parties> executor = new ExecutorCompletionService<>(threadPool);
		final BatchIterator<Integer> iterator = new StandardBatchIterator<>(nos, PARTIES_BATCH_SIZE);
		int nbRemainingTasks = 0;
		while (iterator.hasNext()) {
			executor.submit(new PartiesTask(iterator.next(), parts, user, context));
			++ nbRemainingTasks;
		}

		// et on récolte ce que l'on a semé
		final Parties finalResult = new Parties();
		while (nbRemainingTasks > 0) {
			try {
				final Future<Parties> future = executor.poll(1, TimeUnit.SECONDS);
				if (future != null) {
					-- nbRemainingTasks;
					finalResult.getEntries().addAll(future.get().getEntries());
				}
			}
			catch (InterruptedException e) {
				throw new RuntimeException("Method execution was interrupted", e);
			}
			catch (ExecutionException e) {
				final Throwable cause = e.getCause();
				try {
					throw cause;
				}
				catch (RuntimeException | Error | ServiceException c) {
					throw c;
				}
				catch (Throwable t) {
					throw new RuntimeException("Exception lancée pendant le traitement", t);
				}
			}
		}

		return finalResult;
	}

	@Override
	public ImageData getAvatar(final int partyNo) throws ServiceException {
		try {
			return doInTransaction(true, new TxCallback<ImageData>() {
				@Override
				public ImageData execute(TransactionStatus status) throws ServiceException {
					final Tiers tiers = context.tiersService.getTiers(partyNo);
					if (tiers == null) {
						throw new TiersNotFoundException(partyNo);
					}

					return avatarService.getAvatar(tiers, false);
				}
			});
		}
		catch (TxCallbackException e) {
			final Throwable cause = e.getCause();
			if (cause instanceof ServiceException) {
				throw (ServiceException) cause;
			}
			else {
				throw e;
			}
		}
	}
}
