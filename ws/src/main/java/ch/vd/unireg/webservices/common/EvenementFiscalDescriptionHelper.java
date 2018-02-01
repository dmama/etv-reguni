package ch.vd.uniregctb.webservices.common;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalAllegementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDeclarationRappelable;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDeclarationSommable;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalEnvoiLettreBienvenue;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalFlagEntreprise;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalFor;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalImpressionFourreNeutre;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalInformationComplementaire;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalParente;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalRegimeFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalSituationFamille;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAvecMotifs;
import ch.vd.uniregctb.tiers.RegimeFiscal;

public abstract class EvenementFiscalDescriptionHelper {

	/**
	 * Point d'entrée principal pour la détermination d'une chaîne de caractères descriptive pour un événement fiscal
	 * @param evtFiscal l'événement fiscal à décrire
	 * @return une chaîne de caractères descriptive
	 */
	public static String getTextualDescription(@NotNull EvenementFiscal evtFiscal) {
		//noinspection unchecked
		final StringRenderer<? super EvenementFiscal> descriptor = (StringRenderer<? super EvenementFiscal>) DESCRIPTORS.get(evtFiscal.getClass());
		if (descriptor == null) {
			throw new IllegalArgumentException("Descripteur manquant pour la classe " + evtFiscal.getClass());
		}
		return descriptor.toString(evtFiscal);
	}

	/**
	 * Map des fabricants de descriptions d'événements fiscaux
	 */
	private static final Map<Class<? extends EvenementFiscal>, StringRenderer<?>> DESCRIPTORS = buildEvenementFiscalDescriptors();

	/**
	 * Pour conserver un peu de validation sur le type des éléments dans cette map
	 */
	private static <T extends EvenementFiscal> void addToFiscalEventDescriptorMap(Map<Class<? extends EvenementFiscal>, StringRenderer<?>> map,
	                                                                              Class<T> evenementFiscalClass,
	                                                                              StringRenderer<? super T> descriptionRenderer) {
		map.put(evenementFiscalClass, descriptionRenderer);
	}

	private static Map<Class<? extends EvenementFiscal>, StringRenderer<?>> buildEvenementFiscalDescriptors() {
		final Map<Class<? extends EvenementFiscal>, StringRenderer<?>> map = new HashMap<>();
		addToFiscalEventDescriptorMap(map, EvenementFiscalEnvoiLettreBienvenue.class, new EvenementFiscalEnvoiLettreBienvenueDescriptor());
		addToFiscalEventDescriptorMap(map, EvenementFiscalParente.class, new EvenementFiscalParenteDescriptor());
		addToFiscalEventDescriptorMap(map, EvenementFiscalFor.class, new EvenementFiscalForDescriptor());
		addToFiscalEventDescriptorMap(map, EvenementFiscalAllegementFiscal.class, new EvenementFiscalAllegementFiscalDescriptor());
		addToFiscalEventDescriptorMap(map, EvenementFiscalFlagEntreprise.class, new EvenementFiscalFlagEntrepriseDescriptor());
		addToFiscalEventDescriptorMap(map, EvenementFiscalSituationFamille.class, new EvenementFiscalSituationFamilleDescriptor());
		addToFiscalEventDescriptorMap(map, EvenementFiscalInformationComplementaire.class, new EvenementFiscalInformationComplementaireDescriptor());
		addToFiscalEventDescriptorMap(map, EvenementFiscalDeclarationSommable.class, new EvenementFiscalDeclarationSommableDescriptor());
		addToFiscalEventDescriptorMap(map, EvenementFiscalDeclarationRappelable.class, new EvenementFiscalDeclarationRappelableDescriptor());
		addToFiscalEventDescriptorMap(map, EvenementFiscalRegimeFiscal.class, new EvenementFiscalRegimeFiscalDescriptor());
		addToFiscalEventDescriptorMap(map, EvenementFiscalImpressionFourreNeutre.class,new EvenementFiscalImpressionFourreNeutreDescriptor());
		return map;
	}

