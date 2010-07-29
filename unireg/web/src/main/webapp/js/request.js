

var request = new Request( document.location.href);


function Request( pathinfo) {
  this.path = "";
  this.queryString = "";
    this.pathInfo = pathinfo;

    var i = ( this.pathInfo ? this.pathInfo.indexOf( "?") : 0);
    if ( i > 0) {
      this.path = this.pathInfo.substr(0, i);
      this.queryString = this.pathInfo.substr( i+1);
      this.parameters = Request__populateParameter( this.queryString);
    } else {
      this.path = this.pathInfo;
      this.parameters = new Array();
    }
}


Request.prototype.getParameter = function( name, delimiter){
  var value = Request__getParameter( name, this.parameters);
  if ( !value)
    return null;
  if ( delimiter) {
    return value.split( delimiter);
  }
  return value;
}

Request.prototype.setParameter = function( name, value){  
  var temp = Request__getParameter( name, this.parameters);
  if ( temp) {
    temp.value = value;
  } else {
    var item = new Object();
    item.name = name;
    item.value = value;
    this.parameters.push( item);
  }
}

Request.prototype.setParameterList = function( list){
   	if ( typeof item.length == "undefined" ) {
    	item  = [item];
    }
    this.parameters.push( item);
}

Request.prototype.send = function(){
  var url = this.getPathInfo();
  document.location.href = url;
}


Request.prototype.getPathInfo = function(){
  var uri = this.path;
  var param = Request__generateParameter( this.parameters);
  if ( param.length > 0)
    uri += "?" + param;
  return uri;
}



function Request__populateParameter( queryString) {
  var l = queryString.split("&");
  var list = new Array();
  for( i = 0; i < l.length; i++) {
    var s = l[i].split( "=");
    var item = new Object();
    item.name = unescape( s[0]);
    item.value = unescape( s[1]);
    list.push( item);
  }
  return list;
}

function Request__generateParameter( list) {
  var str = "";
  for( i = 0 ; i< list.length; i++) {
    var item = list[i];
    if ( typeof item.length == "undefined" ) {
    	item  = [item];
    }
    for(j = 0; j < item.length; j++) {
      str += escape( item[j].name) + "=" + escape(item[j].value);
      if ( j < (item.length -1))
        str += "&";
    }
    if ( i < (list.length -1))
      str += "&";
  }
  return str;
}


function Request__getParameter( name, list){
  for( i = 0; i < list.length; i++) {
    if ( list[i].name === name)
      return list[i].value;
  }
  return null;
}