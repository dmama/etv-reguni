package ch.vd.uniregctb.role;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Contient les données brutes permettant de générer le rapport "rôles pour les communes".
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class ProduireRolesResults<T extends ProduireRolesResults<T>> extends JobResults<Long, T> {
	
	public enum ErreurType {
		CTB_INVALIDE("le contribuable ne valide pas."), // ---------------------------------------------------
		ASSUJETTISSEMENT("impossible de déterminer l'assujettissement du contribuable"), // ------------------
		EXCEPTION(EXCEPTION_DESCRIPTION); // -----------------------------------------------------------------

		private final String description;

		ErreurType(String description) {
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

		IgnoreType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class Erreur extends Info {
		public final ErreurType raison;

		public Erreur(long noCtb, Integer officeImpotID, ErreurType raison, String details, String nomCtb) {
			super(noCtb, officeImpotID, details, nomCtb);
			this.raison = raison;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public static class Ignore extends Info {
		public final IgnoreType raison;

		public Ignore(long noCtb, Integer officeImpotID, IgnoreType raison, String details, String nomCtb) {
			super(noCtb, officeImpotID, details, nomCtb);
			this.raison = raison;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public enum TypeRoles {
		PP,
		PM
	}

	// paramètres d'entrée
	public final int annee;
	public final RegDate dateTraitement;
	public final int nbThreads;

	// données de sortie
	public int ctbsTraites = 0;
	public final List<Ignore> ctbsIgnores = new LinkedList<>();
	public final List<Erreur> ctbsEnErrors = new LinkedList<>();
	public boolean interrompu;

	public ProduireRolesResults(int anneePeriode, int nbThreads, RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.annee = anneePeriode;
		this.nbThreads = nbThreads;
		this.dateTraitement = dateTraitement;
	}

	/**
	 * Point d'entrée pour la prise en compte d'un nouveau for
	 * @param infoFor informations sur le for à prendre en compte
	 * @param ctb contribuable concerné
	 * @param assujettissement (optionnel) assujettissement en cours de traitement
	 * @param dateFinAssujettissementPrecedent
	 * @param annee année des rôles
	 * @param noOfsCommune numéro OFS de la commune concernée
	 * @param adresseService le service de calcul d'adresses
	 * @param tiersService le service des tiers
	 */
	public abstract void digestInfoFor(InfoFor infoFor, Contribuable ctb, Assujettissement assujettissement, RegDate dateFinAssujettissementPrecedent, int annee, int noOfsCommune, AdresseService adresseService, TiersService tiersService);

	/**
	 * @return le type de rôle géré par cette instance de résulats (PP vs. PM)
	 */
	public abstract TypeRoles getTypeRoles();

	/**
	 * @param ctb un contribuable
	 * @param tiersService le service des tiers
	 * @return la liste des périodes fiscales (théorique, i.e. qui ne fait pas intervenir un quelconque calcul d'assujettissement) du contribuable dans la période où il possède des fors principaux
	 * (si son dernier for principal est ouvert, la dernière période fiscale fournie correspond à la période fiscale courante)
	 */
	public abstract List<DateRange> getPeriodesFiscales(Contribuable ctb, TiersService tiersService);

	public void addErrorCtbInvalide(Contribuable ctb) {
		ctbsEnErrors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.CTB_INVALIDE, null, getNom(ctb.getNumero())));
	}

	public void addErrorErreurAssujettissement(Contribuable ctb, String details) {
		ctbsEnErrors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.ASSUJETTISSEMENT, details, getNom(ctb.getNumero())));
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

	@Override
	public void addErrorException(Long id, Exception e) {
		ctbsEnErrors.add(new Erreur(id, null, ErreurType.EXCEPTION, buildErrorMessage(e), getNom(id)));
	}

	public void addErrorException(Contribuable ctb, Exception e) {
		ctbsEnErrors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.EXCEPTION, buildErrorMessage(e), getNom(ctb.getNumero())));
	}

	public void addCtbIgnoreDonneesIncoherentes(Contribuable ctb, String details) {
		ctbsIgnores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.DONNEES_INCOHERENTES, details, getNom(ctb.getNumero())));
	}

	public void addCtbIgnoreDiplomateSuisse(Contribuable ctb) {
		ctbsIgnores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.DIPLOMATE_SUISSE, null, getNom(ctb.getNumero())));
	}

	public void addCtbIgnoreSourcierGris(Contribuable ctb) {
		ctbsIgnores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.SOURCIER_GRIS, null, getNom(ctb.getNumero())));
	}

	@Override
	public void addAll(T rapport) {
		this.ctbsTraites += rapport.ctbsTraites;
		this.ctbsEnErrors.addAll(rapport.ctbsEnErrors);
		this.ctbsIgnores.addAll(rapport.ctbsIgnores);
	}

	@Override
	public void end() {
		// tri des erreurs et des contribuables ignorés
		final Comparator<Info> comparator = new CtbComparator<>();
		Collections.sort(ctbsEnErrors, comparator);
		Collections.sort(ctbsIgnores, comparator);

		super.end();
	}

	public abstract Set<Integer> getNoOfsCommunesTraitees();
}
