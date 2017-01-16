package ch.vd.uniregctb.evenement.retourdi.pm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.uniregctb.adresse.AdresseAdapter;
import ch.vd.uniregctb.adresse.AdresseCivileAdapter;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.StringComparator;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeAdresseCivil;

/**
 * Utilisable pour l'adresse de l'entreprise comme pour l'adresse du mandataire, pour toute entité
 * d'adresse qui peut se représenter comme une succession de lignes plus ou moins libres ou par une donnée structurée
 */
public abstract class AdresseRaisonSociale {

	private static final Logger LOGGER = LoggerFactory.getLogger(AdresseRaisonSociale.class);

	/**
	 * @param infraService service infrastructure
	 * @param adresseService service de composition d'adresses
	 * @param dateReference date de référence
	 * @return une représentation textuelle lisible de l'adresse
	 */
	public abstract String toDisplayString(ServiceInfrastructureService infraService, AdresseService adresseService, RegDate dateReference);

	/**
	 * @param infraService service infrastructure
	 * @param tiersService service tiers
	 * @param dateReference date de référence
	 * @return une pair composée de la raison sociale et d'une adresse, si disponible / identifiable
	 */
	@Nullable
	public abstract Pair<String, Adresse> split(ServiceInfrastructureService infraService, TiersService tiersService, RegDate dateReference);

	/**
	 * @return l'information de contact fournie
	 */
	@Nullable
	public abstract String getContact();

	/**
	 * @return un destinataire
	 */
	@Nullable
	public abstract DestinataireAdresse getDestinataire();

	/**
	 * @return <code>true</code> si cette adresse ne contient que des informations sur le destinataire, par construction (parce que les autres ont été explicitement et volontairement ignorées)
	 */
	public boolean isDestinataireSeul() {
		return false;
	}

	/**
	 * Cas d'une saisie libre : 5 lignes complètement libres + un NPA et une localité postale
	 */
	public static final class Brutte extends AdresseRaisonSociale {

		private final String ligne1;
		private final String ligne2;
		private final String ligne3;
		private final String ligne4;
		private final String ligne5;
		private final String personneContact;
		private final String npa;
		private final String localite;

		public Brutte(String ligne1, String ligne2, String ligne3, String ligne4, String ligne5, String personneContact, String npa, String localite) {
			this.ligne1 = StringUtils.trimToNull(ligne1);
			this.ligne2 = StringUtils.trimToNull(ligne2);
			this.ligne3 = StringUtils.trimToNull(ligne3);
			this.ligne4 = StringUtils.trimToNull(ligne4);
			this.ligne5 = StringUtils.trimToNull(ligne5);
			this.personneContact = StringUtils.trimToNull(personneContact);
			this.npa = StringUtils.trimToNull(npa);
			this.localite = StringUtils.trimToNull(localite);
		}

		public String getLigne1() {
			return ligne1;
		}

		public String getLigne2() {
			return ligne2;
		}

		public String getLigne3() {
			return ligne3;
		}

		public String getLigne4() {
			return ligne4;
		}

		public String getLigne5() {
			return ligne5;
		}

		public String getPersonneContact() {
			return personneContact;
		}

		public String getNpa() {
			return npa;
		}

		public String getLocalite() {
			return localite;
		}

		public boolean isEmpty() {
			return ligne1 == null && ligne2 == null && ligne3 == null && ligne4 == null && ligne5 == null && npa == null && localite == null;
		}

		@Override
		public String toDisplayString(ServiceInfrastructureService infraService, AdresseService adresseService, RegDate dateReference) {
			return toDisplayString();
		}

		private String toDisplayString() {
			final StringBuilder b = new StringBuilder();
			final String[] strings = { ligne1, ligne2, ligne3, ligne4, ligne5, npa, localite };
			for (String s : strings) {
				if (s != null) {
					if (b.length() > 0) {
						b.append(" / ");
					}
					b.append(s);
				}
			}
			return b.toString();
		}

