package ch.vd.uniregctb.role;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.uniregctb.type.TypeAdresseTiers;
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
import ch.vd.uniregctb.common.JobResults;
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
public class ProduireRolesResults extends JobResults<Long, ProduireRolesResults> {
	
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
			TERMINE_DANS_PF("Termniné"),            // l'assujettissement s'est terminé dans la période fiscale
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
		private final List<String> nomsPrenoms;
		private final List<String> nosAvs;
		private final String[] adresseEnvoi;
		private final List<InfoFor> fors = new ArrayList<InfoFor>();

		public InfoContribuable(Contribuable ctb, int annee, AdresseService adresseService, TiersService tiersService) {
			this.noCtb = ctb.getNumero();

			AdresseEnvoiDetaillee adresseEnvoi;
			try {
				adresseEnvoi = adresseService.getAdresseEnvoi(ctb, RegDate.get(annee, 12, 31), TypeAdresseTiers.COURRIER, false);
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

			nomsPrenoms = new ArrayList<String>(2);
			nosAvs = new ArrayList<String>(2);
			fillNomsPrenomsEtNosAvs(ctb, annee, tiersService, nomsPrenoms, nosAvs);
		}
		
		private static void fillNomsPrenomsEtNosAvs(Contribuable ctb, int annee, TiersService tiersService, List<String> nomsPrenoms, List<String> nosAvs) {
			if (ctb instanceof PersonnePhysique) {
				final PersonnePhysique pp = (PersonnePhysique) ctb;
				final String nomPrenom = tiersService.getNomPrenom(pp);
				final String noAvs = tiersService.getNumeroAssureSocial(pp);
				nomsPrenoms.add(nomPrenom);
				nosAvs.add(noAvs);
			}
			else if (ctb instanceof MenageCommun) {
				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple((MenageCommun) ctb, annee);
				final PersonnePhysique principal = ensemble.getPrincipal();
				final PersonnePhysique conjoint = ensemble.getConjoint();
				if (principal != null) {
					nomsPrenoms.add(tiersService.getNomPrenom(principal));
					nosAvs.add(tiersService.getNumeroAssureSocial(principal));
				}
				if (conjoint != null) {
					nomsPrenoms.add(tiersService.getNomPrenom(conjoint));
					nosAvs.add(tiersService.getNumeroAssureSocial(conjoint));
				}
			}
		}

		public void addFor(InfoFor infoFor) {
			fors.add(infoFor);
		}

		/**
		 * Comparateur de motifs de rattachement : DOMICILE, ACTIVITE_INDEPENDANTE, IMMEUBLE_PRIVE puis tous les autres de manière indiférenciée
		 */
		private static final Comparator<MotifRattachement> COMPARATOR_MOTIF_RATTACHEMENT = new Comparator<MotifRattachement>() {

			private final List<MotifRattachement> ORDRE_MOTIFS_RATTACHEMENT = Arrays.asList(MotifRattachement.DOMICILE, MotifRattachement.ACTIVITE_INDEPENDANTE, MotifRattachement.IMMEUBLE_PRIVE);

			public int compare(MotifRattachement o1, MotifRattachement o2) {
				final int indexMotif1 = ORDRE_MOTIFS_RATTACHEMENT.indexOf(o1);
				final int indexMotif2 = ORDRE_MOTIFS_RATTACHEMENT.indexOf(o2);
				if (indexMotif1 == indexMotif2) {
					return 0;
				}
				else if (indexMotif1 == -1) {
					return 1;
				}
				else if (indexMotif2 == -1) {
					return -1;
				}
				else {
					return indexMotif1 - indexMotif2;
				}
			}
		};

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

		/**
		 * Prends le tout premier for et renvoie sa date d'ouverture et son motif d'ouverture
		 */
		public Pair<RegDate, MotifFor> getInfosOuverture() {
			if (fors.isEmpty()) {
				return null;
			}
			else if (fors.size() == 1) {
				return extractInfosOuverture(fors.get(0));
			}
			else {
				final List<InfoFor> aTrier = new ArrayList<InfoFor>(fors);
				Collections.sort(aTrier, COMPARATOR_OUVERTURE);
				return extractInfosOuverture(aTrier.get(0));
			}
		}

		private static Pair<RegDate, MotifFor> extractInfosOuverture(InfoFor info) {
			if (info != null && info.dateOuverture != null) {
				return new Pair<RegDate, MotifFor>(info.dateOuverture, info.motifOuverture);
			}
			else {
				return null;
			}
		}

		/**
		 * Prend le dernier for et extrait son motif et sa date de fermeture
		 */
		public Pair<RegDate, MotifFor> getInfosFermeture() {
			if (fors.isEmpty()) {
				return null;
			}
			else if (fors.size() == 1) {
				return extractInfosFermeture(fors.get(0));
			}
			else {
				final List<InfoFor> aTrier = new ArrayList<InfoFor>(fors);
				Collections.sort(aTrier, COMPARATOR_FERMETURE);
				return extractInfosFermeture(aTrier.get(0));
			}
		}

		private static Pair<RegDate, MotifFor> extractInfosFermeture(InfoFor info) {
			if (info != null && info.dateFermeture != null) {
				return new Pair<RegDate, MotifFor>(info.dateFermeture, info.motifFermeture);
			}
			else {
				return null;
			}
		}

		public TypeAssujettissement getTypeAssujettissementDansCommune() {
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
			TypeContribuable typeCtb = null;
			if (fors.size() > 0) {
				typeCtb = fors.get(fors.size() - 1).typeCtb;
			}
			return typeCtb;
		}

		public TypeContribuable getAncienTypeContribuable() {
			TypeContribuable typeCtb = null;
			if (fors.size() > 0) {
				typeCtb = fors.get(fors.size() - 1).ancienTypeCtb;
			}
			return typeCtb;
		}

		public List<String> getNomsPrenoms() {
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

		/**
		 * Constructeur pour les assujettissements sur la période
		 */
		public InfoFor(TypeContribuable typeCtb, RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture, InfoContribuable.TypeAssujettissement typeAssujettissement, boolean forPrincipal, MotifRattachement motifRattachement) {
			this(typeCtb, dateOuverture, motifOuverture, dateFermeture, motifFermeture, typeAssujettissement, null, forPrincipal, motifRattachement);
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
		 */
		public InfoFor(RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture, TypeContribuable ancienTypeCtb, boolean forPrincipal, MotifRattachement motifRattachement) {
			this(TypeContribuable.NON_ASSUJETTI, dateOuverture, motifOuverture, dateFermeture, motifFermeture, InfoContribuable.TypeAssujettissement.NON_ASSUJETTI, ancienTypeCtb, forPrincipal, motifRattachement);
		}

		/**
		 * Constructeur interne factorisé
		 */
		private InfoFor(TypeContribuable typeCtb, RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture,
		               InfoContribuable.TypeAssujettissement typeAssujettissement, TypeContribuable ancienTypeCtb, boolean forPrincipal,
		               MotifRattachement motifRattachement) {

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
	/** renseigné en cas de sélection d'une seule commune */
	public final Integer noOfsCommune;
	/** renseigné en cas de sélection d'un office d'impôt */
	public final Integer noColOID;

	// données de sortie
	public final Map<Integer, InfoCommune> infosCommunes = new HashMap<Integer, InfoCommune>();
	public int ctbsTraites = 0;
	public List<Ignore> ctbsIgnores = new ArrayList<Ignore>();
	public List<Erreur> ctbsEnErrors = new ArrayList<Erreur>();
	public boolean interrompu;

	/** résultats pour toutes les communes vaudoises */
	public ProduireRolesResults(int anneePeriode, RegDate dateTraitement) {
		this(anneePeriode, null, null, dateTraitement);
	}

	/** résultats pour une seule commune vaudoise ou un seul office d'impôt */
	public ProduireRolesResults(int anneePeriode, Integer noOfsCommune, Integer noColOID, RegDate dateTraitement) {
		this.annee = anneePeriode;
		this.noOfsCommune = noOfsCommune;
		this.noColOID = noColOID;
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

	public void addErrorException(Long id, Exception e) {
		ctbsEnErrors.add(new Erreur(id, null, ErreurType.EXCEPTION, e.getMessage()));
	}

	public void addErrorException(Contribuable ctb, Exception e) {
		ctbsEnErrors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.EXCEPTION, e.getMessage()));
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
}
