package ch.vd.uniregctb.rf;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.tiers.TiersService;

public class RapprocherCtbResults extends JobResults<ProprietaireFoncier, RapprocherCtbResults> {

	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION), // ------------------------------------------
		CTB_INCONNU("Le contribuable est inconnu"),
		CTB_NON_PP_NI_MC("Le contribuable n'est ni une personne physique ni un ménage commun");

		private final String description;

		ErreurType(String description) {
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

	//Données en entrée
	public RegDate dateTraitement;

	// Données de processing
	public int nbCtbsTotal = 0;
	public final List<Erreur> ctbsEnErreur = new LinkedList<>();
	public final List<ProprietaireRapproche> listeRapproche = new LinkedList<>();
	public boolean interrompu;

	public RapprocherCtbResults(RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.dateTraitement = dateTraitement;
	}

	/**
	 * Un élément ajouté ici ne devra pas faire l'objet d'un ajout dans la liste des rapprochements (c'est pourquoi on incrémente le compteur)
	 * @param p la donnée fournie par le registre foncier
	 * @param e l'exception levée
	 */
	@Override
	public void addErrorException(ProprietaireFoncier p, Exception e) {
		++ nbCtbsTotal;
		ctbsEnErreur.add(new Erreur(p.getNumeroContribuable(), null, ErreurType.EXCEPTION, e.getMessage(), getNom(p.getNumeroContribuable())));
	}

	@Override
	public void addAll(RapprocherCtbResults rapport) {
		nbCtbsTotal += rapport.nbCtbsTotal;
		ctbsEnErreur.addAll(rapport.ctbsEnErreur);
		listeRapproche.addAll(rapport.listeRapproche);
	}

	@Override
	public void end() {
		super.end();
		listeRapproche.sort(Comparator.comparingLong(ProprietaireRapproche::getNumeroRegistreFoncier));
	}

	/**
	 * Ajoute une ligne au rapport final avec les données du registre foncier et ce que nous en aurons trouvé
	 * @param proprioRapproche les données consolidées
	 */
	public void addProprietaireRapproche(ProprietaireRapproche proprioRapproche) {
		++ nbCtbsTotal;
		listeRapproche.add(proprioRapproche);
	}

	/**
	 * Un élément ajouté ici devra également être ajouté à la liste des rapprochement (on n'incrémentera donc pas le nombre total de contribuables)
	 * @param p données fournie par le registre foncier
	 * @param raison raison de l'erreur
	 * @param details éventuelle description plus précise du problème
	 */
	public void addError(ProprietaireFoncier p, ErreurType raison, @Nullable String details) {
		ctbsEnErreur.add(new Erreur(p.getNumeroContribuable(), null, raison, details, getNom(p.getNumeroContribuable())));
	}

	public Map<ProprietaireRapproche.CodeRetour, Integer> getStats() {
		final int[] tableau = new int[ProprietaireRapproche.CodeRetour.values().length];
		for (ProprietaireRapproche p : listeRapproche) {
			++ tableau[p.getResultat().ordinal()];
		}
		final Map<ProprietaireRapproche.CodeRetour, Integer> map = new EnumMap<>(ProprietaireRapproche.CodeRetour.class);
		for (ProprietaireRapproche.CodeRetour code : ProprietaireRapproche.CodeRetour.values()) {
			final int index = code.ordinal();
			map.put(code, tableau[index]);
		}
		return map;
	}
}
