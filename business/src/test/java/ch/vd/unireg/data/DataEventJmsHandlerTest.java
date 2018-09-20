package ch.vd.unireg.data;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.evenement.fiscal.MockEvenementFiscalSender;
import ch.vd.unireg.xml.event.data.v1.DataEvent;

public class DataEventJmsHandlerTest extends BusinessTest {

	private DataEventJmsHandler handler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		handler = new DataEventJmsHandler();
		handler.setDataEventService(new MockDataEventService());
		handler.setEvenementFiscalSender(new MockEvenementFiscalSender());
		handler.setHibernateTemplate(hibernateTemplate);
		handler.afterPropertiesSet();
	}

	private static <T> Set<Class<? extends T>> getAllSubclasses(Class<T> superclass) throws ClassNotFoundException {
		final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AssignableTypeFilter(superclass));

		final Set<BeanDefinition> components = provider.findCandidateComponents("ch/vd");
		final Set<Class<? extends T>> subclasses = new HashSet<>();
		for (BeanDefinition component : components) {
			//noinspection unchecked
			final Class<? extends T> cls = (Class<? extends T>) Class.forName(component.getBeanClassName());
			subclasses.add(cls);
		}
		return subclasses;
	}

	@Test
	public void testCompletudeMapHandlers() throws Exception {
		final Map<Class<? extends DataEvent>, DataEventJmsHandler.Handler<? extends DataEvent>> handlers = handler.getHandlers();
		Assert.assertNotNull(handlers);

		// vérifions que tous les handlers sont non-nuls
		for (Map.Entry<Class<? extends DataEvent>, DataEventJmsHandler.Handler<? extends DataEvent>> entry : handlers.entrySet()) {
			Assert.assertNotNull("Le handler pour la classe " + entry.getKey().getName() + " est null...", entry.getValue());
		}

		// récupération de toutes les classes héritées de DataEvent pour vérifier qu'elles sont toutes prises en compte
		final Set<Class<? extends DataEvent>> classes = getAllSubclasses(DataEvent.class);
		Assert.assertNotNull(classes);
		int nbSubclassesFound = 0;
		for (Class<? extends DataEvent> clazz : classes) {
			// pas besoin de regarder les classes abstraites...
			if (!Modifier.isAbstract(clazz.getModifiers())) {
				Assert.assertTrue("La classe " + clazz.getName() + " n'est pas prise en compte dans la map des handlers...", handlers.containsKey(clazz));
				++ nbSubclassesFound;
			}
		}
		Assert.assertEquals("Il semblerait que le classpath des tests ne soit pas complet (ou que certaines clés de la map des handlers soient out-of-bounds...)", handlers.size(), nbSubclassesFound);
	}
}
