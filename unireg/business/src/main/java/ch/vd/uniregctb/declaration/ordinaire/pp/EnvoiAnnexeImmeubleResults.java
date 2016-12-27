package ch.vd.uniregctb.declaration.ordinaire.pp;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;

public class EnvoiAnnexeImmeubleResults extends JobResults<ContribuableAvecImmeuble, EnvoiAnnexeImmeubleResults> {

	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION),
		ERREUR_NUMERO_INVALIDE("Le numéro de contribuable est invalide");
		// --------------------------------------------------------------


		private final String description;

		ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public enum IgnoreType {
		CTB_NON_ASSUJETTI("Le contribuable n'est pas assujetti à la fin de la période fiscale");

		private final String description;

		IgnoreType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class InfoCtbImmeuble extends Info {
		public final int nbAnnexeEnvoyee;

		public InfoCtbImmeuble(Long noCtb, Integer officeImpotID, int nbAnnexe, String nomCtb) {
			super((noCtb == null ? 0 : noCtb), officeImpotID, "Impression OK", nomCtb);
			this.nbAnnexeEnvoyee = nbAnnexe;
		}


		@Override
		public String getDescriptionRaison() {
			return null;
		}
	}

	public static class Erreur extends Info {
		public final ErreurType raison;

		public Erreur(Long noCtb, Integer officeImpotID, ErreurType raison, String details, String nomCtb) {
			super((noCtb == null ? 0 : noCtb), officeImpotID, details, nomCtb);
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

	// Paramètres d'entrée
	public final int annee;
	public final RegDate dateTraitement;
	public final String nomFichier;
	public final int nbMaxAnnexes;

	// Données de processing
	public int nbCtbsTotal;
	public final List<InfoCtbImmeuble> infoCtbTraites = new LinkedList<>();
	public final List<Long> ctbsTraites = new LinkedList<>();
	public final List<Ignore> ctbsIgnores = new LinkedList<>();
	public final List<Erreur> ctbsEnErrors = new LinkedList<>();
	public boolean interrompu;

	public EnvoiAnnexeImmeubleResults(int annee, RegDate dateTraitement, String nomFichier, int nbMaxAnnexes, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.annee = annee;
		this.dateTraitement = dateTraitement;
		this.nomFichier = nomFichier;
		this.nbMaxAnnexes = nbMaxAnnexes;
	}

	public void addCtbTraites(Long noCtb) {
		ctbsTraites.add(noCtb);
	}

	private static String getExceptionMessage(Exception e) {
		final String msg = e.getMessage();
		if (StringUtils.isBlank(msg)) {
			return e.getClass().getName();
		}
		else {
			return msg;
		}
	}

	@Override
	public void addErrorException(ContribuableAvecImmeuble ctb, Exception e) {
		ctbsEnErrors.add(new Erreur(ctb.getNumeroContribuable(), null, ErreurType.EXCEPTION, getExceptionMessage(e), getNom(ctb.getNumeroContribuable())));
	}

	public void addErrorException(Contribuable ctb, Exception e) {
		ctbsEnErrors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.EXCEPTION, getExceptionMessage(e), getNom(ctb.getNumero())));
	}

	public void addErreurNoContribuableInvalide(ContribuableAvecImmeuble ctb, String msg) {
		ctbsEnErrors.add(new Erreur(ctb.getNumeroContribuable(), null, ErreurType.ERREUR_NUMERO_INVALIDE, msg, getNom(ctb.getNumeroContribuable())));
	}

	public void addInfoCtbTraites(Contribuable ctb, int nbAnnexes) {
		infoCtbTraites.add(new InfoCtbImmeuble(ctb.getNumero(), ctb.getOfficeImpotId(), nbAnnexes, getNom(ctb.getNumero())));
	}

	public void addIgnoreCtbNonAssujetti(Contribuable ctb, int periode) {
		ctbsIgnores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.CTB_NON_ASSUJETTI, "Non assujetti pour la fin de période fiscale " + periode, getNom(ctb.getNumero())));
	}

	@Override
	public void addAll(EnvoiAnnexeImmeubleResults rapport) {
		if (rapport != null) {
			this.nbCtbsTotal += rapport.nbCtbsTotal;
			this.ctbsTraites.addAll(rapport.ctbsTraites);
			this.ctbsEnErrors.addAll(rapport.ctbsEnErrors);
			this.ctbsIgnores.addAll(rapport.ctbsIgnores);
			this.infoCtbTraites.addAll(rapport.infoCtbTraites);
		}
	}

	@Override
	public void end() {
		super.end();
		Collections.sort(ctbsTraites);
		final CtbComparator<Info> comparator = new CtbComparator<>();
		Collections.sort(infoCtbTraites, comparator);
		// tri des erreurs et des contribuables ignorés
		Collections.sort(ctbsEnErrors, comparator);
		Collections.sort(ctbsIgnores, comparator);

	}
}
