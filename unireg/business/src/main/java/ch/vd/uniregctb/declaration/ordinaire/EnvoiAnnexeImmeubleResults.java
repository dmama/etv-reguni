package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.tiers.Contribuable;

public class EnvoiAnnexeImmeubleResults<R extends EnvoiAnnexeImmeubleResults> extends JobResults<ContribuableAvecImmeuble, R> {

	public static enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION);
		// --------------------------------------------------------------


		private final String description;

		private ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static enum IgnoreType {
		CTB_NON_ASSUJETTI("Le contribuable n'est pas assujetti à la fin de la période fiscale");

		private final String description;

		private IgnoreType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class InfoCtbImmeuble extends Info {
		public final int nbAnnexeEnvoyee;

		public InfoCtbImmeuble(Long noCtb, Integer officeImpotID, int nbAnnexe) {
			super((noCtb == null ? 0 : noCtb), officeImpotID, "Impression OK");
			this.nbAnnexeEnvoyee = nbAnnexe;
		}


		@Override
		public String getDescriptionRaison() {
			return null;
		}
	}

	public static class Erreur extends Info {
		public final ErreurType raison;

		public Erreur(Long noCtb, Integer officeImpotID, ErreurType raison, String details) {
			super((noCtb == null ? 0 : noCtb), officeImpotID, details);
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

	// Paramètres d'entrée
	public final int annee;
	public final RegDate dateTraitement;
	public final String nomFichier;
	public final int nombreAnnexe;

	// Données de processing
	public int nbCtbsTotal;
	public final List<InfoCtbImmeuble> infoCtbTraites = new ArrayList<InfoCtbImmeuble>();
	public final List<Long> ctbsTraites = new ArrayList<Long>();
	public final List<Ignore> ctbsIgnores = new ArrayList<Ignore>();
	public final List<Erreur> ctbsEnErrors = new ArrayList<Erreur>();
	public boolean interrompu;

	public EnvoiAnnexeImmeubleResults(int annee, RegDate dateTraitement, String nomFichier, int nombreAnnexe) {
		this.annee = annee;
		this.dateTraitement = dateTraitement;
		this.nomFichier = nomFichier;
		this.nombreAnnexe = nombreAnnexe;
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
		ctbsEnErrors.add(new Erreur(ctb.getNumeroContribuable(), null, ErreurType.EXCEPTION, getExceptionMessage(e)));
	}

	public void addErrorException(Contribuable ctb, Exception e) {
		ctbsEnErrors.add(new Erreur(ctb.getNumero(), ctb.getOfficeImpotId(), ErreurType.EXCEPTION, getExceptionMessage(e)));
	}

	public void addInfoCtbTraites(Contribuable ctb, int nbAnnexes) {
		infoCtbTraites.add(new InfoCtbImmeuble(ctb.getNumero(), ctb.getOfficeImpotId(), nbAnnexes));
	}

	public void addIgnoreCtbNonAssujetti(Contribuable ctb, int periode) {
		ctbsIgnores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.CTB_NON_ASSUJETTI, "Non assujetti pour la fin de période fiscale " + periode));
	}

	@Override
	public void addAll(R rapport) {
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
		Collections.sort(infoCtbTraites, new CtbComparator<InfoCtbImmeuble>());
		// tri des erreurs et des contribuables ignorés
		Collections.sort(ctbsEnErrors, new CtbComparator<Erreur>());
		Collections.sort(ctbsIgnores, new CtbComparator<Ignore>());

	}
}
