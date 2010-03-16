


var __contextPath ="";
var scripts = document.getElementsByTagName("SCRIPT");
for( var i = 0; i < scripts.length; i++) {
  var src = scripts[i].src;
  var index = src.indexOf( "custom.js");
  if ( index >= 0) {
    __contextPath = src.substring(0, index-4);
  }
}

/* Obtient le path du contexte actuelle de l'application
 *
*/
function getContextPath() {
  return __contextPath;
}

/*
 * initialise l'API springxt par les valeurs par défauts.
*/
if (typeof(XT) !="undefined") { 
	XT.defaultLoadingElementId = 'loadingImage';
	XT.defaultLoadingImage =getContextPath() + '/images/loading.gif';
	XT.defaultErrorHandler = function(ajaxRequest, exception) {
	    alert(exception.message);
	};
}

var Class = {
  create: function() {
    return function() {
      this.initialize.apply(this, arguments);
    }
  }
}


Object.extend = function(destination, source) {
  for (var property in source) {
    destination[property] = source[property];
  }
  return destination;
}

Object.extend(Object, {
  inspect: function(object) {
    try {
      if (object === undefined) return 'undefined';
      if (object === null) return 'null';
      return object.inspect ? object.inspect() : object.toString();
    } catch (e) {
      if (e instanceof RangeError) return '...';
      throw e;
    }
  },

  keys: function(object) {
    var keys = [];
    for (var property in object)
      keys.push(property);
    return keys;
  },

  values: function(object) {
    var values = [];
    for (var property in object)
      values.push(object[property]);
    return values;
  },

  clone: function(object) {
    return Object.extend({}, object);
  }
});


/* La classe Element contient les principales fonctions permettant d'utiliser un element DOM sans
 * se soucier du navigateur utilisé.
 */
 if (!window.Element)
  var Element = new Object();



Element.extend = function(element) {
  if (!element || _nativeExtensions || element.nodeType == 3) return element;

  if (!element._extended && element.tagName && element != window) {
    var methods = Object.clone(Element.Methods), cache = Element.extend.cache;

    if (element.tagName == 'FORM')
      Object.extend(methods, Form);
      
    for (var property in methods) {
      var value = methods[property];
      if (typeof value == 'function' && !(property in element))
        element[property] = cache.findOrStore(value);
    }
  }

  element._extended = true;
  return element;
};

Element.extend.cache = {
	  findOrStore: function(value) {
	    return this[value] = this[value] || function() {
	      return value.apply(null, [this].concat(A$(arguments)));
	    }
	  }
};

var _nativeExtensions = false;

Element.addMethods = function(methods) {
  Object.extend(Element.Methods, methods || {});

  function copy(methods, destination, onlyIfAbsent) {
    onlyIfAbsent = onlyIfAbsent || false;
    var cache = Element.extend.cache;
    for (var property in methods) {
      var value = methods[property];
      if (!onlyIfAbsent || !(property in destination))
        destination[property] = cache.findOrStore(value);
    }
  }

  if (typeof HTMLElement != 'undefined') {
    copy(Element, HTMLElement.prototype);
    copy(Form, HTMLFormElement.prototype);
    _nativeExtensions = true;
  }
}





