/**
 * initialise l'API springxt par les valeurs par défauts.
 */
if (typeof(XT) !="undefined") {
	XT.defaultLoadingElementId = 'loadingImage';
	XT.defaultLoadingImage = getContextPath() + '/images/loading.gif';
	XT.defaultErrorHandler = function(ajaxRequest, exception) {
	    alert(exception.message);
	};
}

Object.extend = function(destination, source) {
  for (var property in source) {
    destination[property] = source[property];
  }
  return destination;
}

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
	    		(link.target =='' || link.target =="_self") && link.onclick == null)
	},
 
	setIsModified : function( modified) {
		this.isModified = modified;
		this.onChange();
	},


	onChange : function() {
		this.inputTarget.value = this.isModified;
		if( this.isModified) {	
			var form = document.forms[this.formName];
			var saveSubmit = form.elements[this.submitSaveName];
			if ( saveSubmit) saveSubmit.disabled = false;
		}	
	},
 
	attachObserver : function( theForm, modified) {
		if (typeof theForm == 'string') {
			theForm = document.forms[theForm];
		}
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
	            if ((type == "text" || type == "hidden" || type == "password")) {
	            	$(element).change(function() {
	                	self.setIsModified(true);
	                });
	                if (type == "text") {
						$(element).change(function(event) {
							event = event || window.event;
							return self.onkeyup( event);
						});
	                }        
	            }
	            else if(type == "checkbox" || type == "radio"){
					$(element).click(function() {
						self.setIsModified(true);
					});
	            }
	            else if (type == "submit") {
	            	if (element.name === this.submitSaveName) {
	            		element.disabled = true;
		            	$(element).click(function(ev) {
							ev = ev || window.event;
							if (!self.submitSaveConfirmation(this)) {
								return Event.stop(ev);
							}
		                });
	                }
	            }
	            else if (type == "reset") {
					$(element).click(function(ev) {
						ev = ev || window.event
						if (!self.submitResetConfirmation(this)) {
							return Event.stop(ev);
						}
					});
	            }           
	        }
	        else if (tagName == "select") {
				$(element).change(function(ev) {
					self.setIsModified( true);
				});
	        }
	        else if (tagName == "textarea") {
	            $(element).change(function(ev) {
	             	self.setIsModified( true);
				});
	            $(element).keyup(function(event) {
					event = event || window.event;
					return self.onkeyup( event);
				});
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
	  	if (!this.isModified || confirm(this.messageSaveSubmitConfirmation)) {
			var form = document.forms[this.formName];
			var saveSubmit = form.elements[this.submitSaveName];
			if (saveSubmit) {
				saveSubmit.disabled = true;
			}

			// d'où le "__confirmed_save" que l'on voit ensuite dans les contrôleurs...
			var elementName = "__confirmed_" + this.submitSaveName;
			var confirmedSave = form.elements[elementName];
			if (confirmedSave == null) {
				confirmedSave = document.createElement("INPUT");
				confirmedSave.type = "hidden";
				confirmedSave.name = elementName;
				form.appendChild(confirmedSave);
			}
			confirmedSave.value = "yes";
			form.submit();
			return true;
	  	}
	  	else {
	  		return false;
	  	}
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
