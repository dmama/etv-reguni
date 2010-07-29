

var Tabulation = {};

Tabulation.classErrorTab = 'tab-with-errors';
Tabulation.tabInError = new Array();

Tabulation.attachObserver = function(name, observer) {
	if (!this.observers)
		this.observers = [];
	this.observers.push( [name,observer]);
}


Tabulation.onChange = function(element) {
	if ( this.observers) {
		for(var i = 0; i < this.observers.length; i++) {
			if ( this.observers[i] && this.observers[i][0].toLowerCase() === 'change')
				 this.observers[i][1](element);
		}
	}
	if ( element) {
		this.storeCurrentTabulation( element);
	}
}

Tabulation.show = function(element) {
	this._show( element, "current");
}

Tabulation.setTabInError = function(tabName) {
	this.tabInError.push(tabName);
	var element = E$(tabName);
	var line = this.getLine(element);
	Element.addClassName( line, this.classErrorTab);
}

Tabulation._show = function(element, css) {
	element = E$(element);
	if ( element) {
		this.reset(element);
		var line = this.getLine(element);
		Element.addClassName( line, css);
		var content= E$( "tabContent_" + line.id);
		content.style.display = '';
		this.onChange( line.id);
	} else {
		this.onChange( null);
	}
}

Tabulation.showAll = function(parent) {
	parent = E$(parent);
	if ( parent) {
		var contents = this.getContents(parent);
	 	for(var i = 0; i < contents.length; i++) {
	 		var item = E$(contents[i]);
	 		item.style.display="";
	 	}
	}
}

Tabulation.showFirst = function(parent) {	
	parent = E$(parent);
	if ( parent) {
		var tabs = this.getTabulations(parent);
		if (  tabs.length > 0) {
			this.show( tabs[0]);
		}
	}
}

Tabulation.reset = function(element) {
 	element = E$(element);
 	var tabs = this.getTabulations(element);
 	for(var i = 0; i < tabs.length; i++) {
 		var item = E$(tabs[i]); 		
 		//Element.removeClassName(item, this.classErrorTab);
 		Element.removeClassName(item, "current");
 	}
 	
 	var contents = Tabulation.getContents(element);
 	for(var i = 0; i < contents.length; i++) {
 		var item = E$(contents[i]);
 		item.style.display="none";
 	}
}


Tabulation.getLine = function(element) {
 	element = E$(element);
 	var line = element;
 	while ( line !== null || line.tagName === "HTML") {
 		if ( line.tagName === "LI")
 			return line;
 		line = line.parentNode;
 	}
 	return null;
}

Tabulation.getLu = function(element) {
 	element = E$(element);
 	var ul = element;
 	while ( ul !== null || ul.tagName === "HTML") {
 		if ( ul.tagName === "UL")
 			return ul;
 		ul = ul.parentNode;
 	}
 	return null;
}

Tabulation.getTabulations = function(element) {
 	element = E$(element);
 	var lu = this.getLu(element);
 	var lis = lu.getElementsByTagName("LI");
 	var ar = new Array();
 	for(var i = 0; i < lis.length; i++) {
 		ar.push(lis[i].id);
 	}
 	return ar;
}

Tabulation.getContents = function(element) {
 	var tmp = this.getTabulations( element);
 	var ar = new Array();
 	for(var i = 0; i < tmp.length; i++) {
 		ar.push("tabContent_" +tmp[i]);
 	}
 	return ar;
}

Tabulation.setCurrentTabulation = function(name) {
	if ( typeof name == "string")
		name = name;
	else {
		name = name.name;
	}
	this.show(name);
}

Tabulation.storeCurrentTabulation = function(name) {
	var element = E$(name);
	var lu = this.getLu( element);
	XT.doAjaxAction("storeCurrentTabulation", element, 
		{
			currentTabulation : name,
			tabulationId: lu.id
		}, 
		{
			 clearQueryString: true
    	});
}

Tabulation.restoreCurrentTabulation = function(parent) {	
	var element = E$(parent);
	XT.doAjaxAction("getCurrentTabulation", element, 
		{
			tabulationId: element.id
		},
		{
			 clearQueryString: true
    	});
}