Element.Methods = {

/*
 * Cette fonction permet de déplacer un élément dans la fenêtre. 
*/
	move : function(element, newXCoordinate, newYCoordinate) {
		element = E$(element);
	    // get a reference to the cross-browser style object and make sure the object exists
	    var styleObject = element.getStyle();
	    if(styleObject) {
	      styleObject.left = newXCoordinate + "px";
	      styleObject.top = newYCoordinate + "px";
	      return true;
	    } else {
	      // we couldn't find the object, so we can't very well move it
	      return false;
	    }
	},


	/*
	 * Obtient la position relative et la dimension de 'element'.
	*/
	getRectBounds : function(element) {
		element = E$(element);
	    var result = new Object();
	    result.x = 0;
	    result.y = 0;
	    result.width = 0;
	    result.height = 0;
	
	     result.x = element.offsetLeft;
	     result.y = element.offsetTop;
	    if (element.offsetWidth && element.offsetHeight) {
	        result.width = element.offsetWidth;
	        result.height = element.offsetHeight;
	     } else if (element.style && element.style.pixelWidth && element.style.pixelHeight) {
	        result.width = element.style.pixelWidth;
	        result.height = element.style.pixelHeight;
	    }
	    return result;
	},

	/*
	 * Obtient la position absolut de 'element' et sa dimension.
	*/
	getElementPosition : function(element) {
		element = E$(element);
	    var result = new Object();
	    result.x = 0;
	    result.y = 0;
	    result.width = 0;
	    result.height = 0;
	    if (element.offsetParent) {
	        result.x = element.offsetLeft;
	        result.y = element.offsetTop;
	        var parent = element.offsetParent;
	        while (parent) {
	            result.x += parent.offsetLeft;
	            result.y += parent.offsetTop;
	            var parentTagName = parent.tagName.toLowerCase();
	            if (parentTagName != "table" &&
	                parentTagName != "body" &&
	                parentTagName != "html" &&
	                parentTagName != "div" &&
	                parent.clientTop &&
	                parent.clientLeft) {
	                result.x += parent.clientLeft;
	                result.y += parent.clientTop;
	            }
	            parent = parent.offsetParent;
	        }
	    }
	    else
	    if (element.left && element.top) {
	        result.x = element.left;
	        result.y = element.top;
	    }
	    else {
	        if (element.x) {
	            result.x = element.x;
	        }
	        if (element.y) {
	            result.y = element.y;
	        }
	    }
	    if (element.offsetWidth && element.offsetHeight) {
	        result.width = element.offsetWidth;
	        result.height = element.offsetHeight;
	    }
	    else if (element.style && element.style.pixelWidth && element.style.pixelHeight) {
	        result.width = element.style.pixelWidth;
	        result.height = element.style.pixelHeight;
	    }
	    return result;
	},

	/*
	 * Obtient le style de 'element'.
	*/
	getStyle : function(element) {
		element = E$(element);
	    // cross-browser function to get an object's style object given its id
	    if(document.getElementById) {
	      // W3C DOM
	      return element.style;
	    } else if (document.all) {
	      // MSIE 4 DOM
	      return element.style;
	    } else if (document.layers) {
	      // NN 4 DOM.. note: this won't find nested layers
	      return element;
	    } else {
	      return false;
	    }
	},

	/*
	 * Permet de permuter la visibilité de 'element'.
	*/
	toggle : function(element) {
	    element = E$(element);
	    Element[Element.visible(element) ? 'hide' : 'show'](element);
	    return element;
	},
 
	 /*
	 * Permet de cacher  'element'.
	*/ 
	hide : function(element) {
	    E$(element).style.display = 'none';
	    return element;
	},

	 /*
	 * Permet de savoir si 'element' est visible ou pas.
	*/ 
	visible : function(element) {
	    return E$(element).style.display != 'none';
	},

	 /*
	 * Permet de ne pas rendre visible  'element'.
	*/ 
	 show : function(element) {
	    E$(element).style.display = '';
	    return element;
	 },

	/*
	* Obtient l'indication si 'element' contient la css class 'className'.
	*/
	hasClassName: function(element, className) {
	    if (!(element = E$(element))) return;
	    var elementClassName = element.className;
	    if (elementClassName.length == 0) return false;
	    if (elementClassName == className ||
	        elementClassName.match(new RegExp("(^|\\s)" + className + "(\\s|$)")))
	      return true;
	    return false;
	  },
  
	 /*
	 * Ajoute un css class  à  'element'.
	*/ 
	addClassName : function( element, className, begin) {
		if (!(element = E$(element))) return;
	    var current = element.className;
	    if (current) {
	        if (current.charAt(current.length - 1) != ' ') {
	            current += ' ';
	        }
	        current += className;
	    } else {
	        current = className;
	    }
	    element.className = current;    
	    return element;
	},

	 /*
	 * Efface un css class  à  'element'.
	*/ 
	 removeClassName : function(element, className) {
		if (!(element = E$(element))) return;
	    var current = element.className;
	    if (current) {
	        if (current.substring(current.length - className.length - 1, current.length) == ' ' + className) {
	            element.className = current.substring(0, current.length - className.length - 1);
	            return;
	        }
	        if (current == className) {
	            element.className = "";
	            return;
	        }
	        var index = current.indexOf(' ' + className + ' ');
	        if (index != -1) {
	            element.className = current.substring(0, index) + ' ' + current.substring(index + className.length + 2, current.length);
	            return;
	        }
	        if (current.substring(0, className.length) == className + ' ') {
	            element.className = current.substring(className.length + 1, current.length);
	        }
	    }
	    return element;
	},
	
	/*
	* Permet d'ajouter un css class, s'il elle n'est contenu dans 'element', sinon de l'enlever.
	*/
	toggleClassName : function( element, className) {
		if (!(element = E$(element))) return;
		if( element.hasClassName(className)) 
			element.removeClassName(className);
		else
			element.addClassName(className);
	    return element;
	},

	 /*
	 * Obtient la dimension de la view de la fenêtre.
	*/ 
	getViewportSize : function() {
	  var size = new Object();
	  size.width = 0;
	  size.height = 0;
	  if (typeof window.innerWidth != 'undefined') {
	   size.width = window.innerWidth;
	   size.height = window.innerHeight;
	  } else if (typeof document.documentElement != 'undefined' &&
	           typeof document.documentElement.clientWidth != 'undefined' &&
	           document.documentElement.clientWidth != 0) {
	    size.width = document.documentElement.clientWidth;
	    size.height =document.documentElement.clientHeight;
	  } else {
	     var body = this.getElementByTagName(document, 'body');
	     if ( body) {
	       size.width = body.clientWidth;
	       size.height = body.clientHeight;
	     }
	  }
	
	  return size;
	},
	
	/**
	 * Obtient le premier élément enfant de 'element' avec le nom 'tagName'.
	*/
	getElementByTagName : function(element, tagName) {
		element = E$(element);
	    var elements = element.getElementsByTagName(tagName);
	    if (elements && elements.length > 0) {
	        return elements[0];
	    }
	    else return null;
	},
	

	/*
	* Enregistre un EventHandler sur 'element'.
	*/
	addObserver : function(element, name, observer,useCapture) {  	
		element =E$(element);
		if ( element.addEventListener ) {
		  if ( name === "propertychange") name = 'DOMAttrModified';
		  else if (element == document && name === 'load' ) name = 'DOMContentLoaded';
	      element.addEventListener(name, observer, useCapture);
	    } else if (element.attachEvent)  {
	    	element.attachEvent('on' + name,observer);
	    }
	},

	/*
	* Enlève le EventHandler de 'element'.
	*/
	removeObserver : function(element, name, observer,useCapture) {
		element = E$(element);
		if (element.detachEvent) {
		  element.detachEvent('on' + name, observer);
		} else {
		  element.removeEventListener(name, observer, useCapture);
		}
	},

	/*
	* Déclenche l'évnement de type 'name' sur 'element'.
	*/
	fireObserver : function(element, name) {
		element = E$(element);
		if( document.createEvent ) {
		  var evObj = document.createEvent('HTMLEvents');
		  evObj.initEvent( name, true, false );
		  element.dispatchEvent(evObj);
		} else if( document.createEventObject ) {
		  element.fireEvent('on'+name);
		}
	},

	/*
	* Définit le texte dans 'element'.
	*/
	setText : function(element, text) {
		element = E$(element);
		if(document.all){
			element.innerText = text;
		} else{
			element.textContent = text;
		}
	 },
	 
	 /*
	 * Obtient le texte dans 'element'.
	 */
	 getText : function(element) {
		element = E$(element);
		if(document.all){
			return element.innerText;
		} else{
			return element.textContent;
		}
	 }
}
 
