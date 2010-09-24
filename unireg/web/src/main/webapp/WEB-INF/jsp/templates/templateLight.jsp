<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<tiles:getAsString name='refresh' ignore='true'/>
		<link rel="SHORTCUT ICON" href="<c:url value="/images/favicon.ico"/>">
		<link media="screen" href="<c:url value="/css/x/layout.css"/>" rel="stylesheet" type="text/css">
		<link media="screen" href="<c:url value="/css/x/tabs.css"/>" rel="stylesheet" type="text/css">
		<link media="screen" href="<c:url value="/css/x/tools.css"/>" rel="stylesheet" type="text/css">
		<link media="screen" href="<c:url value="/css/x/displaytag.css"/>" rel="stylesheet" type="text/css">
		<link media="screen" href="<c:url value="/css/x/suggest.css"/>" rel="stylesheet" type="text/css">
		<link media="screen" href="<c:url value="/css/x/unireg.css"/>" rel="stylesheet" type="text/css">
		<link media="screen" href="<c:url value="/css/x/thickbox.css"/>" rel="stylesheet" type="text/css">
		<link media="screen" href="<c:url value="/css/x/tooltip.css"/>" rel="stylesheet" type="text/css">

		<link media="print" href="<c:url value="/css/print/layout.css"/>" rel="stylesheet" type="text/css">
		<link media="print" href="<c:url value="/css/print/tabs.css"/>" rel="stylesheet" type="text/css">
		<link media="print" href="<c:url value="/css/print/tools.css"/>" rel="stylesheet" type="text/css">
		<link media="print" href="<c:url value="/css/print/displaytag.css"/>" rel="stylesheet" type="text/css">
		<link media="print" href="<c:url value="/css/print/unireg.css"/>" rel="stylesheet" type="text/css">
		<link media="print" href="<c:url value="/css/print/thickbox.css"/>" rel="stylesheet" type="text/css">
		
		
		<title><tiles:getAsString name='title' ignore='false'/></title>
		<tiles:getAsString name='head' ignore='true'/>
	</head>
	<body>
		<table border="0" cellpadding="0" cellspacing="0" width="100%">
		<tr>
		<td width="1%" valign="top" rowspan="2">
			<div id="sommaire">
				<div style="position: absolute;top: 5px;left: 5px;"><div id="loadingImage" style="visibility: hidden;"><img id="loadingImageSign" src="<c:url value="/images/loading.gif"/>" /></div></div>
				<div class="canton">
					<a href="http://www.vd.ch" target="_blank">
						<span class="label"><fmt:message key="label.canton.vaud" /></span>
					</a>
				</div>
				<tiles:getAsString name='links' ignore='true'/>
			</div>
		</td>
		<td valign="top"  width="99%">
			<div id="header" >
				<span class="departement"><a href="http://www.aci.vd.ch" target="_blank"><fmt:message key="label.aci" /></a></span>
				<div class="application"></div>
			</div>
		</td>
	</tr>
	<tr>
		<td width="99%" valign="top" height="450px">
			<div id="content">
				<div class="outils">
					<h3>Outils</h3>
					<tiles:getAsString name='tools' ignore='true'/>
					<ul>
						<li>
						<unireg:user></unireg:user>
						</li>
						<tiles:getAsString name='vue' ignore='true'/>
						<li>
							<a href="<c:url value='/'/>" title="Recherche" accesskey="d"><fmt:message key="label.recherche" /></a>
						</li>
						<li>
							<a href="<c:url value='/docs/aide.pdf'/>" title="Aide" accesskey="a"><fmt:message key="label.aide" /></a>
						</li>
						<li>
							<a href="/fiscalite/kbaci/" title="Base ACI" accesskey="b"><fmt:message key="label.kbaci" /></a>
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
					<h1><tiles:getAsString name='title' ignore='true'/></h1>
					<tiles:getAsString name='body' />
				</div>
			</div>
		</td>
	</tr>
	<tr>
		<td colspan="2" style="vertical-align: bottom">
			<div id="footer">
				<fmt:message key="version" />
			</div>
		</td>
		</tr>
		</table>
	</body>

</html>
