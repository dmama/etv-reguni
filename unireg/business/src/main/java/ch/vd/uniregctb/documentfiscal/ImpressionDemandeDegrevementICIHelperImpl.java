package ch.vd.uniregctb.documentfiscal;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

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
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.editique.ConstantesEditique;
import ch.vd.uniregctb.editique.EditiqueAbstractHelperImpl;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiquePrefixeHelper;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.MineRF;
import ch.vd.uniregctb.registrefoncier.PartCoproprieteRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;
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
		map.put(BienFondRF.class, "Bien-fonds");
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
			final CTypeInfoEnteteDocument infoEnteteDocument = buildInfoEnteteDocument(entreprise, demande.getDateEnvoi(), TRAITE_PAR, NOM_SERVICE_EXPEDITEUR, infraService.getACIOIPM(), infraService.getCAT(), IMPOT_COMPLEMENTAIRE_IMMEUBLES);
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
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}

	private CTypeImmeuble buildInfoImmeuble(DemandeDegrevementICI demande) {
		final int periodeFiscale = demande.getPeriodeFiscale();
		final RegDate dateReference = RegDate.get(periodeFiscale, 1, 1);
		final ImmeubleRF immeuble = demande.getImmeuble();
		final String nomCommune = Optional.ofNullable(registreFoncierService.getCommune(immeuble, dateReference)).map(Commune::getNomOfficiel).orElse(null);

		final CTypeImmeuble type = new CTypeImmeuble();
		type.setCommune(nomCommune);
		type.setMontantFiscalRF(Optional.ofNullable(registreFoncierService.getEstimationFiscale(immeuble, dateReference)).map(Object::toString).orElse(null));
		type.setNature(getNatureImmeuble(immeuble, dateReference));
		type.setNoParcelle(registreFoncierService.getNumeroParcelleComplet(immeuble, dateReference));
		type.setType(getTypeImmeuble(immeuble));
		return type;
	}

	static String getTypeImmeuble(ImmeubleRF immeuble) {
		return Optional.of(immeuble)
				.map(Object::getClass)
				.map(TYPES_IMMEUBLE::get)
				.orElse(null);
	}

	static String getNatureImmeuble(ImmeubleRF immeuble, RegDate dateReference) {
		final List<String> composantesNature = immeuble.getSurfacesAuSol().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(ss -> ss.isValidAt(dateReference))        // immeuble -> surfaces au sol valides non-annulés
				.sorted(Comparator.comparingInt(SurfaceAuSolRF::getSurface).reversed())
				.map(SurfaceAuSolRF::getType)
				.map(StringUtils::trimToNull)
				.filter(Objects::nonNull)                         // surface au sol -> type
				.distinct()                                       // ça ne sert à rien de répêter plusieurs fois la même chose
				.collect(Collectors.toList());

		if (composantesNature.isEmpty()) {
			return null;
		}

		// [SIFISC-23178] on prend des composantes entières (sauf si la première est déjà trop grande)
		while (true) {
			final String nature = composantesNature.stream().collect(Collectors.joining(" / "));
			if (nature.length() <= NATURE_SIZE_LIMIT) {
				return nature;
			}

			if (composantesNature.size() == 1) {
				// un seul élément trop grand... abréviation
				return StringUtils.abbreviate(nature, NATURE_SIZE_LIMIT);
			}

			// on enlève un élément et on ré-essaie
			composantesNature.remove(composantesNature.size() - 1);
		}
	}

	private String getSiegeEntreprise(Entreprise entreprise, RegDate dateReference) {
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

	private static String buildCodeBarres(DemandeDegrevementICI demande) {
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

		final Pair<STypeZoneAffranchissement, String> infosAffranchissement = getInformationsAffranchissement(adresseEnvoi, false, ServiceInfrastructureService.noOIPM);
		assigneIdEnvoi(infoDoc, entreprise, infosAffranchissement);
		infoDoc.setAffranchissement(new CTypeAffranchissement(infosAffranchissement.getLeft(), null));
		infoDoc.setVersionXSD(VERSION_XSD);

		infoDoc.setCodDoc(CODE_DOCUMENT_DEMANDE_DEGREVEMENT_ICI);
		infoDoc.setPopulations(ConstantesEditique.POPULATION_PM);
		infoDoc.setPrefixe(EditiquePrefixeHelper.buildPrefixeInfoDocument(TypeDocumentEditique.DEMANDE_DEGREVEMENT_ICI));
		infoDoc.setTypDoc(TYPE_DOCUMENT_CO);

		return infoDoc;
	}

}
