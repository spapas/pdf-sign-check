# pdf-sign-check
A java / spring boot application to help you check your signed pdf documents.

Using the API:

curl -X POST http://172.19.130.142:8081/ -F file=@test.pdf -F json=on

or with python requests:

r =  requests.post('http://localhost:8081', files = {'file': file.read(), }, data={'json': 'on'} )

## Rationale

The organization I work for is a heavy user of signed PDF documents mainly for verification, timestamping and non-repudiation. Actually, all the internal correspondance is done using signed PDF documents and also many internal applications use signed PDF documents confirm various actions. This application offers a REST API so that other apps can easily test if a document is properly signed or not (and get some of the details of the signature). A common workflow is the following:

1. The user does some action in an Application X
1. The Application X generates a PDF document / receipt of that action
1. The user downloads the PDF document and signs it using his USB key
1. The user uploads the signed PDF document
1. Application X uses this project (pdf-sign-check) to make sure that the uploaded document is properly signed and has the correct details (for example signed by the correct user, some documents may need more than one signature etc)

One thing that may seem strange to people not familiar with digital signatures are steps 2-3-4: Why the user needs  to download the PDF document, sign it externally and re-upload it so it could be checked? Unfortunately signing PDF documents is possible only through client-side application (Adobe Acrobat, JSignPDF, custom client side apps etc) the whole download pdf - sign it - re-upload it is necessary.

## Installation

This is a spring boot application thus you should follow the instructions of the spring boott project: https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html. The app doesn't have any external dependencies like databases etc, you just upload the PDF and get the response, nothing is saved or triggered.
