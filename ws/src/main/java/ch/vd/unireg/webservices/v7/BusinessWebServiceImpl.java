package ch.vd.unireg.webservices.v7;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallback;
import ch.vd.registre.base.tx.TxCallbackException;
import ch.vd.registre.base.xml.XmlUtils;
import ch.vd.shared.batchtemplate.BatchResults;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.avatar.AvatarService;
import ch.vd.unireg.avatar.ImageData;
import ch.vd.unireg.avatar.TypeAvatar;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.BatchIterator;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.ProgrammingException;
import ch.vd.unireg.common.StandardBatchIterator;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.declaration.AjoutDelaiDeclarationException;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.declaration.source.ListeRecapService;
import ch.vd.unireg.efacture.EFactureService;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalV3Factory;
import ch.vd.unireg.hibernate.HibernateCallback;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.indexer.EmptySearchCriteriaException;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.tiers.GlobalTiersSearcher;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.ServiceEntreprise;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.jms.BamMessageHelper;
import ch.vd.unireg.jms.BamMessageSender;
import ch.vd.unireg.metier.assujettissement.AssujettissementException;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.PeriodeImposition;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.metier.bouclement.BouclementService;
import ch.vd.unireg.metier.bouclement.ExerciceCommercialHelper;
import ch.vd.unireg.metier.periodeexploitation.PeriodeExploitationService;
import ch.vd.unireg.metier.piis.PeriodeImpositionImpotSourceService;
import ch.vd.unireg.parametrage.DelaisAccordablesOnlineDIPM;
import ch.vd.unireg.parametrage.DelaisAccordablesOnlineDIPP;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.parametrage.ParametreDemandeDelaisOnline;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscaleDAO;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscalePM;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.registrefoncier.BatimentRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.situationfamille.SituationFamilleService;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDelaiDeclaration;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.type.delai.Delai;
import ch.vd.unireg.validation.ValidationService;
import ch.vd.unireg.webservices.common.AccessDeniedException;
import ch.vd.unireg.webservices.common.EvenementFiscalDescriptionHelper;
import ch.vd.unireg.webservices.common.WebServiceHelper;
import ch.vd.unireg.ws.ack.v7.AckStatus;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckResult;
import ch.vd.unireg.ws.deadline.v7.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v7.DeadlineResponse;
import ch.vd.unireg.ws.deadline.v7.DeadlineStatus;
import ch.vd.unireg.ws.fiscalevents.v7.FiscalEvent;
import ch.vd.unireg.ws.fiscalevents.v7.FiscalEvents;
import ch.vd.unireg.ws.groupdeadline.v7.GroupDeadlineValidationRequest;
import ch.vd.unireg.ws.groupdeadline.v7.GroupDeadlineValidationResponse;
import ch.vd.unireg.ws.groupdeadline.v7.RejectionReason;
import ch.vd.unireg.ws.groupdeadline.v7.TaxDeclarationInfo;
import ch.vd.unireg.ws.groupdeadline.v7.ValidationResult;
import ch.vd.unireg.ws.landregistry.v7.BuildingEntry;
import ch.vd.unireg.ws.landregistry.v7.BuildingList;
import ch.vd.unireg.ws.landregistry.v7.CommunityOfOwnersEntry;
import ch.vd.unireg.ws.landregistry.v7.CommunityOfOwnersList;
import ch.vd.unireg.ws.landregistry.v7.ImmovablePropertyEntry;
import ch.vd.unireg.ws.landregistry.v7.ImmovablePropertyList;
import ch.vd.unireg.ws.landregistry.v7.ImmovablePropertySearchResult;
import ch.vd.unireg.ws.modifiedtaxpayers.v7.PartyNumberList;
import ch.vd.unireg.ws.parties.v7.Entry;
import ch.vd.unireg.ws.parties.v7.Parties;
import ch.vd.unireg.ws.security.v7.PartyAccess;
import ch.vd.unireg.ws.security.v7.SecurityListResponse;
import ch.vd.unireg.ws.security.v7.SecurityResponse;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.error.v1.ErrorType;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.infra.taxoffices.v1.TaxOffices;
import ch.vd.unireg.xml.infra.v1.TaxOfficesBuilder;
import ch.vd.unireg.xml.party.communityofheirs.v1.CommunityOfHeirs;
import ch.vd.unireg.xml.party.landregistry.v1.Building;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwners;
import ch.vd.unireg.xml.party.landregistry.v1.ImmovableProperty;
import ch.vd.unireg.xml.party.landregistry.v1.ImmovablePropertyInfo;
import ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationKey;
import ch.vd.unireg.xml.party.v5.BuildingBuilder;
import ch.vd.unireg.xml.party.v5.CommunityOfHeirsBuilder;
import ch.vd.unireg.xml.party.v5.CommunityOfOwnersBuilder;
import ch.vd.unireg.xml.party.v5.EasementRightHolderComparator;
import ch.vd.unireg.xml.party.v5.ImmovablePropertyBuilder;
import ch.vd.unireg.xml.party.v5.ImmovablePropertyInfoBuilder;
import ch.vd.unireg.xml.party.v5.InternalPartyPart;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.v5.PartyBuilder;
import ch.vd.unireg.xml.party.v5.PartyInfo;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;

import static ch.vd.unireg.webservices.v7.DeadlineExtensionError.AUCUNE_DECLARATION;
import static ch.vd.unireg.webservices.v7.DeadlineExtensionError.DECLARATION_ANNULEE;
import static ch.vd.unireg.webservices.v7.DeadlineExtensionError.DECLARATION_ECHUE;
import static ch.vd.unireg.webservices.v7.DeadlineExtensionError.DECLARATION_RAPPELEE;
import static ch.vd.unireg.webservices.v7.DeadlineExtensionError.DECLARATION_RETOURNEE;
import static ch.vd.unireg.webservices.v7.DeadlineExtensionError.DECLARATION_SOMMEE;
import static ch.vd.unireg.webservices.v7.DeadlineExtensionError.DECLARATION_SUSPENDUE;
import static ch.vd.unireg.webservices.v7.DeadlineExtensionError.DELAI_DEJA_ACCORDE;
import static ch.vd.unireg.webservices.v7.DeadlineExtensionError.PLUSIEURS_DECLARATIONS;

