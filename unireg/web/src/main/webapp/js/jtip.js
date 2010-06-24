

var toolTip = new Tooltip();

if( window.attachEvent) {
	window.attachEvent("onload", function() {toolTip.init()});
} else if (window.addEventListener) {
	window.addEventListener( "load" , function() {toolTip.init()}, true );
}

Tooltip.prototype.init  = function(){
		var self = this;		
		var anchors = document.getElementsByTagName("A");
		for( var i=0; i < anchors.length; i++) {			
			var anchor = anchors[i];			
			if( anchor.className === "jTip"||anchor.className === "civTip" || anchor.className === "staticTip" || anchor.className === "adrTip") {		
				anchor.onmouseover =  function(){
					self.show(this);
				};
				anchor.onmouseout =  function(){
					//window.status = "";
					self.hide(this);
				};
				anchor.onclick = function(){return false};
				anchor.onmousemove =  function(ev){
					if ( !ev)
     			 		var ev = window.event;
     			 	var element =  (ev.target || ev.srcElement);
     			 	//window.status = element.id;
     			 	if ( element == this) 
						return self.mouseMove(ev);
				   else 
				   	   return self.hide(this);
				};
			}
		}		 
}

// create the tooltip object
function Tooltip(){
    // setup properties of tooltip object
    this.id= "tooltip-main";
    this.offsetx = 10;
    this.offsety = 20;    
    this.ieFix  = null;
}

/**
* Open ToolTip. The title attribute of the htmlelement is the text of the tooltip
* Call this method on the mouseover event on your htmlelement
*/
Tooltip.prototype.show = function (htmlelement) {
    var item = E$(this.id);
    
    if ( item == null) {
      item = document.createElement("DIV");
      item.id = this.id;
      Element.hide(item);
      item.style.position = "absolute";
      item = document.body.appendChild( item);
      if (!this.ieFix) {
	      var ua = navigator.userAgent.toLowerCase();
	      var isIE        = ( (ua.indexOf('msie') != -1) && (ua.indexOf('opera') == -1) && (ua.indexOf('webtv') == -1) );
	      var versionMajor = parseInt(navigator.appVersion);
	      if ( isIE && versionMajor < 7) {      
		      var iframe = document.createElement("iframe");
		      iframe.id =  this.id + "-frame";        	     
		      Element.getStyle(iframe).position = "absolute";
		  	  iframe.frameBorder = "no";
		  	  iframe.border = "0";
		  	  iframe.scrolling = "no";
		  	  iframe.zIndex = 1;
		  	  Element.hide(iframe);
		  	  document.body.appendChild( iframe);
		  	  this.ieFix = iframe;
	  	  }
  	  }
    } 
    if ( item == null)
       return;

    if (!this.hasFocus) {
    	if (htmlelement.className === "jTip") {
    		XT.doAjaxAction('includeTooltip', htmlelement, {'elementId' : item.id, 'link' : htmlelement.href }, { 'clearQueryString': true});
    	}
    	else if (htmlelement.className === "civTip") {
    		XT.doAjaxAction('showCivilData', htmlelement, {'elementId' : item.id, 'tiersId' : htmlelement.name }, { 'clearQueryString': true});
    	}
    	else if (htmlelement.className === "staticTip") {
    		var c = E$(htmlelement.id + "-tooltip");
    		item.innerHTML = c.innerHTML;
    	}
    	else if (htmlelement.className === "adrTip") {
    		XT.doAjaxAction('showAdresseData', htmlelement, {'elementId' : item.id, 'tiersId' : htmlelement.name }, { 'clearQueryString': true});
    	}
    	
	}
	this.hasFocus = true;
    return false;
}

/**
* hide tooltip
*/
Tooltip.prototype.hide = function (htmlelement) {	
    var item = E$(this.id);
    //window.status = "hide" + item.id;
    if ( item) {
    	Element.hide(item);
	}	   
	if (this.ieFix) {
		 Element.hide(this.ieFix);
	}
	this.hasFocus = false;
}



// Moves the tooltip element
Tooltip.prototype.mouseMove = function (ev) {

	var item = E$(this.id);
    if ( item == null){
        return true;
    }
    if ( !ev)
      var ev = window.event;
    Element.show( item);	    
    if ( this.ieFix) {
		 Element.show( this.ieFix);
	}
    var position = Element.getElementPosition(document.body);

	// Détermine les coordonnées du pointeur de la souris
    var x;
    var y;
    { // from: http://www.quirksmode.org/js/events_properties.html
		var posx = 0;
		var posy = 0;
		if (ev.pageX || ev.pageY) 	{
			posx = ev.pageX;
			posy = ev.pageY;
		}
		else if (ev.clientX || ev.clientY) 	{
			posx = ev.clientX + document.body.scrollLeft
				+ document.documentElement.scrollLeft;
			posy = ev.clientY + document.body.scrollTop
				+ document.documentElement.scrollTop;
		}
		// posx and posy contain the mouse position relative to the document
		// Do something with this information
		x = posx;
		y = posy;
	}

	// Optim : on s'assure que le tooltip reste dans la zone visible de la page
    var result = Element.getElementPosition(item);
    var bound = Element.getViewportSize();
    //window.status = "width: " + bound.width + " height: " + bound.height;
    var xMax = bound.width + document.body.scrollLeft + document.documentElement.scrollLeft - 20;
    var yMax = bound.height + document.body.scrollTop + document.documentElement.scrollTop - 20;
    if (x + result.width > xMax) {
        x = xMax - result.width;
    }
    if (y + result.height > yMax) {
        y = yMax - result.height;
    }

	// On déplace le tooltip
    //window.status = "x: " + x + " y: " + y;
    Element.move(item, x + this.offsetx , y + this.offsety);
    if ( this.ieFix) {
    	var size = Element.getElementPosition(item);
    	Element.move(this.ieFix, size.x , size.y);
		 this.ieFix.width = size.width;
		 this.ieFix.height = size.height;
	}
}

