package ch.vd.unireg.documentfiscal;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.editique.ConstantesEditique;
import ch.vd.unireg.editique.EditiqueAbstractHelperImpl;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiquePrefixeHelper;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.unireg.registrefoncier.EstimationRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.MineRF;
import ch.vd.unireg.registrefoncier.PartCoproprieteRF;
import ch.vd.unireg.registrefoncier.ProprieteParEtageRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.xml.editique.pm.CTypeAffranchissement;
import ch.vd.unireg.xml.editique.pm.CTypeDegrevementImm;
import ch.vd.unireg.xml.editique.pm.CTypeImmeuble;
import ch.vd.unireg.xml.editique.pm.CTypeInfoArchivage;
import ch.vd.unireg.xml.editique.pm.CTypeInfoDocument;
import ch.vd.unireg.xml.editique.pm.CTypeInfoEnteteDocument;
import ch.vd.unireg.xml.editique.pm.FichierImpression;
import ch.vd.unireg.xml.editique.pm.STypeZoneAffranchissement;

public class ImpressionDemandeDegrevementICIHelperImpl extends EditiqueAbstractHelperImpl implements ImpressionDemandeDegrevementICIHelper {

	private static final String CODE_DOCUMENT_DEMANDE_DEGREVEMENT_ICI = TypeDocumentEditique.DEMANDE_DEGREVEMENT_ICI.getCodeDocumentEditique().substring(0, 4);

	/**
	 * Le champ "nature" doit être limité à 100 caractères en longueur
	 */
	private static final int NATURE_SIZE_LIMIT = 100;

	private RegistreFoncierService registreFoncierService;

	private static final Map<Class<? extends ImmeubleRF>, String> TYPES_IMMEUBLE = buildTypesImmeuble();

	private static Map<Class<? extends ImmeubleRF>, String> buildTypesImmeuble() {
		final Map<Class<? extends ImmeubleRF>, String> map = new HashMap<>();
		map.put(ProprieteParEtageRF.class, "PPE");
		map.put(DroitDistinctEtPermanentRF.class, "DDP");
		map.put(MineRF.class, "Mine");
		map.put(BienFondsRF.class, "Bien-fonds");
		map.put(PartCoproprieteRF.class, "Copropriété");
		return map;
	}