		@Nullable
		@Override
		public Pair<String, Adresse> split(ServiceInfrastructureService infraService, TiersService tiersService, RegDate dateReference) {
			// tout d'abord, interprétation du npa et de la localité pour trouver les bonnes rues officielles
			// puis comparaison de ce que l'on a avec ces fameuses rues...

			// si pas de NPA, c'est plutôt simple...
			if (npa == null) {
				return null;
			}

			// est-ce un NPA utilisable ?
			final int npaEntier;
			try {
				npaEntier = Integer.parseInt(npa);
				if (npaEntier < 0 || npaEntier > 9999) {
					throw new NumberFormatException("Un NPA suisse devrait être compris entre 0 et 9999");
				}
			}
			catch (NumberFormatException e) {
				LOGGER.error("Information de NPA non-compatible avec un NPA suisse : '" + npa + "'.", e);
				return null;
			}

			// on a donc quelque chose...
			final List<Localite> localitesPostales = getLocalitesPostalesCandidates(npaEntier, localite, dateReference, infraService);
			if (localitesPostales.isEmpty()) {
				LOGGER.error(String.format("Aucune localité postale suisse ne correspond au NPA donné (%s).", npaEntier));
				return null;
			}

			// un joli log peut grandement aider...
			final StringRenderer<Localite> onrpRenderer = localite -> Objects.toString(localite.getNoOrdre());
			final String descriptionLocalite = String.format("%d %s (onrp %s)", npaEntier, localite, CollectionsUtils.toString(localitesPostales, onrpRenderer, ", "));

			// quelles sont les rues officielles de ces localités postales ?
			final List<Rue> ruesOfficielles = new LinkedList<>();
			for (Localite localitePostale : localitesPostales) {
				final List<Rue> rues = infraService.getRues(localitePostale);
				if (rues != null) {
					ruesOfficielles.addAll(rues);
				}
			}
			if (ruesOfficielles.isEmpty()) {
				LOGGER.warn(String.format("Aucune rue connue sur la localité postale %s.", descriptionLocalite));
				return null;
			}

			// analyse des lignes
			final SplitData data = comparerRues(descriptionLocalite, ruesOfficielles);
			if (data == null) {
				// rien trouvé...
				LOGGER.warn(String.format("Aucune rue identifiée sur la localité postale %s depuis les lignes fournies (%s).",
				                          descriptionLocalite,
				                          toDisplayString()));
				return null;
			}

			// on a trouvé une rue !!!
			// -> tout ce qui est avant est une raison sociale, et on essaie de constuire une adresse avec le reste
			final String raisonSociale = data.extractRaisonSociale();

			// et l'adresse avec le reste ?
			final Map<Integer, Localite> localitesParONRP = new HashMap<>(localitesPostales.size());
			for (Localite localite : localitesPostales) {
				localitesParONRP.put(localite.getNoOrdre(), localite);
			}
			final Adresse adresse = data.buildAdresse(localitesParONRP.get(data.rueIdentifiee.getNoLocalite()));
			return Pair.of(raisonSociale, adresse);
		}

		/**
		 * @return un tableau (dans le même ordre que les lignes données) des lignes non-vides, de la ligne 1 à la ligne 5
		 */
		@NotNull
		private String[] getLignes1a5() {
			final List<String> lignes = new ArrayList<>(5);
			final String[] lignesBruttes = { ligne1, ligne2, ligne3, ligne4, ligne5 };
			for (String ligne : lignesBruttes) {
				if (ligne != null) {
					lignes.add(ligne);
				}
			}
			return lignes.toArray(new String[lignes.size()]);
		}

