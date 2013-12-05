package ch.vd.uniregctb.taglibs.formInput;

import javax.servlet.jsp.JspException;

import org.springframework.web.servlet.tags.form.TagWriter;

interface Editor {
	abstract void generate(TagWriter tagWriter, String value) throws JspException;
}
