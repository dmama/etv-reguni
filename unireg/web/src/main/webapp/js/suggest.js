

function AutoComplete(srcElementFieldId, contentId)  {	
	this.count = 0;
	this.formContext = null;
	this.id = srcElementFieldId;
	this.selectedNodeIndex = -1;
	this.currentSelectedIndex = -1;
	this.hasChanged = false;
	this.reDataField = /\{(\w*)\}/g;
	this.srcElement = E$(srcElementFieldId);
	this.divContent = E$(contentId);	
    this.ieFix = null;    
	this.initialize();		
	this.start()
}

AutoComplete.prototype.setAutoSynchrone = function( autoSynchro) {
	this.autoSynchro = autoSynchro;
}

AutoComplete.prototype.setDataTextField = function( dataTextField) {

	this.dataTextField = dataTextField;
	if (this.dataTextField) {
		this.parametersString = new Array();
	    var arr;
	    while ((arr = this.reDataField.exec(this.dataTextField)) != null) {
	    	//alert(arr.index + "-" + arr.lastIndex + "\t" + arr);
       		this.parametersString.push( arr[1]);
       	}
    }
}

AutoComplete.prototype.start = function() {
	this.starting = true;
	this.adjustCss();
}

AutoComplete.prototype.stop = function() {
	this.starting = false;
	this.adjustCss();
}

AutoComplete.prototype.setDataValueField = function( dataValueField) {
	this.dataValueField = dataValueField;
}
AutoComplete.prototype.setDataSource = function( dataSource) {
	this.dataSource = dataSource;
}
	
AutoComplete.prototype.adjustCss = function() {
	var empty = this.srcElement.value === '';
	if (!this.autoSynchro) {
		empty =  !(this.currentSelectedIndex >=0);
	}
	this.srcElement.removeClassName( "empty");
	this.srcElement.removeClassName(  "readonly");
	if (this.srcElement.readOnly) {
		this.srcElement.addClassName(  "readonly");
	} else {
    	if (this.starting && empty) this.srcElement.addClassName(  "empty");
    }
}

AutoComplete.prototype.initialize = function() {	
	this.clear();
	var self = this;	
	this.currentSelectedIndex = -1;
	this.dataTextField = null;
	this.formContext = null;
	this.dataValueField = null;
	this.dataSource = null;
	this.lineDecorator = null;
	this.onChange = null;
	this.showing = false;
	this.autoSynchro = true;
	this.hasChanged = false;
	
    if ( this.srcElement) {
    	this.srcElement.removeClassName(  "autocomplete");
    	this.srcElement.addClassName(  "autocomplete");    	
    	this.srcElement.autocomplete = "off";
    	this.srcElement.setAttribute("autocomplete", "off");    
    	this.adjustCss();
    	
    	this.srcElement.onfocus = function(event) {
    		if (self.hasChanged) {
	    		self.adjustCss();
	    	}
    	}
    	Element.addObserver(this.srcElement, "propertychange",  function(event) {
     		event = event || window.event;
     		var attrName = event.propertyName || event.attrName; 
    		if ( attrName.toLowerCase() == "readonly") {
    			self.adjustCss();
    		}
    	}, false);
	    this.srcElement.onblur = function(event) {	    	
	    	event = event || window.event;
	    	var status = true;
     		if (!self.toElement) {
	    		self.hide(); 	 
	    		self.clearFormContext();
	    		if (self.hasChanged) {
		    		var item = self.getNode( self.currentSelectedIndex);
		    		if ( !item) {
						self.currentSelectedIndex = -1;
						if ( !self.autoSynchro) {
							self.srcElement.value = "";
						}
					}  	else  if ( self.onChange) {	
	    				var xtItem = item.xt_getItem();
						status = self.onChange( xtItem);
					}
					self.adjustCss();				
				}				
	    	} 
	    	return status;
	    };    	    
       this.srcElement.onkeyup = function(event) {
            return self.onkeyup(event);
        };
        this.srcElement.onkeydown = function(event) {
            return self.onkeydown(event);
        };
    }
    if( this.divContent) {
    	this.divContent.getStyle().position = "absolute";
    	this.divContent.onmouseover = function(event) {
            self.toElement = true;             
            return false;
        };
        this.divContent.onmouseout = function(event) {
            self.toElement = false;             
            return false;
        };
    }
    this.hide();
}

AutoComplete.prototype.clear = function() {	
	this.hasFocus = false;	
	this.selectedNodeIndex = -1;
	this.currentSelectedIndex = -1;
	this.count = 0;
	while(this.divContent.childNodes.length>0) {
           this.divContent.removeChild(this.divContent.childNodes[0]);
     }
     this.divContent.getStyle().height = "0px";
}


