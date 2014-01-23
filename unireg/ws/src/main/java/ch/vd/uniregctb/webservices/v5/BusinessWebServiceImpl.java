package ch.vd.uniregctb.webservices.v5;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
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
import ch.vd.shared.batchtemplate.BatchResults;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.ws.ack.v1.AckStatus;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckResult;
import ch.vd.unireg.ws.deadline.v1.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v1.DeadlineResponse;
import ch.vd.unireg.ws.deadline.v1.DeadlineStatus;
import ch.vd.unireg.ws.modifiedtaxpayers.v1.PartyNumberList;
import ch.vd.unireg.ws.security.v1.SecurityResponse;
import ch.vd.unireg.ws.taxoffices.v1.TaxOffice;
import ch.vd.unireg.ws.taxoffices.v1.TaxOffices;
import ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclarationKey;
import ch.vd.unireg.xml.party.v3.PartyInfo;
import ch.vd.unireg.xml.party.v3.PartyType;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.source.ListeRecapService;
import ch.vd.uniregctb.indexer.EmptySearchCriteriaException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.jms.BamMessageHelper;
import ch.vd.uniregctb.jms.BamMessageSender;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
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

public class BusinessWebServiceImpl implements BusinessWebService {

	private static final int DECLARATION_ACK_BATCH_SIZE = 50;

	private static final Set<CategorieImpotSource> CIS_SUPPORTEES = EnumHelper.getCategoriesImpotSourceAutorisees();

	private SecurityProviderInterface securityProvider;
	private PlatformTransactionManager transactionManager;
	private TiersService tiersService;
	private TiersDAO tiersDAO;
	private DeclarationImpotService diService;
	private BamMessageSender bamSender;
	private ServiceInfrastructureService infraService;
	private ListeRecapService lrService;
	private GlobalTiersSearcher tiersSearcher;

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setDiService(DeclarationImpotService diService) {
		this.diService = diService;
	}

	public void setBamSender(BamMessageSender bamSender) {
		this.bamSender = bamSender;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setLrService(ListeRecapService lrService) {
		this.lrService = lrService;
	}

	public void setTiersSearcher(GlobalTiersSearcher tiersSearcher) {
		this.tiersSearcher = tiersSearcher;
	}

	private <T> T doInTransaction(boolean readonly, TransactionCallback<T> callback) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(readonly);
		return template.execute(callback);
	}

	@Override
	public SecurityResponse getSecurityOnParty(String user, int partyNo) {
		final Niveau niveau = securityProvider.getDroitAcces(user, partyNo);
		return new SecurityResponse(user, partyNo, EnumHelper.toXml(niveau));
	}

