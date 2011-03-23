package ch.vd.uniregctb.role;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.GentilComparator;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.role.ProduireRolesResults.InfoContribuable.TypeContribuable;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

/**
 * Contient les données brutes permettant de générer le rapport "rôles pour les communes".
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class ProduireRolesResults extends JobResults<Long, ProduireRolesResults> {
	
	private static final Logger LOGGER = Logger.getLogger(ProduireRolesResults.class);

	public enum ErreurType {
		CTB_INVALIDE("le contribuable ne valide pas."), // ---------------------------------------------------
		ASSUJETTISSEMENT("impossible de déterminer l'assujettissement du contribuable"), // ------------------
		EXCEPTION(EXCEPTION_DESCRIPTION); // -----------------------------------------------------------------

		private final String description;

		private ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public enum IgnoreType {
		DONNEES_INCOHERENTES("les données sont incohérente"), // -----------------------------------------------
		SOURCIER_GRIS("le contribuable est un sourcier gris"), // ----------------------------------------------
		DIPLOMATE_SUISSE("le contribuable est un diplomate Suisse basé à l'étranger"); // ----------------------

		private final String description;

		private IgnoreType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class Erreur extends Info {
		public final ErreurType raison;

		public Erreur(long noCtb, Integer officeImpotID, ErreurType raison, String details) {
			super(noCtb, officeImpotID, details);
			this.raison = raison;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public static class Ignore extends Info {
		public final IgnoreType raison;

		public Ignore(long noCtb, Integer officeImpotID, IgnoreType raison, String details) {
			super(noCtb, officeImpotID, details);
			this.raison = raison;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public static class InfoCommune {

		private final int noOfs;
		private final Map<Long, InfoContribuable> infosContribuables = new HashMap<Long, InfoContribuable>();

		public InfoCommune(int noOfs) {
			super();
			this.noOfs = noOfs;
		}

		public int getNoOfs() {
			return noOfs;
		}

		public InfoContribuable getInfoPourContribuable(long noCtb) {
			return infosContribuables.get(noCtb);
		}

		public InfoContribuable getOrCreateInfoPourContribuable(Contribuable ctb, int annee, AdresseService adresseService, TiersService tiersService) {
			final Long key = ctb.getNumero();
			InfoContribuable info = infosContribuables.get(key);
			if (info == null) {
				info = new InfoContribuable(ctb, annee, adresseService, tiersService);
				infosContribuables.put(key, info);
			}
			return info;
		}

		public Map<Long, InfoContribuable> getInfosContribuables() {
			return infosContribuables;
		}

		public void addAll(InfoCommune value) {
			this.infosContribuables.putAll(value.infosContribuables);
		}
	}

	/**
	 * Informations sur un contribuable pour une commune et une période fiscale données
	 */
	public static class InfoContribuable {

		public enum TypeContribuable {
			ORDINAIRE("vaudois ordinaire"), // --------------------------------------------------------------------------------
			HORS_CANTON("hors canton"), // ----------------------------------------------------------------------------
			HORS_SUISSE("hors Suisse"), // ----------------------------------------------------------------------------
			SOURCE("source"), // ---------------------------------------------------------------------------------
			DEPENSE("dépense"), // -------------------------------------------------------------------------------
			MIXTE("sourcier mixte"), // ----------------------------------------------------------------------------------------
			NON_ASSUJETTI("non-assujetti");

			private final String description;

			private TypeContribuable(String description) {
				this.description = description;
			}

			public String description() {
				return description;
			}
		}

		/**
		 * Type d'assujettissement : notons que les modalités sont données
		 * dans un ordre de priorité croissante (POURSUIVI_PF est prioritaire sur TERMINE_DANS_PF,
		 * qui est prioritaire sur NON_ASSUJETTI)
		 */
		public enum TypeAssujettissement {
			NON_ASSUJETTI("Non assujetti"),         // l'assujettissement s'est terminé avant le début de la période fiscale
			TERMINE_DANS_PF("Terminé"),             // l'assujettissement s'est terminé dans la période fiscale
			POURSUIVI_APRES_PF("Poursuivi");        // l'assujetissement existe et se poursuit dans la période fiscale suivante

			private final String description;

			private TypeAssujettissement(String description) {
				this.description = description;
			}

			public String description() {
				return description;
			}
		}

		public final long noCtb;
		private final List<NomPrenom> nomsPrenoms;
		private final List<String> nosAvs;
		private final String[] adresseEnvoi;
		private final List<InfoFor> fors = new ArrayList<InfoFor>();

		public InfoContribuable(Contribuable ctb, int annee, AdresseService adresseService, TiersService tiersService) {
			this.noCtb = ctb.getNumero();

			AdresseEnvoiDetaillee adresseEnvoi;
			try {
				adresseEnvoi = adresseService.getAdresseEnvoi(ctb, RegDate.get(annee, 12, 31), TypeAdresseFiscale.COURRIER, false);
			}
			catch (AdresseException e) {
				LOGGER.warn("Résolution de l'adresse du contribuable " + ctb.getNumero() + " impossible", e);
				adresseEnvoi = null;
			}

			if (adresseEnvoi != null) {
				this.adresseEnvoi = adresseEnvoi.getLignes();
			}
			else {
				this.adresseEnvoi = null;
			}

			nomsPrenoms = new ArrayList<NomPrenom>(2);
			nosAvs = new ArrayList<String>(2);
			fillNomsPrenomsEtNosAvs(ctb, annee, tiersService, nomsPrenoms, nosAvs);
		}

		/**
		 * Génère un clone (surtout pour la collection des fors qui ne doit pas être modifiée sur l'original
		 * quand on ajoute des éléments sur cette nouvelle structure)
		 * @param original
		 */
		public InfoContribuable(InfoContribuable original) {
			this.noCtb = original.noCtb;
			this.adresseEnvoi = original.adresseEnvoi;
			this.nomsPrenoms = original.nomsPrenoms;
			this.nosAvs = original.nosAvs;
			fors.addAll(original.fors);
		}
		
		private static void fillNomsPrenomsEtNosAvs(Contribuable ctb, int annee, TiersService tiersService, List<NomPrenom> nomsPrenoms, List<String> nosAvs) {
			if (ctb instanceof PersonnePhysique) {
				final PersonnePhysique pp = (PersonnePhysique) ctb;
				final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(pp);
				final String noAvs = tiersService.getNumeroAssureSocial(pp);
				nomsPrenoms.add(nomPrenom);
				nosAvs.add(noAvs);
			}
			else if (ctb instanceof MenageCommun) {
				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple((MenageCommun) ctb, annee);
				final PersonnePhysique principal = ensemble.getPrincipal();
				final PersonnePhysique conjoint = ensemble.getConjoint();
				if (principal != null) {
					nomsPrenoms.add(tiersService.getDecompositionNomPrenom(principal));
					nosAvs.add(tiersService.getNumeroAssureSocial(principal));
				}
				if (conjoint != null) {
					nomsPrenoms.add(tiersService.getDecompositionNomPrenom(conjoint));
					nosAvs.add(tiersService.getNumeroAssureSocial(conjoint));
				}
			}
		}

		public void addFor(InfoFor infoFor) {
			fors.add(infoFor);
		}

		public void copyForsFrom(InfoContribuable other) {
			fors.addAll(other.fors);
		}

		/**
		 * Comparateur de motifs de rattachement : DOMICILE, ACTIVITE_INDEPENDANTE, IMMEUBLE_PRIVE puis tous les autres de manière indiférenciée
		 */
		private static final Comparator<MotifRattachement> COMPARATOR_MOTIF_RATTACHEMENT = new GentilComparator<MotifRattachement>(Arrays.asList(MotifRattachement.DOMICILE, MotifRattachement.ACTIVITE_INDEPENDANTE, MotifRattachement.IMMEUBLE_PRIVE));

		/**
		 * Comparateur par date d'ouverture croissante, puis principal/non-principal, puis motif de rattachement
		 */
		private static final Comparator<InfoFor> COMPARATOR_OUVERTURE = new Comparator<InfoFor>() {
			public int compare(InfoFor o1, InfoFor o2) {
				int compare = NullDateBehavior.EARLIEST.compare(o1.dateOuverture, o2.dateOuverture);
				if (compare == 0) {
					compare = - Boolean.valueOf(o1.forPrincipal).compareTo(o2.forPrincipal);        // principal avant non-principal
					if (compare == 0) {
						compare = COMPARATOR_MOTIF_RATTACHEMENT.compare(o1.motifRattachement, o2.motifRattachement);
					}
				}
				return compare;
			}
		};

		/**
		 * Comparateur par date de fermeture décroissante, puis principal/non-principal puis motif de rattachement
		 */
		private static final Comparator<InfoFor> COMPARATOR_FERMETURE = new Comparator<InfoFor>() {
			public int compare(InfoFor o1, InfoFor o2) {
				int compare = - NullDateBehavior.LATEST.compare(o1.dateFermeture, o2.dateFermeture);
				if (compare == 0) {
					compare = - Boolean.valueOf(o1.forPrincipal).compareTo(o2.forPrincipal);        // principal avant non-principal
					if (compare == 0) {
						compare = COMPARATOR_MOTIF_RATTACHEMENT.compare(o1.motifRattachement, o2.motifRattachement);
					}
				}
				return compare;
			}
		};

		private static final Comparator<InfoFor> COMPARATOR_GESTION = new Comparator<InfoFor>() {
			public int compare(InfoFor o1, InfoFor o2) {
				int compare = - NullDateBehavior.LATEST.compare(o1.dateFermeture, o2.dateFermeture);
				if (compare == 0) {
					compare = - (o1.typeAssujettissement.ordinal() - o2.typeAssujettissement.ordinal());
					if (compare == 0) {
						compare = - Boolean.valueOf(o1.forPrincipal).compareTo(o2.forPrincipal);        // principal avant non-principal
						if (compare == 0) {
							compare = COMPARATOR_MOTIF_RATTACHEMENT.compare(o1.motifRattachement, o2.motifRattachement);
						}
					}
				}
				return compare;
			}
		};

		private InfoFor getPremierForSelonComparateur(Comparator<InfoFor> comparator) {
			final InfoFor infoFor;
			if (fors.isEmpty()) {
				infoFor = null;
			}
			else if (fors.size() == 1) {
				infoFor = fors.get(0);
			}
			else {
				final List<InfoFor> aTrier = new ArrayList<InfoFor>(fors);
				Collections.sort(aTrier, comparator);
				infoFor = aTrier.get(0);
			}
			return infoFor;
		}
		
		/**
		 * Prends le tout premier for et renvoie sa date d'ouverture et son motif d'ouverture
		 */
		public Pair<RegDate, MotifFor> getInfosOuverture() {
			final InfoFor forOuverture = getPremierForSelonComparateur(COMPARATOR_OUVERTURE);
			if (forOuverture == null || forOuverture.dateOuverture == null) {
				return null;
			}
			else {
				return new Pair<RegDate, MotifFor>(forOuverture.dateOuverture, forOuverture.motifOuverture);
			}
		}

		/**
		 * Prend le dernier for et extrait son motif et sa date de fermeture
		 */
		public Pair<RegDate, MotifFor> getInfosFermeture() {
			final InfoFor forFermeture = getPremierForSelonComparateur(COMPARATOR_FERMETURE);
			if (forFermeture == null || forFermeture.dateFermeture == null) {
				return null;
			}
			else {
				return new Pair<RegDate, MotifFor>(forFermeture.dateFermeture, forFermeture.motifFermeture);
			}
		}

		public TypeAssujettissement getTypeAssujettissementAgrege() {
			TypeAssujettissement type = TypeAssujettissement.NON_ASSUJETTI;
			for (InfoFor infoFor : fors) {
				final TypeAssujettissement candidat = infoFor.typeAssujettissement;
				if (candidat.ordinal() > type.ordinal()) {
					type = candidat;
				}
			}
			return type;
		}

		public TypeContribuable getTypeCtb() {
			final TypeContribuable typeCtb;
			final InfoFor forGestion = getForGestionFinPeriode();
			if (forGestion == null) {
				typeCtb = null;
			}
			else {
				typeCtb = forGestion.typeCtb;
			}
			return typeCtb;
		}

		public TypeContribuable getAncienTypeContribuable() {
			final TypeContribuable typeCtb;
			final InfoFor forGestion = getForGestionFinPeriode();
			if (forGestion == null) {
				typeCtb = null;
			}
			else {
				typeCtb = forGestion.ancienTypeCtb;
			}
			return typeCtb;
		}

		public int getNoOfsDerniereCommune() {
			final InfoFor forGestion = getForGestionFinPeriode();
			if (forGestion != null) {
				return forGestion.ofsCommune;
			}
			else {
				throw new RuntimeException("Ne devrait pas être appelé sur un contribuable sans aucun for (" + noCtb + ")");
			}
		}

		private InfoFor getForGestionFinPeriode() {
			return getPremierForSelonComparateur(COMPARATOR_GESTION);
		}

		public List<NomPrenom> getNomsPrenoms() {
			return nomsPrenoms;
		}

		public List<String> getNosAvs() {
			return nosAvs;
		}

		public String[] getAdresseEnvoi() {
			return adresseEnvoi;
		}
	}

	public static class InfoFor implements DateRange {

		public final TypeContribuable typeCtb;
		public final RegDate dateOuverture;
		public final RegDate dateFermeture;
		public final MotifFor motifOuverture;
		public final MotifFor motifFermeture;
		public final InfoContribuable.TypeAssujettissement typeAssujettissement;
		public final TypeContribuable ancienTypeCtb;
		public final boolean forPrincipal;
		public final MotifRattachement motifRattachement;
		public final int ofsCommune;

		/**
		 * Constructeur pour les assujettissements sur la période
		 */
		public InfoFor(TypeContribuable typeCtb, RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture,
		               InfoContribuable.TypeAssujettissement typeAssujettissement, boolean forPrincipal, MotifRattachement motifRattachement, int ofsCommune) {
			this(typeCtb, dateOuverture, motifOuverture, dateFermeture, motifFermeture, typeAssujettissement, null, forPrincipal, motifRattachement, ofsCommune);
			Assert.isTrue(typeAssujettissement == InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF || typeAssujettissement == InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF);
		}

		/**
		 * Constructeur pour les fors fermés dans la période fiscale, et qui ont donné lieu à une fin d'assujettissement
		 * à la fin de la période fiscale précédente
		 * @param dateOuverture date d'ouverture du for déterminant pour l'assujettissement maintenant terminé
		 * @param motifOuverture motif d'ouverture de ce même for
		 * @param dateFermeture date de fermeture du for déterminant (ne devrait pas être nulle)
		 * @param motifFermeture motif de fermeture du for déterminant (ne devrait pas être nul)
		 * @param ancienTypeCtb type du contribuable dans la période fiscale précédente (il est maintenant non-assujetti)
		 * @param ofsCommune numéro OFS étendu de la commune du for (vaudois!)
		 */
		public InfoFor(RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture, TypeContribuable ancienTypeCtb, boolean forPrincipal,
		               MotifRattachement motifRattachement, int ofsCommune) {
			this(TypeContribuable.NON_ASSUJETTI, dateOuverture, motifOuverture, dateFermeture, motifFermeture, InfoContribuable.TypeAssujettissement.NON_ASSUJETTI, ancienTypeCtb, forPrincipal, motifRattachement, ofsCommune);
		}

		/**
		 * Constructeur interne factorisé
		 */
		private InfoFor(TypeContribuable typeCtb, RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture,
		                InfoContribuable.TypeAssujettissement typeAssujettissement, TypeContribuable ancienTypeCtb, boolean forPrincipal,
		                MotifRattachement motifRattachement, int ofsCommune) {

			Assert.notNull(dateOuverture);
			Assert.notNull(motifOuverture);

			this.typeCtb = typeCtb;
			this.dateOuverture = dateOuverture;
			this.dateFermeture = dateFermeture;
			this.motifOuverture = motifOuverture;
			this.motifFermeture = motifFermeture;
			this.typeAssujettissement = typeAssujettissement;
			this.ancienTypeCtb = ancienTypeCtb;
			this.forPrincipal = forPrincipal;
			this.motifRattachement = motifRattachement;
			this.ofsCommune = ofsCommune;
		}

		public RegDate getDateDebut() {
			return dateOuverture;
		}

		public RegDate getDateFin() {
			return dateFermeture;
		}

		public boolean isValidAt(RegDate date) {
			return RegDateHelper.isBetween(date, dateOuverture, dateFermeture, NullDateBehavior.LATEST);
		}
	}

	// paramètres d'entrée
	public final int annee;
	public final RegDate dateTraitement;
	public final int nbThreads;

	// données de sortie
	public final Map<Integer, InfoCommune> infosCommunes = new HashMap<Integer, InfoCommune>();
	public int ctbsTraites = 0;
	public final List<Ignore> ctbsIgnores = new ArrayList<Ignore>();
	public final List<Erreur> ctbsEnErrors = new ArrayList<Erreur>();
	public boolean interrompu;

	public ProduireRolesResults(int anneePeriode, int nbThreads, RegDate dateTraitement) {
		this.annee = anneePeriode;
		this.nbThreads = nbThreads;
		this.dateTraitement = dateTraitement;
	}

	public InfoCommune getInfoPourCommune(Integer noOfsCommune) {
		return infosCommunes.get(noOfsCommune);
	}

	public InfoCommune getOrCreateInfoPourCommune(Integer noOfsCommune) {
		InfoCommune info = infosCommunes.get(noOfsCommune);
		if (info == null) {
			info = new InfoCommune(noOfsCommune);
			infosCommunes.put(noOfsCommune, info);
		}
		return info;
	}

	public void addErrorCtbInvalide(Contribuable ctb) {
		ctbsEnErrors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.CTB_INVALIDE, null));
	}

	public void addErrorErreurAssujettissement(Contribuable ctb, String details) {
		ctbsEnErrors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.ASSUJETTISSEMENT, details));
	}

	private static String buildErrorMessage(Exception e) {
		final String embeddedMessage = e.getMessage();
		if (StringUtils.isBlank(embeddedMessage)) {
			return String.format("%s: %s", e.getClass().getName(), Arrays.toString(e.getStackTrace()));
		}
		else {
			return embeddedMessage;
		}
	}

	public void addErrorException(Long id, Exception e) {
		ctbsEnErrors.add(new Erreur(id, null, ErreurType.EXCEPTION, buildErrorMessage(e)));
	}

	public void addErrorException(Contribuable ctb, Exception e) {
		ctbsEnErrors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.EXCEPTION, buildErrorMessage(e)));
	}

	public void addCtbIgnoreDonneesIncoherentes(Contribuable ctb, String details) {
		ctbsIgnores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.DONNEES_INCOHERENTES, details));
	}

	public void addCtbIgnoreDiplomateSuisse(Contribuable ctb) {
		ctbsIgnores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.DIPLOMATE_SUISSE, null));
	}

	public void addCtbIgnoreSourcierGris(Contribuable ctb) {
		ctbsIgnores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.SOURCIER_GRIS, null));
	}

	public void addAll(ProduireRolesResults rapport) {
		if (rapport != null) {
			this.ctbsTraites += rapport.ctbsTraites;
			this.ctbsEnErrors.addAll(rapport.ctbsEnErrors);
			this.ctbsIgnores.addAll(rapport.ctbsIgnores);
			
			for (Map.Entry<Integer, InfoCommune> e: rapport.infosCommunes.entrySet()) {
				InfoCommune thisInfo = getOrCreateInfoPourCommune(e.getKey());
				thisInfo.addAll(e.getValue());
			}
		}
	}

	@Override
	public void end() {
		super.end();

		// tri des erreurs et des contribuables ignorés
		Collections.sort(ctbsEnErrors, new CtbComparator<Erreur>());
		Collections.sort(ctbsIgnores, new CtbComparator<Ignore>());
	}

	/**
	 * Construit un agrégat des données par contribuables pour ce qui concerne les communes indiquées
	 * @param nosOfsCommunes listes des numéros OFS des communes à considérer
	 * @return les informations pour les contribuables agrégées pour l'ensemble des communes données
	 */
	public Map<Long, InfoContribuable> buildInfosPourRegroupementCommunes(Collection<Integer> nosOfsCommunes) {

		final Map<Long, InfoContribuable> map = new HashMap<Long, InfoContribuable>();

		// boucle sur chacune des communes demandées
		for (Integer noOfsCommune : nosOfsCommunes) {
			if (noOfsCommune != null) {

				final InfoCommune infoCommune = infosCommunes.get(noOfsCommune);
				if (infoCommune != null) {

					// boucle sur tous les contribuables connus dans cette commune
					final Map<Long, InfoContribuable> infosCtbsCommune = infoCommune.getInfosContribuables();
					for (Map.Entry<Long, InfoContribuable> infoCtb : infosCtbsCommune.entrySet()) {
						final Long noCtb = infoCtb.getKey();
						final InfoContribuable infoDejaConnue = map.get(noCtb);
						if (infoDejaConnue != null) {
							// le contribuable était déjà connu sur une autre commune de la liste -> fusion nécessaire
							infoDejaConnue.copyForsFrom(infoCtb.getValue());
						}
						else {
							// il faut cloner l'info pour éviter que les fusions successives ne détruisent les données initiales
							final InfoContribuable clone = new InfoContribuable(infoCtb.getValue());
							map.put(noCtb, clone);
						}
					}
				}
			}
		}
		return map;
	}
}
