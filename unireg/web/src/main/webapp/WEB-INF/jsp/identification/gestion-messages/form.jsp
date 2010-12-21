<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<table>
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%">
			<b><fmt:message key="label.message" /></b>
		</td>
		<td width="25%">&nbsp;</td>
		<td width="25%">
			<b><fmt:message key="label.personne" /></b>
		</td>
		<td width="25%">&nbsp;</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%">
			<fmt:message key="label.type.message" />&nbsp;:
		</td>
		<td width="25%">
			<form:select path="typeMessage">
				<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
				<form:options items="${typesMessage}" />
			</form:select>	
		</td>
		<td width="25%">
			<fmt:message key="label.nom" />&nbsp;:
		</td>
		<td width="25%">
			<form:input  path="nom" id="nom" cssErrorClass="input-with-errors" />
			<form:errors path="nom" cssClass="error"/>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%">
			<fmt:message key="label.periode.fiscale" />&nbsp;:
		</td>
		<td width="25%">
			<form:select path="periodeFiscale">
				<form:option value="-1" ><fmt:message key="option.TOUTES" /></form:option>
				<form:options items="${periodesFiscales}" />
			</form:select>
		</td>
		<td width="25%">
			<fmt:message key="label.prenoms" />&nbsp;:
		</td>
		<td width="25%">
			<form:input  path="prenoms" id="prenoms" cssErrorClass="input-with-errors" />
			<form:errors path="prenoms" cssClass="error"/>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%">
			<fmt:message key="label.emetteur" />&nbsp;:
		</td>
		<td width="25%">
			<form:select path="emetteurId">
				<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
				<form:options items="${emetteurs}" />
			</form:select>
		</td>
		<td width="25%">
			<fmt:message key="label.navs13" />&nbsp;:
		</td>
		<td width="25%">
			<form:input  path="NAVS13" id="NAVS13" cssErrorClass="input-with-errors" />
			<form:errors path="NAVS13" cssClass="error"/>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%">
			<fmt:message key="label.priorite" />&nbsp;:
		</td>
		<td width="25%">
			<form:select path="prioriteEmetteur">
				<form:option value="TOUS" ><fmt:message key="option.TOUTES" /></form:option>
				<form:options items="${priorites}" />
			</form:select>
		</td>
		<td width="25%">
			<fmt:message key="label.date.naissance" />&nbsp;:
		</td>
		<td width="25%">
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="dateNaissance" />
				<jsp:param name="id" value="dateNaissance" />
			</jsp:include>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%">
			<fmt:message key="label.date.message" />&nbsp;:
		</td>
		<td width="25%">
			<fmt:message key="label.date.message.debut" />&nbsp;
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="dateMessageDebut" />
				<jsp:param name="id" value="dateMessageDebut" />
			</jsp:include>&nbsp;
			<fmt:message key="label.date.message.fin" />&nbsp;
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="dateMessageFin" />
				<jsp:param name="id" value="dateMessageFin" />
			</jsp:include>
		</td>
		<td width="25%"><fmt:message key="label.navs11" />:</td>
		<td width="25%">
		 	<form:input  path="NAVS11" id="NAVS11" cssErrorClass="input-with-errors" />
			<form:errors path="NAVS11" cssClass="error"/>
		</td>
	</tr>
	
		<c:choose>
			<c:when test="${messageEnCours}">
				<authz:authorize ifAnyGranted="ROLE_MW_IDENT_CTB_GEST_BO,ROLE_MW_IDENT_CTB_ADMIN,ROLE_MW_IDENT_CTB_VISU">
		

				<tr class="<unireg:nextRowClass/>" >
					<td width="25%">
						<fmt:message key="label.etat.message" />&nbsp;:
					</td>
					<td width="25%">
						<form:select path="etatMessage">
							<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
							<form:options items="${etatsMessage}" />
						</form:select>
					</td>
					<td width="25%">&nbsp;</td>
					<td width="25%">&nbsp;</td>
				</tr>	
		
				</authz:authorize>
			</c:when>		
		
			<c:otherwise>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%">
							<fmt:message key="label.etat.message" />&nbsp;:
						</td>
						<td width="25%">
							<form:select path="etatMessage">
								<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
								<form:options items="${etatsMessage}" />
							</form:select>
						</td>
						<td width="25%">
							<fmt:message key="label.identification.traitement.user" />&nbsp;:
						</td>
						<td width="25%">
							<form:select path="traitementUser">
								<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
								<form:options items="${traitementUsers}" />
							</form:select>
						</td>
					</tr>	
			</c:otherwise>
		</c:choose>
	
	
	
</table>
<!-- Debut Boutons -->
<table border="0">
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%">&nbsp;</td>
		<td width="25%">
			<div class="navigation-action"><input type="submit" value="<fmt:message key="label.bouton.rechercher"/>" name="rechercher"/></div>
		</td>
		<td width="25%">
			<div class="navigation-action"><input type="submit" value="<fmt:message key="label.bouton.effacer"/>" name="effacer" /></div>		
		</td>
		<td width="25%">&nbsp;</td>
	</tr>
</table>
<!-- Fin Boutons -->
