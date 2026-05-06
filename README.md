An application using Java to interact with Kraken Websocket API. Application use Java 17 and Maven 3.9.8

How to run:
- run docker-compose up -d to start kafka

endpoints:
- /v1/kraken/connect/public: connect to public websocket
- /v1/kraken/close/public: close connection
- v1/kraken/unsubscribe: unsubsribe
- v1/kraken/subscribe

Sample request:
curl --location 'http://localhost:8080/v1/kraken/subscribe' \
--form 'channel="book"' \
--form 'symbol="ALGO/USD"'
