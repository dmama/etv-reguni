package ch.vd.unireg.xml;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.common.WithoutSpringTest;

public class DataHelperTest extends WithoutSpringTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataHelperTest.class);

	/**
	 * Vérification que toutes les méthodes coreToXML...(T extends Enum&lt;T&gt;) ont prévu quelque chose
	 * pour chacune des modalités existantes du type énuméré
	 */
	@Test
	public void testCoreToXmlEnumConversionsNoCrashingCase() throws Exception {

		// boucle sur toutes les méthodes statiques publiques coreToXML... qui prennent un type énuméré en paramètre
		final Method[] methods = DataHelper.class.getDeclaredMethods();
		int nbTestedMethod = 0;
		for (Method method : methods) {
			final int methodModifiers = method.getModifiers();
			if (Modifier.isPublic(methodModifiers) && Modifier.isStatic(methodModifiers) && method.getName().startsWith("coreToXML")) {
				final Class<?>[] parameterTypes = method.getParameterTypes();
				if (parameterTypes.length == 1 && parameterTypes[0].isEnum()) {
					final Class<?> enumType = parameterTypes[0];
					LOGGER.info(String.format("Test de la méthode %s(%s)", method.getName(), enumType));
					++ nbTestedMethod;

					// on va tester toutes les valeurs de l'enum en entrée et vérifier que cela n'explose pas
					// (peu importe ici que la réponse de la méthode soit parfois nulle...)
					for (Object modalite : enumType.getEnumConstants()) {
						try {
							method.invoke(null, modalite);
							LOGGER.info(String.format("Test de l'appel à %s(%s.%s) OK", method.getName(), enumType.getName(), modalite));
						}
						catch (Exception e) {
							LOGGER.error(String.format("Test de l'appel à %s(%s) KO", method.getName(), modalite), e);
							Assert.fail(String.format("Méthode %s(%s.%s) a explosé avec une exception %s (%s)",
							                          method.getName(), enumType.getName(), modalite, e.getClass().getName(), e.getMessage()));
						}
					}
				}
			}
		}

		// juste pour vérifier qu'on ne s'est pas complètement lourdé et que l'on teste bien quelque chose...
		Assert.assertTrue(Integer.toString(nbTestedMethod), nbTestedMethod > 1);
	}

}