	public void setRegistreFoncierService(RegistreFoncierService registreFoncierService) {
		this.registreFoncierService = registreFoncierService;
	}

	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		return TypeDocumentEditique.DEMANDE_DEGREVEMENT_ICI;
	}

	@Override
	public FichierImpression.Document buildDocument(DemandeDegrevementICI demande, RegDate dateTraitement, boolean duplicata) throws EditiqueException {
		try {
			final Entreprise entreprise = demande.getEntreprise();
			final CTypeInfoDocument infoDocument = buildInfoDocument(getAdresseEnvoi(entreprise), entreprise);
			final CTypeInfoArchivage infoArchivage = buildInfoArchivagePM(getTypeDocumentEditique(), construitCleArchivage(demande), entreprise.getNumero(), dateTraitement);
			final String titre = String.format("%s %d", IMPOT_COMPLEMENTAIRE_IMMEUBLES, demande.getPeriodeFiscale());
			final RegDate dateEnvoi = duplicata ? dateTraitement : demande.getDateEnvoi();
			final CTypeInfoEnteteDocument infoEnteteDocument = buildInfoEnteteDocumentPM(entreprise, dateEnvoi, CAT_TRAITE_PAR, CAT_NOM_SERVICE_EXPEDITEUR, infraService.getACIOIPM(), infraService.getCAT(), titre);
			final CTypeDegrevementImm lettre = new CTypeDegrevementImm(XmlUtils.regdate2xmlcal(RegDate.get(demande.getPeriodeFiscale())),
			                                                           getSiegeEntreprise(entreprise, dateTraitement),
			                                                           buildCodeBarres(demande),
			                                                           demande.getCodeControle(),
			                                                           buildInfoImmeuble(demande));
			final FichierImpression.Document document = new FichierImpression.Document();
			document.setInfoDocument(infoDocument);
			document.setInfoArchivage(infoArchivage);
			document.setInfoEnteteDocument(infoEnteteDocument);
			document.setLettreDegrevementImm(lettre);
			return document;
		}
		catch (EditiqueException e) {
			throw e;
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}

	@Override
	public CTypeImmeuble buildInfoImmeuble(DemandeDegrevementICI demande) throws EditiqueException {
		final int periodeFiscale = demande.getPeriodeFiscale();
		final RegDate dateReference = RegDate.get(periodeFiscale, 1, 1);
		final ImmeubleRF immeuble = demande.getImmeuble();
		final String nomCommune = Optional.ofNullable(DemandeDegrevementICIHelper.getCommune(demande, registreFoncierService)).map(Commune::getNomOfficiel).orElse(null);

		final CTypeImmeuble type = new CTypeImmeuble();
		type.setCommune(nomCommune);
		type.setMontantFiscalRF(Optional.ofNullable(DemandeDegrevementICIHelper.getEstimationFiscale(demande, registreFoncierService)).map(EstimationRF::getMontant).map(Object::toString).orElse(null));
		type.setNature(DemandeDegrevementICIHelper.getNatureImmeuble(demande, NATURE_SIZE_LIMIT));
		type.setNoParcelle(DemandeDegrevementICIHelper.getNumeroParcelleComplet(demande, registreFoncierService));
		type.setType(getTypeImmeuble(immeuble));

		// [SIFISC-23531] de toute façon, ces données manquantes ne passeraient pas la rampe de la XSD éditique, clarifions le message autant que possible
		if (type.getCommune() == null || type.getNoParcelle() == null) {
			throw new EditiqueException(String.format("Immeuble %d sans donnée de commune et/ou de numéro de parcelle à la date de référence %s", immeuble.getId(), RegDateHelper.dateToDisplayString(dateReference)));
		}
		return type;
	}

	static String getTypeImmeuble(ImmeubleRF immeuble) {
		return TYPES_IMMEUBLE.get(immeuble.getClass());
	}

	@Override
	@Nullable
	public String getSiegeEntreprise(Entreprise entreprise, RegDate dateReference) {
		final ForFiscalPrincipalPM ffp = entreprise.getForFiscalPrincipalAt(dateReference);
		final String siege;
		if (ffp != null) {
			switch (ffp.getTypeAutoriteFiscale()) {
			case COMMUNE_HC:
			case COMMUNE_OU_FRACTION_VD:
				siege = Optional.of(ffp.getNumeroOfsAutoriteFiscale())
						.map(ofs -> infraService.getCommuneByNumeroOfs(ofs, dateReference))
						.map(Commune::getNomOfficielAvecCanton)
						.orElse(null);
				break;
			case PAYS_HS:
				siege = Optional.of(ffp.getNumeroOfsAutoriteFiscale())
						.map(ofs -> infraService.getPays(ofs, dateReference))
						.map(Pays::getNomCourt)
						.orElse(null);
				break;
			default:
				throw new IllegalArgumentException("Type d'autorité fiscale invalide : " + ffp.getTypeAutoriteFiscale());
			}
		}
		else {
			siege = null;
		}
		return siege;
	}

	@Override
	public String buildCodeBarres(DemandeDegrevementICI demande) {
		return String.format("070041000%04d%09d00%02d%05d",
		                     demande.getPeriodeFiscale(),
		                     demande.getEntreprise().getNumero(),
		                     ServiceInfrastructureService.noOIPM,
		                     demande.getNumeroSequence());
	}

	@Override
	public String construitIdDocument(DemandeDegrevementICI demande) {
		return String.format("DD %04d %d %09d %s",
		                     demande.getPeriodeFiscale(),
		                     demande.getNumeroSequence(),
		                     demande.getEntreprise().getNumero(),
		                     new SimpleDateFormat("yyyyMMddHHmmssSSS").format(DateHelper.getCurrentDate()));
	}

	@Override
	public String construitCleArchivage(DemandeDegrevementICI demande) {
		return String.format(
				"%04d%05d %s %s",
				demande.getPeriodeFiscale(),
				demande.getNumeroSequence(),
				StringUtils.rightPad("DD ICI", 16, ' '),
				new SimpleDateFormat("MMddHHmmssSSS").format(
						DateHelper.getCurrentDate()
				)
		);
	}

	private static CTypeInfoDocument buildInfoDocument(AdresseEnvoiDetaillee adresseEnvoi, Entreprise entreprise) {
		final CTypeInfoDocument infoDoc = new CTypeInfoDocument();

		final Pair<STypeZoneAffranchissement, String> infosAffranchissement = getInformationsAffranchissementPM(adresseEnvoi,
		                                                                                                        false,
		                                                                                                        ServiceInfrastructureService.noOIPM);
		final STypeZoneAffranchissement zoneAffranchissement = assigneIdEnvoiPM(infoDoc, entreprise, infosAffranchissement);
		infoDoc.setAffranchissement(new CTypeAffranchissement(zoneAffranchissement, null));
		infoDoc.setVersionXSD(VERSION_XSD_PM);

		infoDoc.setCodDoc(CODE_DOCUMENT_DEMANDE_DEGREVEMENT_ICI);
		infoDoc.setPopulations(ConstantesEditique.POPULATION_PM);
		infoDoc.setPrefixe(EditiquePrefixeHelper.buildPrefixeInfoDocument(TypeDocumentEditique.DEMANDE_DEGREVEMENT_ICI));
		infoDoc.setTypDoc(TYPE_DOCUMENT_CO);

		return infoDoc;
	}

}