@SuppressWarnings("Duplicates")
public class BusinessWebServiceImpl implements BusinessWebService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BusinessWebServiceImpl.class);

	private static final int DECLARATION_ACK_BATCH_SIZE = 50;
	private static final int PARTIES_BATCH_SIZE = 20;

	protected static final int MAX_BATCH_SIZE = 100;

	private static final Set<CategorieImpotSource> CIS_SUPPORTEES = EnumHelper.getCategoriesImpotSourceAutorisees();

	private static final Set<TypeAvatar> TA_IGNORES = EnumHelper.getTypesAvatarsIgnores();

	private static final Map<Class<? extends Tiers>, PartyFactory<?>> PARTY_FACTORIES = buildPartyFactoryMap();

	private final Context context = new Context();
	private GlobalTiersSearcher tiersSearcher;
	private ExecutorService threadPool;
	private AvatarService avatarService;
	private ParametrePeriodeFiscaleDAO parametrePeriodeFiscaleDAO;

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

	public void setAssujettissementService(AssujettissementService service) {
		context.assujettissementService = service;
	}

	public void setPeriodeExploitationService(PeriodeExploitationService service) {
		context.periodeExploitationService = service;
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

	public void setServiceEntreprise(ServiceEntreprise service) {
		context.serviceEntreprise = service;
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

	public void setExerciceCommercialHelper(ExerciceCommercialHelper exerciceCommercialHelper) {
		context.exerciceCommercialHelper = exerciceCommercialHelper;
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

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		context.evenementFiscalService = evenementFiscalService;
	}

	public void setRegistreFoncierService(RegistreFoncierService registreFoncierService) {
		context.registreFoncierService = registreFoncierService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRegimeFiscalService(RegimeFiscalService regimeFiscalService) {
		context.regimeFiscalService = regimeFiscalService;
	}

	public void setValidationService(ValidationService validationService) {
		context.validationService = validationService;
	}

	public void setBouclementService(BouclementService bouclementService) {
		context.bouclementService = bouclementService;
	}

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeDAO) {
		context.periodeDAO = periodeDAO;
	}

	public void setParametrePeriodeFiscaleDAO(ParametrePeriodeFiscaleDAO parametrePeriodeFiscaleDAO) {
		this.parametrePeriodeFiscaleDAO = parametrePeriodeFiscaleDAO;
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
	public SecurityListResponse getSecurityOnParties(@NotNull String user, @NotNull List<Integer> partyNos) {

		// on charge les données sur plusieurs threads
		final List<PartyAccess> partyAccesses = CollectionsUtils.parallelMap(partyNos, partyNo -> resolvePartyAccess(user, partyNo), threadPool);

		// on retourne la liste triée
		partyAccesses.sort(Comparator.comparing(PartyAccess::getPartyNo));
		return new SecurityListResponse(user, partyAccesses);
	}

	@NotNull
	private PartyAccess resolvePartyAccess(@NotNull String user, int partyNo) {
		try {
			final Niveau niveau = context.securityProvider.getDroitAcces(user, partyNo);
			return new PartyAccess(partyNo, EnumHelper.toXml(niveau), null);
		}
		catch (Exception e) {
			return new PartyAccess(partyNo, null, new ch.vd.unireg.xml.error.v1.Error(ErrorType.TECHNICAL, e.getMessage()));
		}
	}

	@Override
	public void setAutomaticRepaymentBlockingFlag(final int partyNo, final boolean blocked) {
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
	public boolean getAutomaticRepaymentBlockingFlag(final int partyNo) {
		return doInTransaction(true, status -> {
			final Tiers tiers = context.tiersService.getTiers(partyNo);
			if (tiers == null) {
				throw new TiersNotFoundException(partyNo);
			}
			return tiers.getBlocageRemboursementAutomatique() != null && tiers.getBlocageRemboursementAutomatique() ? Boolean.TRUE : Boolean.FALSE;
		});
	}

	@Override
	public OrdinaryTaxDeclarationAckResponse ackOrdinaryTaxDeclarations(OrdinaryTaxDeclarationAckRequest request) {
		final RegDate dateRetour = ch.vd.unireg.xml.DataHelper.xmlToCore(request.getDate());
		final String source = request.getSource();

		final OrdinaryTaxDeclarationAckBatchResult result = new OrdinaryTaxDeclarationAckBatchResult();
		final BatchTransactionTemplateWithResults<TaxDeclarationKey, OrdinaryTaxDeclarationAckBatchResult> template = new BatchTransactionTemplateWithResults<>(request.getDeclaration(),
		                                                                                                                                                        DECLARATION_ACK_BATCH_SIZE,
		                                                                                                                                                        Behavior.REPRISE_AUTOMATIQUE,
		                                                                                                                                                        context.transactionManager, null);
		template.execute(result, new BatchWithResultsCallback<TaxDeclarationKey, OrdinaryTaxDeclarationAckBatchResult>() {
			@Override
			public boolean doInTransaction(List<TaxDeclarationKey> keys, OrdinaryTaxDeclarationAckBatchResult result) {
				for (TaxDeclarationKey key : keys) {
					quittancerDeclaration(key, source, dateRetour, result);
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
	 * Classe de résultats pour l'utilisation du {@link BatchTransactionTemplateWithResults} dans le quittancement de DI
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

	private void quittancerDeclaration(TaxDeclarationKey key, String source, RegDate dateRetour, OrdinaryTaxDeclarationAckBatchResult result) {
		try {
			final int partyNo = key.getTaxpayerNumber();
			final int pf = key.getTaxPeriod();
			final int noSeq = key.getSequenceNumber();

			WebServiceHelper.checkPartyReadWriteAccess(context.securityProvider, partyNo);

			final Tiers tiers = context.tiersService.getTiers(partyNo);
			if (tiers == null) {
				result.addCasTraite(key, AckStatus.ERROR_UNKNOWN_PARTY, null);
			}
			else {
				if (tiers instanceof ContribuableImpositionPersonnesPhysiques) {
					WebServiceHelper.checkAccess(context.securityProvider, Role.DI_QUIT_PP);
				}
				else if (tiers instanceof Entreprise) {
					WebServiceHelper.checkAccess(context.securityProvider, Role.DI_QUIT_PM);
				}

				if (tiers.isDebiteurInactif()) {
					result.addCasTraite(key, AckStatus.ERROR_INACTIVE_DEBTOR, null);
				}
				else if (tiers instanceof Contribuable) {
					final Contribuable ctb = (Contribuable) tiers;
					if (ctb.getDernierForFiscalPrincipal() == null) {
						result.addCasTraite(key, AckStatus.ERROR_TAX_LIABILITY, "Aucun for principal.");
					}

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
	 *
	 * @param di                la DI quittancée
	 * @param dateQuittancement date de quittancement de la DI
	 */
	private void sendQuittancementToBam(DeclarationImpotOrdinaire di, RegDate dateQuittancement) {
		final long ctbId = di.getTiers().getNumero();
		final int annee = di.getPeriode().getAnnee();
		final int noSequence = di.getNumero();
		try {
			final Map<String, String> bamHeaders = BamMessageHelper.buildCustomBamHeadersForQuittancementDeclaration(di, dateQuittancement, null);
			final String businessId = String.format("%d-%d-%d-%s", ctbId, annee, noSequence, new SimpleDateFormat("yyyyMMddHHmmssSSS").format(DateHelper.getCurrentDate()));
			final String processDefinitionId =
					di instanceof DeclarationImpotOrdinairePM ? BamMessageHelper.PROCESS_DEFINITION_ID_PAPIER_PM : BamMessageHelper.PROCESS_DEFINITION_ID_PAPIER_PP;       // pour le moment, tous les quittancements par le WS concenent les DI "papier"
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
		final List<DeclarationImpotOrdinaire> declarations = contribuable.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, annee, true);
		if (!declarations.isEmpty()) {
			for (DeclarationImpotOrdinaire di : declarations) {
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
	public DeadlineResponse newOrdinaryTaxDeclarationDeadline(final int partyNo, final int pf, final int seqNo, DeadlineRequest request) throws AccessDeniedException {
		final RegDate nouveauDelai = ch.vd.unireg.xml.DataHelper.xmlToCore(request.getNewDeadline());
		final RegDate dateObtention = ch.vd.unireg.xml.DataHelper.xmlToCore(request.getGrantedOn());

		try {
			return doInTransaction(false, new TxCallback<DeadlineResponse>() {
				@Override
				public DeadlineResponse execute(TransactionStatus status) throws AccessDeniedException {

					// on va chercher le contribuable et la déclaration
					final Tiers tiers = context.tiersService.getTiers(partyNo);
					if (tiers == null) {
						throw new TiersNotFoundException(partyNo);
					}

					if (!(tiers instanceof Contribuable)) {
						throw new ObjectNotFoundException("Le tiers donné n'est pas un contribuable.");
					}
					final Contribuable ctb = (Contribuable) tiers;

					if (ctb instanceof ContribuableImpositionPersonnesPhysiques) {
						WebServiceHelper.checkAccess(context.securityProvider, Role.DI_DELAI_PP);
					}
					else if (ctb instanceof Entreprise) {
						WebServiceHelper.checkAccess(context.securityProvider, Role.DI_DELAI_PM);
					}

					final DeclarationImpotOrdinaire di = findDeclaration(ctb, pf, seqNo);
					if (di == null) {
						throw new ObjectNotFoundException("Déclaration d'impôt inexistante.");
					}

					try {
						// on ajoute le délai
						context.diService.ajouterDelaiDI(di, dateObtention, nouveauDelai, EtatDelaiDocumentFiscal.ACCORDE, null);
						return new DeadlineResponse(DeadlineStatus.OK, null);
					}
					catch (AjoutDelaiDeclarationException e) {
						return new DeadlineResponse(getDeadlineStatus(e.getRaison()), e.getMessage());
					}
				}
			});
		}
		catch (TxCallbackException e) {
			throw (AccessDeniedException) e.getCause();
		}
	}

	@NotNull
	private static DeadlineStatus getDeadlineStatus(@NotNull AjoutDelaiDeclarationException.Raison raison) {
		switch (raison) {
		case DECLARATION_ANNULEE:
			return DeadlineStatus.ERROR_CANCELLED_DECLARATION;
		case DATE_OBTENTION_INVALIDE:
			return DeadlineStatus.ERROR_INVALID_GRANTED_ON;
		case DELAI_DEJA_EXISTANT:
		case DATE_DELAI_INVALIDE:
			return DeadlineStatus.ERROR_INVALID_DEADLINE;
		case MAUVAIS_ETAT_DECLARATION:
			return DeadlineStatus.ERROR_BAD_DECLARATION_STATUS;
		default:
			throw new IllegalArgumentException("Type de raison inconnu = [" + raison + "]");
		}
	}

	/**
	 * Le type de demande de validation de délais
	 */
	enum TypeDemande {
		UNITAIRE,
		GROUPEE
	}

	@NotNull
	@Override
	public GroupDeadlineValidationResponse validateGroupDeadlineRequest(@NotNull GroupDeadlineValidationRequest request, @NotNull RegDate today) {
		final int periodeFiscale = request.getTaxPeriod();
		final List<Integer> ctbIds = request.getTaxPayerNumber();
		final TypeDemande typeDemande = ctbIds.size() > 1 ? TypeDemande.GROUPEE : TypeDemande.UNITAIRE;

		return doInTransaction(true, status -> {
			final List<ValidationResult> validationResults = ctbIds.stream()
					.map(id -> validateDeadlineRequest(periodeFiscale, id, typeDemande, today))
					.collect(Collectors.toList());
			return new GroupDeadlineValidationResponse(validationResults, 0, null);
		});
	}

	/**
	 * Valide de la demande de délai pour un contribuable et une période fiscale donnés.
	 *
	 * @param periodeFiscale la période fiscale considérée
	 * @param ctbId          le numéro de contribuable considéré
	 * @param typeDemande    le type de demande concernée
	 * @param today          la date du jour
	 * @return la liste des résultats de validation (il y a un résultat par déclaration émise)
	 */
	@NotNull
	ValidationResult validateDeadlineRequest(int periodeFiscale, int ctbId, @NotNull TypeDemande typeDemande, @NotNull RegDate today) {

		// vérification sur le tiers lui-même
		final Tiers tiers = context.tiersDAO.get((long) ctbId);
		if (tiers == null) {
			return buildIneligibleCtbResult(ctbId, null, "Le contribuable n'existe pas.");
		}
		if (!(tiers instanceof ContribuableImpositionPersonnesPhysiques) && !(tiers instanceof Entreprise)) {
			return buildIneligibleCtbResult(ctbId, tiers, "Le tiers n'est pas un contribuable (" + tiers.getType().getDescription() + ").");
		}
		if (tiers.isAnnule()) {
			return buildIneligibleCtbResult(ctbId, tiers, "Le contribuable est annulé.");
		}
		if (!context.validationService.validate(tiers).getErrorsList().isEmpty()) {
			return buildIneligibleCtbResult(ctbId, tiers, "Une incohérence de données sur le contribuable empêche sa modification (validation).");
		}

		// on vérifie si le contribuable est éligible
		final List<PeriodeImposition> periodeImpositions;
		try {
			periodeImpositions = context.periodeImpositionService.determine((Contribuable) tiers, periodeFiscale);
		}
		catch (AssujettissementException e) {
			LOGGER.warn(e.getMessage(), e);
			return buildIneligibleCtbResult(ctbId, tiers, "Impossible de calculer l'assujettissement du contribuable (" + e.getMessage() + ").");
		}
		if (periodeImpositions == null || periodeImpositions.isEmpty()) {
			return buildIneligibleCtbResult(ctbId, tiers, "Le contribuable n'est pas éligible car il n'a pas de période d'imposition en " + periodeFiscale + ".");
		}
		final int nbPeriodes = periodeImpositions.size();
		if (nbPeriodes > 1) {
			return buildIneligibleCtbResult(ctbId, tiers, "Le contribuable n'est pas éligible car il possède plusieurs périodes d'imposition en " + periodeFiscale + ".");
		}

		final PeriodeImposition periodeImposition = periodeImpositions.get(nbPeriodes - 1);
		final TypeContribuable typeContribuable = periodeImposition.getTypeContribuable();
		if (typeContribuable != TypeContribuable.VAUDOIS_ORDINAIRE) {
			return buildIneligibleCtbResult(ctbId, tiers, "Le contribuable n'est pas éligible car il n'est pas assujetti au rôle de manière illimitée en " + periodeFiscale + " (" + typeContribuable.description() + ").");
		}

		final RegDate dateProchainBouclement;
		final ParametreDemandeDelaisOnline paramsDemandeDelaisOnline;
		if (tiers instanceof ContribuableImpositionPersonnesPhysiques) {
			dateProchainBouclement = null;  // par définition, les PPs n'ont pas de date de bouclement
			final RegDate finPeriode = RegDate.get(periodeFiscale, 12, 31);
			if (periodeImposition.getDateFin().isBefore(finPeriode)) {
				return buildIneligibleCtbResult(ctbId, tiers, "Le contribuable n'est pas éligible car il n'est plus imposé en fin de période fiscale " + periodeFiscale + ".");
			}
			paramsDemandeDelaisOnline = parametrePeriodeFiscaleDAO.getParamsDemandeDelaisOnline(periodeFiscale, ParametreDemandeDelaisOnline.Type.PP);
			if (paramsDemandeDelaisOnline == null) {
				return buildIneligibleCtbResult(ctbId, tiers, "Le contribuable n'est pas éligible car il n'y a pas de délai configuré sur la période fiscale " + periodeFiscale + ".");
			}
		}
		else {
			final Entreprise entreprise = (Entreprise) tiers;
			dateProchainBouclement = context.bouclementService.getDateProchainBouclement(entreprise.getBouclements(), periodeImposition.getDateFin(), true);
			if (periodeImposition.getDateFin().isBefore(dateProchainBouclement)) {
				return buildIneligibleCtbResult(ctbId, tiers, "Le contribuable n'est pas éligible car il n'est plus imposé à la date de son prochain bouclement pour la période fiscale " + periodeFiscale + ".");
			}
			paramsDemandeDelaisOnline = parametrePeriodeFiscaleDAO.getParamsDemandeDelaisOnline(periodeFiscale, ParametreDemandeDelaisOnline.Type.PM);
			if (paramsDemandeDelaisOnline == null) {
				return buildIneligibleCtbResult(ctbId, tiers, "Le contribuable n'est pas éligible car il n'y a pas de délai configuré sur la période fiscale " + periodeFiscale + ".");
			}
		}

		// vérification sur les déclarations
		final List<DeclarationImpotOrdinaire> declaration = tiers.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, periodeFiscale, true);
		if (declaration.isEmpty()) {
			return buildRejectedDeadlineResult(ctbId, tiers, true, AUCUNE_DECLARATION.getCode(), "Il n'existe aucune déclaration sur la période " + periodeFiscale + ".");
		}

		final List<DeclarationImpotOrdinaire> declarationValides = declaration.stream() // déclarations non-annulées et triées par ordre chronologique croissant
				.filter(AnnulableHelper::nonAnnule)
				.collect(Collectors.toList());

		if (declarationValides.isEmpty()) {
			// il n'y a pas de déclaration non-annulée
			return buildRejectedDeadlineResult(ctbId, tiers, true, DECLARATION_ANNULEE.getCode(), "La déclaration existante sur la période " + periodeFiscale + " est annulée.");
		}
		if (declarationValides.size() > 1) {
			return buildRejectedDeadlineResult(ctbId, tiers, true, PLUSIEURS_DECLARATIONS.getCode(), "Il existe plusieurs déclarations sur la période " + periodeFiscale + ".");
		}

		// on détermine un résultat pour la déclaration valide
		final DeclarationImpotOrdinaire declarationValide = declarationValides.get(declarationValides.size() - 1);
		return validateDeadlineForDeclaration(periodeFiscale, tiers, declarationValide, dateProchainBouclement, periodeImposition, paramsDemandeDelaisOnline, typeDemande, today);
	}

	/**
	 * Valide la demande de délai pour la déclaration spécifiée.
	 *
	 * @param periodeFiscale            la période fiscale considérée
	 * @param tiers                     le tiers qui possède la déclaration
	 * @param declaration               la déclaration considérée
	 * @param dateProchainBouclement    la date de prochain bouclement (par rapport à la période d'imposition de la déclaration). Obligatoire pour les PMs, null pour les PPs.
	 * @param periodeImposition         la période d'imposition du tiers
	 * @param paramsDemandeDelaisOnline les paramètres de délais pour les demandes de délais online
	 * @param typeDemande               le type de demande
	 * @param today                     la date du jour
	 * @return le résultat de la validation
	 */
	@NotNull
	private ValidationResult validateDeadlineForDeclaration(int periodeFiscale,
	                                                        @NotNull Tiers tiers,
	                                                        @NotNull DeclarationImpotOrdinaire declaration,
	                                                        @Nullable RegDate dateProchainBouclement,
	                                                        @NotNull PeriodeImposition periodeImposition,
	                                                        @NotNull ParametreDemandeDelaisOnline paramsDemandeDelaisOnline,
	                                                        @NotNull TypeDemande typeDemande,
	                                                        @NotNull RegDate today) {

		final ValidationResult result = new ValidationResult();
		result.setTaxPayerNumber(tiers.getNumero().intValue());
		result.setTaxPayerType(DataHelper.getPartyType(tiers.getType()));
		result.setEligible(true);
		result.setTaxDeclaration(new TaxDeclarationInfo(DataHelper.coreToWeb(declaration.getDateDebut()), DataHelper.coreToWeb(declaration.getDateFin()), declaration.getNumero(), null));

		final RegDate dateEmissionDeclaration = declaration.getDateExpedition();
		final TypeEtatDocumentFiscal etat = declaration.getDernierEtat().getEtat();
		final DelaiDeclaration dernierDelaiAccorde = (DelaiDeclaration) declaration.getDernierDelaiAccorde();
		final RegDate delaiDejaAccorde = dernierDelaiAccorde.getDelaiAccordeAu();

		if (etat != TypeEtatDocumentFiscal.EMIS) {
			// déclaration pas dans le bon état
			result.setRejectionReason(buildRejectionReasonPourDeclarationNonEmise(etat, periodeFiscale));
			return result;
		}

		// on va chercher les délais théoriques
		final List<RegDate> delaisTheoriques;
		if (tiers instanceof ContribuableImpositionPersonnesPhysiques) {
			delaisTheoriques = getDelaisTheoriquesPP(typeDemande, paramsDemandeDelaisOnline, today);
		}
		else if (tiers instanceof Entreprise) {
			if (dateProchainBouclement == null) {
				throw new IllegalArgumentException("La date de prochain bouclement n'est pas renseignée sur le PM n°" + tiers.getNumero());
			}
			delaisTheoriques = getDelaisTheoriquesPM(dateEmissionDeclaration, dateProchainBouclement, periodeImposition, typeDemande, paramsDemandeDelaisOnline, today);
		}
		else {
			throw new IllegalArgumentException("Type de tiers non supporté [" + tiers.getClass() + "]");
		}
		if (delaisTheoriques.isEmpty()) {
			// pas de délais théoriques, ce que la demande n'est pas valable (hors délai ou pas de délai configuré)
			return buildIneligibleCtbResult(tiers.getNumero().intValue(), tiers,
			                                "Le contribuable n'est pas éligible car il n'y a pas de délai accordable en date du " +
					                                RegDateHelper.dateToDisplayString(today) + " pour la période fiscale " + periodeFiscale + ".");
		}

		// on ne garde que les délais qui sont réellement accordables
		final List<RegDate> delaisAccordables = delaisTheoriques.stream()
				.filter(delai -> isDelaiAccordable(delai, dernierDelaiAccorde))
				.collect(Collectors.toList());
		if (delaisAccordables.isEmpty()) {
			// pas de nouveau délai accordable, ce que le délai déjà accordé est suffisant
			result.setRejectionReason(new RejectionReason(DELAI_DEJA_ACCORDE.getCode(), "Il y a déjà un délai accordé au " + RegDateHelper.dateToDisplayString(delaiDejaAccorde) + ".", null));
			return result;
		}

		// on a un ou des délais accordables, tout va bien
		result.getProposedDeadlines().addAll(delaisAccordables.stream()
				                                     .map(DataHelper::coreToWeb)
				                                     .collect(Collectors.toList()));
		return result;
	}

	/**
	 * Détermine si le délai spécifié peut réellement être accordé.
	 */
	private static boolean isDelaiAccordable(@NotNull RegDate delaiCandidat, @NotNull DelaiDeclaration dernierDelaiAccorde) {
		final RegDate delaiDejaAccorde = dernierDelaiAccorde.getDelaiAccordeAu();
		// le délai candidat est plus loin que le dernier délai existant -> OK
		return delaiCandidat.isAfter(delaiDejaAccorde) ||
				// [FISCPROJ-873] le délai candidat est identique au délai existant *et* le délai existant est implicite -> OK aussi
				(delaiCandidat == delaiDejaAccorde && dernierDelaiAccorde.getTypeDelai() == TypeDelaiDeclaration.IMPLICITE);
	}

	/**
	 * Calcule les délais théoriques qui peuvent être accordés sur une DI d'une personne physique.
	 *
	 * @param typeDemande le type de demande
	 * @param parametres  les paramètres de délais pour les demandes de délais online PP
	 * @param today       la date du jour
	 * @return les délais théoriques calculés
	 */
	@NotNull
	private List<RegDate> getDelaisTheoriquesPP(@NotNull TypeDemande typeDemande, @NotNull ParametreDemandeDelaisOnline parametres, @NotNull RegDate today) {

		if (parametres.getTypeTiers() != ParametreDemandeDelaisOnline.Type.PP) {
			throw new IllegalArgumentException();
		}

		// on va chercher les délais pour la période courante
		final DelaisAccordablesOnlineDIPP paramsPeriode = parametres.getPeriodesDelais().stream()
				.map(DelaisAccordablesOnlineDIPP.class::cast)
				.filter(d -> d.isValidAt(today))
				.findFirst()
				.orElse(null);
		if (paramsPeriode == null) {
			// si un contribuable demande un délai en 2019 pour une DI 2017, il n'y aura pas de période correspondante
			return Collections.emptyList();
		}

		final List<DayMonth> delais;
		switch (typeDemande) {
		case UNITAIRE:
			delais = paramsPeriode.getDelaisDemandeUnitaire();
			break;
		case GROUPEE:
			delais = paramsPeriode.getDelaisDemandeGroupee();
			break;
		default:
			throw new IllegalArgumentException("Type de demande inconnu = [" + typeDemande + "]");
		}

		// on converti les délais en dates
		return delais.stream()
				.map(d -> d.nextAfterOrEqual(today))
				.sorted()
				.distinct()
				.collect(Collectors.toList());
	}

	/**
	 * Calcule les délais théoriques qui peuvent être accordés sur une DI d'une personne morale.
	 *
	 * @param dateEmissionDeclaration la date d'émission de la déclaration
	 * @param dateProchainBouclement  la prochaine date de bouclement
	 * @param periodeImposition       la période d'imposition qui correspond à la déclaration spécifiée
	 * @param typeDemande             le type de demande
	 * @param parametres              les paramètres de délais pour les demandes de délais online PP
	 * @param today                   la date du jour
	 * @return les délais théoriques calculés
	 */
	@NotNull
	private List<RegDate> getDelaisTheoriquesPM(@NotNull RegDate dateEmissionDeclaration,
	                                            @NotNull RegDate dateProchainBouclement,
	                                            @NotNull PeriodeImposition periodeImposition,
	                                            @NotNull TypeDemande typeDemande,
	                                            @NotNull ParametreDemandeDelaisOnline parametres,
	                                            @NotNull RegDate today) {

		if (parametres.getTypeTiers() != ParametreDemandeDelaisOnline.Type.PM) {
			throw new IllegalArgumentException();
		}

		// on détermine la date de référence (date d'émission de la DI ou date de bouclement de la PM) à partir des paramètres de la PF
		final RegDate dateReference;
		final TypeContribuable typeContribuable = periodeImposition.getTypeContribuable();
		final PeriodeFiscale pf = parametres.getPeriodefiscale();
		final ParametrePeriodeFiscalePM params = pf.getParametrePeriodeFiscalePM(typeContribuable);
		if (params == null) {
			throw new IllegalArgumentException("Pas de paramètrage trouvé pour le type de contribuable " + typeContribuable + " et la PF " + pf.getAnnee());
		}
		switch (params.getReferenceDelaiInitial()) {
		case EMISSION:
			dateReference = dateEmissionDeclaration;
			break;
		case FIN_PERIODE:
			dateReference = dateProchainBouclement;
			break;
		default:
			throw new IllegalArgumentException("Valeur inconnue pour le type de référence du délai initial : " + params.getReferenceDelaiInitial());
		}

		// on va chercher les délais pour la période courante
		final List<DelaisAccordablesOnlineDIPM> list = parametres.getPeriodesDelais().stream()
				.map(DelaisAccordablesOnlineDIPM.class::cast)
				.sorted(Comparator.comparingInt(DelaisAccordablesOnlineDIPM::getIndex))
				.filter(d -> today.isAfterOrEqual(d.getDelaiDebut().apply(dateReference)))
				.collect(Collectors.toList());
		if (list.isEmpty()) {
			// pas de période paramétrée, pas de délai
			return Collections.emptyList();
		}
		final DelaisAccordablesOnlineDIPM paramsPeriode = list.get(list.size() - 1); // on prend le plus récent des valides (les délais PM n'ont pas de date de "fin")

		final List<Delai> delais;
		switch (typeDemande) {
		case UNITAIRE:
			delais = paramsPeriode.getDelaisDemandeUnitaire();
			break;
		case GROUPEE:
			delais = paramsPeriode.getDelaisDemandeGroupee();
			break;
		default:
			throw new IllegalArgumentException("Type de demande inconnu = [" + typeDemande + "]");
		}

		// on converti les délais en dates
		return delais.stream()
				.map(d -> d.apply(dateReference))
				.sorted()
				.distinct()
				.collect(Collectors.toList());
	}

	/**
	 * Construit une réponse négative pour le cas où c'est le contribuable qui n'est pas éligible.
	 *
	 * @param ctbId   le numéro du tiers considéré
	 * @param tiers   le contribuable considéré
	 * @param message le message de la réponse négative
	 * @return la réponse construite
	 */
	private static ValidationResult buildIneligibleCtbResult(int ctbId, @Nullable Tiers tiers, @NotNull String message) {
		final ValidationResult result = new ValidationResult();
		result.setTaxPayerNumber(ctbId);
		result.setTaxPayerType(tiers == null ? null : DataHelper.getPartyType(tiers.getType()));
		result.setEligible(false);
		result.setNonEligibleReason(message);
		return result;
	}

	/**
	 * Construit une réponse négative pour une demande de validation d'un ajout de délai
	 *
	 * @param ctbId    le numéro du tiers considéré
	 * @param tiers    le contribuable considéré
	 * @param eligible si le contribuable est éligible
	 * @param code     le code de la réponse négative
	 * @param message  le message de la réponse négative
	 * @return la réponse construite
	 */
	private static ValidationResult buildRejectedDeadlineResult(int ctbId, @Nullable Tiers tiers, boolean eligible, @NotNull String code, @NotNull String message) {
		final ValidationResult result = new ValidationResult();
		result.setTaxPayerNumber(ctbId);
		result.setTaxPayerType(tiers == null ? null : DataHelper.getPartyType(tiers.getType()));
		result.setEligible(eligible);
		result.setRejectionReason(new RejectionReason(code, message, null));
		return result;
	}

	@NotNull
	private static RejectionReason buildRejectionReasonPourDeclarationNonEmise(@NotNull TypeEtatDocumentFiscal etat, int periodeFiscale) {
		final RejectionReason reason;
		switch (etat) {
		case RETOURNE:
			reason = new RejectionReason(DECLARATION_RETOURNEE.getCode(), "La déclaration est déjà retournée sur la période " + periodeFiscale + ".", null);
			break;
		case RAPPELE:
			reason = new RejectionReason(DECLARATION_RAPPELEE.getCode(), "La déclaration est déjà rappelée sur la période " + periodeFiscale + ".", null);
			break;
		case SOMME:
			reason = new RejectionReason(DECLARATION_SOMMEE.getCode(), "La déclaration est déjà sommée sur la période " + periodeFiscale + ".", null);
			break;
		case SUSPENDU:
			reason = new RejectionReason(DECLARATION_SUSPENDUE.getCode(), "La déclaration est suspendue sur la période " + periodeFiscale + ".", null);
			break;
		case ECHU:
			reason = new RejectionReason(DECLARATION_ECHUE.getCode(), "La déclaration est échue sur la période " + periodeFiscale + ".", null);
			break;
		default:
			throw new ProgrammingException("On ne devrait jamais arriver ici");
		}
		return reason;
	}

	@Override
	public TaxOffices getTaxOffices(final int municipalityId, @Nullable final RegDate date) {
		return doInTransaction(true, transactionStatus -> TaxOfficesBuilder.newTaxOffices(municipalityId, date, context));
	}

	@Override
	public PartyNumberList getModifiedTaxPayers(final Date since, final Date until) {
		return doInTransaction(true, status -> {
			final List<Long> longList = context.tiersDAO.getListeCtbModifies(since, until);
			final List<Integer> intList = new ArrayList<>(longList.size());
			for (Long id : longList) {
				// [SIPM] il faut écarter les établissements (les identifiants ne sont pas utilisables avec GetParty/GetParties) et ils étaient de fait écartés auparavant car il n'y en avait pas...
				if (id != null && (id < Etablissement.ETB_GEN_FIRST_ID || id > Etablissement.ETB_GEN_LAST_ID)) {
					intList.add(id.intValue());
				}
			}
			return new PartyNumberList(intList);
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
	public DebtorInfo getDebtorInfo(final int debtorNo, final int pf) {
		return doInTransaction(true, status -> {
			final Tiers tiers = context.tiersDAO.get(debtorNo, false);
			if (!(tiers instanceof DebiteurPrestationImposable)) {
				throw new ObjectNotFoundException("Pas de débiteur de prestation imposable avec le numéro " + debtorNo);
			}

			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
			final List<? extends DateRange> lrEmises = dpi.getDeclarationsDansPeriode(DeclarationImpotSource.class, pf, false);
			final List<DateRange> lrManquantes = context.lrService.findLRsManquantes(dpi, RegDate.get(pf, 12, 31), new ArrayList<>());
			final List<DateRange> lrManquantesInPf = extractIntersecting(lrManquantes, new DateRangeHelper.Range(RegDate.get(pf, 1, 1), RegDate.get(pf, 12, 31)));
			return new DebtorInfo(debtorNo, pf, lrManquantesInPf.size() + lrEmises.size(), lrEmises.size(), null);
		});
	}

	@Override
	public List<PartyInfo> searchParty(@Nullable String partyNo, @Nullable String name, SearchMode nameSearchMode, @Nullable String townOrCountry,
	                                   @Nullable RegDate dateOfBirth, @Nullable String socialInsuranceNumber, @Nullable String uidNumber, @Nullable Integer taxResidenceFSOId,
	                                   boolean onlyActiveMainTaxResidence, @Nullable Set<PartySearchType> partyTypes, @Nullable DebtorCategory debtorCategory,
	                                   @Nullable Boolean activeParty, @Nullable Long oldWithholdingNumber) throws IndexerException {
		final TiersCriteria criteria = new TiersCriteria();
		if (StringUtils.isNotBlank(partyNo)) {
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
			criteria.setDateNaissanceInscriptionRC(dateOfBirth);
			criteria.setNumeroAVS(StringUtils.trimToNull(socialInsuranceNumber));
			if (taxResidenceFSOId != null) {
				criteria.setNoOfsFor(Integer.toString(taxResidenceFSOId));
				criteria.setForPrincipalActif(onlyActiveMainTaxResidence);
			}
			criteria.setTypesTiers(EnumHelper.toCore(partyTypes));
			criteria.setCategorieDebiteurIs(ch.vd.unireg.xml.EnumHelper.xmlToCore(debtorCategory));
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
			if (data != null
					&& (data.getCategorieImpotSource() == null || CIS_SUPPORTEES.contains(data.getCategorieImpotSource()))
					&& (data.getTypeAvatar() == null || !TA_IGNORES.contains(data.getTypeAvatar()))) {
				final PartyInfo info = ch.vd.unireg.xml.DataHelper.coreToXMLv5(data);
				result.add(info);
			}
		}
		return result;
	}

	private interface PartyFactory<T extends Tiers> {
		Party buildParty(T tiers, @Nullable Set<InternalPartyPart> parts, Context context) throws ServiceException;
	}

	private static <T extends Tiers> void addToPartyFactoryMap(Map<Class<? extends Tiers>, PartyFactory<?>> map, Class<T> clazz, PartyFactory<T> factory) {
		map.put(clazz, factory);
	}

	private static Map<Class<? extends Tiers>, PartyFactory<?>> buildPartyFactoryMap() {
		final Map<Class<? extends Tiers>, PartyFactory<?>> map = new HashMap<>();
		addToPartyFactoryMap(map, PersonnePhysique.class, new NaturalPersonPartyFactory());
		addToPartyFactoryMap(map, MenageCommun.class, new CommonHouseholdPartyFactory());
		addToPartyFactoryMap(map, DebiteurPrestationImposable.class, new DebtorPartyFactory());
		addToPartyFactoryMap(map, Entreprise.class, new CorporationPartyFactory());
		addToPartyFactoryMap(map, CollectiviteAdministrative.class, new AdministrativeAuthorityPartyFactory());
		addToPartyFactoryMap(map, AutreCommunaute.class, new OtherCommunityPartyFactory());
		addToPartyFactoryMap(map, Etablissement.class, new EstablishmentPartyFactory());
		return map;
	}

	private static final class NaturalPersonPartyFactory implements PartyFactory<PersonnePhysique> {
		@Override
		public Party buildParty(PersonnePhysique pp, @Nullable Set<InternalPartyPart> parts, Context context) throws ServiceException {
			return PartyBuilder.newNaturalPerson(pp, parts, context);
		}
	}

	private static final class CommonHouseholdPartyFactory implements PartyFactory<MenageCommun> {
		@Override
		public Party buildParty(MenageCommun mc, @Nullable Set<InternalPartyPart> parts, Context context) throws ServiceException {
			return PartyBuilder.newCommonHousehold(mc, parts, context);
		}
	}

	private static final class DebtorPartyFactory implements PartyFactory<DebiteurPrestationImposable> {
		@Override
		public Party buildParty(DebiteurPrestationImposable dpi, @Nullable Set<InternalPartyPart> parts, Context context) throws ServiceException {
			return PartyBuilder.newDebtor(dpi, parts, context);
		}
	}

	private static final class CorporationPartyFactory implements PartyFactory<Entreprise> {
		@Override
		public Party buildParty(Entreprise pm, @Nullable Set<InternalPartyPart> parts, Context context) throws ServiceException {
			return PartyBuilder.newCorporation(pm, parts, context);
		}
	}

	private static final class AdministrativeAuthorityPartyFactory implements PartyFactory<CollectiviteAdministrative> {
		@Override
		public Party buildParty(CollectiviteAdministrative ca, @Nullable Set<InternalPartyPart> parts, Context context) throws ServiceException {
			return PartyBuilder.newAdministrativeAuthority(ca, parts, context);
		}
	}

	private static final class OtherCommunityPartyFactory implements PartyFactory<AutreCommunaute> {
		@Override
		public Party buildParty(AutreCommunaute ac, @Nullable Set<InternalPartyPart> parts, Context context) throws ServiceException {
			return PartyBuilder.newOtherCommunity(ac, parts, context);
		}
	}

	private static final class EstablishmentPartyFactory implements PartyFactory<Etablissement> {
		@Override
		public Party buildParty(Etablissement etb, @Nullable Set<InternalPartyPart> parts, Context context) throws ServiceException {
			return PartyBuilder.newEstablishment(etb, parts, context);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends Tiers> Party buildParty(Tiers tiers, @Nullable Set<InternalPartyPart> parts, Context context) throws ServiceException {
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

	@Nullable
	@Override
	public Party getParty(final int partyNo, @Nullable final Set<InternalPartyPart> parts) throws ServiceException {
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

	@NotNull
	private static Set<TiersDAO.Parts> toCoreAvecForsFiscaux(@Nullable Set<InternalPartyPart> parts) {
		Set<TiersDAO.Parts> set = ch.vd.unireg.xml.DataHelper.xmlToCoreV5(parts);
		if (set == null) {
			set = EnumSet.noneOf(TiersDAO.Parts.class);
		}
		// les fors fiscaux sont nécessaires pour déterminer les dates de début et de fin d'activité.
		set.add(TiersDAO.Parts.FORS_FISCAUX);
		return set;
	}

	@NotNull
	private List<Entry> resolvePartyBatch(@NotNull List<Integer> ids, @Nullable Set<InternalPartyPart> parts) {

		final List<Entry> entries = new ArrayList<>(ids.size());

		// vérification des droits d'accès et de l'existence des tiers
		final Iterator<Integer> idIterator = ids.iterator();
		final Set<Long> idLongSet = new HashSet<>(ids.size());
		while (idIterator.hasNext()) {
			final int id = idIterator.next();
			try {
				WebServiceHelper.checkPartyReadAccess(context.securityProvider, id);
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
				catch (ServiceException e) {
					final ErrorType errorType;
					if (e.getInfo() instanceof BusinessExceptionInfo) {
						errorType = ErrorType.BUSINESS;
					}
					else if (e.getInfo() instanceof AccessDeniedExceptionInfo) {
						errorType = ErrorType.ACCESS;
					}
					else {
						errorType = ErrorType.TECHNICAL;
					}

					LOGGER.error("Exception au mapping xml du tiers " + t.getNumero(), e);
					entries.add(new Entry(t.getNumero().intValue(), null, new ch.vd.unireg.xml.error.v1.Error(errorType, e.getMessage())));
				}
				catch (ObjectNotFoundException e) {
					LOGGER.error("Exception au mapping xml du tiers " + t.getNumero(), e);
					entries.add(new Entry(t.getNumero().intValue(), null, new ch.vd.unireg.xml.error.v1.Error(ErrorType.BUSINESS, e.getMessage())));
				}
				catch (Exception e) {
					LOGGER.error("Exception au mapping xml du tiers " + t.getNumero(), e);
					entries.add(new Entry(t.getNumero().intValue(), null, new ch.vd.unireg.xml.error.v1.Error(ErrorType.TECHNICAL, e.getMessage())));
				}
				idLongSet.remove(t.getNumero());
			}
		}

		// le reliquat sont des erreurs techniques (= bugs ?)
		for (long id : idLongSet) {
			entries.add(new Entry((int) id, null, new ch.vd.unireg.xml.error.v1.Error(ErrorType.TECHNICAL, "Erreur inattendue.")));
		}

		return entries;
	}

	@NotNull
	@Override
	public Parties getParties(List<Integer> partyNos, @Nullable Set<InternalPartyPart> parts) {

		// on enlève les doublons sur les numéros de tiers (et les éventuels <i>null</i>)
		final Set<Integer> nos = new HashSet<>(partyNos);
		nos.remove(null);

		if (nos.size() > MAX_BATCH_SIZE) {
			throw new BadRequestException("Le nombre de tiers demandés ne peut dépasser " + MAX_BATCH_SIZE);
		}

		final String currentPrincipal = AuthenticationHelper.getCurrentPrincipal();
		final Integer currentOID = AuthenticationHelper.getCurrentOID();
		if (currentOID == null) {
			throw new IllegalArgumentException("L'OID courant de l'utilisateur [" + currentPrincipal + "] n'est pas défini.");
		}

		// on découpe les ids en lots
		final List<List<Integer>> batches = new ArrayList<>();
		final BatchIterator<Integer> iterator = new StandardBatchIterator<>(nos, PARTIES_BATCH_SIZE);
		while (iterator.hasNext()) {
			batches.add(iterator.next());
		}

		// on charge les données sur plusieurs threads
		final List<List<Entry>> batchEntries = CollectionsUtils.parallelMap(batches, batch -> {
			// chaque thread doit s'exécuter dans une transaction propre
			return executeInReadOnlyTx(currentPrincipal, currentOID, status -> {
				// on ne veut vraiment pas modifier la base
				return executeInManualFlush(session -> resolvePartyBatch(batch, parts));
			});
		}, threadPool);

		// on construit le résultat final
		final Parties parties = new Parties();
		final List<Entry> partiesEntries = parties.getEntries();
		batchEntries.forEach(partiesEntries::addAll);
		return parties;
	}

	@Override
	public CommunityOfHeirs getCommunityOfHeirs(int deceasedId) {
		return doInTransaction(true, status ->
				Optional.ofNullable(context.tiersDAO.get((long) deceasedId))
						.map(CommunityOfHeirsBuilder::newCommunity)
						.orElse(null));
	}

	@Override
	public ImageData getAvatar(final int partyNo) throws ServiceException {
		try {
			return doInTransaction(true, new TxCallback<ImageData>() {
				@Override
				public ImageData execute(TransactionStatus status) {
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

	@Override
	public FiscalEvents getFiscalEvents(final int partyNo) {
		return doInTransaction(true, status -> {
			final Tiers tiers = context.tiersDAO.get(partyNo, false);
			if (tiers == null) {
				throw new ObjectNotFoundException("Le tiers " + partyNo + " n'existe pas");
			}

			final Collection<EvenementFiscal> evts = context.evenementFiscalService.getEvenementsFiscaux(tiers);
			final List<EvenementFiscal> sortedList;
			if (evts == null || evts.isEmpty()) {
				sortedList = Collections.emptyList();
			}
			else {
				sortedList = AnnulableHelper.sansElementsAnnules(evts);     // on n'est jamais trop prudent...
				sortedList.sort((o1, o2) -> {
					int comparison = NullDateBehavior.EARLIEST.compare(o1.getDateValeur(), o2.getDateValeur());
					if (comparison == 0) {
						comparison = o1.getLogCreationDate().compareTo(o2.getLogCreationDate());
					}
					return comparison;
				});
			}

			final List<FiscalEvent> list = new ArrayList<>(sortedList.size());
			for (EvenementFiscal evtFiscal : sortedList) {
				final ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscal xml = EvenementFiscalV3Factory.buildOutputData(evtFiscal);
				list.add(new FiscalEvent(evtFiscal.getLogCreationUser(),
				                         XmlUtils.date2cal(evtFiscal.getLogCreationDate()),
				                         EvenementFiscalDescriptionHelper.getTextualDescription(evtFiscal),
				                         xml,
				                         null));
			}
			return new FiscalEvents(list, 0, null);
		});
	}

	@Nullable
	@Override
	public ImmovableProperty getImmovableProperty(long immoId) {
		return doInTransaction(true, status ->
				Optional.ofNullable(context.registreFoncierService.getImmeuble(immoId))
						.map((immeuble) -> ImmovablePropertyBuilder.newImmovableProperty(immeuble,
						                                                                 context.registreFoncierService::getCapitastraURL,
						                                                                 context.registreFoncierService::getContribuableIdFor,
						                                                                 new EasementRightHolderComparator(context.tiersService)))
						.orElse(null));
	}

	@Nullable
	@Override
	public ImmovableProperty getImmovablePropertyByLocation(int municipalityFsoId, int parcelNumber, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3) {
		return doInTransaction(true, status ->
				Optional.ofNullable(context.registreFoncierService.getImmeuble(municipalityFsoId, parcelNumber, index1, index2, index3))
						.map((immeuble) -> ImmovablePropertyBuilder.newImmovableProperty(immeuble,
						                                                                 context.registreFoncierService::getCapitastraURL,
						                                                                 context.registreFoncierService::getContribuableIdFor,
						                                                                 new EasementRightHolderComparator(context.tiersService)))
						.orElse(null));
	}

	@NotNull
	@Override
	public ImmovablePropertySearchResult findImmovablePropertyByLocation(int municipalityFsoId, int parcelNumber, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3) {
		return doInTransaction(true, status -> {
			final List<ImmovablePropertyInfo> entries = context.registreFoncierService.findImmeublesParSituation(municipalityFsoId, parcelNumber, index1, index2, index3).stream()
					.map(ImmovablePropertyInfoBuilder::newInfo)
					.collect(Collectors.toList());
			return new ImmovablePropertySearchResult(entries, 0, null);
		});
	}

	@NotNull
	@Override
	public ImmovablePropertyList getImmovableProperties(List<Long> immoIds) {

		if (immoIds.size() > MAX_BATCH_SIZE) {
			throw new BadRequestException("Le nombre d'immeubles demandés ne peut dépasser " + MAX_BATCH_SIZE);
		}

		final String currentPrincipal = AuthenticationHelper.getCurrentPrincipal();
		final Integer currentOID = AuthenticationHelper.getCurrentOID();
		if (currentOID == null) {
			throw new IllegalArgumentException("L'OID courant de l'utilisateur [" + currentPrincipal + "] n'est pas défini.");
		}

		// on charge les données sur plusieurs threads
		final List<ImmovablePropertyEntry> entries = CollectionsUtils.parallelMap(immoIds, immoId -> {
			//noinspection CodeBlock2Expr
			return executeInReadOnlyTx(currentPrincipal, currentOID, status -> resolveImmovablePropertyEntry(immoId));
		}, threadPool);

		// on retourne la liste triée
		entries.sort(Comparator.comparing(ImmovablePropertyEntry::getImmovablePropertyId));
		return new ImmovablePropertyList(entries);
	}

	@NotNull
	private ImmovablePropertyEntry resolveImmovablePropertyEntry(long immoId) {
		try {
			final ImmeubleRF immeuble = context.registreFoncierService.getImmeuble(immoId);
			if (immeuble == null) {
				return new ImmovablePropertyEntry(immoId, null, new ch.vd.unireg.xml.error.v1.Error(ErrorType.BUSINESS, "L'immeuble n°[" + immoId + "] n'existe pas."));
			}
			else {
				final ImmovableProperty immovableProperty = ImmovablePropertyBuilder.newImmovableProperty(immeuble,
				                                                                                          context.registreFoncierService::getCapitastraURL,
				                                                                                          context.registreFoncierService::getContribuableIdFor,
				                                                                                          new EasementRightHolderComparator(context.tiersService));
				return new ImmovablePropertyEntry(immoId, immovableProperty, null);
			}
		}
		catch (Exception e) {
			return new ImmovablePropertyEntry(immoId, null, new ch.vd.unireg.xml.error.v1.Error(ErrorType.TECHNICAL, e.getMessage()));
		}
	}

	@Nullable
	@Override
	public Building getBuilding(long buildingId) {
		return doInTransaction(true, status ->
				Optional.ofNullable(context.registreFoncierService.getBatiment(buildingId))
						.map(BuildingBuilder::newBuilding)
						.orElse(null));
	}

	@NotNull
	@Override
	public BuildingList getBuildings(List<Long> buildingIds) {

		if (buildingIds.size() > MAX_BATCH_SIZE) {
			throw new BadRequestException("Le nombre de bâtiments demandés ne peut dépasser " + MAX_BATCH_SIZE);
		}

		final String currentPrincipal = AuthenticationHelper.getCurrentPrincipal();
		final Integer currentOID = AuthenticationHelper.getCurrentOID();
		if (currentOID == null) {
			throw new IllegalArgumentException("L'OID courant de l'utilisateur [" + currentPrincipal + "] n'est pas défini.");
		}

		// on charge les données sur plusieurs threads
		final List<BuildingEntry> entries = CollectionsUtils.parallelMap(buildingIds, buildingId -> {
			//noinspection CodeBlock2Expr
			return executeInReadOnlyTx(currentPrincipal, currentOID, status -> resolveBuildingEntry(buildingId));
		}, threadPool);

		// on retourne la liste triée
		entries.sort(Comparator.comparing(BuildingEntry::getBuildingId));
		return new BuildingList(entries);
	}

	private BuildingEntry resolveBuildingEntry(long batimentId) {
		try {
			final BatimentRF batiment = context.registreFoncierService.getBatiment(batimentId);
			if (batiment == null) {
				return new BuildingEntry(batimentId, null, new ch.vd.unireg.xml.error.v1.Error(ErrorType.BUSINESS, "Le bâtiment n°[" + batimentId + "] n'existe pas."));
			}
			else {
				final Building immovableProperty = BuildingBuilder.newBuilding(batiment);
				return new BuildingEntry(batimentId, immovableProperty, null);
			}
		}
		catch (Exception e) {
			return new BuildingEntry(batimentId, null, new ch.vd.unireg.xml.error.v1.Error(ErrorType.TECHNICAL, e.getMessage()));
		}
	}

	@Nullable
	@Override
	public CommunityOfOwners getCommunityOfOwners(long communityId) {
		return doInTransaction(true, status ->
				Optional.ofNullable(context.registreFoncierService.getCommunaute(communityId))
						.map(communaute -> CommunityOfOwnersBuilder.newCommunity(communaute,
						                                                         context.registreFoncierService::getContribuableIdFor,
						                                                         context.registreFoncierService::getCommunauteMembreInfo))
						.orElse(null));
	}

	@NotNull
	@Override
	public CommunityOfOwnersList getCommunitiesOfOwners(List<Long> communityIds) {

		if (communityIds.size() > MAX_BATCH_SIZE) {
			throw new BadRequestException("Le nombre de communautés demandées ne peut dépasser " + MAX_BATCH_SIZE);
		}

		final String currentPrincipal = AuthenticationHelper.getCurrentPrincipal();
		final Integer currentOID = AuthenticationHelper.getCurrentOID();
		if (currentOID == null) {
			throw new IllegalArgumentException("L'OID courant de l'utilisateur [" + currentPrincipal + "] n'est pas défini.");
		}

		// on charge les données sur plusieurs threads
		final List<CommunityOfOwnersEntry> entries = CollectionsUtils.parallelMap(communityIds, communityId -> {
			//noinspection CodeBlock2Expr
			return executeInReadOnlyTx(currentPrincipal, currentOID, status -> resolveCommunityEntry(communityId));
		}, threadPool);

		// on retourne le résultat global
		entries.sort(Comparator.comparing(CommunityOfOwnersEntry::getCommunityOfOwnersId));
		return new CommunityOfOwnersList(entries);
	}

	private CommunityOfOwnersEntry resolveCommunityEntry(long communityId) {
		try {
			final CommunauteRF communaute = context.registreFoncierService.getCommunaute(communityId);
			if (communaute == null) {
				return new CommunityOfOwnersEntry(communityId, null, new ch.vd.unireg.xml.error.v1.Error(ErrorType.BUSINESS, "La communauté n°[" + communityId + "] n'existe pas."));
			}
			else {
				final CommunityOfOwners community = CommunityOfOwnersBuilder.newCommunity(communaute,
				                                                                          context.registreFoncierService::getContribuableIdFor,
				                                                                          context.registreFoncierService::getCommunauteMembreInfo);
				return new CommunityOfOwnersEntry(communityId, community, null);
			}
		}
		catch (Exception e) {
			return new CommunityOfOwnersEntry(communityId, null, new ch.vd.unireg.xml.error.v1.Error(ErrorType.TECHNICAL, e.getMessage()));
		}
	}

	@Override
	public @NotNull String getSwaggerJson() throws IOException {
		final File file = ResourceUtils.getFile("classpath:ws-v7-swagger.json"); // note : le fichier 'ws-v7-swagger.json' est généré par le plugin 'swagger-maven-plugin' lors du build de /ws
		return FileUtils.readFileToString(file);
	}

	private <T> T executeInReadOnlyTx(@NotNull String currentPrincipal, int currentOID, @NotNull TransactionCallback<T> callback) {
		AuthenticationHelper.pushPrincipal(currentPrincipal, currentOID);
		try {
			return doInTransaction(true, callback);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private <T> T executeInManualFlush(@NotNull HibernateCallback<T> callback) {
		return context.hibernateTemplate.execute(FlushMode.MANUAL, callback);
	}
}

