# pdf-sign-check
A java/spring boot application to help you check your signed pdf documents. You upload the PDF file and you'll get its signing information. For more info on digital signing PDFs visit: https://developers.itextpdf.com/content/itext-7-digital-signatures-pdf. Please notice that this is a highly specialized app; if you need it, you'll know it!

## Rationale

The organization I work for is a heavy user of signed PDF documents mainly for verification, timestamping and non-repudiation. Actually, all the internal correspondence is done using signed PDF documents and also many internal applications use signed PDF documents confirm various actions. This application offers a REST API so that other apps can easily test if a document is properly signed or not (and get some of the details of the signature). A common workflow is the following:

1. The user does some action in an Application X
1. The Application X generates a PDF document/receipt of that action
1. The user downloads the PDF document and signs it using his USB key
1. The user uploads the signed PDF document
1. Application X uses this project (pdf-sign-check) to make sure that the uploaded document is properly signed and has the correct details (for example signed by the correct user, some documents may need more than one signature etc)

One thing that may seem strange to people not familiar with digital signatures are steps 2-3-4: Why the user needs  to download the PDF document, sign it externally and re-upload it so it could be checked? Unfortunately signing PDF documents is possible only through client-side application (Adobe Acrobat, JSignPDF, custom client side apps etc) the whole download pdf — sign it — re-upload it is necessary.

## Installation

This is a spring boot application thus you should follow the instructions of the spring boot project: https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html. The app doesn't have any external dependencies like databases etc, you just upload the PDF and get the response, nothing is saved or triggered.

You can configure the listening port and the max file size using `src/main/resourecs/application.properties` (or by
overriding these properties f.e like proposed here: https://spapas.github.io/2016/03/31/spring-boot-settings/).

We run this in production using the integration with init services in linux as proposed here: https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html#deployment-install.
You can create a standalone runnable `.jar` (just use `package.bat`) and copy it to the prod server. Then just create a soflink from
/etc/init.d/signcheck -> to your jar. You should then be able to run it as an .sh script for example:

```
[serafeim@prod ~]$ /etc/init.d/signcheck
Usage: /etc/init.d/signcheck {start|stop|force-stop|restart|force-reload|status|run}
```  

Activate it to autorun on boot using the proper tool (f.e `chkconfig` on Centos).

## Running it in development

You can run the project using maven:

```
mvn spring-boot:run -DaddResources=True
```

You can then visit the application at http://127.0.0.1:8081 to see the (really simple) web interface or call it through the REST API. Properties can be configured through ``src/main/resources/application.properties``.

## Usage

Using the API:

```
curl -X POST http://172.19.130.142:8081/ -F file=@test.pdf -F json=on
```
or with python requests:

```
r =  requests.post('http://localhost:8081', files = {'file': file.read(), }, data={'json': 'on'} )
```

Or just visit the site and use the web interface.
