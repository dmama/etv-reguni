<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<link media="screen" href="<c:url value="/css/x/jquery-ui.css"/>" rel="stylesheet" type="text/css">

<%--@elvariable id="uniregEnvironnement" type="java.lang.String"--%>
<c:if test="${uniregEnvironnement == 'Developpement'}">
	<link media="screen" href="<c:url value="/css/x/layout.css"/>" rel="stylesheet" type="text/css">
	<link media="screen" href="<c:url value="/css/x/tabs.css"/>" rel="stylesheet" type="text/css">
	<link media="screen" href="<c:url value="/css/x/tools.css"/>" rel="stylesheet" type="text/css">
	<link media="screen" href="<c:url value="/css/x/displaytag.css"/>" rel="stylesheet" type="text/css">
	<link media="screen" href="<c:url value="/css/x/unireg.css"/>" rel="stylesheet" type="text/css">
	<link media="screen" href="<c:url value="/css/x/tooltip.css"/>" rel="stylesheet" type="text/css">
	<link media="screen" href="<c:url value="/css/x/jquery.jgrowl.css"/>" rel="stylesheet" type="text/css">
</c:if>
<c:if test="${uniregEnvironnement != 'Developpement'}">
	<link media="screen" href="<c:url value="/css/x/screen-all.css"/>" rel="stylesheet" type="text/css">
</c:if>

<c:if test="${uniregEnvironnement == 'Developpement'}">
	<link media="print" href="<c:url value="/css/print/common.css"/>" rel="stylesheet" type="text/css">
	<link media="print" href="<c:url value="/css/print/displaytag.css"/>" rel="stylesheet" type="text/css">
	<link media="print" href="<c:url value="/css/print/layout.css"/>" rel="stylesheet" type="text/css">
	<link media="print" href="<c:url value="/css/print/tabs.css"/>" rel="stylesheet" type="text/css">
	<link media="print" href="<c:url value="/css/print/tools.css"/>" rel="stylesheet" type="text/css">
	<link media="print" href="<c:url value="/css/print/unireg.css"/>" rel="stylesheet" type="text/css">
</c:if>
<c:if test="${uniregEnvironnement != 'Developpement'}">
	<link media="print" href="<c:url value="/css/print/print-all.css"/>" rel="stylesheet" type="text/css">
</c:if>