		/**
		 * Retrouve la localité postale indiquée par le NPA et le nom (si aucun nom n'est donné / ne correspond, toute les localités correspondantes seront retournées)
		 * @param npa npa suisse d'une localité
		 * @param nom nom associé au NPA (des fois qu'il y en aurait plusieurs)
		 * @param dateReference date de référence pour les résolutions de nom...
		 * @param infraService service infrastructure
		 * @return la localité trouvée, ou <code>null</code> si rien de bien convaincant
		 */
		@NotNull
		private static List<Localite> getLocalitesPostalesCandidates(int npa, String nom, RegDate dateReference, ServiceInfrastructureService infraService) {
			// toutes les localités avec le NPA
			final List<Localite> all = infraService.getLocalitesByNPA(npa, dateReference);
			if (all == null || all.isEmpty()) {
				return Collections.emptyList();
			}

			// filtrage sur les données valides à la date de référence, et indexation par nom
			final Map<String, Localite> parNom = new HashMap<>(2 * all.size());
			for (Localite localite : all) {
				parNom.put(canonize(localite.getNom()), localite);
				parNom.put(canonize(localite.getNomAbrege()), localite);
			}

			final String nomCanonise = canonize(nom);
			final Localite unique = parNom.get(nomCanonise);
			if (unique != null) {
				return Collections.singletonList(unique);
			}
			else {
				return all.isEmpty() ? Collections.emptyList() : all;
			}
		}

		/**
		 * Donnée récupérée après l'analyse des lignes, qui permet d'extraire la raison sociale, la rue, le numéro de maison...
		 */
		private static final class SplitData {

			final String[] lignes;
			final Rue rueIdentifiee;
			final int indexDebutRue;
			final int indexFinRue;

			public SplitData(String[] lignes, Rue rueIdentifiee, int indexDebutRue, int indexFinRue) {
				this.lignes = lignes;
				this.rueIdentifiee = rueIdentifiee;
				this.indexDebutRue = indexDebutRue;
				this.indexFinRue = indexFinRue;
			}

			@Nullable
			private static String concatFromTo(int indexDebut, int indexFin, String[] lignes) {
				if (indexDebut > indexFin) {
					return null;
				}
				String s = lignes[indexDebut];
				for (int i = indexDebut + 1 ; i <= indexFin ; ++ i) {
					s = concat(s, lignes[i]);
				}
				return s;
			}

			@Nullable
			public String extractRaisonSociale() {
				return concatFromTo(0, indexDebutRue - 1, lignes);
			}

			@Nullable
			private String extractNumeroMaison() {
				// on reconstitue d'abord la ligne complète de la rue
				final String lignesRue = concatFromTo(indexDebutRue, indexFinRue, lignes);
				if (lignesRue != null) {
					final String apresRue = lignesRue.substring(rueIdentifiee.getDesignationCourrier().length());
					if (StringUtils.isNotBlank(apresRue)) {
						// on va dire que le premier mot trouvé ici est le numéro de maison
						final String[] words = apresRue.trim().split("\\b");
						if (words.length > 0) {
							return words[0];
						}
					}
				}
				return null;
			}

			public Adresse buildAdresse(final Localite localitePostale) {
				final String numeroMaison = extractNumeroMaison();
				return new Adresse() {
					@Override
					public CasePostale getCasePostale() {
						return null;
					}

					@Override
					public RegDate getDateDebut() {
						return null;
					}

					@Override
					public RegDate getDateFin() {
						return null;
					}

					@Override
					public String getLocalite() {
						return localitePostale.getNom();
					}

					@Override
					public String getNumero() {
						return numeroMaison;
					}

					@Override
					public Integer getNumeroOrdrePostal() {
						return localitePostale.getNoOrdre();
					}

					@Override
					public String getNumeroPostal() {
						return localitePostale.getNPA().toString();
					}

					@Override
					public String getNumeroPostalComplementaire() {
						return null;
					}

					@Override
					public Integer getNoOfsPays() {
						return ServiceInfrastructureService.noOfsSuisse;
					}

					@Override
					public String getRue() {
						// pas la peine de résoudre le nom, puisqu'on a un numéro
						return null;
					}

					@Override
					public Integer getNumeroRue() {
						return rueIdentifiee.getNoRue();
					}

					@Override
					public String getNumeroAppartement() {
						return null;
					}

					@Override
					public String getTitre() {
						return null;
					}

					@Override
					public TypeAdresseCivil getTypeAdresse() {
						return TypeAdresseCivil.COURRIER;
					}

					@Override
					public Integer getEgid() {
						return null;
					}

					@Override
					public Integer getEwid() {
						return null;
					}

					@Nullable
					@Override
					public Localisation getLocalisationPrecedente() {
						return null;
					}

					@Nullable
					@Override
					public Localisation getLocalisationSuivante() {
						return null;
					}

					@Nullable
					@Override
					public Integer getNoOfsCommuneAdresse() {
						return null;
					}
				};
			}
		}

