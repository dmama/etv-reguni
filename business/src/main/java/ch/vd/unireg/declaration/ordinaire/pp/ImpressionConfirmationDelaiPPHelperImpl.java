package ch.vd.unireg.declaration.ordinaire.pp;

import java.text.SimpleDateFormat;
import java.util.Optional;

import noNamespace.ConfirmationDelaiDocument;
import noNamespace.ConfirmationDelaiDocument.ConfirmationDelai;
import noNamespace.FichierImpressionDocument;
import noNamespace.InfoArchivageDocument;
import noNamespace.InfoDocumentDocument1;
import noNamespace.InfoDocumentDocument1.InfoDocument;
import noNamespace.InfoEnteteDocumentDocument1;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import noNamespace.TypAdresse;
import noNamespace.TypFichierImpression;
import noNamespace.TypFichierImpression.Document;
import noNamespace.TypPeriode;
import noNamespace.TypPeriode.Entete;
import noNamespace.TypPeriode.Entete.ImpCcn;
import noNamespace.TypPeriode.Entete.Tit;
import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.editique.ConstantesEditique;
import ch.vd.unireg.editique.EditiqueAbstractLegacyHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiquePrefixeHelper;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class ImpressionConfirmationDelaiPPHelperImpl extends EditiqueAbstractLegacyHelper implements ImpressionConfirmationDelaiPPHelper {

	private static final String VERSION_XSD = "1.0";

	private ServiceInfrastructureService infraService;
	private MessageHelper messageHelper;

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}
	public void setMessageHelper(MessageHelper messageHelper) {
		this.messageHelper = messageHelper;
	}

	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		return TypeDocumentEditique.CONFIRMATION_DELAI;
	}

	@Override
	public FichierImpressionDocument remplitConfirmationDelai(ImpressionConfirmationDelaiHelperParams params, String idArchivage) throws EditiqueException {
		try {
			final FichierImpressionDocument mainDocument = FichierImpressionDocument.Factory.newInstance();
			final TypFichierImpression typeFichierImpression = mainDocument.addNewFichierImpression();
			final InfoDocument infoDocument = remplitInfoDocument(params);
			final InfoArchivageDocument.InfoArchivage infoArchivage = remplitInfoArchivage(params, idArchivage);
			final InfoEnteteDocument infoEnteteDocument = remplitEnteteDocument(params);
			final Document document = typeFichierImpression.addNewDocument();
			document.setConfirmationDelai(remplitSpecifiqueConfirmationDelai(params));
			document.setInfoEnteteDocument(infoEnteteDocument);
			document.setInfoDocument(infoDocument);
			document.setInfoArchivage(infoArchivage);
			typeFichierImpression.setDocumentArray(new Document[]{document});
			return mainDocument;
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}

	}

	private ConfirmationDelai remplitSpecifiqueConfirmationDelai(ImpressionConfirmationDelaiHelperParams params) throws EditiqueException {

		final ConfirmationDelai confirmationDelai = ConfirmationDelaiDocument.Factory.newInstance().addNewConfirmationDelai();
		confirmationDelai.setDateAccord(RegDateHelper.toIndexString(params.getDateAccord()));
		final TypPeriode periode = confirmationDelai.addNewPeriode();
		final TypeDocumentEditique typeDocumentEditique = getTypeDocumentEditique();
		periode.setPrefixe(EditiquePrefixeHelper.buildPrefixePeriode(typeDocumentEditique));
		periode.setOrigDuplicat(ORIGINAL);
		periode.setHorsSuisse("");
		periode.setHorsCanton("");
		periode.setAnneeFiscale(params.getDi().getPeriode().getAnnee().toString());
		periode.setDateDecompte(RegDateHelper.toIndexString(params.getDi().getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.EMIS).getDateObtention()));
		periode.setDatDerCalculAc("");

		final Entete entete = periode.addNewEntete();
		final Tit tit = entete.addNewTit();
		tit.setPrefixe(EditiquePrefixeHelper.buildPrefixeTitreEntete(typeDocumentEditique));
		tit.setLibTit(messageHelper.getMessage("lettre.demande.delai.di.editique.title", params.getDi().getPeriode().getAnnee()));
		final ImpCcn impCcn = entete.addNewImpCcn();
		impCcn.setPrefixe(EditiquePrefixeHelper.buildPrefixeImpCcnEntete(typeDocumentEditique));
		impCcn.setLibImpCcn(messageHelper.getMessage("lettre.demande.delai.di.editique.sous.title"));
		final String formuleAppel = adresseService.getFormulePolitesse(params.getDi().getTiers(), null).getFormuleAppel();
		confirmationDelai.setCivil(formuleAppel);
		confirmationDelai.setOFS(legacyEditiqueHelper.getCommune(params.getDi()));
		return confirmationDelai;
	}

	private InfoEnteteDocument remplitEnteteDocument(ImpressionConfirmationDelaiHelperParams params) throws EditiqueException {
		final InfoEnteteDocument infoEnteteDocument = InfoEnteteDocumentDocument1.Factory.newInstance().addNewInfoEnteteDocument();

		try {
			infoEnteteDocument.setPrefixe(EditiquePrefixeHelper.buildPrefixeEnteteDocument(getTypeDocumentEditique()));

			final TypAdresse porteAdresse = legacyEditiqueHelper.remplitPorteAdresse(params.getDi().getTiers(), infoEnteteDocument);
			infoEnteteDocument.setPorteAdresse(porteAdresse);

			// [SIFISC-20149] l'expéditeur de la confirmation de délai de DI PP doit être la nouvelle entité si applicable, sinon, le CAT
			final int noCaExpeditrice = Optional.ofNullable(getNoCollectiviteAdministrativeEmettriceSelonEtiquettes(params.getDi().getTiers(), RegDate.get())).orElse(ServiceInfrastructureService.noCAT);
			final CollectiviteAdministrative caExpeditrice = tiersService.getCollectiviteAdministrative(noCaExpeditrice);

			final Expediteur expediteur = legacyEditiqueHelper.remplitExpediteur(caExpeditrice, infoEnteteDocument);
			expediteur.setDateExpedition(RegDateHelper.toIndexString(RegDate.get()));
			expediteur.setTraitePar(params.getTraitePar());
			if(params.getServiceTraiterPar()!=null){
			expediteur.setSrvExp(params.getServiceTraiterPar());}

			// ici c'est un peu tordu : on veut afficher le numéro de téléphone du CAT dans l'entête mais
			// le numéro de téléphone du collaborateur dans le cadre (affaire traitée par), là où est
			// normalement prévue l'adresse e-mail du collaborateur
			if (!StringUtils.isBlank(params.getNoTelephone())) {
				expediteur.setAdrMes(params.getNoTelephone());
			}

			// [SIFISC-23700] les coordonnées téléphoniques doivent toujours venir du CAT
			final ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative cat = infraService.getCAT();
			expediteur.setNumTelephone(cat.getNoTelephone());
			expediteur.setNumFax(cat.getNoFax());

			infoEnteteDocument.setExpediteur(expediteur);
			final Destinataire destinataire = legacyEditiqueHelper.remplitDestinataire(params.getDi().getTiers(), infoEnteteDocument);
			infoEnteteDocument.setDestinataire(destinataire);
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
		return infoEnteteDocument;
	}

	private InfoDocument remplitInfoDocument(ImpressionConfirmationDelaiHelperParams params) throws EditiqueException {
		final InfoDocument infoDocument = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
		final String prefixe = EditiquePrefixeHelper.buildPrefixeInfoDocument(getTypeDocumentEditique());
		infoDocument.setPrefixe(prefixe);
		infoDocument.setTypDoc("CD");
		infoDocument.setCodDoc("CONF_DEL");
		infoDocument.setVersion(VERSION_XSD);
		infoDocument.setLogo(LOGO_CANTON);
		infoDocument.setPopulations(ConstantesEditique.POPULATION_PP);
		infoDocument.setIdEnvoi("");
		legacyEditiqueHelper.remplitAffranchissement(infoDocument, params.getDi().getTiers());
		return infoDocument;
	}

	private InfoArchivageDocument.InfoArchivage remplitInfoArchivage(ImpressionConfirmationDelaiHelperParams params, String idArchivage) {
		return legacyEditiqueHelper.buildInfoArchivage(getTypeDocumentEditique(), params.getDi().getTiers().getNumero(), idArchivage, params.getDateAccord());
	}

	@Override
	public String construitIdArchivageDocument(ImpressionConfirmationDelaiHelperParams params) {
		final String idDelai = createStringIdDelai(params);
		return String.format(
				"%s %s %s",
				idDelai,
				StringUtils.rightPad("Confirmation Delai", 19, ' '),
				new SimpleDateFormat("MMddHHmmssSSS").format(
						params.getLogCreationDateDelai()
				)
		);
	}

	protected String createStringIdDelai(ImpressionConfirmationDelaiHelperParams params) {
		final String stringId = params.getIdDelai().toString();
		if (stringId.length() > 6) {
			return StringUtils.substring(stringId, stringId.length() - 6, stringId.length());
		}
		else if (stringId.length() < 6) {
			return StringUtils.leftPad(stringId, 6, '0');

		}
		return stringId;
	}

	@Override
	public String construitIdDocument(DelaiDeclaration delai) {
		final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) delai.getDeclaration();
		final String principal = AuthenticationHelper.getCurrentPrincipal();
		final long ts = (System.nanoTime() % 1000000000L) / 1000L;      // fraction de seconde en microsecondes
		return String.format("%d %02d %09d %d_%s_%d", di.getPeriode().getAnnee(), di.getNumero(), di.getTiers().getNumero(), delai.getId(), principal, ts);
	}
}
