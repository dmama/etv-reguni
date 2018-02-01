package ch.vd.unireg.taglibs;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.JavaScriptUtils;

public enum EscapeMode {
	NONE {
		@Override
		public String escape(String s) {
			return s;
		}
	},
	HTML {
		@Override
		public String escape(String s) {
			return HtmlUtils.htmlEscape(s);
		}
	},
	JAVASCRIPT {
		@Override
		public String escape(String s) {
			return JavaScriptUtils.javaScriptEscape(s);
		}
	};

	public abstract String escape(String s);

	@Nullable
	public static EscapeMode fromString(String s) {
		if (StringUtils.isNotBlank(s)) {
			for (EscapeMode mode : EscapeMode.values()) {
				if (mode.name().equalsIgnoreCase(s)) {
					return mode;
				}
			}
		}
		return null;
	}
}
