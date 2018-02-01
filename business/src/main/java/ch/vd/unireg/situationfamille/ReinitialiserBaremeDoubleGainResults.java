package ch.vd.unireg.situationfamille;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.tiers.SituationFamilleMenageCommun;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;

/**
 * Résultats détaillés du batch de réinitialisation des barèmes double-gain.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ReinitialiserBaremeDoubleGainResults extends JobResults<Long, ReinitialiserBaremeDoubleGainResults> {

	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION);

		private final String description;

		ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public enum IgnoreType {
		BAREME_NON_DOUBLE_GAIN("Le barème n'est pas double-gain.");

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
		public final long situationId;

		public Erreur(long noCtb, Integer officeImpotID, long situationId, ErreurType raison, String details, String nomCtb) {
			super(noCtb, officeImpotID, details, nomCtb);
			this.raison = raison;
			this.situationId = situationId;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public static class Ignore extends Info {
		public final IgnoreType raison;
		public final long situationId;

		public Ignore(long noCtb, Integer officeImpotID, long situationId, IgnoreType raison, String details, String nomCtb) {
			super(noCtb, officeImpotID, details, nomCtb);
			this.raison = raison;
			this.situationId = situationId;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description + ' ' + details;
		}
	}

	public static class Situation {
		public final long ancienneId;
		public final long nouvelleId;
		public final long ctbId;
		public final Integer officeImpotID;

		public Situation(long ctbId, Integer officeImpotID, long ancienneId, long nouvelleId) {
			this.ancienneId = ancienneId;
			this.nouvelleId = nouvelleId;
			this.ctbId = ctbId;
			this.officeImpotID = officeImpotID;
		}
	}

	// Paramètres d'entrée
	public final RegDate dateTraitement;

	// Données de processing
	public int nbSituationsTotal;
	public final List<Situation> situationsTraitees = new ArrayList<>();
	public final List<Ignore> situationsIgnorees = new ArrayList<>();
	public final List<Erreur> situationsEnErrors = new ArrayList<>();

	public boolean interrompu;

	public ReinitialiserBaremeDoubleGainResults(RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.dateTraitement = dateTraitement;
	}

	public void addSituationsTraitee(SituationFamilleMenageCommun ancienne, SituationFamilleMenageCommun nouvelle) {
		++nbSituationsTotal;
		final Tiers tiers = ancienne.getContribuable();
		situationsTraitees.add(new Situation(tiers.getNumero(), tiers.getOfficeImpotId(), ancienne.getId(), nouvelle.getId()));
	}

	public void addIgnoreBaremeNonDoubleGain(SituationFamilleMenageCommun situation, String message) {
		++nbSituationsTotal;
		final Tiers tiers = situation.getContribuable();
		situationsIgnorees.add(new Ignore(tiers.getNumero(), tiers.getOfficeImpotId(), situation.getId(),
				IgnoreType.BAREME_NON_DOUBLE_GAIN, message, getNom(tiers.getNumero())));
	}

	@Override
	public void addErrorException(Long situationDI, Exception e) {
		++nbSituationsTotal;
		situationsEnErrors.add(new Erreur(0, null, situationDI, ErreurType.EXCEPTION, e.getMessage(), StringUtils.EMPTY));
	}

	@Override
	public void addAll(ReinitialiserBaremeDoubleGainResults rapport) {
		nbSituationsTotal += rapport.nbSituationsTotal;
		situationsTraitees.addAll(rapport.situationsTraitees);
		situationsEnErrors.addAll(rapport.situationsEnErrors);
	}
}
