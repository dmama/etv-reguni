package ch.vd.uniregctb.utils;

import java.util.Date;

import javax.servlet.jsp.PageContext;

import org.displaytag.decorator.DisplaytagColumnDecorator;
import org.displaytag.exception.DecoratorException;
import org.displaytag.properties.MediaTypeEnum;

import ch.vd.registre.base.date.DateHelper;

public class LongDateWrapper implements DisplaytagColumnDecorator {

	
	public Object decorate(Object columnValue, PageContext pageContext, MediaTypeEnum media) throws DecoratorException

	{
		Date sDate = (Date) columnValue;
		String sDateRtr = DateHelper.dateToDisplayString(sDate);
		
		return sDateRtr;
	}
}
