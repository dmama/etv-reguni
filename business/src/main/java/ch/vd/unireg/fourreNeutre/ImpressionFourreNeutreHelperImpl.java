package ch.vd.unireg.fourreNeutre;

import java.text.SimpleDateFormat;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.editique.ConstantesEditique;
import ch.vd.unireg.editique.EditiqueAbstractHelperImpl;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiquePrefixeHelper;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.xml.editique.pm.CTypeAffranchissement;
import ch.vd.unireg.xml.editique.pm.CTypeInfoDocument;
import ch.vd.unireg.xml.editique.pm.CTypeInfoEnteteDocument;
import ch.vd.unireg.xml.editique.pm.FichierImpression;
import ch.vd.unireg.xml.editique.pm.STypeZoneAffranchissement;

public class ImpressionFourreNeutreHelperImpl extends EditiqueAbstractHelperImpl implements ImpressionFourreNeutreHelper {
	private static final String CODE_DOCUMENT_FOURRE_NEUTRE = TypeDocumentEditique.FOURRE_NEUTRE.getCodeDocumentEditique().substring(0, 4);

	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		return TypeDocumentEditique.FOURRE_NEUTRE;
	}

	@Override
	public FichierImpression.Document buildDocument(FourreNeutre fourre, RegDate dateTraitement) throws EditiqueException {
		final Tiers tiers =  fourre.getTiers();
		final String codeBarre = calculCodeBarre(fourre);
		try {
			final CTypeInfoDocument infoDocument = buildInfoDocument(getAdresseEnvoi(tiers), tiers);
			final CTypeInfoEnteteDocument infoEnteteDocument = buildInfoEnteteDocumentPM(tiers, dateTraitement, CAT_TRAITE_PAR, CAT_NOM_SERVICE_EXPEDITEUR, infraService.getACI(), infraService.getCAT());
			final FichierImpression.Document.FourreNeutre fourreNeutre = new FichierImpression.Document.FourreNeutre(XmlUtils.regdate2xmlcal(RegDate.get(fourre.getPeriodeFiscale())), codeBarre);
			final FichierImpression.Document document = new FichierImpression.Document();
			document.setInfoDocument(infoDocument);
			document.setInfoEnteteDocument(infoEnteteDocument);
			document.setFourreNeutre(fourreNeutre);
			return document;
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}

	@Override
	public String construitIdDocument(FourreNeutre fourreNeutre) {
		return String.format("FRNTR %09d %04d %s",
				fourreNeutre.getTiers().getNumero(),
				fourreNeutre.getPeriodeFiscale(),
				new SimpleDateFormat("MMddHHmmssSSS").format(DateHelper.getCurrentDate()));
	}

	@Nullable
	@Override
	public FichierImpression.Document buildCopieMandatairePM(FichierImpression.Document original, Contribuable destinataire, RegDate dateReference) throws EditiqueException {
		return null;
	}

	@Nullable
	@Override
	public ch.vd.unireg.xml.editique.pp.FichierImpression.Document buildCopieMandatairePP(ch.vd.unireg.xml.editique.pp.FichierImpression.Document original, Contribuable destinataire, RegDate dateReference) throws EditiqueException {
		return null;
	}

	private static CTypeInfoDocument buildInfoDocument(AdresseEnvoiDetaillee adresseEnvoi, Tiers tiers) {
		final CTypeInfoDocument infoDoc = new CTypeInfoDocument();

		final Pair<STypeZoneAffranchissement, String> infosAffranchissement = getInformationsAffranchissementPM(adresseEnvoi,
		                                                                                                        false,
		                                                                                                        ServiceInfrastructureService.noOIPM);
		infoDoc.setAffranchissement(new CTypeAffranchissement(infosAffranchissement.getLeft(), null));
		infoDoc.setVersionXSD(VERSION_XSD_PM);

		infoDoc.setCodDoc(CODE_DOCUMENT_FOURRE_NEUTRE);


		infoDoc.setPopulations(getConstantesEditiqueValue(tiers));
		infoDoc.setPrefixe(EditiquePrefixeHelper.buildPrefixeInfoDocument(TypeDocumentEditique.FOURRE_NEUTRE));

		return infoDoc;
	}

	static String calculCodeBarre(FourreNeutre f) {
		final Tiers tiers = f.getTiers();
		return String.format("%04d%09d%04d", f.getPeriodeFiscale(), tiers.getNumero(), 0);
	}

	private static String getConstantesEditiqueValue(Tiers tiers) {
		if (tiers instanceof ContribuableImpositionPersonnesPhysiques) {
			return ConstantesEditique.POPULATION_PP;
		}
		else if (tiers instanceof Entreprise || tiers instanceof Etablissement) {
			return ConstantesEditique.POPULATION_PM;
		}
		else  if (tiers instanceof DebiteurPrestationImposable) {
			return ConstantesEditique.POPULATION_IS;
		}
		else {
			throw new IllegalArgumentException("type de tiers non pris en charge:" + tiers.getClass().getName());
		}

	}
}
