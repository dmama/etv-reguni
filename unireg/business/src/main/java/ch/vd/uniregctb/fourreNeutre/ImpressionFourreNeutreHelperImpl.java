package ch.vd.uniregctb.fourreNeutre;

import java.text.SimpleDateFormat;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.editique.unireg.CTypeAffranchissement;
import ch.vd.editique.unireg.CTypeInfoDocument;
import ch.vd.editique.unireg.CTypeInfoEnteteDocument;
import ch.vd.editique.unireg.FichierImpression;
import ch.vd.editique.unireg.STypeZoneAffranchissement;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.editique.ConstantesEditique;
import ch.vd.uniregctb.editique.EditiqueAbstractHelperImpl;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiquePrefixeHelper;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.Tiers;

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
			final CTypeInfoEnteteDocument infoEnteteDocument = buildInfoEnteteDocument(tiers, dateTraitement, TRAITE_PAR, NOM_SERVICE_EXPEDITEUR, infraService.getACI(), infraService.getCAT());
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
	public FichierImpression.Document buildCopieMandataire(FichierImpression.Document original, Contribuable destinataire, RegDate dateReference) throws EditiqueException {
		return null;
	}

	private static CTypeInfoDocument buildInfoDocument(AdresseEnvoiDetaillee adresseEnvoi, Tiers tiers) {
		final CTypeInfoDocument infoDoc = new CTypeInfoDocument();

		final Pair<STypeZoneAffranchissement, String> infosAffranchissement = getInformationsAffranchissement(adresseEnvoi, false, ServiceInfrastructureService.noOIPM);
		infoDoc.setAffranchissement(new CTypeAffranchissement(infosAffranchissement.getLeft(), null));
		infoDoc.setVersionXSD(VERSION_XSD);

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
