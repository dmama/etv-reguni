package ch.vd.uniregctb.evenement.civil.interne.correction.relation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.civil.data.TypeRelationVersIndividu;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;

/**
 * Evénement de traitement des corrections de relation : filiations/parentés et conjoints
 */
public class CorrectionRelation extends EvenementCivilInterne {

	public CorrectionRelation(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(AttributeIndividu.CONJOINTS);
		parts.add(AttributeIndividu.PARENTS);
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		// on arrive ici que s'il n'y a pas de différence dans les conjoints
		// on va re-calculer les parentés pour être sûr, et puis c'est tout...
		// rien de spécial à faire, l'intercepteur fait tout le boulot...
		return HandleStatus.TRAITE;
	}

	@Override
	protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		if (hasDifferenceSurConjoints()) {
			// dans le cas où les conjoints ne sont pas ceux que l'on croit, on part en traitement manuel
			erreurs.addErreur(String.format("L'historique des conjoints fiscaux n'est pas réconciliable de manière univoque avec les données civiles."));
		}
	}

	private static final StringRenderer<RelationConjoint> REL_CNJNT_RENDERER = new StringRenderer<RelationConjoint>() {
		@Override
		public String toString(RelationConjoint rc) {
			return String.format("%s -> %d", DateRangeHelper.toDisplayString(rc.dateDebut, rc.dateFin), rc.noIndividuConjoint);
		}
	};

	private boolean hasDifferenceSurConjoints() {
		final List<RelationConjoint> fisc = getConjointsFiscaux(getPrincipalPP());
		final List<RelationConjoint> civ = getConjointsCivils(getIndividu());

		final boolean hasDifference = RelationConjoint.hasDifference(fisc, civ);
		if (hasDifference) {
			Audit.error(getNumeroEvenement(),
			            String.format("Conjoints différents : fiscal = {%s}, civil = {%s}",
			                          CollectionsUtils.toString(fisc, REL_CNJNT_RENDERER, ", "),
			                          CollectionsUtils.toString(civ, REL_CNJNT_RENDERER, ", ")));
		}
		return hasDifference;
	}

	/**
	 * Retourne une liste triée sans doublon des relations de conjoints fiscaux de la personne physique indiquée
	 * @param pp personne physique dont on cherche les conjoints fiscaux
	 * @return une liste triée
	 */
	@NotNull
	private List<RelationConjoint> getConjointsFiscaux(PersonnePhysique pp) {
		final Set<RelationConjoint> set = new TreeSet<>();
		for (RapportEntreTiers ret : pp.getRapportsSujet()) {
			if (!ret.isAnnule() && ret instanceof AppartenanceMenage) {
				final EnsembleTiersCouple couple = context.getTiersService().getEnsembleTiersCouple(pp, ret.getDateDebut());
				final PersonnePhysique conjoint = couple.getConjoint(pp);
				if (conjoint != null) {
					set.add(RelationConjoint.from(ret.getDateDebut(), ret.getDateFin(), conjoint));
				}
			}
		}
		return set.isEmpty() ? Collections.<RelationConjoint>emptyList() : new ArrayList<>(set);
	}

	/**
	 * Retourne une liste triée sans doublon des relations de conjoints civils de l'individu indiqué (en prenant en compte les éventuelles séparations)
	 * @param individu individu dont on cherche les conjoints civils
	 * @return une liste triée
	 */
	@NotNull
	private static List<RelationConjoint> getConjointsCivils(Individu individu) {
		final Set<RelationConjoint> set = new TreeSet<>();
		if (individu.getConjoints() != null) {
			for (RelationVersIndividu relation : individu.getConjoints()) {
				if (relation.getTypeRelation() == TypeRelationVersIndividu.CONJOINT) {
					final List<EtatCivil> etatsCivils = getEtatsCivilsActifs(individu, relation);
					RegDate finEtatCivil = relation.getDateFin();
					for (EtatCivil ec : CollectionsUtils.revertedOrder(etatsCivils)) {
						if (ec.getTypeEtatCivil() == TypeEtatCivil.MARIE || ec.getTypeEtatCivil() == TypeEtatCivil.PACS) {
							set.add(RelationConjoint.from(ec.getDateDebut(), finEtatCivil, relation));
						}
						if (ec.getDateDebut() == null) {
							break;
						}
						else {
							finEtatCivil = ec.getDateDebut().getOneDayBefore();
						}
					}
				}
			}
		}
		return set.isEmpty() ? Collections.<RelationConjoint>emptyList() : new ArrayList<>(set);
	}

	@NotNull
	private static List<EtatCivil> getEtatsCivilsActifs(Individu individu, RelationVersIndividu relation) {
		final List<EtatCivil> all = individu.getEtatsCivils().asList();
		final List<EtatCivil> actifs = new ArrayList<>(all.size());
		for (EtatCivil candidate : all) {
			if (RegDateHelper.isBetween(candidate.getDateDebut(), relation.getDateDebut(), relation.getDateFin(), NullDateBehavior.EARLIEST)) {
				actifs.add(candidate);
			}
		}
		return actifs.isEmpty() ? Collections.<EtatCivil>emptyList() : actifs;
	}
}
