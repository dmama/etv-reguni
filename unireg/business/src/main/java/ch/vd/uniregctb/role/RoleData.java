package ch.vd.uniregctb.role;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.LignesAdresse;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.LocalisationFiscale;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Classe de base des données présentées dans les rôles des communes, OID, OIPM... pour tous
 * types de contribuables
 */
public abstract class RoleData {

	private static final Logger LOGGER = LoggerFactory.getLogger(RoleData.class);

	public enum TypeContribuable {
		ORDINAIRE("Vaudois ordinaire"),
		HORS_CANTON("Hors canton"),
		HORS_SUISSE("Hors Suisse"),
		SOURCE("Source"),
		DEPENSE("Dépense"),
		MIXTE("Sourcier mixte");

		public final String displayLabel;

		TypeContribuable(String displayLabel) {
			this.displayLabel = displayLabel;
		}

		public static TypeContribuable fromModeImposition(ModeImposition modeImposition) {
			switch (modeImposition) {
			case DEPENSE:
				return DEPENSE;
			case ORDINAIRE:
			case INDIGENT:
				return ORDINAIRE;
			case MIXTE_137_1:
			case MIXTE_137_2:
				return MIXTE;
			case SOURCE:
				return SOURCE;
			default:
				throw new IllegalArgumentException("Mode d'imposition inconnu ici : " + modeImposition);
			}
		}
	}

	public final long noContribuable;
	public final int noOfsCommune;
	public final String nomCommune;
	public final String[] adresseEnvoi;
	public final TypeContribuable typeContribuable;
	public final LocalisationFiscale domicileFiscal;
	public final String nomDomicileFiscal;

	public RoleData(Contribuable contribuable, int ofsCommune, int annee, AdresseService adresseService, ServiceInfrastructureService infrastructureService) {
		final RegDate dateReference = RegDate.get(annee, 12, 31);
		this.noContribuable = contribuable.getNumero();
		this.noOfsCommune = ofsCommune;
		this.nomCommune = fillNomCommune(ofsCommune, dateReference, infrastructureService);
		this.adresseEnvoi = fillAdresseEnvoi(contribuable, dateReference, adresseService);
		this.typeContribuable = fillTypeContribuable(contribuable, dateReference);
		this.domicileFiscal = fillDomicileFiscal(contribuable, dateReference);
		this.nomDomicileFiscal = fillNomDomicileFiscal(this.domicileFiscal, dateReference, infrastructureService);
	}

	@Override
	public String toString() {
		return String.format("Contribuable %d, %s, domicilié à %s/%d",
		                     noContribuable,
		                     typeContribuable,
		                     Optional.ofNullable(domicileFiscal).map(LocalisationFiscale::getTypeAutoriteFiscale).orElse(null),
		                     Optional.ofNullable(domicileFiscal).map(LocalisationFiscale::getNumeroOfsAutoriteFiscale).orElse(null));
	}

	@Nullable
	private static String fillNomCommune(int ofsCommune, RegDate dateReference, ServiceInfrastructureService infrastructureService) {
		try {
			final Commune commune = infrastructureService.getCommuneByNumeroOfs(ofsCommune, dateReference);
			return Optional.ofNullable(commune).map(Commune::getNomOfficiel).orElse(null);
		}
		catch (ServiceInfrastructureException e) {
			LOGGER.error("Impossible de récupérer le nom de la commune " + ofsCommune + " au " + RegDateHelper.dateToDisplayString(dateReference), e);
			return null;
		}
	}

	@Nullable
	private static String[] fillAdresseEnvoi(Contribuable contribuable, RegDate dateReference, AdresseService adresseService) {
		try {
			final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(contribuable, dateReference, TypeAdresseFiscale.COURRIER, false);
			return Optional.ofNullable(adresse)
					.map(AdresseEnvoiDetaillee::getLignes)
					.map(LignesAdresse::asTexte)
					.orElse(null);
		}
		catch (AdresseException e) {
			LOGGER.error("Impossible de récupérer l'adresse du contribuable " + FormatNumeroHelper.numeroCTBToDisplay(contribuable.getNumero()) + " au " + RegDateHelper.dateToDisplayString(dateReference), e);
			return null;
		}
	}

