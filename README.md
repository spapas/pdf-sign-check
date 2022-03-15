
# pdf-sign-check
A java/spring boot application to help you *sign* and *check* your signed pdf documents. 
For checking, you upload the PDF file and you'll get its signing information.
For signing, you upload the PDF file along with some parameters for the signature and
you'll get a digitally signed PDF (you need a proper certificate in a keystore to use this feature).
For more info on digital signing PDFs visit: https://developers.itextpdf.com/content/itext-7-digital-signatures-pdf. Please notice that this is a highly specialized app; if you need it, you'll know it!

## Rationale for checks

The organization I work for is a heavy user of signed PDF documents mainly for verification, timestamping and non-repudiation. Actually, all the internal correspondence is done using signed PDF documents and also many internal applications use signed PDF documents confirm various actions. This application offers a REST API so that other apps can easily test if a document is properly signed or not (and get some of the details of the signature). A common workflow is the following:

1. The user does some action in an Application X
2. The Application X generates a PDF document/receipt of that action
3. The user downloads the PDF document and signs it using his USB key
4. The user uploads the signed PDF document
5. Application X uses this project (pdf-sign-check) to make sure that the uploaded document is properly signed and has the correct details (for example signed by the correct user, some documents may need more than one signature etc)

One thing that may seem strange to people not familiar with digital signatures are steps 2-3-4: Why the user needs  to download the PDF document, sign it externally and re-upload it so it could be checked? Unfortunately signing PDF documents is possible only through client-side application (Adobe Acrobat, JSignPDF, custom client side apps etc) the whole download pdf — sign it — re-upload it is necessary.

## Rationale for signing

It is actually possible to automatically digitally sign a document as a part of the workflow of a 
business process
so as to avoid the download-sign-upload-check steps I described earlier. The produced PDF document 
will be signed by a global key that is owned by your Organization. To do that you need to 
create such a key and make sure that you have fixed the bureaucracy needed so as the documents
that are signed by this key will be valid. So now the flow will be:

1. The user does some action in application A
2. Application X generates a PDF document/receipt of that action
3. Application X automatically uses this project (pdf-sign-check) to add a digital signature to the document
4. The user downloads the signed document and can legally use it wherever he wishes

## Installation

This is a spring boot application thus you should follow the instructions of the spring boot project: https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html. The app doesn't have any external dependencies like databases etc, you just upload the PDF and get the response, nothing is saved or triggered.

You can configure the listening port and the max file size using `src/main/resources/application.properties` (or by
overriding these properties f.e like proposed here: https://spapas.github.io/2016/03/31/spring-boot-settings/).

I recommend creating a config/application.properties that will be used to set
some properties that shouldn't be put under version control (for example your
keystore password or the api key)

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

You can then visit the application at http://127.0.0.1:8081 for check and
http://127.0.0.1:8081/sign for signing
to see the (really simple) 
web interface or call it through the REST API. 
Properties can be configured through ``src/main/resources/application.properties``.

Create a test certificate for signing if needed:

```
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes -subj /CN=MyHost.com
openssl pkcs12 -export -in cert.pem -inkey key.pem -out myfile.p12 -name "Alias of cert"
```

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

## Signing Usage 

Using the API:

```
curl -X POST -F file=@..\test.pdf -F apikey=123  -F "visibleLine1=Digitally signed" -F "visibleLine2="MMAIP" -F "signName=MMAIP" -F "signReason=Identical Copy" -F "signLocation=Piraeus"  http://127.0.0.1:8081/sign --output koko.pdf
```
or with python requests:

```
pdf_content = requests.post('http://localhost:8081/sign', files = {'file': open("../test3.pdf", "rb").read(), }, data={'visibleLine1': 'ΔΟκΙΜΗ ΤΕΣΤ', 'apikey': '123'} ).content
```

## Security

Please notice that checking pdf files is allowed by everybody. However, for signing you need to set a **very strong api key and be very careful about it.** If somebody gets your api key he'll be able to sign *any document* he wants with your organization's signature!