	private static final class EvenementFiscalImpressionFourreNeutreDescriptor implements StringRenderer<EvenementFiscalImpressionFourreNeutre>{
		@Override
		public String toString(EvenementFiscalImpressionFourreNeutre object) {
			return "Impression d'une fourre neutre";
		}
	}

	private static final class EvenementFiscalEnvoiLettreBienvenueDescriptor implements StringRenderer<EvenementFiscalEnvoiLettreBienvenue> {
		@Override
		public String toString(EvenementFiscalEnvoiLettreBienvenue object) {
			return "Envoi d'une lettre de bienvenue";
		}
	}

	private static final class EvenementFiscalParenteDescriptor implements StringRenderer<EvenementFiscalParente> {
		@Override
		public String toString(EvenementFiscalParente object) {
			switch (object.getType()) {
			case FIN_AUTORITE_PARENTALE:
				return "Fin de l'autorité parentale sur l'enfant " + FormatNumeroHelper.numeroCTBToDisplay(object.getEnfant().getNumero());
			case NAISSANCE:
				return "Naissance de l'enfant " + FormatNumeroHelper.numeroCTBToDisplay(object.getEnfant().getNumero());
			default:
				throw new IllegalArgumentException("Type d'opération invalide sur une relation de parenté : " + object.getType());
			}
		}
	}

	private static final class EvenementFiscalForDescriptor implements StringRenderer<EvenementFiscalFor> {
		@Override
		public String toString(EvenementFiscalFor object) {
			final ForFiscal ff = object.getForFiscal();
			final String typeFor = ff.isPrincipal() ? "principal" : ff.isDebiteur() ? "débiteur" : "secondaire";
			switch (object.getType()) {
			case ANNULATION:
				return String.format("Annulation d'un for %s", typeFor);
			case CHGT_MODE_IMPOSITION:
				return "Changement de mode d'imposition";
			case FERMETURE:
				final String motifFermeture;
				if (ff instanceof ForFiscalAvecMotifs && ((ForFiscalAvecMotifs) ff).getMotifFermeture() != null) {
					motifFermeture = String.format(" pour motif '%s'", ((ForFiscalAvecMotifs) ff).getMotifFermeture().getDescription(false));
				}
				else {
					motifFermeture = StringUtils.EMPTY;
				}
				return String.format("Fermeture d'un for %s%s", typeFor, motifFermeture);
			case OUVERTURE:
				final String motifOuverture;
				if (ff instanceof ForFiscalAvecMotifs && ((ForFiscalAvecMotifs) ff).getMotifOuverture() != null) {
					motifOuverture = String.format(" pour motif '%s'", ((ForFiscalAvecMotifs) ff).getMotifOuverture().getDescription(true));
				}
				else {
					motifOuverture = StringUtils.EMPTY;
				}
				return String.format("Ouverture d'un for %s%s", typeFor, motifOuverture);
			default:
				throw new IllegalArgumentException("Type d'opération invalide sur un for : " + object.getType());
			}
		}
	}

	private static final class EvenementFiscalAllegementFiscalDescriptor implements StringRenderer<EvenementFiscalAllegementFiscal> {
		@Override
		public String toString(EvenementFiscalAllegementFiscal object) {
			switch (object.getType()) {
			case ANNULATION:
				return "Annulation d'un allègement fiscal";
			case FERMETURE:
				return "Clôture d'un allègement fiscal";
			case OUVERTURE:
				return "Ouverture d'un nouvel allègement fiscal";
			default:
				throw new IllegalArgumentException("Type d'opération invalide sur un allègement fiscal : " + object.getType());
			}
		}
	}

	private static final class EvenementFiscalFlagEntrepriseDescriptor implements StringRenderer<EvenementFiscalFlagEntreprise> {
		@Override
		public String toString(EvenementFiscalFlagEntreprise object) {
			switch (object.getType()) {
			case ANNULATION:
				return "Annulation d'une spécificité d'entreprise";
			case FERMETURE:
				return "Clôture d'une spéficicité d'entreprise";
			case OUVERTURE:
				return "Ouverture d'une nouvelle spécificité d'entreprise";
			default:
				throw new IllegalArgumentException("Type d'opération invalide sur un flag d'entreprise : " + object.getType());
			}
		}
	}

