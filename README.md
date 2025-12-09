# Project Description

The following code provides the implementation for the master's thesis: **Coordinating Digital Twin Interactions with Apache Ditto**

## Docker Setup
   To start the project a Docker environment has to be build. All commands must be executed in a terminal. 
1. First clone the repository

   `git clone https://github.com/eclipse/ditto.git`

   The directory contains all the docker compose configurations required.
   All necessary docker images will be downloaded automaticaly after starting the environment.
2. Open directory

   `cd ditto/deployment/docker`

3. Start the environment
   
   To start them the type the following line in the folder directory. Ensure that docker and docker compose are installed.

   `docker-compose up -d`

4. Open Eclipse Ditto UI:
5. 
   `http://localhost:8080/`

6. To stop the environment:

   `docker-compose down`

## InfluxDB Setup

1. Run following comand in the terminal
   
`docker run -d --name influxdb -p 8086:8086 -v influxdb-data:/var/lib/influxdb2 influxdb:2`

This will create a docler image and start the container. Afterwards access the following URL: `localhost:8086`

2. Follow the registration setup.
     - Account name
     - Passwort,
     - Organization
     - Bucket.
   
   Look for the Config file in the provided code and replace:
     - **INFLUX_ORG** -> your organization name
     - **INFLUX_BUCKET** -> your bucket name
   
3. Afterwards a token will be created. Again store the token in the config file:
     - **INFLUX_TOKEN** -> your generated token.

The stored values can be seen in the **Data Explorer** in InfluxDB after running the code and pressing the **submit** button
