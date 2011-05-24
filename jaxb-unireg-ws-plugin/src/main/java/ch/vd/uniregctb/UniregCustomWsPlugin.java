package ch.vd.uniregctb;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.logging.Logger;

import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMethod;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;

public class UniregCustomWsPlugin extends Plugin {

	private static final Logger LOGGER = Logger.getLogger(UniregCustomWsPlugin.class.getName());

	private static final String EQUALS_DATE = "\n    @Override\n" +
			"    public boolean equals(Object obj) {\n" +
			"        if (this == obj)\n" +
			"            return true;\n" +
			"        if (obj == null)\n" +
			"        return false;\n" +
			"    if (getClass() != obj.getClass())\n" +
			"        return false;\n" +
			"    Date other = (Date) obj;\n" +
			"    return day == other.day && month == other.month && year == other.year;\n" +
			"}";

	@Override
	public String getOptionName() {
		return "Xunireg-ws";
	}

	@Override
	public String getUsage() {
		return "  -Xunireg-ws        :  special customizations for Unireg";
	}

	@Override
	public boolean run(final Outline outline, final Options options, final ErrorHandler errorHandler) {
		// For each defined class

		for (final ClassOutline classOutline : outline.getClasses()) {
			if (classOutline.implClass.name().equals("Date")) {
				classOutline.implClass.direct(EQUALS_DATE);
			}
		}
		return true;
	}
}
