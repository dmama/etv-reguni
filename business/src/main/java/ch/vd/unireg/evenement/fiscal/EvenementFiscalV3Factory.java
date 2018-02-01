package ch.vd.unireg.evenement.fiscal;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.event.fiscal.v3.AnnulationAllegementFiscal;
import ch.vd.unireg.xml.event.fiscal.v3.AnnulationFlagEntreprise;
import ch.vd.unireg.xml.event.fiscal.v3.AnnulationFor;
import ch.vd.unireg.xml.event.fiscal.v3.AnnulationRegimeFiscal;
import ch.vd.unireg.xml.event.fiscal.v3.CategorieTiers;
import ch.vd.unireg.xml.event.fiscal.v3.ChangementModeImposition;
import ch.vd.unireg.xml.event.fiscal.v3.ChangementSituationFamille;
import ch.vd.unireg.xml.event.fiscal.v3.DeclarationImpot;
import ch.vd.unireg.xml.event.fiscal.v3.EnvoiLettreBienvenue;
import ch.vd.unireg.xml.event.fiscal.v3.FermetureAllegementFiscal;
import ch.vd.unireg.xml.event.fiscal.v3.FermetureFlagEntreprise;
import ch.vd.unireg.xml.event.fiscal.v3.FermetureFor;
import ch.vd.unireg.xml.event.fiscal.v3.FermetureRegimeFiscal;
import ch.vd.unireg.xml.event.fiscal.v3.FinAutoriteParentale;
import ch.vd.unireg.xml.event.fiscal.v3.InformationComplementaire;
import ch.vd.unireg.xml.event.fiscal.v3.ListeRecapitulative;
import ch.vd.unireg.xml.event.fiscal.v3.Naissance;
import ch.vd.unireg.xml.event.fiscal.v3.OuvertureAllegementFiscal;
import ch.vd.unireg.xml.event.fiscal.v3.OuvertureFlagEntreprise;
import ch.vd.unireg.xml.event.fiscal.v3.OuvertureFor;
import ch.vd.unireg.xml.event.fiscal.v3.OuvertureRegimeFiscal;
import ch.vd.unireg.xml.event.fiscal.v3.TypeEvenementFiscalDeclarationRappelable;
import ch.vd.unireg.xml.event.fiscal.v3.TypeEvenementFiscalDeclarationSommable;
import ch.vd.unireg.xml.event.fiscal.v3.TypeInformationComplementaire;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.tiers.AllegementFiscalCommune;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalAvecMotifs;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.EnumHelper;

public abstract class EvenementFiscalV3Factory {

	private static final Map<Class<? extends EvenementFiscal>, OutputDataFactory<? extends EvenementFiscal, ? extends ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscal>> FACTORIES = buildOutputDataFactories();

	/**
	 * Exception lancée par les factories qui indique que l'événement fiscal n'est pas supporté pour le canal v3
	 */
	private static class NotSupportedInHereException extends Exception {
	}

	private interface OutputDataFactory<I extends EvenementFiscal, O extends ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscal> {
		@NotNull
		O build(@NotNull I evenementFiscal) throws NotSupportedInHereException;
	}

	private static <I extends EvenementFiscal, O extends ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscal>
	void registerOutputDataFactory(Map<Class<? extends EvenementFiscal>, OutputDataFactory<? extends EvenementFiscal, ? extends ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscal>> map,
	                               Class<I> inputClass,
	                               OutputDataFactory<? super I, O> factory) {
		map.put(inputClass, factory);
	}

