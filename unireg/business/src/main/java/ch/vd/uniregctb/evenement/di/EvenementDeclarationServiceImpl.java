package ch.vd.uniregctb.evenement.di;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.RegDateHelper;
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

	private static final String NUMERO_SEQUENCE = "numeroSequenceFourre";
	private static final String PERIODE_IMPOSITION = "periodeImposition";

	private TiersDAO tiersDAO;
	private ValidationService validationService;
	private DeclarationImpotService diService;
	private BamEventSender bamEventSender;

	@Override
	public void onEvent(EvenementDeclaration event, Map<String, String> incomingHeaders) throws EvenementDeclarationException {
		if (event instanceof QuittancementDI) {
			onQuittancementDI((QuittancementDI) event, incomingHeaders);
		}
		else {
			throw new IllegalArgumentException("Type d'événement inconnu = " + event.getClass());
		}
	}

	private void onQuittancementDI(QuittancementDI quittance, Map<String, String> incomingHeaders) throws EvenementDeclarationException {
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
		sendQuittancementToBam(ctbId, annee, declarations, incomingHeaders);

		// et finalement on quittance les déclarations
		quittancerDeclarations(ctb, declarations, quittance, quittance.getSource());
	}

	private void sendQuittancementToBam(long ctbId, int annee, List<Declaration> declarations, Map<String, String> incomingHeaders) throws EvenementDeclarationException {
		final String processDefinitionId = EsbMessageHelper.getProcessDefinitionId(incomingHeaders);
		final String processInstanceId = EsbMessageHelper.getProcessInstanceId(incomingHeaders);
		if (StringUtils.isNotBlank(processDefinitionId) && StringUtils.isNotBlank(processInstanceId)) {
			try {
				final Map<String, String> bamHeaders = buildCustomBamHeadersForQuittancement(declarations);
				bamEventSender.sendEventBamQuittancementDi(processDefinitionId, processInstanceId, String.format("%d-%d", ctbId, annee), ctbId, annee, bamHeaders);
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

	@Nullable
	private Map<String, String> buildCustomBamHeadersForQuittancement(List<Declaration> declarations) {
		final StringBuilder bNoSequences = new StringBuilder();
		final StringBuilder bPeriodes = new StringBuilder();
		for (Declaration d : declarations) {
			if (!d.isAnnule()) {
				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) d;
				if (bNoSequences.length() > 0) {
					bNoSequences.append(";");
				}
				bNoSequences.append(String.format("%02d", di.getNumero()));

				if (bPeriodes.length() > 0) {
					bPeriodes.append(";");
				}
				bPeriodes.append(String.format("%s-%s", RegDateHelper.dateToDisplayString(di.getDateDebut()), RegDateHelper.dateToDisplayString(di.getDateFin())));
			}
		}

		final Map<String, String> bamHeaders;
		if (bNoSequences.length() > 0 || bPeriodes.length() > 0) {
			bamHeaders = new HashMap<String, String>(2);
			bamHeaders.put(NUMERO_SEQUENCE, bNoSequences.toString());
			bamHeaders.put(PERIODE_IMPOSITION, bPeriodes.toString());
		}
		else {
			bamHeaders = null;
		}
		return bamHeaders;
	}

	private void quittancerDeclarations(Contribuable ctb, List<Declaration> declarations, QuittancementDI quittance, String source) {
		for (Declaration declaration : declarations) {
			if (!declaration.isAnnule()) {
				diService.quittancementDI(ctb, (DeclarationImpotOrdinaire) declaration, quittance.getDate(), source);
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
