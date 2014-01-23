package ch.vd.uniregctb.webservices.v5;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchResults;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.unireg.ws.ack.v1.AckStatus;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckResult;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationKey;
import ch.vd.unireg.ws.deadline.v1.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v1.DeadlineResponse;
import ch.vd.unireg.ws.deadline.v1.DeadlineStatus;
import ch.vd.unireg.ws.security.v1.SecurityResponse;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.jms.BamMessageHelper;
import ch.vd.uniregctb.jms.BamMessageSender;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.webservices.common.AccessDeniedException;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.common.WebServiceHelper;

public class BusinessWebServiceImpl implements BusinessWebService {

	private static final int DECLARATION_ACK_BATCH_SIZE = 50;

	private SecurityProviderInterface securityProvider;
	private PlatformTransactionManager transactionManager;
	private TiersService tiersService;
	private DeclarationImpotService diService;
	private BamMessageSender bamSender;

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setDiService(DeclarationImpotService diService) {
		this.diService = diService;
	}

	public void setBamSender(BamMessageSender bamSender) {
		this.bamSender = bamSender;
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
	public void setAutomaticRepaymentBlockingFlag(final int partyNo, UserLogin login, final boolean blocked) throws AccessDeniedException {
		WebServiceHelper.checkAccess(securityProvider, login, Role.VISU_ALL);
		WebServiceHelper.checkPartyReadWriteAccess(securityProvider, login, partyNo);

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
	public boolean getAutomaticRepaymentBlockingFlag(final int partyNo, UserLogin login) throws AccessDeniedException {
		WebServiceHelper.checkAccess(securityProvider, login, Role.VISU_ALL);
		WebServiceHelper.checkPartyReadAccess(securityProvider, login, partyNo);

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
	public OrdinaryTaxDeclarationAckResponse ackOrdinaryTaxDeclarations(final UserLogin login, OrdinaryTaxDeclarationAckRequest request) throws AccessDeniedException {
		WebServiceHelper.checkAccess(securityProvider, login, Role.DI_QUIT_PP);

		final RegDate dateRetour = ch.vd.uniregctb.xml.DataHelper.xmlToCore(request.getDate());
		final String source = request.getSource();

		final OrdinaryTaxDeclarationAckBatchResult result = new OrdinaryTaxDeclarationAckBatchResult();
		final BatchTransactionTemplateWithResults<OrdinaryTaxDeclarationKey, OrdinaryTaxDeclarationAckBatchResult> template = new BatchTransactionTemplateWithResults<>(request.getDeclaration(),
		                                                                                                                                                                DECLARATION_ACK_BATCH_SIZE,
		                                                                                                                                                                Behavior.REPRISE_AUTOMATIQUE,
		                                                                                                                                                                transactionManager, null);
		template.execute(result, new BatchWithResultsCallback<OrdinaryTaxDeclarationKey, OrdinaryTaxDeclarationAckBatchResult>() {
			@Override
			public boolean doInTransaction(List<OrdinaryTaxDeclarationKey> keys, OrdinaryTaxDeclarationAckBatchResult result) throws Exception {
				for (OrdinaryTaxDeclarationKey key : keys) {
					quittancerDeclaration(login, key, source, dateRetour, result);
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
	private static final class OrdinaryTaxDeclarationAckBatchResult implements BatchResults<OrdinaryTaxDeclarationKey, OrdinaryTaxDeclarationAckBatchResult> {

		private final List<OrdinaryTaxDeclarationAckResult> list = new LinkedList<>();

		@Override
		public void addErrorException(OrdinaryTaxDeclarationKey key, Exception e) {
			list.add(new OrdinaryTaxDeclarationAckResult(key, AckStatus.UNEXPECTED_ERROR, WebServiceHelper.buildExceptionMessage(e)));
		}

		public void addCasTraite(OrdinaryTaxDeclarationKey key, AckStatus status, String message) {
			list.add(new OrdinaryTaxDeclarationAckResult(key, status, message));
		}

		@Override
		public void addAll(OrdinaryTaxDeclarationAckBatchResult ordinaryTaxDeclarationAckBatchResult) {
			list.addAll(ordinaryTaxDeclarationAckBatchResult.getList());
		}

		public List<OrdinaryTaxDeclarationAckResult> getList() {
			return list;
		}
	}

	private void quittancerDeclaration(UserLogin userLogin, OrdinaryTaxDeclarationKey key, String source, RegDate dateRetour, OrdinaryTaxDeclarationAckBatchResult result) {
		try {
			final int partyNo = key.getPartyNo();
			final int pf = key.getTaxPeriod();
			final int noSeq = key.getSequenceNo();

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
	public DeadlineResponse newOrdinaryTaxDeclarationDeadline(final int partyNo, final int pf, final int seqNo, UserLogin login, DeadlineRequest request) throws AccessDeniedException {
		WebServiceHelper.checkAccess(securityProvider, login, Role.DI_DELAI_PP);

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
}