	private static Map<Class<? extends EvenementFiscal>, OutputDataFactory<? extends EvenementFiscal, ? extends ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscal>> buildOutputDataFactories() {
		final Map<Class<? extends EvenementFiscal>, OutputDataFactory<? extends EvenementFiscal, ? extends ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscal>> map = new HashMap<>();
		registerOutputDataFactory(map, EvenementFiscalAllegementFiscal.class, new AllegementFactory());
		registerOutputDataFactory(map, EvenementFiscalDeclarationSommable.class, new DeclarationSommableFactory());
		registerOutputDataFactory(map, EvenementFiscalDeclarationRappelable.class, new DeclarationRappelableFactory());
		registerOutputDataFactory(map, EvenementFiscalFor.class, new ForFactory());
		registerOutputDataFactory(map, EvenementFiscalInformationComplementaire.class, new InformationComplementaireFactory());
		registerOutputDataFactory(map, EvenementFiscalParente.class, new ParenteFactory());
		registerOutputDataFactory(map, EvenementFiscalRegimeFiscal.class, new RegimeFiscalFactory());
		registerOutputDataFactory(map, EvenementFiscalSituationFamille.class, new SituationFamilleFactory());
		registerOutputDataFactory(map, EvenementFiscalFlagEntreprise.class, new FlagEntrepriseFactory());
		registerOutputDataFactory(map, EvenementFiscalEnvoiLettreBienvenue.class, new EnvoiLettreBienvenueFactory());
		return map;
	}

	private static CategorieTiers extractCategorieTiers(Tiers tiers) {
		if (tiers instanceof DebiteurPrestationImposable) {
			return CategorieTiers.IS;
		}
		if (tiers instanceof ContribuableImpositionPersonnesPhysiques) {
			return CategorieTiers.PP;
		}
		if (tiers instanceof ContribuableImpositionPersonnesMorales) {
			return CategorieTiers.PM;
		}
		throw new IllegalArgumentException("Type de tiers non-supporté : " + tiers.getClass().getSimpleName());
	}

