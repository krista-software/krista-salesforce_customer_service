[<span style = "color : blue ; text-decoration: none">< Back<span>](/)

# Connecting Krista Salesforce Customer Service Module Extension

### Steps for connection

To set up the connection, you must provide the **Client ID**, **Client Secret**, **Username**, **Password**, and **Security Token** in the extension setup (obtained from the Salesforce portal). Optionally, you can also provide the **Instance URL** and set the **Sandbox** flag if you are connecting to a Salesforce Sandbox.
![Salesforce Marketing Attributes](../_media/customerServiceAttributes.png)

* Refer to the following tabs for Salesforce Customer Service Extension Attributes.
    * For **Client ID** and **Client Secret**, refer to the **Manage Consumer Details** tab from your registered web application in Salesforce Portal.

  ![Salesforce Marketing Client ID and Client Secret](../_media/clientIdClientSecret.png)
    * For **Username** and **Password**, you can use salesforce login credentials as **username** and **password**.
    * For **Security Token**, you must use the Security Token that you received on your registered email after you reset it on Salesforce system.
    * For **Instance URL** (optional), use your Salesforce domain base URL (for example, https://mydomain.my.salesforce.com). If left blank, the extension will determine it automatically during OAuth.
    * **Sandbox** (optional): Enable this if connecting to a Salesforce Sandbox; leave disabled for Production.


### Authentication flows used by this extension

- If you provide an Instance URL, the extension uses OAuth 2.0 Client Credentials to obtain a token for a designated integration user.
  - Prerequisites in Salesforce Connected App (Manage → Edit Policies): Enable “OAuth 2.0 Client Credentials Flow” and set “Run As” to your Integration User.
  - Instance URL must be your org base domain, for example: https://your-domain.my.salesforce.com
    - Do not use the Lightning domain (…lightning.force.com)
    - Do not include a trailing slash
- If you leave Instance URL blank, the extension guides you through user authorization and stores a refresh token (standard OAuth authorization code flow).
- Username/Password + Security Token is used only as a fallback where OAuth is not configured.

* After you have provided the username, password, client id, client secret, and security token , click on the **Validate Attributes**.


* Once you click on validate , an authentication page will appear. Click on that tab and authorize yourself using the same account which is used to register your application in Salesforce Portal.


* After you are authorized, click on **Save Changes**.


* Now navigate to Diagnosis and click on **Test Connection** to confirm that the connection is established. When the connection is established, you are good to go with the requests of the extension.
