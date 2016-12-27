package ch.vd.uniregctb.tiers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.JobResults;

public class ExclureContribuablesEnvoiResults extends JobResults<Long, ExclureContribuablesEnvoiResults> {

	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION), // ------------------------------------------
		CTB_INCONNU("Le contribuable est inconnu");

		private final String description;

		ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public enum IgnoreType {
		DATE_LIMITE_EXISTANTE("Le contribuable possède déjà une date limite plus grande grande que la date spécifiée");

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

	// Paramètres d'entrée
	public final List<Long> ctbsIds;
	public final RegDate dateLimiteExclusion;

	// Données de processing
	public int nbCtbsTotal;
	public final List<Ignore> ctbsIgnores = new ArrayList<>();
	public final List<Erreur> ctbsEnErrors = new ArrayList<>();

	public boolean interrompu;

	public ExclureContribuablesEnvoiResults(List<Long> ctbsIds, RegDate dateLimiteExclusion, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.ctbsIds = ctbsIds;
		this.dateLimiteExclusion = dateLimiteExclusion;
	}

	@Override
	public void addErrorException(Long ctbID, Exception e) {
		++nbCtbsTotal;
		ctbsEnErrors.add(new Erreur(0, null, ErreurType.EXCEPTION, e.getMessage(), StringUtils.EMPTY));
	}

	public void addErrorCtbInconnu(long ctbID) {
		++nbCtbsTotal;
		ctbsEnErrors.add(new Erreur(ctbID, null, ErreurType.CTB_INCONNU, "", getNom(ctbID)));
	}

	public void addIgnoreDateLimiteExistante(Contribuable ctb, String message) {
		++nbCtbsTotal;
		ctbsIgnores.add(new Ignore(ctb.getNumero(), ctb.getOfficeImpotId(), IgnoreType.DATE_LIMITE_EXISTANTE, message, getNom(ctb.getNumero())));
	}

	@Override
	public void addAll(ExclureContribuablesEnvoiResults rapport) {
		nbCtbsTotal += rapport.nbCtbsTotal;
		ctbsIgnores.addAll(rapport.ctbsIgnores);
		ctbsEnErrors.addAll(rapport.ctbsEnErrors);
	}
}