Object.extend(Element, Element.Methods);


 /*
 * Obtient l'élement correspondant à identifiant 'element'.
*/ 
function E$( element) {
  if (typeof element == 'string')
    element = document.getElementById(element);
  return Element.extend(element);
}

 /*
 * Obtient l'élément <FORM>  avec le nom correspondant.
*/ 
function F$( form) {
  if (typeof form == 'string')
    form = document.forms[form];
  if (form) form = E$(form);
  return form;
}

function A$(args) {
	var results = [];
    for (var i = 0, length = args.length; i < length; i++)
      results.push(args[i]);
    return results;
}

/*
 * Cette classe permet 
*/
var Form = {
	doPostBack : function(theForm, eventTarget, eventArgument) {
	    if (!theForm.onsubmit || (theForm.onsubmit() != false)) {	    	
	        if ( theForm.__TARGET__) theForm.__TARGET__.value = eventTarget;
	        if ( theForm.__EVENT_ARGUMENT__) theForm.__EVENT_ARGUMENT__.value = eventArgument;
	        theForm.submit();
	    }
	},
	doAjaxActionPostBack : function(theForm, event, eventTarget,eventArgument) {
		eventTarget = E$(eventTarget);
		theForm = E$(theForm);
		var eventId = eventTarget.name;
		if( !eventId) {
			eventId = eventTarget.id;
		}
		eventArgument = (  !eventArgument ? {}: eventArgument);
		XT.doAjaxAction( eventId+event ,eventTarget, eventArgument,
		{
			clearQueryString: true,
			formName:theForm.name
		});
	},
	doAjaxSubmitPostBack : function(theForm, event, eventTarget, eventArgument) {
		eventTarget = E$(eventTarget);
		theForm = E$(theForm);
		var eventId = eventTarget.name;
		if( !eventId) {
			eventId = eventTarget.id;
		}
		eventArgument = (  !eventArgument ? {}: eventArgument);
		XT.doAjaxSubmit( eventId+event,eventTarget, eventArgument,
		{
			clearQueryString: true,
			formName:theForm.name
		});
	}
};

 
if (!window.Event) {
  var Event = new Object();
}

