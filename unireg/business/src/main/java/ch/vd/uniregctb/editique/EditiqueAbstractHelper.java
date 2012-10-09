package ch.vd.uniregctb.editique;

import noNamespace.InfoDocumentDocument1;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public abstract class EditiqueAbstractHelper {

	public static final Logger LOGGER = Logger.getLogger(EditiqueAbstractHelper.class);

	//
	// Différents types de préfixes à générer
	//
	private static final String DOCUM = "DOCUM";
	private static final String FOLDE = "FOLDE";
	private static final String HAUT1 = "HAUT1";
	private static final String PERIO = "PERIO";
	private static final String TITIM = "TITIM";
	private static final String IMPCC = "IMPCC";
	private static final String BVRST = "BVRST";

	//
	// Différentes constantes
	//
	protected static final String ORIGINAL = "ORG";
	protected static final String LOGO_CANTON = "CANT";
	protected static final String POPULATION_PP = "PP";
	protected static final String POPULATION_IS = "IS";

	protected AdresseService adresseService;
	protected TiersService tiersService;
	protected EditiqueHelper editiqueHelper;

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setEditiqueHelper(EditiqueHelper editiqueHelper) {
		this.editiqueHelper = editiqueHelper;
	}

	protected static String buildPrefixeInfoDocument(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, DOCUM);
	}

	protected static String buildPrefixeInfoArchivage(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, FOLDE);
	}

	protected static String buildPrefixeEnteteDocument(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, HAUT1);
	}

	protected static String buildPrefixePeriode(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, PERIO);
	}

	protected static String buildPrefixeTitreEntete(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, TITIM);
	}

	protected static String buildPrefixeImpCcnEntete(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, IMPCC);
	}

	protected static String buildPrefixeBvrStandard(TypeDocumentEditique typeDocument) {
		return buildSpecificPrefix(typeDocument, BVRST);
	}

	private static String buildSpecificPrefix(TypeDocumentEditique typeDocument, String specificSuffix) {
		return String.format("%s%s", typeDocument.getCodeDocumentEditique(), specificSuffix);
	}

	protected void remplitAffranchissement(InfoDocumentDocument1.InfoDocument infoDocument, Tiers tiers, @Nullable RegDate dateReference, boolean isMiseSousPliImpossible) throws EditiqueException {
		try {
			AdresseEnvoiDetaillee adresseEnvoiDetaillee = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);

			String idEnvoi = "";
			//SIFISC-4146
			// seuls les cas d'adresse incomplète doivent partir aux OIDs,
			// les autres ne doivent pas avoir le champ idEnvoi renseigné et donc doivent avoir une zone d'affranchissement correcte
			if (!adresseEnvoiDetaillee.isIncomplete() && !isMiseSousPliImpossible) {
				idEnvoi = "";
			}
			else {
				// [UNIREG-1257] tenir compte de l'OID valide durant la période de validité de la déclaration
				final Integer officeImpotId = tiersService.getOfficeImpotIdAt(tiers, dateReference);
				if (officeImpotId != null) {
					idEnvoi = officeImpotId.toString();
				}
			}
			infoDocument.setIdEnvoi(idEnvoi);
			editiqueHelper.remplitAffranchissement(infoDocument, adresseEnvoiDetaillee);
		}
		catch (EditiqueException e) {
			throw e;
		}
		catch (Exception e) {
			final String originalMessage = StringUtils.trimToNull(e.getMessage());
			final String originalMessagePart = originalMessage == null ? StringUtils.EMPTY : String.format(" (%s)", originalMessage);
			final String message = "Exception lors du calcul de l'affranchissement de l'adresse du tiers " + tiers.getNumero() + originalMessagePart;
			LOGGER.error("Exception lors du calcul de l'affranchissement de l'adresse du tiers " + tiers.getNumero(), e);
			throw new EditiqueException(message);
		}
	}
}
