package ch.vd.uniregctb.xml;

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

import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

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
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv1", TypePermis.class), EnumSet.of(TypePermis.SAISONNIER));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv2", TypePermis.class), EnumSet.of(TypePermis.SAISONNIER));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv3", TypePermis.class), EnumSet.of(TypePermis.SAISONNIER));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv4", TypePermis.class), EnumSet.of(TypePermis.SAISONNIER));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv1", TypeRapportEntreTiers.class), EnumSet.of(TypeRapportEntreTiers.PARENTE, TypeRapportEntreTiers.ASSUJETTISSEMENT_PAR_SUBSTITUTION, TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE, TypeRapportEntreTiers.MANDAT, TypeRapportEntreTiers.FUSION_ENTREPRISES));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv2", TypeRapportEntreTiers.class), EnumSet.of(TypeRapportEntreTiers.PARENTE, TypeRapportEntreTiers.ASSUJETTISSEMENT_PAR_SUBSTITUTION, TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE, TypeRapportEntreTiers.MANDAT, TypeRapportEntreTiers.FUSION_ENTREPRISES));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv3", TypeRapportEntreTiers.class), EnumSet.of(TypeRapportEntreTiers.PARENTE, TypeRapportEntreTiers.ASSUJETTISSEMENT_PAR_SUBSTITUTION, TypeRapportEntreTiers.MANDAT));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv1", CategorieImpotSource.class), EnumSet.of(CategorieImpotSource.PARTICIPATIONS_HORS_SUISSE, CategorieImpotSource.EFFEUILLEUSES));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv1", TypeDocument.class), EnumSet.of(TypeDocument.LISTE_RECAPITULATIVE, TypeDocument.E_FACTURE_ATTENTE_CONTACT, TypeDocument.E_FACTURE_ATTENTE_SIGNATURE, TypeDocument.DECLARATION_IMPOT_APM, TypeDocument.DECLARATION_IMPOT_PM, TypeDocument.QUESTIONNAIRE_SNC));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv2", TypeDocument.class), EnumSet.of(TypeDocument.LISTE_RECAPITULATIVE, TypeDocument.E_FACTURE_ATTENTE_CONTACT, TypeDocument.E_FACTURE_ATTENTE_SIGNATURE, TypeDocument.DECLARATION_IMPOT_APM, TypeDocument.DECLARATION_IMPOT_PM, TypeDocument.QUESTIONNAIRE_SNC));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv3", TypeDocument.class), EnumSet.of(TypeDocument.LISTE_RECAPITULATIVE, TypeDocument.E_FACTURE_ATTENTE_CONTACT, TypeDocument.E_FACTURE_ATTENTE_SIGNATURE, TypeDocument.DECLARATION_IMPOT_APM, TypeDocument.DECLARATION_IMPOT_PM, TypeDocument.QUESTIONNAIRE_SNC));
		knownCrashingEnums.put(Pair.<String, Class<? extends Enum>>of("coreToXMLv4", TypeDocument.class), EnumSet.of(TypeDocument.LISTE_RECAPITULATIVE, TypeDocument.E_FACTURE_ATTENTE_CONTACT, TypeDocument.E_FACTURE_ATTENTE_SIGNATURE, TypeDocument.QUESTIONNAIRE_SNC));

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
					for (Object modalite : enumType.getEnumConstants()) {
						try {
							method.invoke(null, modalite);
							LOGGER.info(String.format("Test de l'appel à %s(%s.%s) OK", method.getName(), enumType.getName(), modalite));

							if (expectedCrashingModalities != null && expectedCrashingModalities.contains(modalite)) {
								// ben ça n'explose plus plus ?
								Assert.fail(String.format("Prière de mettre à jour le test, apparemment, l'appel à %s(%s.%s) n'explose plus...",
								                          method.getName(), enumType.getName(), modalite));
							}
						}
						catch (Exception e) {
							if (expectedCrashingModalities == null || !expectedCrashingModalities.contains(modalite)) {
								LOGGER.error(String.format("Test de l'appel à %s(%s) KO", method.getName(), modalite), e);
								Assert.fail(String.format("Méthode %s(%s.%s) a explosé avec une exception %s (%s)",
								                          method.getName(), enumType.getName(), modalite, e.getClass().getName(), e.getMessage()));
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
