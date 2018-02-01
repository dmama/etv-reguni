package ch.vd.unireg.taglibs.formInput;

import javax.servlet.jsp.JspException;

import org.springframework.web.servlet.tags.form.TagWriter;

interface Editor {
	void generate(TagWriter tagWriter, String value) throws JspException;
}