AutoComplete.prototype.createControl = function(resultat) {		
	try {				
		var text = this.getSelectedText();
		var self = this;		
		this.clear();		
		this.selectedNodeIndex = 0;
       var output = document.createElement("div");       
       output.className = "autocomplete";
       this.divContent.appendChild( output);
         
         var table = document.createElement("table");
         table.cellPadding = 0;
         table.cellSpacing = 0;
         table.border = 0;
         var length = resultat.result.length;
        for (var i=0;i<length;i++) {
        	var item = resultat.result[i];
        	var value = eval("item."+ this.dataValueField);
        	if (value) {
        		this.count++;
	        	var row = table.insertRow(-1);
	        	row.id = this.id + "_" + i;
	        	row.xt_index = i;
	        	row.className ="sr";
	        	row.xt_item = item;
	        	row.xt_getItem = function() {
	        		return this.xt_item;
	        	}
	        	row.xt_getValue = function() {
	        		return eval("this.xt_item."+ self.dataValueField);
	        	}
	        	row.xt_getIndex = function() {
	        		return this.xt_index;
	        	}
	        	row.onmouseover = function() {
	        		self.selectNode(this.xt_getIndex());
	        	}
	        	row.onmouseout = function() {
	        		self._unSelectCurrentNode();
	        	}
	        	row.onclick = function() {
	        		self.selectNode(this.xt_getIndex);
	        		self.setCurrentSelectedItem(this);
	        		self.clearFormContext();
	        		self.hide();
	        	}
	        	this.createLine( row, item);
        	}
        }       
        output.appendChild( table);

		if (this.autoSynchro) {        
	        var item = this.getNode( this.selectedNodeIndex);
		     if (item) {	     	
		     		//this.typeAhead(item.xt_getValue());
		     		this.selectNode( this.selectedNodeIndex);
		     }
	     } else {
	     	this.selectedNodeIndex = -1;
	     	if ( this.count == 1) {
	     		this.setCurrentSelectedItem( 0);
	     		return;
	     	}
	     }
	     if ( this.count == 0) {
	     	this.hide();
	     } else {
	     	this.show();	     	
	     	this._adjustSize( this.divContent) ;
	     }
	          
    } catch( ex) {
		alert(ex.message);
		throw ex;
	} finally {
		//
	}
	
}

AutoComplete.prototype.createLine = function(row, result) {	
	if (this.lineDecorator) {
		this.lineDecorator.decorate(row, result);
	}	

	if ( !this.dataTextField) {
		var field = this.dataValueField;
		field = eval( "result."+field);
	} else {
		var field = new String(this.dataTextField);		
		if( this.parametersString && this.parametersString.length > 0) {
			for ( var i = 0; i < this.parametersString.length; i++) {
				var variable= this.parametersString[i] ;				
				var val = eval( "result."+ variable);
				var re = new RegExp( "{"+ variable + "}", "g");
				field = field.replace(re , val);  
			}			
		} else {
			field = eval( "result."+field);
		}
	}
	
    var cell1 = row.insertCell(-1);
    Element.setText(cell1, field);
    cell1.noWrap = "true";
}

AutoComplete.prototype.selectNode = function( index) {
    var item =this.getNode( index);
    if (item) {  
	    this._unSelectCurrentNode();
	    item.addClassName( "selected");
	    this.selectedNodeIndex = index;	    	   
    }   
}

AutoComplete.prototype.getNode = function( id) {
	if ( id >= 0) {
    	return E$(this.id +"_"+  id);
    }
    return null;
}

AutoComplete.prototype._unSelectCurrentNode = function() {
    if ( this.selectedNodeIndex  >=0) {
        var old =this.getNode(  this.selectedNodeIndex);  
        old.removeClassName( "selected");
        this.selectedNodeIndex = -1;
    }
}


 AutoComplete.prototype.sendQuery = function(key)  {
 	if (!this.starting) {
 		return;
 	} 	 	
 	this.hide();
 	var self = this;
	XT.doAjaxAction( this.dataSource, this.srcElement, 
		{
			selectedValue : key,
			NAME : this.id
		}, 
		{
			 clearQueryString: true
    	});
 }



