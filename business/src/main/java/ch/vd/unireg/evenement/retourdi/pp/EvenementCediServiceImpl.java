package ch.vd.unireg.evenement.retourdi.pp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.coordfin.CoordonneesFinancieresService;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.ModeleDocumentDAO;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.jms.BamMessageHelper;
import ch.vd.unireg.jms.BamMessageSender;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbMessageHelper;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.validation.ValidationService;

public class EvenementCediServiceImpl implements EvenementCediService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementCediServiceImpl.class);

	private TiersDAO tiersDAO;
	private PeriodeFiscaleDAO periodeFiscaleDAO;
	private ModeleDocumentDAO modeleDocumentDAO;
	private ValidationService validationService;
	private BamMessageSender bamMessageSender;
	private CoordonneesFinancieresService coordonneesFinancieresService;

	@Override
	public void onEvent(EvenementCedi event, Map<String, String> incomingHeaders) throws EvenementCediException {

		if (event instanceof RetourDI) {
			onRetourDI((RetourDI) event, incomingHeaders);
		}
		else {
			throw new IllegalArgumentException("Type d'événement inconnu = " + event.getClass());
		}
	}

	protected void onRetourDI(RetourDI scan, Map<String, String> incomingHeaders) throws EvenementCediException {

		// On récupère le contribuable correspondant
		final long ctbId = scan.getNoContribuable();
		final Contribuable ctb = (Contribuable) tiersDAO.get(ctbId);
		if (ctb == null) {
			throw new EvenementCediException(EsbBusinessCode.CTB_INEXISTANT, "Le contribuable n°" + ctbId + " n'existe pas.");
		}

		final ValidationResults results = validationService.validate(ctb);
		if (results.hasErrors()) {
			throw new EvenementCediException(EsbBusinessCode.TIERS_INVALIDE, "Le contribuable n°" + ctbId + " ne valide pas (" + results.toString() + ").");
		}

		// On s'assure que l'on est bien cohérent avec les données en base
		if (ctb.isDebiteurInactif()) {
			throw new EvenementCediException(EsbBusinessCode.CTB_DEBITEUR_INACTIF, "Le contribuable n°" + ctbId + " est un débiteur inactif, il n'aurait pas dû recevoir de déclaration d'impôt.");
		}

		final int annee = scan.getPeriodeFiscale();
		final List<DeclarationImpotOrdinaire> declarations = ctb.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, annee, false);
		if (declarations.isEmpty()) {
			throw new EvenementCediException(EsbBusinessCode.DECLARATION_ABSENTE, "Le contribuable n°" + ctbId + " ne possède pas de déclaration pour la période fiscale " + annee + '.');
		}

		final int noSequenceDI = scan.getNoSequenceDI();
		final DeclarationImpotOrdinaire declaration = findDeclaration(noSequenceDI, declarations);
		if (declaration != null) {
			// on envoie l'information au BAM
			sendRetourDiToBAM(ctbId, annee, incomingHeaders);
			// On met-à-jour le type de déclaration
			updateTypeDocument(declaration, scan);
		}else {
			LOGGER.warn("Le contribuable n°" + ctbId + " ne possède pas de déclaration pour la période fiscale "
							+ annee + " avec le numéro de séquence " + noSequenceDI + ". Le contribuables sera quand même mis à jour avce les informations retournées");
		}

		// on met-à-jour les coordonnées financières
		updateCoordonneesFinancieres(ctb, scan);

		// On met-à-jour les informations personnelles
		updateInformationsPersonnelles(ctb, scan);

	}

	private void sendRetourDiToBAM(long ctbId, int annee, Map<String, String> incomingHeaders) throws EvenementCediException {
		final String processDefinitionId = EsbMessageHelper.getProcessDefinitionId(incomingHeaders);
		final String processInstanceId = EsbMessageHelper.getProcessInstanceId(incomingHeaders);
		if (StringUtils.isNotBlank(processDefinitionId) && StringUtils.isNotBlank(processInstanceId)) {
			try {
				final String businessId = String.format("%d-%d-%s", ctbId, annee, new SimpleDateFormat("yyyyMMddHHmmssSSS").format(DateHelper.getCurrentDate()));
				final Map<String, String> bamHeaders = BamMessageHelper.buildCustomBamHeadersForRetourDi(incomingHeaders);
				bamMessageSender.sendBamMessageRetourDi(processDefinitionId, processInstanceId, businessId, ctbId, annee, bamHeaders);
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new EvenementCediException(EsbBusinessCode.BAM, String.format("Erreur à la notification au BAM du retour de la DI %d du contribuable %d", annee, ctbId), e);
			}
		}
		else {
			LOGGER.warn(String.format("ProcessDefinitionId (%s) et/ou processInstanceId (%s) manquant : pas de notification au BAM de la réception du retour de la DI %d du contribuable %d.",
			                          processDefinitionId, processInstanceId, annee, ctbId));
		}
	}

	/**
	 * Met-à-jour le type de la déclaration du contribuable (manuelle, vaudtax) par rapport au format de la déclaration d'impôt réellement retournée.
	 *
	 * @param declaration la déclaration dont on veut mettre-à-jour le type de document
	 * @param scan        les informations renseignées dans le déclaration
	 */
	private void updateTypeDocument(DeclarationImpotOrdinaire declaration, RetourDI scan) {

		final int annee = scan.getPeriodeFiscale();
		final PeriodeFiscale periode = periodeFiscaleDAO.getPeriodeFiscaleByYear(annee);
		final ModeleDocument vaudTax = modeleDocumentDAO.getModelePourDeclarationImpotOrdinaire(periode, TypeDocument.DECLARATION_IMPOT_VAUDTAX);
		final ModeleDocument complete = modeleDocumentDAO.getModelePourDeclarationImpotOrdinaire(periode, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
		Assert.notNull(vaudTax);
		Assert.notNull(complete);

		final RetourDI.TypeDocument typeDocumentScanne = scan.getTypeDocument();
		if (typeDocumentScanne != null) {
			final TypeDocument typeDocument = declaration.getModeleDocument().getTypeDocument();
			switch (typeDocumentScanne) {
			case VAUDTAX:
				if (typeDocument == TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL || typeDocument == TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH) {
					declaration.setModeleDocument(vaudTax);
				}
				break;
			case MANUSCRITE:
				if (typeDocument == TypeDocument.DECLARATION_IMPOT_VAUDTAX) {
					declaration.setModeleDocument(complete);
				}
				break;
			default:
				throw new IllegalArgumentException("Type de document inconnu = [" + typeDocumentScanne + ']');
			}
		}
	}

	/**
	 * Met-à-jour les informations personnelles du contribuable à partir des informations renseignées dans la déclaration retournée.
	 *
	 * @param ctb  le contribuable à mettre-à-jour
	 * @param scan les informations renseignées dans le déclaration
	 */
	private void updateInformationsPersonnelles(Contribuable ctb, RetourDI scan) {

		if (StringUtils.isNotBlank(scan.getNoTelephone())) {
			ctb.setNumeroTelephonePrive(LengthConstants.streamlineField(scan.getNoTelephone(), LengthConstants.TIERS_NUMTEL, true));
		}

		if (StringUtils.isNotBlank(scan.getNoMobile())) {
			ctb.setNumeroTelephonePortable(LengthConstants.streamlineField(scan.getNoMobile(), LengthConstants.TIERS_NUMTEL, true));
		}

		if (StringUtils.isNotBlank(scan.getEmail())) {
			ctb.setAdresseCourrierElectronique(LengthConstants.streamlineField(scan.getEmail(), LengthConstants.TIERS_EMAIL, true));
		}
	}

	/**
	 * [SIFISC-20035] Met-à-jour les coordonnées financières si nécessaire (en gardant l'historique)
	 *
	 * @param ctb  le contribuable dont les coordonnées financières doivent être mises-à-jour
	 * @param scan les données de retour de la DI
	 */
	private void updateCoordonneesFinancieres(@NotNull Contribuable ctb, @NotNull RetourDI scan) {
		final RegDate dateTraitement = RegDateHelper.get(scan.getDateTraitement());
		if (dateTraitement == null) {
			throw new IllegalArgumentException("La date de traitement = [" + scan.getDateTraitement() + "] n'est pas valable");
		}
		coordonneesFinancieresService.detectAndUpdateCoordonneesFinancieres(ctb, scan.getTitulaireCompte(), scan.getIban(), dateTraitement, (currentIban, newIban) -> {});
	}

	/**
	 * Recherche la declaration pour une année et un numéro de déclaration dans l'année
	 *
	 * @param noSequenceDI le numéro de séquence de la déclaration
	 * @param declarations les déclaration de la période considérée
	 * @return la déclaration correspondante
	 */
	public static DeclarationImpotOrdinaire findDeclaration(int noSequenceDI, List<DeclarationImpotOrdinaire> declarations) {

		DeclarationImpotOrdinaire declaration = null;

		if (declarations != null && !declarations.isEmpty()) {
			for (DeclarationImpotOrdinaire di : declarations) {
				if (noSequenceDI != 0) {
					if (di.getNumero() == noSequenceDI) {
						declaration = di;
						break;
					}
				}
				// Dans le cas ou le numero dans l'année n'est pas spécifié on prend la première DI trouvée sur la période
				else {
					declaration = di;
				}
			}
		}

		return declaration;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

	public void setModeleDocumentDAO(ModeleDocumentDAO modeleDocumentDAO) {
		this.modeleDocumentDAO = modeleDocumentDAO;
	}

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	public void setBamMessageSender(BamMessageSender bamMessageSender) {
		this.bamMessageSender = bamMessageSender;
	}

	public void setCoordonneesFinancieresService(CoordonneesFinancieresService coordonneesFinancieresService) {
		this.coordonneesFinancieresService = coordonneesFinancieresService;
	}
}
