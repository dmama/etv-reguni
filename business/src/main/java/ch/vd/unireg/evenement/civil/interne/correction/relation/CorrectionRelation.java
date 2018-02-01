package ch.vd.unireg.evenement.civil.interne.correction.relation;

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
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.tiers.AppartenanceMenage;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;

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
			erreurs.addErreur("L'historique des conjoints fiscaux n'est pas réconciliable de manière univoque avec les données civiles.");
		}
	}

	private static final StringRenderer<RelationConjoint> REL_CNJNT_RENDERER = rc -> {
		if (rc.noIndividuConjoint == null && rc.conjointFiscalConnu) {
			return String.format("%s -> null (avec conjoint fiscal)", DateRangeHelper.toDisplayString(rc.dateDebut, rc.dateFin));
		}
		else {
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
				else {
					// marié seul
					set.add(RelationConjoint.seul(ret.getDateDebut(), ret.getDateFin()));
				}
			}
		}
		return set.isEmpty() ? Collections.emptyList() : new ArrayList<>(set);
	}

	/**
	 * Retourne une liste triée sans doublon des relations de conjoints civils de l'individu indiqué (en prenant en compte les éventuelles séparations)
	 * @param individu individu dont on cherche les conjoints civils
	 * @return une liste triée
	 */
	@NotNull
	private static List<RelationConjoint> getConjointsCivils(Individu individu) {
		final Set<RelationConjoint> set = new TreeSet<>();
		final List<RelationVersIndividu> conjoints = individu.getConjoints();
		if (conjoints != null) {
			for (RelationVersIndividu relation : conjoints) {
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
						finEtatCivil = ec.getDateDebut().getOneDayBefore();
					}
				}
			}
		}

		// maintenant, on va essayer de détecter les périodes "marié seul"... i.e. les périodes avec un état civil "marié" ou "pacsé" sans conjoint annoncé
		RegDate finEtatCivil = individu.getDateDeces();
		for (EtatCivil ec : CollectionsUtils.revertedOrder(individu.getEtatsCivils().asList())) {
			if (ec.getTypeEtatCivil() == TypeEtatCivil.MARIE || ec.getTypeEtatCivil() == TypeEtatCivil.PACS) {
				final RelationVersIndividu relation = conjoints != null ? DateRangeHelper.rangeAt(conjoints, ec.getDateDebut()) : null;
				if (relation == null) {
					// on dirait bien qu'on a trouvé un état civil "couple" sans conjoint annoncé -> marié seul
					set.add(RelationConjoint.seul(ec.getDateDebut(), finEtatCivil));
				}
			}
			if (ec.getDateDebut() == null) {
				break;
			}
			finEtatCivil = ec.getDateDebut().getOneDayBefore();
		}

		return set.isEmpty() ? Collections.emptyList() : new ArrayList<>(set);
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
		return actifs.isEmpty() ? Collections.emptyList() : actifs;
	}
}
