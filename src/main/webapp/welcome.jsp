<%--
  Created by IntelliJ IDEA.
  User: belozovs
  Date: 1/24/2016
  Time: 4:35 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Welcome to Gaia</title>
</head>
<body>
You are in!

<a href="${pageContext.request.contextPath}/verify">Verify yourself, if you want</a>
<h3>What would you like to do next?</h3>
<ul>
    <li><a href="http://google.com">google.com</a></li>
    <li><a href="http://hpe.com">hpe.com</a></li>
    <li><a href="http://gaia.skydns.local">See your data in Kibana</a></li>
    <li>Do something administrative - not implemented yet</li>
</ul>

</body>
</html>
