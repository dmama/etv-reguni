package ch.vd.uniregctb.decorator;

import org.displaytag.decorator.TableDecorator;

import ch.vd.uniregctb.common.Annulable;

public class TableEntityDecorator extends TableDecorator{
	public String addRowClass(){
		String cssStyle=null;
		Annulable annulable = (Annulable)getCurrentRowObject();
		if(annulable !=null){
			if(annulable.isAnnule()){
				cssStyle = "strike";
			}
		}
		return cssStyle;
	}
}