	private static int safeLongIdToInt(long l) {
		if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Valeur d'identifiant invalide : " + l);
		}
		return (int) l;
	}

	private static class AllegementFactory implements OutputDataFactory<EvenementFiscalAllegementFiscal, ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalAllegementFiscal> {
		@NotNull
		@Override
		public ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalAllegementFiscal build(@NotNull EvenementFiscalAllegementFiscal evenementFiscal) {
			final ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalAllegementFiscal instance = instanciate(evenementFiscal.getType());
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setNumeroTiers(safeLongIdToInt(tiers.getNumero()));
			instance.setCategorieTiers(extractCategorieTiers(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			final AllegementFiscal allegementFiscal = evenementFiscal.getAllegementFiscal();
			instance.setGenreImpot(EnumHelper.coreToXMLv5(allegementFiscal.getTypeImpot()));
			instance.setTypeCollectivite(EnumHelper.coreToXMLv5(allegementFiscal.getTypeCollectivite(),
			                                                    allegementFiscal instanceof AllegementFiscalCommune ? ((AllegementFiscalCommune) allegementFiscal).getNoOfsCommune() : null));
			return instance;
		}
	}

	protected static ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalAllegementFiscal instanciate(EvenementFiscalAllegementFiscal.TypeEvenementFiscalAllegement type) {
		switch (type) {
		case ANNULATION:
			return new AnnulationAllegementFiscal();
		case FERMETURE:
			return new FermetureAllegementFiscal();
		case OUVERTURE:
			return new OuvertureAllegementFiscal();
		default:
			throw new IllegalArgumentException("Type d'événement fiscal d'allègement non-supporté : " + type);
		}
	}

	private static class DeclarationSommableFactory implements OutputDataFactory<EvenementFiscalDeclarationSommable, ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalDeclarationSommable> {
		@NotNull
		@Override
		public ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalDeclarationSommable build(@NotNull EvenementFiscalDeclarationSommable evenementFiscal) {
			final Declaration declaration = evenementFiscal.getDeclaration();
			final ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalDeclarationSommable instance = instanciateSommable(declaration);
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setNumeroTiers(safeLongIdToInt(tiers.getNumero()));
			instance.setCategorieTiers(extractCategorieTiers(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setDateFrom(DataHelper.coreToXMLv2(declaration.getDateDebut()));
			instance.setDateTo(DataHelper.coreToXMLv2(declaration.getDateFin()));
			instance.setType(mapType(evenementFiscal.getTypeAction()));
			return instance;
		}

		private static ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalDeclarationSommable instanciateSommable(Declaration declaration) {
			if (declaration instanceof DeclarationImpotOrdinaire) {
				return new DeclarationImpot();
			}
			if (declaration instanceof DeclarationImpotSource) {
				return new ListeRecapitulative();
			}
			throw new IllegalArgumentException("Type de déclaration non-supporté : " + declaration.getClass().getSimpleName());
		}
	}

	protected static TypeEvenementFiscalDeclarationSommable mapType(EvenementFiscalDeclarationSommable.TypeAction type) {
		switch (type) {
		case ANNULATION:
			return TypeEvenementFiscalDeclarationSommable.ANNULATION;
		case ECHEANCE:
			return TypeEvenementFiscalDeclarationSommable.ECHEANCE;
		case EMISSION:
			return TypeEvenementFiscalDeclarationSommable.EMISSION;
		case QUITTANCEMENT:
			return TypeEvenementFiscalDeclarationSommable.QUITTANCEMENT;
		case SOMMATION:
			return TypeEvenementFiscalDeclarationSommable.SOMMATION;
		default:
			throw new IllegalArgumentException("Type d'action sur une déclaration 'sommable' non-supporté : " + type);
		}
	}

	private static class DeclarationRappelableFactory implements OutputDataFactory<EvenementFiscalDeclarationRappelable, ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalDeclarationRappelable> {
		@NotNull
		@Override
		public ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalDeclarationRappelable build(@NotNull EvenementFiscalDeclarationRappelable evenementFiscal) {
			final Declaration declaration = evenementFiscal.getDeclaration();
			final ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalDeclarationRappelable instance = instanciateRappelable(declaration);
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setNumeroTiers(safeLongIdToInt(tiers.getNumero()));
			instance.setCategorieTiers(extractCategorieTiers(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setDateFrom(DataHelper.coreToXMLv2(declaration.getDateDebut()));
			instance.setDateTo(DataHelper.coreToXMLv2(declaration.getDateFin()));
			instance.setType(mapType(evenementFiscal.getTypeAction()));
			return instance;
		}

		private static ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalDeclarationRappelable instanciateRappelable(Declaration declaration) {
			if (declaration instanceof QuestionnaireSNC) {
				return new ch.vd.unireg.xml.event.fiscal.v3.QuestionnaireSNC();
			}
			throw new IllegalArgumentException("Type de déclaration non-supporté : " + declaration.getClass().getSimpleName());
		}
	}

	protected static TypeEvenementFiscalDeclarationRappelable mapType(EvenementFiscalDeclarationRappelable.TypeAction type) {
		switch (type) {
		case ANNULATION:
			return TypeEvenementFiscalDeclarationRappelable.ANNULATION;
		case EMISSION:
			return TypeEvenementFiscalDeclarationRappelable.EMISSION;
		case QUITTANCEMENT:
			return TypeEvenementFiscalDeclarationRappelable.QUITTANCEMENT;
		case RAPPEL:
			return TypeEvenementFiscalDeclarationRappelable.RAPPEL;
		default:
			throw new IllegalArgumentException("Type d'action sur une déclaration 'rappelable' non-supporté : " + type);
		}
	}

	private static class ForFactory implements OutputDataFactory<EvenementFiscalFor, ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalFor> {
		@NotNull
		@Override
		public ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalFor build(@NotNull EvenementFiscalFor evenementFiscal) {
			final ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalFor instance = instanciate(evenementFiscal.getType());
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setNumeroTiers(safeLongIdToInt(tiers.getNumero()));
			instance.setCategorieTiers(extractCategorieTiers(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			final ForFiscal forFiscal = evenementFiscal.getForFiscal();
			instance.setForPrincipal(forFiscal.isPrincipal());
			instance.setLocalisationFor(EnumHelper.coreToXMLv4(forFiscal.getTypeAutoriteFiscale()));
			if (forFiscal instanceof ForFiscalAvecMotifs) {
				final ForFiscalAvecMotifs avecMotifs = (ForFiscalAvecMotifs) forFiscal;
				if (instance instanceof OuvertureFor) {
					((OuvertureFor) instance).setMotifOuverture(EnumHelper.coreToXMLv4(avecMotifs.getMotifOuverture()));
				}
				else if (instance instanceof FermetureFor) {
					((FermetureFor) instance).setMotifFermeture(EnumHelper.coreToXMLv4(avecMotifs.getMotifFermeture()));
				}
			}
			if (instance instanceof ChangementModeImposition) {
				if (!(forFiscal instanceof ForFiscalPrincipalPP)) {
					throw new IllegalArgumentException("On ne peut changer le mode d'imposition que sur un for fiscal principal PP.");
				}
				final ForFiscalPrincipalPP ffp = (ForFiscalPrincipalPP) forFiscal;
				((ChangementModeImposition) instance).setModeImposition(EnumHelper.coreToXMLv4(ffp.getModeImposition()));
			}
			return instance;
		}
	}

	protected static ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalFor instanciate(EvenementFiscalFor.TypeEvenementFiscalFor type) {
		switch (type) {
		case ANNULATION:
			return new AnnulationFor();
		case CHGT_MODE_IMPOSITION:
			return new ChangementModeImposition();
		case FERMETURE:
			return new FermetureFor();
		case OUVERTURE:
			return new OuvertureFor();
		default:
			throw new IllegalArgumentException("Type d'événement fiscal sur for non-supporté : " + type);
		}
	}

	private static class InformationComplementaireFactory implements OutputDataFactory<EvenementFiscalInformationComplementaire, InformationComplementaire> {
		@NotNull
		@Override
		public InformationComplementaire build(@NotNull EvenementFiscalInformationComplementaire evenementFiscal) {
			final InformationComplementaire instance = new InformationComplementaire();
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setNumeroTiers(safeLongIdToInt(tiers.getNumero()));
			instance.setCategorieTiers(extractCategorieTiers(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setType(mapType(evenementFiscal.getType()));
			return instance;
		}
	}

	protected static TypeInformationComplementaire mapType(EvenementFiscalInformationComplementaire.TypeInformationComplementaire type) {
		switch (type) {
		case ANNULATION_FAILLITE:
			return TypeInformationComplementaire.ANNULATION_FAILLITE;
		case ANNULATION_FUSION:
			return TypeInformationComplementaire.ANNULATION_FUSION;
		case ANNULATION_SCISSION:
			return TypeInformationComplementaire.ANNULATION_SCISSION;
		case ANNULATION_TRANFERT_PATRIMOINE:
			return TypeInformationComplementaire.ANNULATION_TRANSFERT_PATRIMOINE;
		case ANNULATION_SURSIS_CONCORDATAIRE:
			return TypeInformationComplementaire.ANNULATION_SURSIS_CONCORDATAIRE;
		case APPEL_CREANCIERS_CONCORDAT:
			return TypeInformationComplementaire.APPEL_CREANCIERS_CONCORDAT;
		case APPEL_CREANCIERS_TRANSFERT_HS:
			return TypeInformationComplementaire.APPEL_CREANCIERS_TRANSFERT_HS;
		case AUDIENCE_LIQUIDATION_ABANDON_ACTIF:
			return TypeInformationComplementaire.AUDIENCE_LIQUIDATION_ABANDON_ACTIF;
		case AVIS_PREALABLE_OUVERTURE_FAILLITE:
			return TypeInformationComplementaire.AVIS_PREALABLE_OUVERTURE_FAILLITE;
		case CHANGEMENT_FORME_JURIDIQUE_MEME_CATEGORIE:
			return TypeInformationComplementaire.CHANGEMENT_FORME_JURIDIQUE_MEME_CATEGORIE;
		case CLOTURE_FAILLITE:
			return TypeInformationComplementaire.CLOTURE_FAILLITE;
		case CONCORDAT_BANQUE_CAISSE_EPARGNE:
			return TypeInformationComplementaire.CONCORDAT_BANQUE_CAISSE_EPARGNE;
		case ETAT_COLLOCATION_CONCORDAT_ABANDON_ACTIF:
			return TypeInformationComplementaire.ETAT_COLLOCATION_CONCORDAT_ABANDON_ACTIF;
		case ETAT_COLLOCATION_INVENTAIRE_FAILLITE:
			return TypeInformationComplementaire.ETAT_COLLOCATION_INVENTAIRE_FAILLITE;
		case FUSION:
			return TypeInformationComplementaire.FUSION;
		case HOMOLOGATION_CONCORDAT:
			return TypeInformationComplementaire.HOMOLOGATION_CONCORDAT;
		case LIQUIDATION:
			return TypeInformationComplementaire.LIQUIDATION;
		case MODIFICATION_BUT:
			return TypeInformationComplementaire.MODIFICATION_BUT;
		case MODIFICATION_CAPITAL:
			return TypeInformationComplementaire.MODIFICATION_CAPITAL;
		case MODIFICATION_STATUTS:
			return TypeInformationComplementaire.MODIFICATION_STATUTS;
		case PROLONGATION_SURSIS_CONCORDATAIRE:
			return TypeInformationComplementaire.PROLONGATION_SURSIS_CONCORDATAIRE;
		case PUBLICATION_FAILLITE_APPEL_CREANCIERS:
			return TypeInformationComplementaire.PUBLICATION_FAILLITE_APPEL_CREANCIERS;
		case REVOCATION_FAILLITE:
			return TypeInformationComplementaire.REVOCATION_FAILLITE;
		case SCISSION:
			return TypeInformationComplementaire.SCISSION;
		case SURSIS_CONCORDATAIRE:
			return TypeInformationComplementaire.SURSIS_CONCORDATAIRE;
		case SURSIS_CONCORDATAIRE_PROVISOIRE:
			return TypeInformationComplementaire.SURSIS_CONCORDATAIRE_PROVISOIRE;
		case SUSPENSION_FAILLITE:
			return TypeInformationComplementaire.SUSPENSION_FAILLITE;
		case TABLEAU_DISTRIBUTION_DECOMPTE_FINAL_CONCORDAT:
			return TypeInformationComplementaire.TABLEAU_DISTRIBUTION_DECOMPTE_FINAL_CONCORDAT;
		case TRANSFERT_PATRIMOINE:
			return TypeInformationComplementaire.TRANSFERT_PATRIMOINE;
		case VENTE_ENCHERES_FORCEE_IMMEUBLES_FAILLITE:
			return TypeInformationComplementaire.VENTE_ENCHERES_FORCEE_IMMEUBLES_FAILLITE;
		case VENTE_ENCHERES_FORCEE_IMMEUBLES_POURSUITE:
			return TypeInformationComplementaire.VENTE_ENCHERES_FORCEE_IMMEUBLES_POURSUITE;
		default:
			throw new IllegalArgumentException("Type d'information complémentaire non-supporté : " + type);
		}
	}

	private static class ParenteFactory implements OutputDataFactory<EvenementFiscalParente, ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalParente> {
		@NotNull
		@Override
		public ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalParente build(@NotNull EvenementFiscalParente evenementFiscal) {
			final ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalParente instance = instanciate(evenementFiscal.getType());
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setNumeroTiers(safeLongIdToInt(tiers.getNumero()));
			instance.setCategorieTiers(extractCategorieTiers(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setNoContribuableEnfant(safeLongIdToInt(evenementFiscal.getEnfant().getNumero()));
			return instance;
		}
	}

	protected static ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalParente instanciate(EvenementFiscalParente.TypeEvenementFiscalParente type) {
		switch (type) {
		case NAISSANCE:
			return new Naissance();
		case FIN_AUTORITE_PARENTALE:
			return new FinAutoriteParentale();
		default:
			throw new IllegalArgumentException("Type d'événement fiscal de parenté non-supporté : " + type);
		}
	}

	private static class RegimeFiscalFactory implements OutputDataFactory<EvenementFiscalRegimeFiscal, ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalRegimeFiscal> {
		@NotNull
		@Override
		public ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalRegimeFiscal build(@NotNull EvenementFiscalRegimeFiscal evenementFiscal) {
			final ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalRegimeFiscal instance = instanciate(evenementFiscal.getType());
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setNumeroTiers(safeLongIdToInt(tiers.getNumero()));
			instance.setCategorieTiers(extractCategorieTiers(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setScope(EnumHelper.coreToXMLv5(evenementFiscal.getRegimeFiscal().getPortee()));
			return instance;
		}
	}

	protected static ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalRegimeFiscal instanciate(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime type) {
		switch (type) {
		case ANNULATION:
			return new AnnulationRegimeFiscal();
		case FERMETURE:
			return new FermetureRegimeFiscal();
		case OUVERTURE:
			return new OuvertureRegimeFiscal();
		default:
			throw new IllegalArgumentException("Type d'événement fiscal de régime fiscal non-supporté : " + type);
		}
	}

	private static class SituationFamilleFactory implements OutputDataFactory<EvenementFiscalSituationFamille, ChangementSituationFamille> {
		@NotNull
		@Override
		public ChangementSituationFamille build(@NotNull EvenementFiscalSituationFamille evenementFiscal) {
			final ChangementSituationFamille instance = new ChangementSituationFamille();
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setNumeroTiers(safeLongIdToInt(tiers.getNumero()));
			instance.setCategorieTiers(extractCategorieTiers(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			return instance;
		}
	}

	private static class FlagEntrepriseFactory implements OutputDataFactory<EvenementFiscalFlagEntreprise, ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalFlagEntreprise> {
		@NotNull
		@Override
		public ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalFlagEntreprise build(@NotNull EvenementFiscalFlagEntreprise evenementFiscal) {
			final ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalFlagEntreprise instance = instanciate(evenementFiscal.getType());
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setNumeroTiers(safeLongIdToInt(tiers.getNumero()));
			instance.setCategorieTiers(extractCategorieTiers(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setTypeFlag(EnumHelper.coreToXMLv5(evenementFiscal.getFlag().getType()));
			return instance;
		}
	}

	protected static ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalFlagEntreprise instanciate(EvenementFiscalFlagEntreprise.TypeEvenementFiscalFlagEntreprise type) {
		switch (type) {
		case ANNULATION:
			return new AnnulationFlagEntreprise();
		case FERMETURE:
			return new FermetureFlagEntreprise();
		case OUVERTURE:
			return new OuvertureFlagEntreprise();
		default:
			throw new IllegalArgumentException("Type d'événement fiscal de flag entreprise non-supporté : " + type);
		}
	}

	private static class EnvoiLettreBienvenueFactory implements OutputDataFactory<EvenementFiscalEnvoiLettreBienvenue, EnvoiLettreBienvenue> {
		@NotNull
		@Override
		public EnvoiLettreBienvenue build(@NotNull EvenementFiscalEnvoiLettreBienvenue evenementFiscal) {
			final EnvoiLettreBienvenue instance = new EnvoiLettreBienvenue();
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setNumeroTiers(safeLongIdToInt(tiers.getNumero()));
			instance.setCategorieTiers(extractCategorieTiers(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			return instance;
		}
	}

	@Nullable
	public static <T extends EvenementFiscal> ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscal buildOutputData(T evt) {
		//noinspection unchecked
		final OutputDataFactory<? super T, ? extends ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscal> factory = (OutputDataFactory<? super T, ? extends ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscal>) FACTORIES.get(evt.getClass());
		if (factory == null) {
			return null;
		}
		try {
			return factory.build(evt);
		}
		catch (NotSupportedInHereException e) {
			return null;
		}
	}
}