	@Override
	public void setAutomaticRepaymentBlockingFlag(final int partyNo, UserLogin user, final boolean blocked) throws AccessDeniedException {
		WebServiceHelper.checkAccess(securityProvider, user, Role.VISU_ALL);
		WebServiceHelper.checkPartyReadWriteAccess(securityProvider, user, partyNo);

		doInTransaction(false, new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Tiers tiers = tiersService.getTiers(partyNo);
				if (tiers == null) {
					throw new TiersNotFoundException(partyNo);
				}
				tiers.setBlocageRemboursementAutomatique(blocked);
			}
		});
	}

	@Override
	public boolean getAutomaticRepaymentBlockingFlag(final int partyNo, UserLogin user) throws AccessDeniedException {
		WebServiceHelper.checkAccess(securityProvider, user, Role.VISU_ALL);
		WebServiceHelper.checkPartyReadAccess(securityProvider, user, partyNo);

		return doInTransaction(true, new TransactionCallback<Boolean>() {
			@Override
			public Boolean doInTransaction(TransactionStatus status) {
				final Tiers tiers = tiersService.getTiers(partyNo);
				if (tiers == null) {
					throw new TiersNotFoundException(partyNo);
				}
				return tiers.getBlocageRemboursementAutomatique() != null && tiers.getBlocageRemboursementAutomatique() ? Boolean.TRUE : Boolean.FALSE;
			}
		});
	}

	@Override
	public OrdinaryTaxDeclarationAckResponse ackOrdinaryTaxDeclarations(final UserLogin user, OrdinaryTaxDeclarationAckRequest request) throws AccessDeniedException {
		WebServiceHelper.checkAccess(securityProvider, user, Role.DI_QUIT_PP);

		final RegDate dateRetour = ch.vd.uniregctb.xml.DataHelper.xmlToCore(request.getDate());
		final String source = request.getSource();

		final OrdinaryTaxDeclarationAckBatchResult result = new OrdinaryTaxDeclarationAckBatchResult();
		final BatchTransactionTemplateWithResults<TaxDeclarationKey, OrdinaryTaxDeclarationAckBatchResult> template = new BatchTransactionTemplateWithResults<>(request.getDeclaration(),
		                                                                                                                                                                DECLARATION_ACK_BATCH_SIZE,
		                                                                                                                                                                Behavior.REPRISE_AUTOMATIQUE,
		                                                                                                                                                                transactionManager, null);
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
			WebServiceHelper.checkPartyReadWriteAccess(securityProvider, userLogin, partyNo);

			final Tiers tiers = tiersService.getTiers(partyNo);
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
					diService.quittancementDI(ctb, di, dateRetour, source, true);

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
			bamSender.sendBamMessageQuittancementDi(processDefinitionId, processInstanceId, businessId, ctbId, annee, bamHeaders);
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
		WebServiceHelper.checkAccess(securityProvider, user, Role.DI_DELAI_PP);

		final RegDate nouveauDelai = ch.vd.uniregctb.xml.DataHelper.xmlToCore(request.getNewDeadline());
		final RegDate dateObtention = ch.vd.uniregctb.xml.DataHelper.xmlToCore(request.getGrantedOn());
		final RegDate today = RegDate.get();

		return doInTransaction(false, new TransactionCallback<DeadlineResponse>() {
			@Override
			public DeadlineResponse doInTransaction(TransactionStatus status) {

				final Tiers tiers = tiersService.getTiers(partyNo);
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

		final Commune commune = infraService.getCommuneByNumeroOfs(municipalityId, date);
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
				final CollectiviteAdministrative oid = tiersDAO.getCollectiviteAdministrativeForDistrict(codeDistrict);
				final CollectiviteAdministrative oir = tiersDAO.getCollectiviteAdministrativeForRegion(codeRegion);
				return new TaxOffices(new TaxOffice(oid.getNumero().intValue(), oid.getNumeroCollectiviteAdministrative()),
				                      new TaxOffice(oir.getNumero().intValue(), oir.getNumeroCollectiviteAdministrative()),
				                      null);
			}
		});
	}

	@Override
	public PartyNumberList getModifiedTaxPayers(UserLogin user, final Date since, final Date until) throws AccessDeniedException {
		WebServiceHelper.checkAccess(securityProvider, user, Role.VISU_ALL);
		return doInTransaction(true, new TransactionCallback<PartyNumberList>() {
			@Override
			public PartyNumberList doInTransaction(TransactionStatus status) {
				final List<Long> longList = tiersDAO.getListeCtbModifies(since, until);
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
		WebServiceHelper.checkAccess(securityProvider, user, Role.VISU_ALL);
		return doInTransaction(true, new TransactionCallback<DebtorInfo>() {
			@Override
			public DebtorInfo doInTransaction(TransactionStatus status) {
				final Tiers tiers = tiersDAO.get(debtorNo, false);
				if (tiers == null || !(tiers instanceof DebiteurPrestationImposable)) {
					throw new ObjectNotFoundException("Pas de débiteur de prestation imposable avec le numéro " + debtorNo);
				}

				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
				final List<? extends DateRange> lrEmises = dpi.getDeclarationsForPeriode(pf, false);
				final List<DateRange> lrManquantes = lrService.findLRsManquantes(dpi, RegDate.get(pf, 12, 31), new ArrayList<DateRange>());
				final List<DateRange> lrManquantesInPf = extractIntersecting(lrManquantes, new DateRangeHelper.Range(RegDate.get(pf, 1, 1), RegDate.get(pf, 12, 31)));
				return new DebtorInfo(debtorNo, pf, lrManquantesInPf.size() + lrEmises.size(), lrEmises.size(), null);
			}
		});
	}

	@Override
	public List<PartyInfo> searchParty(UserLogin user, @Nullable String partyNo, @Nullable String name, SearchMode nameSearchMode, @Nullable String townOrCountry,
	                                   @Nullable RegDate dateOfBirth, @Nullable String socialInsuranceNumber, @Nullable Integer taxResidenceFSOId,
	                                   boolean onlyActiveMainTaxResidence, @Nullable Set<PartyType> partyTypes, @Nullable DebtorCategory debtorCategory,
	                                   @Nullable Boolean activeParty, @Nullable Long oldWithholdingNumber) throws AccessDeniedException, IndexerException {
		WebServiceHelper.checkAnyAccess(securityProvider, user, Role.VISU_ALL, Role.VISU_LIMITE);
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
}
