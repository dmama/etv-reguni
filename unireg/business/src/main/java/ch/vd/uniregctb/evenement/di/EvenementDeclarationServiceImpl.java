package ch.vd.uniregctb.evenement.di;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.jms.BamEventSender;
import ch.vd.uniregctb.jms.EsbMessageHelper;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.validation.ValidationService;

public class EvenementDeclarationServiceImpl implements EvenementDeclarationService, EvenementDeclarationHandler {

	private static final Logger LOGGER = Logger.getLogger(EvenementDeclarationServiceImpl.class);

	/**
	 * Liste des attributs customs du message entrant qu'il faut transmettre au BAM
	 * (en plus des attributs vraiment obligatoires du style processDefinitionId et processInstanceId...)
	 */
	private static final String[] CUSTOM_HEADERS_TO_PASS_ON_TO_BAM = { "periodeFiscale", "numeroContribuable", "numeroVersion", "numeroSequence" };

	private TiersDAO tiersDAO;
	private ValidationService validationService;
	private DeclarationImpotService diService;
	private BamEventSender bamEventSender;

	@Override
	public void onEvent(EvenementDeclaration event, Map<String, String> customHeaders) throws EvenementDeclarationException {
		if (event instanceof QuittancementDI) {
			onQuittancementDI((QuittancementDI) event, customHeaders);
		}
		else {
			throw new IllegalArgumentException("Type d'événement inconnu = " + event.getClass());
		}
	}

	private void onQuittancementDI(QuittancementDI quittance, Map<String, String> customHeaders) throws EvenementDeclarationException {
		// On récupère le contribuable correspondant
		final long ctbId = quittance.getNumeroContribuable();
		final Contribuable ctb = (Contribuable) tiersDAO.get(ctbId);
		if (ctb == null) {
			throw new EvenementDeclarationException("Le contribuable n°" + ctbId + " n'existe pas.");
		}

		final ValidationResults results = validationService.validate(ctb);
		if (results.hasErrors()) {
			throw new EvenementDeclarationException("Le contribuable n°" + ctbId + " ne valide pas (" + results.toString() + ").");
		}

		// On s'assure que l'on est bien cohérent avec les données en base
		if (ctb.isDebiteurInactif()) {
			throw new EvenementDeclarationException("Le contribuable n°" + ctbId + " est un débiteur inactif, il n'aurait pas dû recevoir de déclaration d'impôt.");
		}

		final int annee = quittance.getPeriodeFiscale();
		final List<Declaration> declarations = ctb.getDeclarationsForPeriode(annee, false);
		if (declarations == null || declarations.isEmpty()) {
			throw new EvenementDeclarationException("Le contribuable n°" + ctbId + " ne possède pas de déclaration pour la période fiscale " + annee + ".");
		}

		// on envoie l'information au BAM
		sendQuittancementToBam(ctbId, annee, customHeaders);

		// et finalement on quittance les déclarations
		quittancerDeclarations(ctb, declarations, quittance);
	}

	private void sendQuittancementToBam(long ctbId, int annee, Map<String, String> customHeaders) throws EvenementDeclarationException {
		final String processDefinitionId = EsbMessageHelper.getProcessDefinitionId(customHeaders);
		final String processInstanceId = EsbMessageHelper.getProcessInstanceId(customHeaders);
		if (StringUtils.isNotBlank(processDefinitionId) && StringUtils.isNotBlank(processInstanceId)) {
			try {
				final Map<String, String> headers = EsbMessageHelper.filterHeaders(customHeaders, CUSTOM_HEADERS_TO_PASS_ON_TO_BAM);
				bamEventSender.sendEventBamQuittancementDi(processDefinitionId, processInstanceId, String.format("%d-%d", ctbId, annee), headers);
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new EvenementDeclarationException(String.format("Erreur à la notification au BAM du quittancement de la DI %d du contribuable %d", annee, ctbId), e);
			}
		}
		else {
			LOGGER.warn(String.format("ProcessDefinitionId (%s) et/ou processInstanceId (%s) manquant : pas de notification au BAM du quittancement de la DI %d du contribuable %d.",
			                          processDefinitionId, processInstanceId, annee, ctbId));
		}
	}

	private void quittancerDeclarations(Contribuable ctb, List<Declaration> declarations, QuittancementDI quittance) {
		for (Declaration declaration : declarations) {
			if (!declaration.isAnnule()) {
				diService.quittancementDI(ctb, (DeclarationImpotOrdinaire) declaration, quittance.getDate());
			}
		}
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	public void setDiService(DeclarationImpotService diService) {
		this.diService = diService;
	}

	public void setBamEventSender(BamEventSender bamEventSender) {
		this.bamEventSender = bamEventSender;
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/di/evenementDeclarationImpot-input-1.xsd");
	}
}
