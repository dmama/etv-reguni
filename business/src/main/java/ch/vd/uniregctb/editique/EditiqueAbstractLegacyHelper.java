package ch.vd.uniregctb.editique;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import noNamespace.InfoDocumentDocument1;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.editique.unireg.CTypeAdresse;
import ch.vd.editique.unireg.STypeZoneAffranchissement;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.etiquette.Etiquette;
import ch.vd.uniregctb.etiquette.EtiquetteTiers;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public abstract class EditiqueAbstractLegacyHelper {

	public static final Logger LOGGER = LoggerFactory.getLogger(EditiqueAbstractLegacyHelper.class);

	//
	// Différentes constantes
	//
	protected static final String ORIGINAL = "ORG";
	protected static final String LOGO_CANTON = "CANT";

	protected AdresseService adresseService;
	protected TiersService tiersService;
	protected LegacyEditiqueHelper legacyEditiqueHelper;

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setLegacyEditiqueHelper(LegacyEditiqueHelper legacyEditiqueHelper) {
		this.legacyEditiqueHelper = legacyEditiqueHelper;
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
			legacyEditiqueHelper.remplitAffranchissement(infoDocument, adresseEnvoiDetaillee);
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

	/**
	 * @param adresseEnvoi l'adresse à laquelle on veut envoyer un document
	 * @param idEnvoiSiEtranger <code>true</code> si une adresse à l'étranger doit être en fait traitée par un idEnvoi
	 * @param valeurIdEnvoi valeur à utiliser
	 * @return un type d'affranchissement et, éventuellement, un idEnvoi à utiliser pour
	 */
	@NotNull
	protected static Pair<STypeZoneAffranchissement, String> getInformationsAffranchissement(AdresseEnvoiDetaillee adresseEnvoi, boolean idEnvoiSiEtranger, int valeurIdEnvoi) {

		final STypeZoneAffranchissement zoneAffranchissement;
		final String idEnvoi;
		if (adresseEnvoi.isIncomplete()) {
			idEnvoi = String.valueOf(ServiceInfrastructureService.noOIPM);
			zoneAffranchissement = STypeZoneAffranchissement.NA;
		}
		else if (adresseEnvoi.getTypeAffranchissement() == null) {
			idEnvoi = String.valueOf(ServiceInfrastructureService.noOIPM);
			zoneAffranchissement = STypeZoneAffranchissement.NA;
		}
		else {
			switch (adresseEnvoi.getTypeAffranchissement()) {
			case SUISSE:
				idEnvoi = null;
				zoneAffranchissement = STypeZoneAffranchissement.CH;
				break;
			case EUROPE:
				idEnvoi = idEnvoiSiEtranger ? String.valueOf(ServiceInfrastructureService.noOIPM) : null;
				zoneAffranchissement = idEnvoi != null ? STypeZoneAffranchissement.NA : STypeZoneAffranchissement.EU;
				break;
			case MONDE:
				idEnvoi = idEnvoiSiEtranger ? String.valueOf(ServiceInfrastructureService.noOIPM) : null;
				zoneAffranchissement = idEnvoi != null ? STypeZoneAffranchissement.NA : STypeZoneAffranchissement.RM;
				break;
			default:
				throw new IllegalArgumentException("Type d'affranchissement non supporté : " + adresseEnvoi.getTypeAffranchissement());
			}
		}

		return Pair.of(zoneAffranchissement, idEnvoi);
	}

	protected AdresseEnvoiDetaillee getAdresseEnvoi(Tiers tiers) throws AdresseException {
		return adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
	}

	protected CTypeAdresse buildAdresse(CollectiviteAdministrative coll) throws AdresseException {
		final Tiers colAdm = tiersService.getCollectiviteAdministrative(coll.getNoColAdm());
		return buildAdresse(colAdm);
	}

	protected CTypeAdresse buildAdresse(Tiers tiers) throws AdresseException {
		final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
		return buildAdresse(adresse);
	}

	@Nullable
	protected CTypeAdresse buildAdresse(AdresseEnvoi adresseEnvoi) {
		final List<String> lignes = new ArrayList<>(6);
		for (String ligne : adresseEnvoi.getLignes()) {
			if (StringUtils.isNotBlank(ligne)) {
				lignes.add(ligne);
			}
		}
		return lignes.isEmpty() ? null : new CTypeAdresse(lignes);
	}

	@Nullable
	protected Integer getNoCollectiviteAdministrativeEmettriceSelonEtiquettes(Tiers tiers, @Nullable RegDate dateReference) {

		final Stream.Builder<Tiers> streamBuilder = Stream.builder();
		streamBuilder.add(tiers);
		if (tiers instanceof MenageCommun) {
			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple((MenageCommun) tiers, null);
			streamBuilder.add(ensemble.getPrincipal());
			streamBuilder.add(ensemble.getConjoint());
		}

		return streamBuilder.build()                                                                            // les tiers un par un
				.filter(Objects::nonNull)                                                                       // seulement ceux qui existent
				.map(Tiers::getEtiquettes)                                                                      // extraction des étiquettes tiers des tiers
				.filter(Objects::nonNull)                                                                       // seulement celles qui existent
				.flatMap(Collection::stream)                                                                    // mise à plat des étiquettes extraites
				.filter(e -> e.isValidAt(dateReference))                                                        // seulement les étiquetages non-annulés valides aujourd'hui
				.map(EtiquetteTiers::getEtiquette)                                                              // passage à l'étiquette elle-même
				.filter(e -> e.isActive() && e.isExpediteurDocuments() && !e.isAnnule())                        // seulement si étiquette valide, non-annulée, et marquée comme intéressante pour l'envoi de documents
				.map(Etiquette::getCollectiviteAdministrative)                                                  // passage à la collectivité administrative liée
				.filter(Objects::nonNull)                                                                       // seulement si une telle collectivité administrative existe
				.map(ch.vd.uniregctb.tiers.CollectiviteAdministrative::getNumeroCollectiviteAdministrative)     // extraction du numéro de la collectivité administrative
				.findFirst()                                                                                    // on prend de tout de façon le premier qui sort
				.orElse(null);
	}
}