AutoComplete.prototype.show = function() {
	this.divContent.show( );
	this.showing = true;
	if ( !this.ieFix) {		
		var ua = navigator.userAgent.toLowerCase();
	    var isIE  = ( (ua.indexOf('msie') != -1) && (ua.indexOf('opera') == -1) && (ua.indexOf('webtv') == -1) );    
	    var versionMajor = parseInt(navigator.appVersion);	      
	    if ( isIE && versionMajor < 7) {      
		      var iframe = document.createElement("iframe");
		      iframe.id =  this.id + "-frame";        	     
		      Element.getStyle(iframe).position = "absolute";
		  	  iframe.frameBorder = "no";
		  	  iframe.border = "0";
		  	  iframe.scrolling = "no";
		  	  iframe.zIndex = 1;
		  	  document.body.appendChild( iframe);		  	  
		  	  this.ieFix = E$(iframe);		  	  
	  	}
  	}
	if ( this.ieFix) {
		 this.ieFix.show( );
	}
	var size = this.srcElement.getElementPosition( );
	var bound = this.divContent.getElementPosition( );
	this.divContent.move( size.x,size.y+ size.height);
	//window.status ="size.x: " +size.x + ", size.y: " + size.y +", size.width : "  + size.width+", size.height : "  + size.height+
	// "sizeC.x: " +bound.x + ", sizeC.y: " + bound.y +", sizeC.width : "  + bound.width+", sizeC.height : "  + bound.height;
};

AutoComplete.prototype.hide = function() {
	this.divContent.hide();
	this.showing = false;
	if (this.ieFix) {
		 this.ieFix.hide();
	}
};

AutoComplete.prototype.setCurrentSelectedItem = function( item) {	
		if ( typeof item === "number") {
			item = this.getNode( item);
		}	
		if ( !item) {
			if ( this.onChange) {			
				this.onChange(null);
			}
			this.currentSelectedIndex = -1;
			if ( !this.autoSynchro) {
				this.srcElement.value = "";
			}
		} else {
			this.hasChanged = this.currentSelectedIndex != item.xt_getIndex();
			if ( this.hasChanged) {
				this.currentSelectedIndex = item.xt_getIndex(); 
				this.srcElement.value = item.xt_getValue();			
				if ( this.onChange) {			
					this.onChange( item.xt_getItem());
				}			
			}
			if ( this.currentSelectedIndex >= 0) {
				this.adjustCss();
			}			
		}
		this.selectRange(99,99);
};

AutoComplete.prototype.clearSelectedValue = function() {
	// propagation onchange
	this.currentSelectedIndex = -1;
	this.selectedNodeIndex = -1;
	if (this.onChange) {			
		this.onChange(null);
	}
	this.adjustCss();	
};

AutoComplete.prototype.getSelectedText  = function() {
        var textbox = this.srcElement ;
        var N = 0;
        if(textbox.createTextRange){
	        // spécifique IE
            var fa=document.selection.createRange().duplicate();
            if  (fa.text)
            	N=fa.text.length;
        } else if(textbox.setSelectionRange ){
        	// Spécifique Gecko
			try {
				N = textbox.selectionEnd - textbox.selectionStart;
			} catch(e) {
		        return textbox.value.substring(0, 0);
			}
        }
        return textbox.value.substring(0, textbox.value.length-N);
    }


AutoComplete.prototype.onkeyup = function(ev) {  
	if ( !ev)
      var ev = window.event;
    if (ev.altKey == true)
        return true;
	var key = ev.keyCode;
	
	//window.status = "keyCode: " + key;
	if ( key == Event.KEY_UP
			|| key == Event.KEY_LEFT
			|| key == Event.KEY_RIGHT
			|| key == Event.KEY_DOWN
			|| key == Event.KEY_RETURN
			|| key == Event.KEY_ESC) {
		// noop
	} else if ( key == Event.KEY_RETURN) {
		if ( this.showing) {			
			return Event.stop( ev);
        }
        return true;
	} else if ( key > 31) {
		this.clearSelectedValue();
	    var text = this.getSelectedText();		
		if ( text && text !=="") {
			this.sendQuery(text);
		} 	
	} else if ( key == Event.KEY_DELETE || key == Event.KEY_BACKSPACE) {
			this.clearSelectedValue();
	}

    //window.status = "ev.keyCode: " +ev.keyCode;
	return true;
}

