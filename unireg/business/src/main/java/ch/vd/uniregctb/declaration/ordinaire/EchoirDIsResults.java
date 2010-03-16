package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.tiers.Tiers;

public class EchoirDIsResults extends JobResults {

	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION), ETAT_DECLARATION_INCOHERENT("L'état de la déclaration est incohérent");

		private final String description;

		private ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public enum IgnoreType {
		DELAI_NON_ECHU("le délai de retour n'est pas échu");

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
		public final long diId;

		public Erreur(long noCtb, Integer officeImpotID, long diId, ErreurType raison, String details) {
			super(noCtb, officeImpotID, details);
			this.raison = raison;
			this.diId = diId;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public static class Ignore extends Info {
		public final IgnoreType raison;
		public final long diId;

		public Ignore(long noCtb, Integer officeImpotID, long diId, IgnoreType raison, String details) {
			super(noCtb, officeImpotID, details);
			this.raison = raison;
			this.diId = diId;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public static class Echue {
		public final long diId;
		public final long ctbId;
		public final Integer officeImpotID;

		public Echue(long ctbId, Integer officeImpotID, long diId) {
			this.diId = diId;
			this.ctbId = ctbId;
			this.officeImpotID = officeImpotID;
		}
	}

	// Paramètres d'entrée
	public final RegDate dateTraitement;

	// Données de processing
	public int nbDIsTotal;
	public List<Echue> disEchues = new ArrayList<Echue>();
	public List<Ignore> disIgnorees = new ArrayList<Ignore>();
	public List<Erreur> disEnErrors = new ArrayList<Erreur>();

	public boolean interrompu;

	public EchoirDIsResults(RegDate dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	public void addDeclarationTraitee(DeclarationImpotOrdinaire di) {
		++nbDIsTotal;
		final Tiers tiers = di.getTiers();
		disEchues.add(new Echue(tiers.getNumero(), tiers.getOfficeImpotId(), di.getId()));
	}

	public void addErrorEtatIncoherent(DeclarationImpotOrdinaire di, String message) {
		++nbDIsTotal;
		final Tiers tiers = di.getTiers();
		disEnErrors
				.add(new Erreur(tiers.getNumero(), tiers.getOfficeImpotId(), di.getId(), ErreurType.ETAT_DECLARATION_INCOHERENT, message));
	}

	public void addErrorException(long idDI, Exception e) {
		++nbDIsTotal;
		disEnErrors.add(new Erreur(0, null, idDI, ErreurType.EXCEPTION, e.getMessage()));
	}

	public void addIgnoreDelaiNonEchu(DeclarationImpotOrdinaire di, String message) {
		++nbDIsTotal;
		final Tiers tiers = di.getTiers();
		disIgnorees.add(new Ignore(tiers.getNumero(), tiers.getOfficeImpotId(), di.getId(), IgnoreType.DELAI_NON_ECHU, message));
	}

	public void add(EchoirDIsResults rapport) {
		nbDIsTotal += rapport.nbDIsTotal;
		disEchues.addAll(rapport.disEchues);
		disIgnorees.addAll(rapport.disIgnorees);
		disEnErrors.addAll(rapport.disEnErrors);
	}
}
