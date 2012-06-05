<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<tiles:getAsString name='refresh' ignore='true'/>
		<link rel="SHORTCUT ICON" href="<c:url value="/images/favicon.ico"/>">

		<link media="screen" href="<c:url value="/css/x/jquery-ui.css"/>" rel="stylesheet" type="text/css">
		<link media="screen" href="<c:url value="/css/x/screen-all.css"/>" rel="stylesheet" type="text/css">

		<%@ include file="/WEB-INF/jsp/include/png-workaround.jsp" %>

		<link media="print" href="<c:url value="/css/print/print-all.css"/>" rel="stylesheet" type="text/css">

		<script type="text/javascript" language="javascript">
			function getContextPath() {
				return '<c:url value="/"/>';
			}
		</script>

		<title><tiles:getAsString name='title' ignore='false'/></title>
		<tiles:getAsString name='head' ignore='true'/>
	</head>
	<body>

			<div id="sommaire">
				<div style="position:absolute; top:5px; left:5px;">
					<div id="loadingImage" style="display:none;"><img src="<c:url value="/images/loading.gif"/>"  alt="loading..."/></div>
				</div>
				<div class="canton">
					<a href="http://www.vd.ch" target="_blank">
						<span class="label"><fmt:message key="label.canton.vaud" /></span>
					</a>
				</div>
				<tiles:getAsString name='links' ignore='true'/>
			</div>

			<div id="header" >
				<span class="departement"><a href="http://www.aci.vd.ch" target="_blank"><fmt:message key="label.aci" /></a></span>
				<div class="application iepngfix"></div>
			</div>

			<div id="content">
				<div id="outils" class="outils">
					<h3>Outils</h3>
					<tiles:getAsString name='tools' ignore='true'/>
					<ul>
						<li>
						<unireg:user/>
						</li>
						<tiles:getAsString name='vue' ignore='true'/>
						<li>
							<a href="<c:url value='/'/>" title="Recherche" accesskey="d"><fmt:message key="label.recherche" /></a>
						</li>
						<li>
							<tiles:getAsString name='fichierAide' ignore='true'/>
						</li>
						<li>
							<a href="/fiscalite/kbaci/advancedSearch.htm" title="Base ACI" accesskey="b"><fmt:message key="label.kbaci" /></a>
						</li>
						<li>
							<a href="/iam/accueil/" target="_blank" title="Portail IAM" accesskey="x"><fmt:message key="label.portail.iam" /></a>
						</li>
						<li>
							<a href="<c:url value='/logoutIAM.do'/>" title="Quitter Unireg" accesskey="l"><fmt:message key="label.deconnexion" /></a>
						</li>
					</ul>
				</div>
				<div style="padding: 5px;">
					<div class="empty" style="height: 10px;">&nbsp;</div>

					<%-- Message flash --%>
					<%--@elvariable id="flash" type="ch.vd.uniregctb.supergra.FlashMessage"--%>
					<c:if test="${flash != null && flash.active}">
						<div id="flashdisplay" class="<c:out value='${flash.displayClass}'/>"><c:out value="${flash.messageForDisplay}"/></div>
						<c:if test="${flash.timeout > 0}">
							<script type="text/javascript">
								$('#flashdisplay').delay(<c:out value="${flash.timeout}"/>).fadeOut('slow');
							</script>
						</c:if>
					</c:if>

					<h1><tiles:getAsString name='title' ignore='true'/></h1>
					<div class="workaround_IE6_bug">
						<tiles:getAsString name='body' />
					</div>
				</div>
			</div>

			<div id="footer">
				<strong>Version <fmt:message key="version" /></strong>&nbsp;&nbsp;&nbsp;(Build: <fmt:message key="buildtime"/>)
				&nbsp;&nbsp;&nbsp;<strong><unireg:environnement/></strong>
			</div>
			
	</body>

</html>
