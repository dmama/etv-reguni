package ch.vd.unireg.xml;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.metier.assujettissement.TypeAssujettissement;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.type.CategorieEtranger;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.type.TypeFlagEntreprise;
import ch.vd.unireg.type.TypePermis;
import ch.vd.unireg.type.TypeRapportEntreTiers;

public class EnumHelperTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataHelperTest.class);

	/**
	 * Vérification que toutes les méthodes coreToXML...(T extends Enum&lt;T&gt;) ont prévu quelque chose
	 * pour chacune des modalités existantes du type énuméré
	 */
	@Test
	public void testCoreToXmlEnumConversionsNoCrashingCase() throws Exception {

		final Map<Pair<String, Class<? extends Enum>>, Set<? extends Enum>> knownCrashingEnums = new HashMap<>();
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv1", CategorieEtranger.class), EnumSet.of(CategorieEtranger._01_SAISONNIER_A));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv2", CategorieEtranger.class), EnumSet.of(CategorieEtranger._01_SAISONNIER_A));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv3", CategorieEtranger.class), EnumSet.of(CategorieEtranger._01_SAISONNIER_A));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv4", CategorieEtranger.class), EnumSet.of(CategorieEtranger._01_SAISONNIER_A));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv5", CategorieEtranger.class), EnumSet.of(CategorieEtranger._01_SAISONNIER_A));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv1", TypePermis.class), EnumSet.of(TypePermis.SAISONNIER));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv2", TypePermis.class), EnumSet.of(TypePermis.SAISONNIER));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv3", TypePermis.class), EnumSet.of(TypePermis.SAISONNIER));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv4", TypePermis.class), EnumSet.of(TypePermis.SAISONNIER));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv5", TypePermis.class), EnumSet.of(TypePermis.SAISONNIER));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv1", TypeRapportEntreTiers.class), EnumSet.of(TypeRapportEntreTiers.PARENTE, TypeRapportEntreTiers.ASSUJETTISSEMENT_PAR_SUBSTITUTION, TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE, TypeRapportEntreTiers.MANDAT, TypeRapportEntreTiers.FUSION_ENTREPRISES, TypeRapportEntreTiers.ADMINISTRATION_ENTREPRISE, TypeRapportEntreTiers.SOCIETE_DIRECTION, TypeRapportEntreTiers.SCISSION_ENTREPRISE, TypeRapportEntreTiers.TRANSFERT_PATRIMOINE, TypeRapportEntreTiers.HERITAGE,TypeRapportEntreTiers.LIENS_ASSOCIES_ET_SNC));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv2", TypeRapportEntreTiers.class), EnumSet.of(TypeRapportEntreTiers.PARENTE, TypeRapportEntreTiers.ASSUJETTISSEMENT_PAR_SUBSTITUTION, TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE, TypeRapportEntreTiers.MANDAT, TypeRapportEntreTiers.FUSION_ENTREPRISES, TypeRapportEntreTiers.ADMINISTRATION_ENTREPRISE, TypeRapportEntreTiers.SOCIETE_DIRECTION, TypeRapportEntreTiers.SCISSION_ENTREPRISE, TypeRapportEntreTiers.TRANSFERT_PATRIMOINE, TypeRapportEntreTiers.HERITAGE,TypeRapportEntreTiers.LIENS_ASSOCIES_ET_SNC));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv3", TypeRapportEntreTiers.class), EnumSet.of(TypeRapportEntreTiers.PARENTE, TypeRapportEntreTiers.ASSUJETTISSEMENT_PAR_SUBSTITUTION, TypeRapportEntreTiers.MANDAT, TypeRapportEntreTiers.ADMINISTRATION_ENTREPRISE, TypeRapportEntreTiers.SOCIETE_DIRECTION, TypeRapportEntreTiers.SCISSION_ENTREPRISE, TypeRapportEntreTiers.TRANSFERT_PATRIMOINE, TypeRapportEntreTiers.HERITAGE,TypeRapportEntreTiers.LIENS_ASSOCIES_ET_SNC));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv1", CategorieImpotSource.class), EnumSet.of(CategorieImpotSource.PARTICIPATIONS_HORS_SUISSE, CategorieImpotSource.EFFEUILLEUSES));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv1", TypeDocument.class), EnumSet.of(TypeDocument.LISTE_RECAPITULATIVE, TypeDocument.E_FACTURE_ATTENTE_CONTACT, TypeDocument.E_FACTURE_ATTENTE_SIGNATURE, TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocument.DECLARATION_IMPOT_APM_LOCAL, TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL, TypeDocument.QUESTIONNAIRE_SNC));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv2", TypeDocument.class), EnumSet.of(TypeDocument.LISTE_RECAPITULATIVE, TypeDocument.E_FACTURE_ATTENTE_CONTACT, TypeDocument.E_FACTURE_ATTENTE_SIGNATURE, TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocument.DECLARATION_IMPOT_APM_LOCAL, TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL, TypeDocument.QUESTIONNAIRE_SNC));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv3", TypeDocument.class), EnumSet.of(TypeDocument.LISTE_RECAPITULATIVE, TypeDocument.E_FACTURE_ATTENTE_CONTACT, TypeDocument.E_FACTURE_ATTENTE_SIGNATURE, TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocument.DECLARATION_IMPOT_APM_LOCAL, TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL, TypeDocument.QUESTIONNAIRE_SNC));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv4", TypeDocument.class), EnumSet.of(TypeDocument.LISTE_RECAPITULATIVE, TypeDocument.E_FACTURE_ATTENTE_CONTACT, TypeDocument.E_FACTURE_ATTENTE_SIGNATURE, TypeDocument.QUESTIONNAIRE_SNC));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv4", TypeDocument.class), EnumSet.of(TypeDocument.LISTE_RECAPITULATIVE, TypeDocument.E_FACTURE_ATTENTE_CONTACT, TypeDocument.E_FACTURE_ATTENTE_SIGNATURE, TypeDocument.QUESTIONNAIRE_SNC));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv5", TypeDocument.class), EnumSet.of(TypeDocument.LISTE_RECAPITULATIVE, TypeDocument.E_FACTURE_ATTENTE_CONTACT, TypeDocument.E_FACTURE_ATTENTE_SIGNATURE, TypeDocument.QUESTIONNAIRE_SNC));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv1", TypeEtatDocumentFiscal.class), EnumSet.of(TypeEtatDocumentFiscal.RAPPELE, TypeEtatDocumentFiscal.SUSPENDU));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv2", TypeEtatDocumentFiscal.class), EnumSet.of(TypeEtatDocumentFiscal.RAPPELE, TypeEtatDocumentFiscal.SUSPENDU));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv3", TypeEtatDocumentFiscal.class), EnumSet.of(TypeEtatDocumentFiscal.RAPPELE, TypeEtatDocumentFiscal.SUSPENDU));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv4", TypeFlagEntreprise.class), EnumSet.of(TypeFlagEntreprise.AUDIT, TypeFlagEntreprise.EXPERTISE, TypeFlagEntreprise.IMIN));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLCorporationv2", TypeAssujettissement.class), EnumSet.of(TypeAssujettissement.DIPLOMATE_SUISSE, TypeAssujettissement.INDIGENT, TypeAssujettissement.MIXTE_137_1, TypeAssujettissement.MIXTE_137_2, TypeAssujettissement.SOURCE_PURE, TypeAssujettissement.VAUDOIS_DEPENSE));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLCorporationv3", TypeAssujettissement.class), EnumSet.of(TypeAssujettissement.DIPLOMATE_SUISSE, TypeAssujettissement.INDIGENT, TypeAssujettissement.MIXTE_137_1, TypeAssujettissement.MIXTE_137_2, TypeAssujettissement.SOURCE_PURE, TypeAssujettissement.VAUDOIS_DEPENSE));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLCorporationv4", TypeAssujettissement.class), EnumSet.of(TypeAssujettissement.DIPLOMATE_SUISSE, TypeAssujettissement.INDIGENT, TypeAssujettissement.MIXTE_137_1, TypeAssujettissement.MIXTE_137_2, TypeAssujettissement.SOURCE_PURE, TypeAssujettissement.VAUDOIS_DEPENSE));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv1", GenrePropriete.class), EnumSet.of(GenrePropriete.PPE, GenrePropriete.FONDS_DOMINANT));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv2", GenrePropriete.class), EnumSet.of(GenrePropriete.PPE, GenrePropriete.FONDS_DOMINANT));

		// boucle sur toutes les méthodes statiques publiques coreToXML... qui prennent un type énuméré en paramètre
		final Method[] methods = EnumHelper.class.getDeclaredMethods();
		int nbTestedMethod = 0;
		for (Method method : methods) {
			final int methodModifiers = method.getModifiers();
			if (Modifier.isPublic(methodModifiers) && Modifier.isStatic(methodModifiers) && method.getName().startsWith("coreToXML")) {
				final Class<?>[] parameterTypes = method.getParameterTypes();
				if (parameterTypes.length == 1 && parameterTypes[0].isEnum()) {
					final Class<? extends Enum> enumType = (Class<? extends Enum>) parameterTypes[0];
					LOGGER.info(String.format("Test de la méthode %s(%s)", method.getName(), enumType));
					++ nbTestedMethod;

					final Set<? extends Enum> expectedCrashingModalities = knownCrashingEnums.get(Pair.<String, Class<? extends Enum>>of(method.getName(), enumType));

					// on va tester toutes les valeurs de l'enum en entrée et vérifier que cela n'explose pas
					// (peu importe ici que la réponse de la méthode soit parfois nulle...)
					for (Enum modalite : enumType.getEnumConstants()) {
						try {
							method.invoke(null, modalite);
							LOGGER.info(String.format("Test de l'appel à %s(%s.%s) OK", method.getName(), enumType.getName(), modalite));

							if (expectedCrashingModalities != null && expectedCrashingModalities.contains(modalite)) {
								// ben ça n'explose plus ?
								Assert.fail(String.format("Prière de mettre à jour le test, apparemment, l'appel à %s(%s.%s) n'explose plus...",
								                          method.getName(), enumType.getName(), modalite));
							}
						}
						catch (InvocationTargetException e) {
							final Throwable cause = e.getCause();
							if (expectedCrashingModalities == null || !expectedCrashingModalities.contains(modalite)) {
								LOGGER.error(String.format("Test de l'appel à %s(%s) KO", method.getName(), modalite), cause);
								Assert.fail(String.format("Méthode %s(%s.%s) a explosé avec une exception %s (%s)",
								                          method.getName(), enumType.getName(), modalite, cause.getClass().getName(), cause.getMessage()));
							}

							LOGGER.warn(String.format("L'appel à %s(%s.%s) explose, mais c'est connu...", method.getName(), enumType.getName(), modalite));
						}
					}
				}
			}
		}

		// juste pour vérifier qu'on ne s'est pas complètement lourdé et que l'on teste bien quelque chose...
		Assert.assertTrue(Integer.toString(nbTestedMethod), nbTestedMethod > 3);
	}

}
