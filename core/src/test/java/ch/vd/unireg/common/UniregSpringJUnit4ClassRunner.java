package ch.vd.uniregctb.common;

import java.io.FileNotFoundException;

import org.junit.runners.model.InitializationError;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Log4jConfigurer;

/**
 * Runner Spring/JUnit spécialisé pour Unireg qui initialise correctement Log4j.
 * <p>
 * TODO (msi) cette solution ne marche pas à 100% parce que la classe SpringJUnit4ClassRunner utilise elle-même un logger.
 * Or, selon la mécanique du classloader, la classe SpringJUnit4ClassRunner est donc initialisée avant UniregSpringJUnit4ClassRunner
 * et la création du logger dans SpringJUnit4ClassRunner émet un warning.
 * Pour éviter ce warnin, la solution serait :
 *  - de ne pas utiliser la classe SpringJUnit4ClassRunner et d'utiliser UniregJUnit4Runner à la place
 *  - d'utiliser les règles SpringClassRule et SpringMethodRule pour initialiser correctement le context Spring sur les tests.
 * Malheureusement, ces deux règles ne sont disponibles qu'avec Spring 4, il faudra donc attendre que l'on fasse la transition.
 */
public class UniregSpringJUnit4ClassRunner extends SpringJUnit4ClassRunner {

	static {
		try {
			Log4jConfigurer.initLogging("classpath:ut/log4j.xml");
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public UniregSpringJUnit4ClassRunner(Class<?> clazz) throws InitializationError {
		super(clazz);
	}
}