		/**
		 * Essaie de récupérer la bonne rue identifiée dans les lignes
		 */
		@Nullable
		private SplitData comparerRues(String npaEtNomLocalite, List<Rue> ruesOfficielles) {

			// constitution d'un tableau des lignes utilisables
			final String[] lignes = getLignes1a5();
			if (lignes.length == 0) {
				LOGGER.warn(String.format("Aucune ligne d'adresse utilisable pour comparer aux rues de la localité postale '%s'.", npaEtNomLocalite));
				return null;
			}
			final String[] lignesMinuscules = new String[lignes.length];
			for (int i = 0 ; i < lignes.length ; ++ i) {
				lignesMinuscules[i] = canonize(lignes[i]);
			}

			// ok, maintenant il faut comparer...
			for (Rue rueOfficielle : ruesOfficielles) {
				final String nomOfficielRue = rueOfficielle.getDesignationCourrier();
				final String nomOfficielRueMinuscules = canonize(nomOfficielRue);
				int index = lignes.length - 1;
				while (index >= 0) {
					final Integer maxIndex = getIndexMaxBonneRue(nomOfficielRueMinuscules, index, lignesMinuscules, lignesMinuscules[index]);
					if (maxIndex != null) {
						// c'est la bonne !!
						return new SplitData(lignes, rueOfficielle, index, maxIndex);
					}
					--index;
				}
			}

			// vraiment rien trouvé...
			return null;
		}

		/**
		 * @param string une chaîne source
		 * @return une chaîne toute en minuscules, en remplaçant les caractères spéciaux par des espaces et les multiples espaces par un seul
		 */
		@Nullable
		private static String canonize(@Nullable String string) {
			if (string == null) {
				return null;
			}
			return StringUtils.trimToNull(StringComparator.toLowerCaseWithoutAccent(string).replaceAll("[^A-Za-z0-9 ]", " ").replaceAll(" {2,}", " "));
		}

		/**
		 * @return <code>true</code> si la rue dont le nom officiel est donné est bien trouvée dans la ligne qui commence à l'index donné (avec éventuellement l'aide des suivantes)
		 */
		@Nullable
		private Integer getIndexMaxBonneRue(String nomOfficielRueMinuscules, int index, String[] lignesMinuscules, String ligne) {
			if (ligne.startsWith(nomOfficielRueMinuscules)) {
				// c'est la bonne, directement ...
				return index;
			}
			else if (nomOfficielRueMinuscules.startsWith(ligne) && index < lignesMinuscules.length - 1) {
				// là, c'est quelqu'un qui a commencé la rue sur ligne et a peut-être terminé sur la/les suivante(s)...

				// essayons de concaténer avec la ligne suivante, pour voir
				final String ligneSuivante = lignesMinuscules[index + 1];
				return getIndexMaxBonneRue(nomOfficielRueMinuscules, index + 1, lignesMinuscules, concat(ligne, ligneSuivante));     // appel récursif
			}
			else {
				// non, vraiment pas...
				return null;
			}
		}

