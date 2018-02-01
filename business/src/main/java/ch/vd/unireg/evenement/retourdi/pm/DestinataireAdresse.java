package ch.vd.unireg.evenement.retourdi.pm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.tiers.DonneeCivileEntreprise;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.IdentificationEntreprise;
import ch.vd.unireg.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.unireg.tiers.Tiers;

/**
 * Utilisable dans les adresses, pour désigner soit une personne physique, soit une organisation
 */
public abstract class DestinataireAdresse {

	/**
	 * @return un tiers bidon contenant juste les informations connues
	 */
	@NotNull
	public abstract Tiers buildDummyTiers();

	@Nullable
	public abstract String getContact();

	@Nullable
	protected static String concatRemovingNulls(String[] source) {
		if (source == null || source.length == 0) {
			return null;
		}
		final List<String> elements = new ArrayList<>(source.length);
		for (String src : source) {
			if (StringUtils.isNotBlank(src)) {
				elements.add(src.trim());
			}
		}
		return StringUtils.trimToNull(CollectionsUtils.toString(elements, StringRenderer.DEFAULT, " ", null));
	}

	/**
	 * Destinataire organisation
	 */
	public static final class Organisation extends DestinataireAdresse {

		private final String numeroIDE;
		private final String raisonSociale1;
		private final String raisonSociale2;
		private final String raisonSociale3;
		private final String contact;

		public Organisation(String numeroIDE, String raisonSociale1, String raisonSociale2, String raisonSociale3, String contact) {
			this.numeroIDE = StringUtils.trimToNull(numeroIDE);
			this.raisonSociale1 = StringUtils.trimToNull(raisonSociale1);
			this.raisonSociale2 = StringUtils.trimToNull(raisonSociale2);
			this.raisonSociale3 = StringUtils.trimToNull(raisonSociale3);
			this.contact = StringUtils.trimToNull(contact);
		}

		public String getNumeroIDE() {
			return numeroIDE;
		}

		@Nullable
		@Override
		public String getContact() {
			return contact;
		}

		public boolean isEmpty() {
			return numeroIDE == null && raisonSociale1 == null && raisonSociale2 == null && raisonSociale3 == null && contact == null;
		}

		@Nullable
		private String buildRaisonSociale() {
			final String[] raisonsSociales = { raisonSociale1, raisonSociale2, raisonSociale3 };
			return concatRemovingNulls(raisonsSociales);
		}

		@NotNull
		@Override
		public Tiers buildDummyTiers() {
			final String raisonSociale = buildRaisonSociale();
			final Set<DonneeCivileEntreprise> donneesCiviles;
			if (StringUtils.isNotBlank(raisonSociale)) {
				donneesCiviles = Collections.singleton(new RaisonSocialeFiscaleEntreprise(null, null, raisonSociale));
			}
			else {
				donneesCiviles = Collections.emptySet();
			}

			final Set<IdentificationEntreprise> ides;
			if (numeroIDE != null) {
				ides = Collections.singleton(new IdentificationEntreprise(numeroIDE));
			}
			else {
				ides = Collections.emptySet();
			}

			/**
			 * Tiers bidon qui ne contient que ces deux données, au maximum...
			 */
			return new Entreprise() {
				@Override
				public Set<DonneeCivileEntreprise> getDonneesCiviles() {
					return donneesCiviles;
				}

				@Override
				public Set<IdentificationEntreprise> getIdentificationsEntreprise() {
					return ides;
				}
			};
		}
	}

	/**
	 * Destinataire personne
	 */
	public static final class Personne extends DestinataireAdresse {

		private final Long numeroAVS;
		private final String prenom;
		private final String nom;
		private final String titre;

		public Personne(Long numeroAVS, String prenom, String nom, String titre) {
			this.numeroAVS = numeroAVS;
			this.titre = titre;
			this.prenom = StringUtils.trimToNull(prenom);
			this.nom = StringUtils.trimToNull(nom);
		}

		public Long getNumeroAVS() {
			return numeroAVS;
		}

		public String getPrenom() {
			return prenom;
		}

		public String getNom() {
			return nom;
		}

		public String getTitre() {
			return titre;
		}

		public boolean isEmpty() {
			return numeroAVS == null && prenom == null && nom == null && titre == null;
		}

		@NotNull
		@Override
		public Tiers buildDummyTiers() {
			return new PersonnePhysiqueAvecCivilite(numeroAVS, titre, prenom, nom);
		}

		@Nullable
		@Override
		public String getContact() {
			// pas de contact pour une personne physique
			return null;
		}
	}

}