Object.extend(Event, {
 
	  KEY_BACKSPACE: 8,
	  KEY_TAB:       9,
	  KEY_RETURN:   13,
	  KEY_ESC:      27,
	  KEY_LEFT:     37,
	  KEY_UP:       38,
	  KEY_RIGHT:    39,
	  KEY_DOWN:     40,
	  KEY_DELETE:   46,
	  KEY_HOME:     36,
	  KEY_END:      35,
	  KEY_PAGEUP:   33,
	  KEY_PAGEDOWN: 34,
	 
	 element: function(event) {
	    return event.target || event.srcElement;
	  },
	 stop : function(event) {
	    if (event.preventDefault) {
	      event.preventDefault();
	      event.stopPropagation();
	    } else {
	      event.returnValue = false;
	      event.cancelBubble = true;
	    }
	    return false;
	  }
  
});

var PeriodicalExecuter = Class.create();


PeriodicalExecuter.prototype = {
	initialize: function(callback, frequency) {
		this.callback = callback;
	    this.frequency = frequency;
	    this.currentlyExecuting = false;
	
	    this.registerCallback();
	},

  registerCallback: function() {
  	var self = this;
    this.timer = setInterval(function() {
    	self.onTimerEvent();
    }, this.frequency * 1000);
  },

  stop: function() {
    if (!this.timer) return;
    clearInterval(this.timer);
    this.timer = null;
  },

  onTimerEvent: function() {
    if (!this.currentlyExecuting) {
      try {
        this.currentlyExecuting = true;
        this.callback(this);
      } finally {
        this.currentlyExecuting = false;
      }
    }
  }
}










 
var Modifier = {

	formName : null,
	isModifiedSending : false,
	isModified : false,
	submitSaveName : "save",
	messageSaveSubmitConfirmation : "Voulez-vous vraiment sauver ?",
	messageResetSubmitConfirmation : "Voulez-vous vraiment annuler vos modification ?",
	messageOverConfirmation : "Voulez-vous vraiment quitter cette page sans sauver ?",
	inputTarget : null,
	isElementOver : function(element) {
		if ( element && element.tagName !== "A")
			return false;
		var link = element;
	    var href = link.href;
		return ( href != null && href !== "" && href.indexOf("#") <0  &&
	    		(link.target =='' || link.target =="_self") && link.onclick == null
	    		&& link.className.indexOf("thickbox") < 0)
	},
 
	setIsModified : function( modified) {
		this.isModified = modified;
		this.onChange();
	},


	onChange : function() {
		this.inputTarget.value = this.isModified;
		if( this.isModified) {	
			var form = F$(this.formName);
			var saveSubmit = form.elements[this.submitSaveName];
			if ( saveSubmit) saveSubmit.disabled = false;
		}	
	},
 
	 attachObserver : function( theForm, modified) {
	 	theForm = F$(theForm);
	 	this.isModifiedSending = modified;
	 	this.formName = theForm.name;
	 	var count = theForm.elements.length;
	    var element;
	    var self = this;
	    for (var i = 0; i < count; i++) {	
	    	var element =  theForm.elements[i];        
	        var tagName = element.tagName.toLowerCase();
	        if (tagName == "input") {
	            var type = element.type;
	            if ((type == "text" || type == "hidden" || type == "password" ||
	                ((type == "checkbox" || type == "radio")))) {
	                Element.addObserver( element, "change" , function() {
	                	self.setIsModified( true);
	                	return true;
	                } , false);        
	                if ( type == "text") {
	                	  Element.addObserver( element, "keyup" , function(event) {
	                	  		event = event || window.event;
			                	return self.onkeyup( event);
			                } , false); 
	                }        
	            } else if ( type== "submit") {
	            	if ( element.name === this.submitSaveName) {
	            		element.disabled = true;
		            	 Element.addObserver( element, "click" , function(ev) {
		            	 	ev = ev || window.event;            	 
		            	 	if (!self.submitSaveConfirmation(this))
		            	 		return Event.stop(ev);	
		                } , false);
	                }
	            } else if ( type == "reset") {
	            	 Element.addObserver( element, "click" , function(ev) {            	
	            	 	ev = ev || window.event
	            	 	if (!self.submitResetConfirmation(this))
	            	 		 	return Event.stop(ev);	
	                } , false);
	            }           
	        }
	        else if (tagName == "select") {
	               Element.addObserver( element, "change" , function(ev) {
	               		self.setIsModified( true);
	                } , false);
	        }
	        else if (tagName == "textarea") {
	             Element.addObserver( element, "change" , function(ev) {
	             	self.setIsModified( true);
	                } , false);
	            Element.addObserver( element, "keyup" , function(event) {
	                	  		event = event || window.event;
			                	return self.onkeyup( event);
			                } , false); 
	        }
	    }
	
	    var links = document.getElementsByTagName("A");
	    var count = links.length;
	    for (var i = 0; i < count; i++) {
	    	var link = links[i];
	    	 var href = link.href;
	    	if ( Modifier.isElementOver(link)) {
	    		var func =link.onclick; 
	    		link.onclick =  function(ev) {
	             		ev = ev || window.event;            	 
		            	if (!self.overConfirmation(this))
		            		return Event.stop(ev);
		            	if ( func) func();
	                }
	    	}	
	    }
	
	    this.inputTarget = document.createElement("INPUT");
	    this.inputTarget.type = "hidden";
	    this.inputTarget.name = "__MODIFIER__";
	    theForm.appendChild( this.inputTarget);
	    this.setIsModified( this.isModifiedSending);
	 },
 
 
	submitSaveConfirmation : function(submit) {
	  	if ( this.isModified)
	  		return confirm(this.messageSaveSubmitConfirmation);
	  	return true; 	
	},
 
	overConfirmation : function(link) {
	  	if ( this.isModified)
	  		return confirm(this.messageOverConfirmation);
	  	return true; 	
	},
  
	submitResetConfirmation : function(reset) {
	  	if ( this.isModified) {
	  		if ( confirm(this.messageResetSubmitConfirmation)) {
	  			this.setIsModified( this.isModifiedSending);
	  			return true;
	  		}
	  		return false;
	  	}
	  	return true; 	
	},
  
	onkeyup : function(event) {  		
			var key = event.keyCode;
			//window.status = "keyCode: " + key;
			if ( key == Event.KEY_UP
					|| key == Event.KEY_LEFT
					|| key == Event.KEY_RIGHT
					|| key == Event.KEY_DOWN
					|| key == Event.KEY_RETURN
					|| key == Event.KEY_ESC) {
				// noop
			} else if ( key == Event.KEY_DELETE || key == Event.KEY_BACKSPACE || key > 31) {
				this.setIsModified( true);
			}
			return true
	  }
  };
  
 Element.addMethods();