	private static final class EvenementFiscalSituationFamilleDescriptor implements StringRenderer<EvenementFiscalSituationFamille> {
		@Override
		public String toString(EvenementFiscalSituationFamille object) {
			return "Modification de la situation de famille";
		}
	}

	private static final class EvenementFiscalInformationComplementaireDescriptor implements StringRenderer<EvenementFiscalInformationComplementaire> {
		@Override
		public String toString(EvenementFiscalInformationComplementaire object) {
			return object.getType().toString();
		}
	}

	private static String declarationDescription(Declaration declaration) {
		if (declaration instanceof DeclarationImpotOrdinaire) {
			return String.format("de la %s %d (%s - %s)",
			                     declaration.getModeleDocument() != null ? declaration.getModeleDocument().getTypeDocument().getDescription() : "déclaration d'impôt",
			                     declaration.getPeriode().getAnnee(),
			                     RegDateHelper.dateToDisplayString(declaration.getDateDebut()),
			                     RegDateHelper.dateToDisplayString(declaration.getDateFin()));
		}
		else if (declaration instanceof DeclarationImpotSource) {
			return String.format("de la liste récapitulative IS de la période (%s - %s)",
			                     RegDateHelper.dateToDisplayString(declaration.getDateDebut()),
			                     RegDateHelper.dateToDisplayString(declaration.getDateFin()));
		}
		else if (declaration instanceof QuestionnaireSNC) {
			return String.format("du questionnaire SNC %d", declaration.getPeriode().getAnnee());
		}
		else {
			return String.format("de la déclaration %d", declaration.getPeriode().getAnnee());
		}
	}

	private static final class EvenementFiscalDeclarationRappelableDescriptor implements StringRenderer<EvenementFiscalDeclarationRappelable> {
		@Override
		public String toString(EvenementFiscalDeclarationRappelable object) {
			final String declaration = declarationDescription(object.getDeclaration());
			switch (object.getTypeAction()) {
			case ANNULATION:
				return String.format("Annulation %s", declaration);
			case EMISSION:
				return String.format("Emission %s", declaration);
			case QUITTANCEMENT:
				return String.format("Quittancement %s", declaration);
			case RAPPEL:
				return String.format("Envoi du rappel %s", declaration);
			default:
				throw new IllegalArgumentException("Type d'opération invalide sur une déclaration : " + object.getTypeAction());
			}
		}
	}

	private static final class EvenementFiscalDeclarationSommableDescriptor implements StringRenderer<EvenementFiscalDeclarationSommable> {
		@Override
		public String toString(EvenementFiscalDeclarationSommable object) {
			final String declaration = declarationDescription(object.getDeclaration());
			switch (object.getTypeAction()) {
			case ANNULATION:
				return String.format("Annulation %s", declaration);
			case EMISSION:
				return String.format("Emission %s", declaration);
			case QUITTANCEMENT:
				return String.format("Quittancement %s", declaration);
			case ECHEANCE:
				return String.format("Echéance %s", declaration);
			case SOMMATION:
				return String.format("Envoi de la sommation %s", declaration);
			default:
				throw new IllegalArgumentException("Type d'opération invalide sur une déclaration : " + object.getTypeAction());
			}
		}
	}

	private static final class EvenementFiscalRegimeFiscalDescriptor implements StringRenderer<EvenementFiscalRegimeFiscal> {
		@Override
		public String toString(EvenementFiscalRegimeFiscal object) {
			final RegimeFiscal.Portee portee = object.getRegimeFiscal().getPortee();
			switch (object.getType()) {
			case ANNULATION:
				return String.format("Annulation d'un régime fiscal %s", portee);
			case FERMETURE:
				return String.format("Clôture d'un régime fiscal %s", portee);
			case OUVERTURE:
				return String.format("Ouverture d'un régime fiscal %s", portee);
			default:
				throw new IllegalArgumentException("Type d'opération invalide sur un régime fiscal : " + object.getType());
			}
		}
	}
}
