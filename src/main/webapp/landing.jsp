<%@ page import="java.util.*" %>
<html>
<body>
<h2>Gaia is so close!</h2>
<a href="${pageContext.request.contextPath}/login.jsp">Click to login</a>
<p>
      <h1>HTTP Request Headers Received</h1>
      <table border="1" cellpadding="4" cellspacing="0">
      <%
         Enumeration eNames = request.getHeaderNames();
         while (eNames.hasMoreElements()) {
            String name = (String) eNames.nextElement();
            String value = request.getHeader(name);
      %>
         <tr><td><%= name %></td><td><%= value %></td></tr>
      <%
         }
      %>
      </table>
</body>
</html>
