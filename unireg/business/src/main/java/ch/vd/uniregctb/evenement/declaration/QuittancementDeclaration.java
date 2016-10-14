package ch.vd.uniregctb.evenement.declaration;

import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.xml.event.declaration.ack.v2.DeclarationAck;
import ch.vd.unireg.xml.event.declaration.v2.DeclarationIdentifier;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.declaration.DeclarationAvecNumeroSequence;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.snc.QuestionnaireSNCService;
import ch.vd.uniregctb.jms.BamMessageHelper;
import ch.vd.uniregctb.jms.BamMessageSender;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.jms.EsbMessageHelper;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.validation.ValidationService;
import ch.vd.uniregctb.xml.DataHelper;

public class QuittancementDeclaration implements EvenementDeclarationHandler<DeclarationAck>, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(QuittancementDeclaration.class);

	private TiersDAO tiersDAO;
	private ValidationService validationService;
	private DeclarationImpotService diService;
	private QuestionnaireSNCService qsncService;
	private BamMessageSender bamMessageSender;

	private Map<Class<? extends DeclarationAvecNumeroSequence>, Quittanceur<?>> quittanceurs;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	public void setDiService(DeclarationImpotService diService) {
		this.diService = diService;
	}

	public void setQsncService(QuestionnaireSNCService qsncService) {
		this.qsncService = qsncService;
	}

	public void setBamMessageSender(BamMessageSender bamMessageSender) {
		this.bamMessageSender = bamMessageSender;
	}

	/**
	 * Interface interne qui permet d'abstraire le quittancement selon le type de déclaration
	 * @param <T> type de déclaration
	 */
	private interface Quittanceur<T extends DeclarationAvecNumeroSequence> {
		/**
		 * Quittance la déclaration donnée
		 * @param declaration la déclaration
		 * @param date la date de la quittance
		 * @param source la source du quittancement
		 * @throws DeclarationException en cas de souci
		 */
		void quittance(T declaration, RegDate date, String source) throws DeclarationException;
	}

	/**
	 * Méthode d'enregistrement d'un quittanceur pour conserver un peu de 'type-safety'...
	 * @param map la map dans laquelle les quittanceurs sont enregistrés
	 * @param declarationClazz la classe (concrète) de déclaration
	 * @param quittanceur le quittanceur associé
	 * @param <T> le type de déclaration
	 */
	private static <T extends DeclarationAvecNumeroSequence> void registerQuittanceur(Map<Class<? extends DeclarationAvecNumeroSequence>, Quittanceur<?>> map,
	                                                                                  Class<T> declarationClazz,
	                                                                                  Quittanceur<? super T> quittanceur) {
		if (Modifier.isAbstract(declarationClazz.getModifiers())) {
			throw new IllegalArgumentException("La classe de déclaration acceptée ici doit être concrète.");
		}
		map.put(declarationClazz, quittanceur);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final Quittanceur<DeclarationImpotOrdinaire> quittanceurDI = (declaration, date, source) -> diService.quittancementDI(declaration.getTiers(), declaration, date, source, true);
		final Quittanceur<QuestionnaireSNC> quittanceurQSNC = (declaration, date, source) -> qsncService.quittancerQuestionnaire(declaration, date, source);

		final Map<Class<? extends DeclarationAvecNumeroSequence>, Quittanceur<?>> map = new HashMap<>();
		registerQuittanceur(map, DeclarationImpotOrdinairePP.class, quittanceurDI);
		registerQuittanceur(map, DeclarationImpotOrdinairePM.class, quittanceurDI);
		registerQuittanceur(map, QuestionnaireSNC.class, quittanceurQSNC);
		this.quittanceurs = map;
	}

	@Override
	public ClassPathResource getXSD() {
		return new ClassPathResource("event/declaration/declaration-ack-2.xsd");
	}

	@Override
	public void handle(DeclarationAck ack, Map<String, String> headers) throws EsbBusinessException {
		final DeclarationIdentifier declaration = ack.getDeclaration();

		// On récupère le contribuable correspondant
		final long ctbId = declaration.getPartyNumber();
		final Contribuable ctb = (Contribuable) tiersDAO.get(ctbId);
		if (ctb == null) {
			throw new EsbBusinessException(EsbBusinessCode.CTB_INEXISTANT, "Le contribuable n°" + ctbId + " n'existe pas.", null);
		}

		final ValidationResults results = validationService.validate(ctb);
		if (results.hasErrors()) {
			throw new EsbBusinessException(EsbBusinessCode.TIERS_INVALIDE, "Le contribuable n°" + ctbId + " ne valide pas (" + results.toString() + ").", null);
		}

		// On s'assure que l'on est bien cohérent avec les données en base
		if (ctb.isDebiteurInactif()) {
			throw new EsbBusinessException(EsbBusinessCode.CTB_DEBITEUR_INACTIF, "Le contribuable n°" + ctbId + " est un débiteur inactif, il n'aurait pas dû recevoir de déclaration.", null);
		}

		final int annee = declaration.getTaxPeriod();
		final List<DeclarationAvecNumeroSequence> declarations = ctb.getDeclarationsDansPeriode(DeclarationAvecNumeroSequence.class, annee, true);
		if (declarations == null || declarations.isEmpty()) {
			throw new EsbBusinessException(EsbBusinessCode.DECLARATION_ABSENTE, "Le contribuable n°" + ctbId + " ne possède pas de déclaration pour la période fiscale " + annee + '.', null);
		}

		// une seule déclaration ou toutes celle de la PF ?
		final List<DeclarationAvecNumeroSequence> aQuittancer;
		if (declaration.getSequenceNumber() != null) {
			// on ne quittance que celle avec le bon numéro de séquence, si tant est qu'on la trouve (et qu'elle est non-annulée)
			final DeclarationAvecNumeroSequence decla = getFirstMatch(declarations, d -> declaration.getSequenceNumber().equals(d.getNumero()));
			if (decla == null) {
				throw new EsbBusinessException(EsbBusinessCode.DECLARATION_ABSENTE, "Le contribuable n°" + ctbId + " ne possède pas de déclaration " + annee + " avec le numéro de séquence " + declaration.getSequenceNumber() + ".", null);
			}
			else if (decla.isAnnule()) {
				throw new EsbBusinessException(EsbBusinessCode.DECLARATION_ABSENTE, "Le contribuable n°" + ctbId + " ne possède pas de déclaration non-annulée " + annee + " avec le numéro de séquence " + declaration.getSequenceNumber() + ".", null);
			}
			aQuittancer = Collections.singletonList(decla);
		}
		else {
			// on quittance toutes les déclarations non-annulées
			aQuittancer = AnnulableHelper.sansElementsAnnules(declarations);
			if (aQuittancer.isEmpty()) {
				throw new EsbBusinessException(EsbBusinessCode.DECLARATION_ABSENTE, "Le contribuable n°" + ctbId + " ne possède pas de déclaration non-annulée pour la période fiscale " + annee + ".", null);
			}
		}

		final RegDate dateQuittancement = DataHelper.xmlToCore(ack.getDate());
		if (dateQuittancement == null) {
			throw new EsbBusinessException(EsbBusinessCode.DECLARATION_NON_QUITTANCEE, "La date de quittance est invalide.", null);
		}

		// on envoie l'information au BAM
		sendQuittancementToBam(ctbId, annee, aQuittancer, dateQuittancement, headers);

		// et finalement on quittance la ou les déclaration(s)
		quittancerDeclarations(aQuittancer, dateQuittancement, ack.getSource());
	}

	@Nullable
	private static <T> T getFirstMatch(Collection<T> source, Predicate<? super T> predicate) {
		if (source == null || source.isEmpty()) {
			return null;
		}
		return source.stream()
				.filter(predicate)
				.findFirst()
				.orElse(null);
	}

	private void sendQuittancementToBam(long ctbId, int annee, List<DeclarationAvecNumeroSequence> declarations, RegDate dateQuittancement, Map<String, String> incomingHeaders) throws EsbBusinessException {
		final String processDefinitionId = EsbMessageHelper.getProcessDefinitionId(incomingHeaders);
		final String processInstanceId = EsbMessageHelper.getProcessInstanceId(incomingHeaders);
		if (StringUtils.isNotBlank(processDefinitionId) && StringUtils.isNotBlank(processInstanceId)) {
			try {
				final Map<String, String> bamHeaders = BamMessageHelper.buildCustomBamHeadersForQuittancementDeclarations(declarations, dateQuittancement, incomingHeaders);
				final String businessId = String.format("%d-%d-%s", ctbId, annee, new SimpleDateFormat("yyyyMMddHHmmssSSS").format(DateHelper.getCurrentDate()));
				bamMessageSender.sendBamMessageQuittancementDi(processDefinitionId, processInstanceId, businessId, ctbId, annee, bamHeaders);
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new EsbBusinessException(EsbBusinessCode.BAM, String.format("Erreur à la notification au BAM du quittancement de la déclaration %d du contribuable %d", annee, ctbId), e);
			}
		}
		else {
			LOGGER.warn(String.format("ProcessDefinitionId (%s) et/ou processInstanceId (%s) manquant : pas de notification au BAM du quittancement de la DI %d du contribuable %d.",
			                          processDefinitionId, processInstanceId, annee, ctbId));
		}
	}

	private void quittancerDeclarations(List<DeclarationAvecNumeroSequence> aQuittancer, RegDate dateQuittance, String source) throws EsbBusinessException {
		for (DeclarationAvecNumeroSequence declaration : aQuittancer) {
			if (!declaration.isAnnule()) {
				final Quittanceur quittanceur = quittanceurs.get(declaration.getClass());
				if (quittanceur == null) {
					throw new IllegalArgumentException("Impossible de quittancer une déclaration de type " + declaration.getClass().getName());
				}
				try {
					//noinspection unchecked
					quittanceur.quittance(declaration, dateQuittance, source);
				}
				catch (DeclarationException e) {
					throw new EsbBusinessException(EsbBusinessCode.DECLARATION_NON_QUITTANCEE, "Impossible que quittancer la déclaration.", e);
				}
			}
		}
	}
}
