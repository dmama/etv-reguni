package ch.vd.uniregctb.declaration.ordinaire.pm;

import java.text.SimpleDateFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import ch.vd.editique.unireg.CTypeAffranchissement;
import ch.vd.editique.unireg.CTypeInfoArchivage;
import ch.vd.editique.unireg.CTypeInfoDocument;
import ch.vd.editique.unireg.CTypeInfoEnteteDocument;
import ch.vd.editique.unireg.FichierImpression;
import ch.vd.editique.unireg.STypeZoneAffranchissement;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.editique.ConstantesEditique;
import ch.vd.uniregctb.editique.EditiqueAbstractHelperImpl;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiquePrefixeHelper;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;

/**
 * Rassemblement des méthodes utiles pour la constitution d'un document de sommation de DI PM à envoyer à l'éditique
 */
public class ImpressionSommationDeclarationImpotPersonnesMoralesHelperImpl extends EditiqueAbstractHelperImpl implements ImpressionSommationDeclarationImpotPersonnesMoralesHelper {

	private static final String CODE_DOCUMENT_SOMMATION_PM = TypeDocumentEditique.SOMMATION_DI_PM.getCodeDocumentEditique().substring(0, 4);

	private DelaisService delaisService;

	public void setDelaisService(DelaisService delaisService) {
		this.delaisService = delaisService;
	}

	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		return TypeDocumentEditique.SOMMATION_DI_PM;
	}

	@Override
	public FichierImpression.Document buildDocument(DeclarationImpotOrdinairePM declaration, RegDate dateSommation, boolean batch) throws EditiqueException {
		try {
			final ContribuableImpositionPersonnesMorales tiers = declaration.getTiers();
			final CTypeInfoDocument infoDocument = buildInfoDocument(getAdresseEnvoi(tiers));
			final CTypeInfoArchivage infoArchivage = buildInfoArchivage(getTypeDocumentEditique(), construitCleArchivageDocument(declaration), tiers.getNumero(), dateSommation);

			final String titre = String.format("INVITATION À DÉPOSER LA DÉCLARATION %d - SOMMATION (du %s au %s)",
			                                   declaration.getPeriode().getAnnee(),
			                                   RegDateHelper.dateToDisplayString(declaration.getDateDebutExerciceCommercial()),
			                                   RegDateHelper.dateToDisplayString(declaration.getDateFinExerciceCommercial()));

			final CTypeInfoEnteteDocument infoEnteteDocument = buildInfoEnteteDocument(tiers, dateSommation, TRAITE_PAR, NOM_SERVICE_EXPEDITEUR, infraService.getACIOIPM(), infraService.getCAT(), titre);
			final FichierImpression.Document.Sommation sommation = buildInfoSommation(declaration, dateSommation, batch);
			return new FichierImpression.Document(infoDocument, infoArchivage, infoEnteteDocument, null, null, null, null, sommation, null, null, null, null);
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}

	private static CTypeInfoDocument buildInfoDocument(AdresseEnvoiDetaillee adresseEnvoi) {
		final CTypeInfoDocument infoDoc = new CTypeInfoDocument();

		final Pair<STypeZoneAffranchissement, String> infosAffranchissement = getInformationsAffranchissement(adresseEnvoi, false, ServiceInfrastructureService.noOIPM);
		infoDoc.setIdEnvoi(infosAffranchissement.getRight());
		infoDoc.setAffranchissement(new CTypeAffranchissement(infosAffranchissement.getLeft(), null));
		infoDoc.setVersionXSD(VERSION_XSD);

		infoDoc.setCodDoc(CODE_DOCUMENT_SOMMATION_PM);
		infoDoc.setPopulations(ConstantesEditique.POPULATION_PM);
		infoDoc.setPrefixe(EditiquePrefixeHelper.buildPrefixeInfoDocument(TypeDocumentEditique.SOMMATION_DI_PM));
		infoDoc.setTypDoc(TYPE_DOCUMENT_CO);

		return infoDoc;
	}

	private FichierImpression.Document.Sommation buildInfoSommation(DeclarationImpotOrdinairePM declaration, RegDate dateTraitement, boolean batch) {
		final FichierImpression.Document.Sommation sommation = new FichierImpression.Document.Sommation();
		sommation.setDateBaseSommation(RegDateHelper.toIndexString(dateTraitement));
		sommation.setCodeControleNIP(declaration.getCodeControle());
		sommation.setPeriodeFiscale(XmlUtils.regdate2xmlcal(RegDate.get(declaration.getPeriode().getAnnee())));
		return sommation;
	}

	public String construitIdDocument(DeclarationImpotOrdinairePM declaration) {
		return String.format(
				"%s %s %s %s",
				declaration.getPeriode().getAnnee().toString(),
				StringUtils.leftPad(declaration.getNumero().toString(), 2, '0'),
				StringUtils.leftPad(declaration.getTiers().getNumero().toString(), 9, '0'),
				new SimpleDateFormat("yyyyMMddHHmmssSSS").format(
						declaration.getLogCreationDate()
				)
		);
	}

	@Override
	public String construitCleArchivageDocument(DeclarationImpotOrdinairePM declaration) {
		return String.format(
				"%s%s %s %s",
				declaration.getPeriode().getAnnee().toString(),
				StringUtils.leftPad(declaration.getNumero().toString(), 2, '0'),
				StringUtils.rightPad("Sommation DI PM", 19, ' '),
				new SimpleDateFormat("MMddHHmmssSSS").format(
						declaration.getLogCreationDate()
				)
		);
	}
}
