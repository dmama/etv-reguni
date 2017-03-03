<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.recapitulatif.deces" />
  	</tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/creation-deces.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">
	    <%--@elvariable id="recap" type="ch.vd.uniregctb.deces.view.DecesRecapView"--%>

	    <!-- Bandeau -->
	    <c:set var="titre"><fmt:message key="label.caracteristiques.personne"/></c:set>
	    <unireg:bandeauTiers numero="${recap.tiersId}" titre="${titre}" cssClass="information"
	                         showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="false" showComplements="true"/>

	    <!-- Formulaire -->
	  	<form:form method="post" id="formRecapDeces"  name="formRecapDeces" commandName="recap">
		    <form:hidden path="tiersId"/>
		    <form:hidden path="marieSeul"/>

		    <fieldset>
			    <legend><span><fmt:message key="label.caracteristiques.deces" /></span></legend>
			    <table>
				    <tr class="<unireg:nextRowClass/>" >
					    <td width="25%"><fmt:message key="label.date.deces" />&nbsp;:</td>
					    <td width="75%">
						    <jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
							    <jsp:param name="path" value="dateDeces" />
							    <jsp:param name="id" value="dateDeces" />
						    </jsp:include>
						    <span style="color: red;">*</span>
					    </td>
				    </tr>
				    <c:if test="${recap.marieSeul}">
					    <tr class="<unireg:nextRowClass/>" >
						    <td width="100%" colspan="2"><fmt:message key="label.deces.nature.marie.seul"/>&nbsp;:</td>
					    </tr>
					    <tr class="<unireg:nextRowClass/>" >
						    <td width="25%">&nbsp;</td>
						    <td width="75%">
							    <form:radiobutton path="veuf" id="nature-decede" value="false"/>
							    <label for="nature-decede"><fmt:message key="label.deces.decede"/></label>
							    <br>
							    <form:radiobutton path="veuf" id="nature-veuf" value="true"/>
							    <label for="nature-veuf"><fmt:message key="label.deces.veuf"/></label>
						    </td>
					    </tr>
				    </c:if>
				    <tr class="<unireg:nextRowClass/>">
					    <td width="25%"><fmt:message key="label.commentaire" />&nbsp;:</td>
					    <td width="75%">
						    <form:textarea path="remarque" id="remarque" cols="80" rows="5"/>
					    </td>
				    </tr>
			    </table>
		    </fieldset>

			<!-- Boutons -->
			<unireg:RetourButton link="list.do" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="javascript:return Page_sauverDeces(event || window.event);" />	
		</form:form>

		<script type="text/javascript" language="Javascript">
			function Page_sauverDeces(event) {
				if(!confirm('Voulez-vous vraiment confirmer ce décès ?')) {
					return Event.stop(event);
			 	}
			 	return true;
			}
		</script>
	</tiles:put>
</tiles:insert>