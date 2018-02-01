package ch.vd.unireg.tiers;

import javax.servlet.http.HttpServletRequest;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

import ch.vd.unireg.common.CollectionsUtils;

public class HistoFlags {

	private final Set<HistoFlag> flags;

	public HistoFlags(HttpServletRequest request) {
		this(request::getParameter);
	}

	public HistoFlags(Function<String, String> parameterExtractor) {
		this.flags = EnumSet.noneOf(HistoFlag.class);
		for (HistoFlag flag : HistoFlag.values()) {
			final String paramName = flag.getParamName();
			final String paramValue = parameterExtractor.apply(paramName);
			if (paramValue != null && Boolean.parseBoolean(paramValue)) {
				this.flags.add(flag);
			}
		}
	}

	public HistoFlags(Set<HistoFlag> raised) {
		this.flags = EnumSet.noneOf(HistoFlag.class);
		this.flags.addAll(raised);
	}

	@Override
	public String toString() {
		return "HistoFlags{" +
				"flags=[" + CollectionsUtils.toString(flags, Enum::name, ", ") + ']' +
				'}';
	}

	public boolean hasHistoFlag(HistoFlag flag) {
		return flags.contains(flag);
	}
}
