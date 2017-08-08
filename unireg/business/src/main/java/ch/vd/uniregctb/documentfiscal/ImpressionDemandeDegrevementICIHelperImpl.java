package ch.vd.uniregctb.documentfiscal;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.editique.unireg.CTypeAffranchissement;
import ch.vd.editique.unireg.CTypeDegrevementImm;
import ch.vd.editique.unireg.CTypeImmeuble;
import ch.vd.editique.unireg.CTypeInfoArchivage;
import ch.vd.editique.unireg.CTypeInfoDocument;
import ch.vd.editique.unireg.CTypeInfoEnteteDocument;
import ch.vd.editique.unireg.FichierImpression;
import ch.vd.editique.unireg.STypeZoneAffranchissement;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.editique.ConstantesEditique;
import ch.vd.uniregctb.editique.EditiqueAbstractHelperImpl;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiquePrefixeHelper;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.MineRF;
import ch.vd.uniregctb.registrefoncier.PartCoproprieteRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;

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
	public FichierImpression.Document buildDocument(DemandeDegrevementICI demande, RegDate dateTraitement) throws EditiqueException {
		try {
			final Entreprise entreprise = demande.getEntreprise();
			final CTypeInfoDocument infoDocument = buildInfoDocument(getAdresseEnvoi(entreprise), entreprise);
			final CTypeInfoArchivage infoArchivage = buildInfoArchivage(getTypeDocumentEditique(), construitCleArchivage(demande), entreprise.getNumero(), dateTraitement);
			final String titre = String.format("%s %d", IMPOT_COMPLEMENTAIRE_IMMEUBLES, demande.getPeriodeFiscale());
			final CTypeInfoEnteteDocument infoEnteteDocument = buildInfoEnteteDocument(entreprise, demande.getDateEnvoi(), TRAITE_PAR, NOM_SERVICE_EXPEDITEUR, infraService.getACIOIPM(), infraService.getCAT(), titre);
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

		final Pair<STypeZoneAffranchissement, String> infosAffranchissement = getInformationsAffranchissement(adresseEnvoi,
		                                                                                                      false,
		                                                                                                      ServiceInfrastructureService.noOIPM);
		final STypeZoneAffranchissement zoneAffranchissement = assigneIdEnvoi(infoDoc, entreprise, infosAffranchissement);
		infoDoc.setAffranchissement(new CTypeAffranchissement(zoneAffranchissement, null));
		infoDoc.setVersionXSD(VERSION_XSD);

		infoDoc.setCodDoc(CODE_DOCUMENT_DEMANDE_DEGREVEMENT_ICI);
		infoDoc.setPopulations(ConstantesEditique.POPULATION_PM);
		infoDoc.setPrefixe(EditiquePrefixeHelper.buildPrefixeInfoDocument(TypeDocumentEditique.DEMANDE_DEGREVEMENT_ICI));
		infoDoc.setTypDoc(TYPE_DOCUMENT_CO);

		return infoDoc;
	}

}