		/**
		 * @param before la chaîne de caractères qui vient d'abord
		 * @param after la chaîne de caractères qui vient ensuite
		 * @return la chaîne de caractères concaténée, sachant que la concaténation se fait par un espace si on a une lettre de chaque côté, et directement sinon
		 */
		private static String concat(String before, String after) {
			final boolean withSpace = Character.isLetter(before.codePointAt(before.length() - 1)) && Character.isLetter(after.codePointAt(0));
			return String.format("%s%s%s", before, withSpace ? " " : StringUtils.EMPTY, after);
		}

		@Nullable
		@Override
		public String getContact() {
			return personneContact;
		}

		@Nullable
		@Override
		public DestinataireAdresse getDestinataire() {
			// il faut utiliser les données qui sortent du split...
			return null;
		}
	}

	/**
	 * Adresse structurée, soit en Suisse, soit à l'étranger
	 */
	public static abstract class Structuree extends AdresseRaisonSociale implements Adresse {

		private final DestinataireAdresse destinataire;
		private final String ligne1;
		private final String ligne2;
		private final String rue;
		private final String numeroMaison;
		private final CasePostale casePostale;
		private final String localite;

		public Structuree(DestinataireAdresse destinataire, String ligne1, String ligne2, String rue, String numeroMaison, CasePostale casePostale, String localite) {
			this.destinataire = destinataire;
			this.ligne1 = ligne1;
			this.ligne2 = ligne2;
			this.rue = rue;
			this.numeroMaison = numeroMaison;
			this.casePostale = casePostale;
			this.localite = localite;
		}

		public DestinataireAdresse getDestinataire() {
			return destinataire;
		}

		@Nullable
		@Override
		public String getContact() {
			// allons demander au destinataire
			return destinataire != null ? destinataire.getContact() : null;
		}

		@Override
		public String getTitre() {
			return StringUtils.defaultIfBlank(ligne1, StringUtils.defaultIfBlank(ligne2, null));
		}

		@Override
		public String getRue() {
			return rue;
		}

		@Override
		public String getNumero() {
			return numeroMaison;
		}

		@Override
		public CasePostale getCasePostale() {
			return casePostale;
		}

		@Override
		public String getLocalite() {
			return localite;
		}

		@Override
		public RegDate getDateDebut() {
			return null;
		}

		@Override
		public RegDate getDateFin() {
			return null;
		}

		@Override
		public boolean isValidAt(RegDate date) {
			return true;
		}

		@Nullable
		@Override
		public Localisation getLocalisationPrecedente() {
			return null;
		}

		@Nullable
		@Override
		public Localisation getLocalisationSuivante() {
			return null;
		}

		@Override
		public Integer getEgid() {
			return null;
		}

		@Override
		public Integer getEwid() {
			return null;
		}

		@Override
		public TypeAdresseCivil getTypeAdresse() {
			return TypeAdresseCivil.COURRIER;
		}

		@Override
		public String getNumeroAppartement() {
			return null;
		}

		@Nullable
		@Override
		public Integer getNoOfsCommuneAdresse() {
			return null;
		}

		@Override
		public String toDisplayString(ServiceInfrastructureService infraService, AdresseService adresseService, RegDate dateReference) {
			final Tiers tiers = destinataire != null ? destinataire.buildDummyTiers() : new PersonnePhysique();
			final AdresseGenerique.Source source = new AdresseGenerique.Source(AdresseGenerique.SourceType.FISCALE, tiers);
			try {
				final AdresseAdapter adapter = new AdresseCivileAdapter(this, dateReference, dateReference, source, false, infraService);
				final AdresseEnvoiDetaillee adresseEnvoi = adresseService.buildAdresseEnvoi(tiers, adapter, dateReference);
				return CollectionsUtils.toString(Arrays.asList(adresseEnvoi.getLignes()), StringRenderer.DEFAULT, " / ");
			}
			catch (DonneesCivilesException e) {
				// cas normalement impossible (produit par le AdresseCivileAdapter dans le cas où le range est faux, mais il ne l'est pas...)
				throw new IllegalStateException(e);
			}
			catch (AdresseException e) {
				// on n'arrive pas à générer l'adresse... tant pis
				LOGGER.warn("Impossible de reconstruire l'adresse...", e);
				return String.format("Erreur à la reconstruction de l'adresse : %s", e.getMessage());
			}
		}