AutoComplete.prototype.onkeydown = function(ev) {  
	if ( !ev)
      var ev = window.event;
    if (ev.altKey == true)
        return true;


	var key = ev.keyCode;
	
	if (key === Event.KEY_ESC) {
		this.hide();
		this.setCurrentSelectedItem(null);
		this.restoreFormContext();
		return true;
	} 
    
    this.storeFormContext();
        
	if ( this.count == 0)
		return true;
		
	 if (key == Event.KEY_UP){
            if ((this.selectedNodeIndex-1) >= 0) {
            	var index = this.selectedNodeIndex;
            	index--
                this.selectNode( index);
                var item = this.getNode( this.selectedNodeIndex);
                 if (item) {
                 	this.scrollIntoView( item, false);
                 	if (this.autoSynchro) {
	                	//this.typeAhead(item.xt_getValue());
	                }
                	this.setCurrentSelectedItem(item);
                }
            }
        } else if (key == Event.KEY_DOWN) {
            if ((this.selectedNodeIndex+1) < this.count) {
            	var index = this.selectedNodeIndex;    
            	index++;    
                this.selectNode(index);
                var item = this.getNode( this.selectedNodeIndex);
                if (item) {
                	this.scrollIntoView( item, true);
                	if (this.autoSynchro) {
	                	//this.typeAhead(item.xt_getValue());
	                }
	                this.setCurrentSelectedItem(item);
                }
            }
        } else if (key == Event.KEY_RETURN) {
        	if (this.showing) {        		
		        this.setCurrentSelectedItem(this.selectedNodeIndex);
		        this.clearFormContext();
	        	this.hide();
	            this.selectRange(99,99);
	            return Event.stop(ev);	            
            }            
        } else if (key == Event.KEY_TAB) {
        	var index = ( this.showing  ? this.selectedNodeIndex : this.currentSelectedIndex);
	       	this.setCurrentSelectedItem(index);
	       	this.clearFormContext();
	       	this.hide();
	       	this.selectRange(99,99);
	        return true;
		} 
	return true;
}

AutoComplete.prototype._adjustSize = function( content) {
		content = E$(content);
        var rectBounds = Element.getViewportSize(); 		
        content.getStyle().width = "";
        content.getStyle().height = "";
		var bound = content.getElementPosition( );
		if (content.scrollHeight) {
			bound.height = bound.height +  content.scrollHeight;
		}
		var height = bound.height;
		if ( bound.y +  bound.height >= rectBounds.height) {
			height = rectBounds.height - bound.y - 20;
			if ( height <= 0) {
				height = 100;
			}
		}		
         //window.status ="rectBounds.h: " +rectBounds.height + ", bound.x: " + bound.x +", bound.y : "  + bound.y +", bound.width : "  + bound.width+", bound.height : "  + bound.height;
         if (height == 0) 
         	height = 20;
         else if (height < 100 &&  this.count < 5) {
         	height = 100;
         }
        content.getStyle().height = height + "px";
        if (bound.width > 0) {
        	content.getStyle().width = (bound.width+20) + "px";
        }        
        if ( this.ieFix) {
			var size = content.getElementPosition();
			this.ieFix.move(size.x, size.y);
			 this.ieFix.width = size.width;
			 this.ieFix.height = size.height;
		}
}

AutoComplete.prototype.scrollIntoView = function(node, alignToTop) {
    if (node)
    	node.scrollIntoView(alignToTop);
}


AutoComplete.prototype.storeFormContext = function() {
	if (this.formContext != null || !this.autoSynchro)
		return;
    this.formContext = {};
    var form = this.srcElement.form;
    var inputs = form.elements;
    for(var i = 0; i < inputs.length; i++) {
    	var input = inputs[i];
    	if ( input.name !== "" && input.name)
    		this.formContext[input.name] = input.value;
    }
}

AutoComplete.prototype.clearFormContext = function() {
    this.formContext = null;
}


AutoComplete.prototype.restoreFormContext = function() {
   if (this.formContext == null) {
   		return;
   }
    var form = this.srcElement.form;
    var inputs = form.elements;
    for(var i = 0; i < inputs.length; i++) {
    	var input = inputs[i];
    	input.value =this.formContext[input.name];
    }
    this.adjustCss();
}



AutoComplete.prototype.typeAhead = function(sSuggestion ) {
      var textbox = this.srcElement;
      //check for support of typeahead functionality
      if (textbox.createTextRange || textbox.setSelectionRange){
      	 var text = this.getSelectedText();
      	 if ( text) {
	          var iLen = text.length;
	          textbox.value = sSuggestion;
	          this.selectRange(iLen, sSuggestion.length);
          }
      }
  };
    
AutoComplete.prototype.selectRange = function(iStart, iLength ) {
     var textbox = this.srcElement;
     //use text ranges for Internet Explorer
      if (textbox.createTextRange) {
          var oRange = textbox.createTextRange();
          oRange.moveStart("character", iStart);
          oRange.moveEnd("character", iLength - textbox.value.length);
          oRange.select();
          //use setSelectionRange() for Mozilla
      } else if (textbox.setSelectionRange) {
          textbox.setSelectionRange(iStart, iLength);
      }
      //set focus back to the textbox
      textbox.focus();
 };
 

