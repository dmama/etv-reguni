<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.param.application" /></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/parametrage-application.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="head">
  		
  		<style type="text/css">
  			th.th1 { 
  				width: 55%;  			 
  			}
  			th.th2 { 
  				width: 10%;			 
  			}
  			th.th3 { 
  				width: 10%; 			 
  			}
  			th.th4 { 
  				width: 25%;		 
  			}			
  			tr.separator {
  				height: 3px;
  			}				
  			input.valeur {
  				width: 35px;
  				text-align: right;
  			}
  			.nombre, .date, td.valeur {
  				text-align: right;
  			}	
			td {
				white-space: nowrap;
			}
  		</style>
		
	  	<script type="text/javascript">
		 $(document).ready(function() {
			 /*
			  * Event Handlers
			  */
				$("#save").click( function() {
					$('#action').val("save")
				});
				$("#reset").click( function() {
					$('#action').val("reset")
				});
		 });
		</script>
	</tiles:put>
  	<tiles:put name="body">
 		<form:form method="post">
 			<table>
 				<thead>
 				<tr>
 					<th class="th1"><fmt:message key="label.param.entete.parametre"/></th>
 					<th class="th3"><fmt:message key="label.param.entete.defaut"/></th>
 					<th class="th2"><fmt:message key="label.param.entete.valeur"/></th> 					
					<th class="th4">&nbsp;</th>
 				</tr>
 				</thead>
				<spring:bind path="premierePeriodeFiscale">		 		
			 		<tr class="odd">
			 			<td><fmt:message key="label.param.premierePeriodeFiscale"/></td>
			 			<td class="nombre">NA</td>
			 			<td class="valeur">
			 				<form:input path="premierePeriodeFiscale" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="4" readonly="true"/>
			 			</td>
						<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr><tr class="separator"><th colspan="4" ></th></tr>
		 		</spring:bind>				
				<spring:bind path="noel">		 		
			 		<tr class="even">
			 			<td><fmt:message key="label.param.noel"/></td>
						<td class="date">NA</td>
			 			<td class="valeur">
			 				<form:input path="noel" cssClass="valeur" cssErrorClass="valeur input-with-errors date" maxlength="5" readonly="true"/>
			 			</td>
						<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr>
		 		</spring:bind>
				<spring:bind path="nouvelAn">		 		
			 		<tr class="odd">
			 			<td><fmt:message key="label.param.nouvelAn"/></td>
			 			<td class="date">NA</td>
			 			<td class="valeur">
			 				<form:input path="nouvelAn" cssClass="valeur date" cssErrorClass="valeur input-with-errors date" maxlength="5" readonly="true"/>
			 			</td>
						<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr>
		 		</spring:bind>
				<spring:bind path="lendemainNouvelAn">		 		
			 		<tr class="even">
			 			<td><fmt:message key="label.param.lendemainNouvelAn"/></td>
						<td class="date">NA</td>
			 			<td class="valeur">
			 				<form:input path="lendemainNouvelAn" cssClass="valeur date" cssErrorClass="valeur input-with-errors date" maxlength="5" readonly="true"/>
			 			</td>
						<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr>
		 		</spring:bind>
				<spring:bind path="feteNationale">		 		
			 		<tr class="odd">
			 			<td><fmt:message key="label.param.feteNationale"/></td>
						<td class="date">NA</td>
			 			<td class="valeur">
			 				<form:input path="feteNationale" cssClass="valeur date" cssErrorClass="valeur input-with-errors date" maxlength="5" readonly="true"/>
			 			</td>
						<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr><tr class="separator"><th colspan="4" ></th></tr>
		 		</spring:bind>
				<spring:bind path="nbMaxParListe">		 		
			 		<tr class="even">
			 			<td><fmt:message key="label.param.nbMaxParListe"/></td>
			 			<td class="nombre">${nbMaxParListeParDefaut}</td>
			 			<td class="valeur">
			 				<form:input path="nbMaxParListe" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="4"/>
			 			</td>
						<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr>
		 		</spring:bind>
				<spring:bind path="nbMaxParPage">		 		
			 		<tr class="odd">
			 			<td><fmt:message key="label.param.nbMaxParPage"/></td>
			 			<td class="nombre">${nbMaxParPageParDefaut}</td>
			 			<td class="valeur">
			 				<form:input path="nbMaxParPage" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
			 			</td>
						<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr><tr class="separator"><th colspan="4" ></th></tr>
		 		</spring:bind>				
				<spring:bind path="delaiAttenteDeclarationImpotPersonneDecedee">		 		
			 		<tr class="even">
			 			<td><fmt:message key="label.param.delaiAttenteDeclarationImpotPersonneDecedee"/></td>
						<td class="nombre">${delaiAttenteDeclarationImpotPersonneDecedeeParDefaut}</td>
			 			<td class="valeur">
			 				<form:input path="delaiAttenteDeclarationImpotPersonneDecedee" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
			 			</td>
						<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr>
		 		</spring:bind>
				<spring:bind path="delaiRetourDeclarationImpotEmiseManuellement">		 		
			 		<tr class="odd">
			 			<td><fmt:message key="label.param.delaiRetourDeclarationImpotEmiseManuellement"/></td>
			 			<td class="nombre">${delaiRetourDeclarationImpotEmiseManuellementParDefaut}</td>
			 			<td class="valeur">
			 				<form:input path="delaiRetourDeclarationImpotEmiseManuellement" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
			 			</td>
			 			<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr>
		 		</spring:bind>		 		
				<spring:bind path="delaiCadevImpressionDeclarationImpot">		 		
			 		<tr class="even">
			 			<td><fmt:message key="label.param.delaiCadevImpressionDeclarationImpot"/></td>
			 			<td class="nombre">${delaiCadevImpressionDeclarationImpotParDefaut}</td>
			 			<td class="valeur">
			 				<form:input path="delaiCadevImpressionDeclarationImpot" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
			 			</td>
						<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr>
		 		</spring:bind>		 		
 				<spring:bind path="delaiEnvoiSommationDeclarationImpot">		 		
			 		<tr class="odd">
			 			<td><fmt:message key="label.param.delaiEnvoiSommationDeclarationImpot"/></td>
						<td class="nombre">${delaiEnvoiSommationDeclarationImpotParDefaut}</td>
			 			<td class="valeur">
			 				<form:input path="delaiEnvoiSommationDeclarationImpot" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
			 			</td>
						<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr>
		 		</spring:bind>
				<spring:bind path="delaiEcheanceSommationDeclarationImpot">		
			 		<tr class="even">
			 			<td><fmt:message key="label.param.delaiEcheanceSommationDeclarationImpot"/></td>
						<td class="nombre">${delaiEcheanceSommationDeclarationImpotParDefaut}</td>
			 			<td class="valeur">
			 				<form:input path="delaiEcheanceSommationDeclarationImpot" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
			 			</td>
						<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr>
		 		</spring:bind>
				<spring:bind path="jourDuMoisEnvoiListesRecapitulatives">
			 		<tr class="odd">
			 			<td><fmt:message key="label.param.jourDuMoisEnvoiListesRecapitulatives"/></td>
						<td class="nombre">${jourDuMoisEnvoiListesRecapitulativesParDefaut}</td>
			 			<td class="valeur">
			 				<form:input path="jourDuMoisEnvoiListesRecapitulatives" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="2"/>
			 			</td>
						<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr>
		 		</spring:bind>
				<spring:bind path="delaiCadevImpressionListesRecapitulatives">		 		
			 		<tr class="even">
			 			<td><fmt:message key="label.param.delaiCadevImpressionListesRecapitulatives"/></td>
			 			<td class="nombre">${delaiCadevImpressionListesRecapitulativesParDefaut}</td>
			 			<td class="valeur">
			 				<form:input path="delaiCadevImpressionListesRecapitulatives" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
			 			</td>
			 			<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr>
		 		</spring:bind>
				<spring:bind path="delaiRetourListeRecapitulative">		 		
			 		<tr class="odd">
			 			<td><fmt:message key="label.param.delaiRetourListeRecapitulative"/></td>
			 			<td class="nombre">${delaiRetourListeRecapitulativeParDefaut}</td>
			 			<td class="valeur">
			 				<form:input path="delaiRetourListeRecapitulative" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
			 			</td>
			 			<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr>
		 		</spring:bind>
				<spring:bind path="delaiEnvoiSommationListeRecapitulative">		 		
			 		<tr class="even">
			 			<td><fmt:message key="label.param.delaiEnvoiSommationListeRecapitulative"/></td>
						<td class="nombre">${delaiEnvoiSommationListeRecapitulativeParDefaut}</td>
			 			<td class="valeur">
			 				<form:input path="delaiEnvoiSommationListeRecapitulative" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
			 			</td>
						<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr>
		 		</spring:bind>
				<spring:bind path="delaiRetourSommationListeRecapitulative">		 		
			 		<tr class="odd">
			 			<td><fmt:message key="label.param.delaiRetourSommationListeRecapitulative"/></td>
			 			<td class="nombre">${delaiRetourSommationListeRecapitulativeParDefaut}</td>
			 			<td class="valeur">
			 				<form:input path="delaiRetourSommationListeRecapitulative" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
			 			</td>
						<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr>
		 		</spring:bind>
				<spring:bind path="delaiEcheanceSommationListeRecapitualtive">		 		
			 		<tr class="even">
			 			<td><fmt:message key="label.param.delaiEcheanceSommationListeRecapitualtive"/></td>
			 			<td class="nombre">${delaiEcheanceSommationListeRecapitualtiveParDefaut}</td>
			 			<td class="valeur">
			 				<form:input path="delaiEcheanceSommationListeRecapitualtive" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
			 			</td>
			 			<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr>
		 		</spring:bind>
				<spring:bind path="delaiRetentionRapportTravailInactif">		 		
			 		<tr class="odd">
			 			<td><fmt:message key="label.param.delaiRetentionRapportTravailInactif"/></td>
			 			<td class="nombre">${delaiRetentionRapportTravailInactifParDefaut}</td>
			 			<td class="valeur">
			 				<form:input path="delaiRetentionRapportTravailInactif" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
			 			</td>
			 			<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr><tr class="separator"><th colspan="4" ></th></tr>
		 		</spring:bind>
		 		<spring:bind path="dateExclusionDecedeEnvoiDI">
			 		<tr class="even">
			 			<td><fmt:message key="label.param.dateExclusionDecedeEnvoiDI"/></td>
			 			<td class="date">${dateExclusionDecedeEnvoiDIParDefaut}</td>
			 			<td class="valeur">
			 				<form:input path="dateExclusionDecedeEnvoiDI" cssClass="valeur date" cssErrorClass="valeur input-with-errors date" maxlength="5"/>
			 			</td>
			 			<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr><tr class="separator"><th colspan="4" ></th></tr>
		 		</spring:bind>
				<spring:bind path="anneeMinimaleForDebiteur">
			 		<tr class="odd">
			 			<td><fmt:message key="label.param.anneeMinimaleForDebiteur"/></td>
			 			<td class="nombre">${anneeMinimaleForDebiteurParDefaut}</td>
			 			<td class="valeur">
			 				<form:input path="anneeMinimaleForDebiteur" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="4"/>
			 			</td>
						<td>
							<c:if test="${status.error}">
			 					&nbsp;<span class="erreur">${status.errorMessage}</span>
			 				</c:if>
						</td>
			 		</tr><tr class="separator"><th colspan="4" ></th></tr>
		 		</spring:bind>
 			</table>
 			<input type="hidden" name="action" id="action" value="" />
 			<input type="submit" id="save" value="<fmt:message key="label.param.action.save"/>"/>
 			<input type="submit" id="reset" value="<fmt:message key="label.param.action.reset"/>" />
 		</form:form>
 	</tiles:put>
</tiles:insert>