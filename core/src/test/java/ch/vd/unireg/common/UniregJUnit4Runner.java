package ch.vd.unireg.common;

import java.io.FileNotFoundException;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.springframework.util.Log4jConfigurer;

/**
 * Runner JUnit spécialisé pour Unireg qui initialise correctement Log4j.
 */
public class UniregJUnit4Runner extends BlockJUnit4ClassRunner {

	static {
		try {
			Log4jConfigurer.initLogging("classpath:ut/log4j.xml");
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public UniregJUnit4Runner(Class<?> clazz) throws InitializationError {
		super(clazz);
	}
}