	@Nullable
	private static TypeContribuable fillTypeContribuable(Contribuable contribuable, RegDate dateReference) {
		final ForFiscalPrincipal ffp = contribuable.getDernierForFiscalPrincipalAvant(dateReference);
		switch (ffp.getTypeAutoriteFiscale()) {
		case COMMUNE_HC:
			return TypeContribuable.HORS_CANTON;
		case PAYS_HS:
			return TypeContribuable.HORS_SUISSE;
		case COMMUNE_OU_FRACTION_VD:
			return Optional.of(ffp)
					.filter(f -> f instanceof ForFiscalPrincipalPP)
					.map(f -> (ForFiscalPrincipalPP) f)
					.map(ForFiscalPrincipalPP::getModeImposition)
					.map(TypeContribuable::fromModeImposition)
					.orElse(TypeContribuable.ORDINAIRE);
		default:
			throw new IllegalArgumentException("Type d'autorité fiscale inconnue : " + ffp.getTypeAutoriteFiscale());
		}
	}

	@Nullable
	private static LocalisationFiscale fillDomicileFiscal(Contribuable contribuable, RegDate dateReference) {
		final ForFiscalPrincipal ffp = contribuable.getDernierForFiscalPrincipalAvant(dateReference);
		if (ffp == null) {
			// cas bizarre sans for principal avant la date de référence mais qui ferait quand-même partie du
			// rôle de l'année de la date de référence... ????
			return null;
		}

		final TypeAutoriteFiscale taf = ffp.getTypeAutoriteFiscale();
		final Integer ofs = ffp.getNumeroOfsAutoriteFiscale();

		// je crée une instance qui ne tient pas le for pour éviter de garder tous les fors en mémoire
		// dans les données collectées (qui peuvent être en très grand nombre dans la consolidation au niveau
		// du rapport final..) ; on ne garde ici que des éléments de toute petite taille sans
		// liens externes, au contraire de ce que pourrait être un for (entité persistante, lien vers le contribuable...)
		return new LocalisationFiscale() {
			@Override
			public TypeAutoriteFiscale getTypeAutoriteFiscale() {
				return taf;
			}

			@Override
			public Integer getNumeroOfsAutoriteFiscale() {
				return ofs;
			}
		};
	}

	@Nullable
	private static String fillNomDomicileFiscal(LocalisationFiscale localisation, RegDate dateReference, ServiceInfrastructureService infraService) {
		if (localisation == null) {
			return null;
		}

		try {
			switch (localisation.getTypeAutoriteFiscale()) {
			case COMMUNE_HC: {
				final Commune commune = infraService.getCommuneByNumeroOfs(localisation.getNumeroOfsAutoriteFiscale(), dateReference);
				return Optional.ofNullable(commune).map(Commune::getNomOfficielAvecCanton).orElse(null);
			}
			case COMMUNE_OU_FRACTION_VD: {
				final Commune commune = infraService.getCommuneByNumeroOfs(localisation.getNumeroOfsAutoriteFiscale(), dateReference);
				return Optional.ofNullable(commune).map(Commune::getNomOfficiel).orElse(null);
			}
			case PAYS_HS: {
				final Pays pays = infraService.getPays(localisation.getNumeroOfsAutoriteFiscale(), dateReference);
				return Optional.ofNullable(pays).map(Pays::getNomCourt).orElse(null);
			}
			default:
				throw new IllegalArgumentException("Type d'autorité fiscale inconnue : " + localisation.getTypeAutoriteFiscale());
			}
		}
		catch (ServiceInfrastructureException e) {
			LOGGER.error("Impossible de récupérer le nom de la localisation fiscale %s/%d", localisation.getTypeAutoriteFiscale(), localisation.getNumeroOfsAutoriteFiscale());
			return null;
		}
	}
}
