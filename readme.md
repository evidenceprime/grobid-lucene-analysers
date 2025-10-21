Build:

> mvn clean install

Build with Docker:
> docker run -v $(pwd):/app -w /app -it maven:3.9.11-eclipse-temurin-8 mvn clean install

Library is built under `grobid-lucene-analysers-0.0.1.jar`. 

This library is distributed under [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0). 