		@Nullable
		@Override
		public Pair<String, Adresse> split(ServiceInfrastructureService infraService, TiersService tiersService, RegDate dateReference) {
			if (destinataire != null) {
				final Tiers tiers = destinataire.buildDummyTiers();
				final String raisonSociale = tiersService.getNomRaisonSociale(tiers);
				return Pair.of(raisonSociale, this);
			}
			else {
				return Pair.of(null, this);
			}
		}
	}

	/**
	 * Adresse avec destinataire seulement (pour les données de contact)
	 */
	public static final class DestinataireSeulement extends Structuree {

		public DestinataireSeulement(DestinataireAdresse destinataire) {
			super(destinataire, null, null, null, null, null, null);
		}

		@Override
		public boolean isDestinataireSeul() {
			return true;
		}

		@Override
		public Integer getNumeroOrdrePostal() {
			return null;
		}

		@Override
		public String getNumeroPostal() {
			return null;
		}

		@Override
		public String getNumeroPostalComplementaire() {
			return null;
		}

		@Override
		public Integer getNoOfsPays() {
			return null;
		}

		@Override
		public Integer getNumeroRue() {
			return null;
		}
	}


	/**
	 * Adresse structurée en Suisse
	 */
	public static final class StructureeSuisse extends Structuree {

		private final Integer numeroRue;
		private final Integer npa;
		private final String npaComplementaire;
		private final Integer noOrdrePoste;

		public StructureeSuisse(DestinataireAdresse destinataire, String ligne1, String ligne2, Integer estrid, String rue, String numeroMaison, CasePostale casePostale, String localite, Integer npa, String npaComplementaire, Integer noOrdrePoste) {
			super(destinataire, ligne1, ligne2, rue, numeroMaison, casePostale, localite);
			this.numeroRue = estrid;
			this.npa = npa;
			this.npaComplementaire = npaComplementaire;
			this.noOrdrePoste = noOrdrePoste;
		}

		@Override
		public Integer getNumeroRue() {
			return numeroRue;
		}

		@Override
		public Integer getNumeroOrdrePostal() {
			return noOrdrePoste;
		}

		@Override
		public String getNumeroPostalComplementaire() {
			return npaComplementaire;
		}

		@Override
		public String getNumeroPostal() {
			return npa != null ? Integer.toString(npa) : null;
		}

		@Override
		public Integer getNoOfsPays() {
			return ServiceInfrastructureService.noOfsSuisse;
		}
	}

	/**
	 * Adresse structurée à l'étranger
	 */
	public static final class StructureeEtranger extends Structuree {

		private final String npaEtranger;
		private final Pays pays;

		public StructureeEtranger(DestinataireAdresse destinataire, String ligne1, String ligne2, String rue, String numeroMaison, CasePostale casePostale, String localite, String npaEtranger, Pays pays) {
			super(destinataire, ligne1, ligne2, rue, numeroMaison, casePostale, localite);
			this.npaEtranger = npaEtranger;
			this.pays = pays;
		}

		@Override
		public Integer getNumeroOrdrePostal() {
			return null;
		}

		@Override
		public String getNumeroPostal() {
			return npaEtranger;
		}

		@Override
		public String getNumeroPostalComplementaire() {
			return null;
		}

		@Override
		public Integer getNoOfsPays() {
			return pays != null ? pays.getNoOFS() : null;
		}

		@Override
		public Integer getNumeroRue() {
			return null;
		}
	}
}
