package ch.vd.unireg.xml.tools;

import java.io.IOException;

import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;

/**
 * Ce plugin permet de spécifier un catalog resolver custom sur xjc.
 */
@SuppressWarnings({"UnusedDeclaration"})
public class CatalogResolverPlugin extends Plugin {

	@Override
	public String getOptionName() {
		return "Xcatalog-resolver";
	}

	@Override
	public String getUsage() {
		return "  -Xcatalog-resolver:<catalog resolver classname> : specify custom catalog resolver";
	}

	private Class<? extends EntityResolver> resolverClass;

	@Override
	public int parseArgument(Options opt, String[] args, int i) throws BadCommandLineException, IOException {
		if (args[i].startsWith("-Xcatalog-resolver")) {
			final String classname = args[i].replace("-Xcatalog-resolver:", "");
			try {
				final Class<?> clazz = Class.forName(classname);
				if (!EntityResolver.class.isAssignableFrom(clazz)) {
					throw new IllegalArgumentException("Class [" + classname + "] doesn't inherit from [" + EntityResolver.class.getName() + "]");
				}
				//noinspection unchecked
				resolverClass = (Class<? extends EntityResolver>) clazz;
				System.out.println("[INFO] CatalogResolverPlugin : using resolver class " + classname);
			}
			catch (RuntimeException e) {
				System.err.println(e.getMessage());
				throw e;
			}
			catch (ClassNotFoundException e) {
				System.err.println("[ERROR] CatalogResolverPlugin : cannot load class " + classname);
				throw new IllegalArgumentException("Class [" + classname + "] not found");
			}
			return 1;
		}
		return 0;
	}

	@Override
	public boolean run(final Outline outline, final Options options, final ErrorHandler errorHandler) {
		if (resolverClass != null) {
			try {
				options.entityResolver = resolverClass.newInstance();
			}
			catch (InstantiationException e) {
				System.err.println(e.getMessage());
				throw new RuntimeException(e);
			}
			catch (IllegalAccessException e) {
				System.err.println(e.getMessage());
				throw new RuntimeException(e);
			}
		}
		return true;
	}
}
