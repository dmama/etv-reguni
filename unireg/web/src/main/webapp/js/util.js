/*
* Supprime les espaces avant et après
*/
function trim(chaine) 
{ 
    if(trim.arguments.length > 1) 
    { 
        var str = trim.arguments[1]; 
        expreg = new RegExp('(^'+ str +'*)|('+ str +'*$)', 'g'); 
    } 
    else 
    { 
        expreg = /(^\s*)|(\s*$)/g; 
    } 
    return chaine.replace(expreg,''); 
} 

function isEmptyString(str) {
    return (!str || 0 === str.length);
}

function isBlankString(str) {
    return (!str || /^\s*$/.test(str));
}

/*
* Converti une chaine (dd.MM.yyyy) en date 
*/
function getDate(strDate, format){	
	if (format == 'dd.MM.yyyy') {  
		day = parseInt(strDate.substring(0,2));
		month = parseInt(strDate.substring(3,5));
		year = parseInt(strDate.substring(6,10));
	}
	else if (format == 'yyyy.MM.dd') {
		year = parseInt(strDate.substring(0,4));
		month = parseInt(strDate.substring(5,7));
		day = parseInt(strDate.substring(8,10));
	}
	else {
		alert("Type de format inconnu !");
	}
	d = new Date();
	d.setDate(day); // 1..31
	d.setMonth(month - 1); // 0..11
	d.setFullYear(year); // 4 digits
	return d;  
}

/*
* Ajoute nombreAnnees années à la date
*/
function addYear(strDate, nombreAnnees, format){
	if (format == 'dd.MM.yyyy') {  
		day = strDate.substring(0,2);
		month = strDate.substring(3,5);
		year = parseInt(strDate.substring(6,10)) + nombreAnnees;
	}
	if (format == 'yyyy.MM.dd') {  
		year = parseInt(strDate.substring(0,4)) + nombreAnnees;
		month = strDate.substring(5,7);
		day = strDate.substring(8,10);
	}
	d = new Date();
	d.setDate(day);
	d.setMonth(month);
	d.setFullYear(year); 
	return d; 
}

/*
* Retourne:
*   0 si date_1=date_2
  *   1 si date_1>date_2
*  -1 si date_1<date_2
*/	  
function compare(date_1, date_2){
  diff = date_1.getTime()-date_2.getTime();
  return (diff==0?diff:diff/Math.abs(diff));
}
