# Service Oriented Computing - Project 2

## How to Run

### Play Framework
* Install latest version of sbt: [SBT](http://www.scala-sbt.org/download.html)
* Clone this repository: `git clone https://github.ncsu.edu/ssrivas8/SOCP2`
* cd to `flight-booking` folder: `cd SOCP1/flight-booking`
* Type `sbt run` to run the application.
* For stopping the server, Press the `Enter` key on keyboard.
* For opening the sbt console, type `sbt` from the command prompt.
* There are following path mappings present in the web application:

| Type | mapping | description |
|---|---|---|
| GET | /                                    | The default Play home page |
| GET | /trips                               | Get a list of trips booked.
| GET | /trips/:tripID                       | Get a list of segments of a trip. A segment is represented by its flight
| GET | /operators                           | Get a list of airline operators.
| GET | /operators/:operator/flights         | Get a list of flights operated by an airline operator
| GET | /operators/:operator/flights/:flight | Get the number of available seats on a flight
| POST | /trip/:from/:to                     | Book a trip. Currently, the $from and $to should always be X and Y. If not, return an error.
| POST | /actor/$airline/confirm_fail        | After this request is posted, corresponding airline actor will reply fail to subsequent Confirm requests without actual processing.
| POST | /actor/$airline/confirm_no_response | After this request is posted, corresponding airline actor will not reply to subsequent Confirm requests without actual processing
| POST | /actor/$airline/reset               | After this request is posted, the actor will reset to normal.

**Note: This project is created in windows environment.

