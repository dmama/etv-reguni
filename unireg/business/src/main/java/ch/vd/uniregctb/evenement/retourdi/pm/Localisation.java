package ch.vd.uniregctb.evenement.retourdi.pm;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.LocalisationFiscale;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Utilisable pour le siège, l'administration effective... pour toute entité, en fait, qui
 * représente un endroit soit donné par un simple nom, soit par une structure comprenant un numéro OFS
 */
public abstract class Localisation {

	/**
	 * @param infraService service infrastructure pour la résolution des différentes valeurs
	 * @param dateReference date de référence pour la résolution du nom
	 * @return le nom de la localisation
	 */
	public abstract String toDisplayString(ServiceInfrastructureService infraService, RegDate dateReference);

	/**
	 * @return la transcription fiscale de la localisation fournie
	 */
	@Nullable
	public abstract LocalisationFiscale transcriptionFiscale(ServiceInfrastructureService infraService, RegDate dateReference);

	/**
	 * Cas d'une saisie libre = juste un nom
	 */
	public static final class SaisieLibre extends Localisation {

		private final String nom;

		public SaisieLibre(String nom) {
			this.nom = StringUtils.trimToNull(nom);
		}

		@Override
		public String toDisplayString(ServiceInfrastructureService infraService, RegDate dateReference) {
			final Commune commune = findCommune(infraService, dateReference);
			return commune != null ? commune.getNomOfficielAvecCanton() : nom;
		}

		@Nullable
		@Override
		public LocalisationFiscale transcriptionFiscale(ServiceInfrastructureService infraService, RegDate dateReference) {
			final Commune commune = findCommune(infraService, dateReference);
			if (commune != null) {
				return new LocalisationFiscale() {
					@Override
					public TypeAutoriteFiscale getTypeAutoriteFiscale() {
						return commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
					}

					@Override
					public Integer getNumeroOfsAutoriteFiscale() {
						return commune.getNoOFS();
					}
				};
			}
			return null;
		}

		/**
		 * Récupère la commune si possible
		 * @param infraService service infrastructure
		 * @param dateReference date de référence
		 * @return la commune fiscale nommée en texte libre, si on la retrouve...
		 */
		@Nullable
		private Commune findCommune(ServiceInfrastructureService infraService, RegDate dateReference) {
			if (nom == null) {
				return null;
			}

			// on cherche dans les communes suisses
			final List<Commune> all = infraService.getCommunes();
			if (all != null && !all.isEmpty()) {
				for (final Commune commune : all) {
					if (commune.isValidAt(dateReference) && (StringEqualityHelper.equals(nom, commune.getNomCourt()) || StringEqualityHelper.equals(nom, commune.getNomOfficiel()))) {
						return commune;
					}
				}
			}

			// rien trouvé...
			return null;
		}
	}

	/**
	 * Cas d'une commune suisse donnée par son numéro OFS
	 */
	public static final class CommuneSuisse extends Localisation {

		private final int noOfsCommune;

		public CommuneSuisse(int noOfsCommune) {
			this.noOfsCommune = noOfsCommune;
		}

		public int getNoOfsCommune() {
			return noOfsCommune;
		}

		@Override
		public String toDisplayString(ServiceInfrastructureService infraService, RegDate dateReference) {
			final Commune commune = infraService.getCommuneByNumeroOfs(noOfsCommune, dateReference);
			if (commune == null) {
				return String.format("Commune inconnue (%d)", noOfsCommune);
			}
			return commune.getNomOfficielAvecCanton();
		}

		@Nullable
		@Override
		public LocalisationFiscale transcriptionFiscale(ServiceInfrastructureService infraService, RegDate dateReference) {
			final Commune commune = infraService.getCommuneByNumeroOfs(noOfsCommune, dateReference);
			if (commune != null) {
				return new LocalisationFiscale() {
					@Override
					public TypeAutoriteFiscale getTypeAutoriteFiscale() {
						return commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
					}

					@Override
					public Integer getNumeroOfsAutoriteFiscale() {
						return noOfsCommune;
					}
				};
			}
			// pas de commune...
			return null;
		}
	}

	/**
	 * Cas d'un pays étranger
	 */
	public static final class Etranger extends Localisation {

		private final int noOfsPays;
		private final String localite;

		public Etranger(int noOfsPays, String localite) {
			this.noOfsPays = noOfsPays;
			this.localite = localite;
		}

		@Override
		public String toDisplayString(ServiceInfrastructureService infraService, RegDate dateReference) {
			final Pays pays = infraService.getPays(noOfsPays, dateReference);
			if (pays == null) {
				return String.format("%s (Pays inconnu - %d)", StringUtils.defaultIfBlank(localite, "?"), noOfsPays);
			}
			if (StringUtils.isBlank(localite)) {
				return pays.getNomCourt();
			}
			else {
				return String.format("%s (%s)", localite, pays.getNomCourt());
			}
		}

		@Nullable
		@Override
		public LocalisationFiscale transcriptionFiscale(ServiceInfrastructureService infraService, RegDate dateReference) {
			final Pays pays = infraService.getPays(noOfsPays, dateReference);
			if (pays != null) {
				return new LocalisationFiscale() {
					@Override
					public TypeAutoriteFiscale getTypeAutoriteFiscale() {
						return TypeAutoriteFiscale.PAYS_HS;
					}

					@Override
					public Integer getNumeroOfsAutoriteFiscale() {
						return noOfsPays;
					}
				};
			}
			// pas de pays...
			return null;
		}
	}
}
