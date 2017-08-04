# pdf-sign-check
A java / spring boot application to help you check for signed pdf documents.

Using the API:

curl -X POST http://172.19.130.142:8081/ -F file=@test.pdf -F json=on
