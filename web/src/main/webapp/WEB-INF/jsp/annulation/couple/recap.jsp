<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="idMenage" type="java.lang.Long"--%>
<%--@elvariable id="dateMenageCommun" type="ch.vd.registre.base.date.RegDate"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.recapitulatif.annulation.menage.commun" />
  	</tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onclick="ouvrirAide('<c:url value='/docs/annulation-couple.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">

	    <unireg:bandeauTiers numero="${idMenage}" showAvatar="true" showLinks="false" showValidation="false" showComplements="false"/>

	    <fieldset class="information">
		    <legend><span><fmt:message key="title.caracteristiques.menage.commun" /></span></legend>
		    <table>
			    <tr class="<unireg:nextRowClass/>" >
				    <td width="25%"><fmt:message key="label.date.menage.commun" />&nbsp;:</td>
				    <td width="75%"><unireg:regdate regdate="${dateMenageCommun}" /></td>
			    </tr>
		    </table>
	    </fieldset>

	    <!-- Debut Boutons -->
	    <unireg:RetourButton link="list.do" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
	    <c:set var="NomBoutonAnnuler"><fmt:message key="label.bouton.annuler.menage"/></c:set>
	    <c:set var="dateDebutMenage"><unireg:regdate regdate="${dateMenageCommun}"/></c:set>
	    <unireg:buttonTo name="${NomBoutonAnnuler}" action="/annulation/couple/commit.do" params="{numeroCple:${idMenage},date:'${dateDebutMenage}'}" method="post" confirm="Voulez-vous vraiment annuler la mise en ménage commun de ces deux personnes ?"/>
	    <!-- Fin Boutons -->

	</tiles:put>
</tiles:insert>