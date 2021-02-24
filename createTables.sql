#  Constructed the relational tables based on the logical data model 

DROP TABLE IF EXISTS Flights;
DROP TABLE IF EXISTS Carriers;
DROP TABLE IF EXISTS Months;
DROP TABLE IF EXISTS Weekdays;
DROP TABLE IF EXISTS Users;
DROP TABLE IF EXISTS CapacityInfo;
DROP TABLE IF EXISTS Reservations;
DROP TABLE IF EXISTS ReservationsCount;


CREATE TABLE Flights(
    Fid INT PRIMARY KEY,
    Month_ID INT not null REFERENCES Months(Mid),
    Day_of_Month INT not null,
    Day_of_Week_ID INT not null REFERENCES Weekdays(Did),
    Carrier_ID VARCHAR(7) not null REFERENCES Carriers(Cid),
    Flight_Num INT not null,
    Origin_City VARCHAR(34) not null,
    Origin_State VARCHAR(47) not null,
    Dest_City VARCHAR(34) not null,
    Dest_State VARCHAR(46) not null,
    Departure_Delay INT not null,
    Taxi_Out INT not null,
    Arrival_Delay INT not null,
    Canceled INT not null,
    Actual_Time INT not null,
    Distance INT not null,
    Capacity INT not null,
    Price INT not null
);

CREATE TABLE Carriers(
    Cid VARCHAR(7) PRIMARY KEY, 
    Name VARCHAR(83)
);

CREATE TABLE Months(
    Mid INT PRIMARY KEY,
    Month VARCHAR(9)
);

CREATE TABLE Weekdays(
    Did INT PRIMARY KEY,
    Day_of_week VARCHAR(9)
);

CREATE TABLE Users (
    username VARCHAR(20) PRIMARY KEY,
    password VARCHAR(20) not null,
    balance INT not null
)

CREATE TABLE Reservations (
    username VARCHAR(20) not null REFERENCES Users,
    date INT not null,
    fid1 INT not null REFERENCES Flights(Fid),
    fid2 INT,
    paid INT not null,
    canceled INT not null,
    rid INT PRIMARY KEY,
    cost INT not null
)

CREATE TABLE CapacityInfo (
    fid INT not null REFERENCES Flights(Fid),
    curCapacity INT not null
)

CREATE TABLE ReservationsCount ( 
    Count INT not null PRIMARY KEY,
)
INSERT INTO ReservationsCount Values(1);

bulk insert Carriers from 'carriers.csv'
with (ROWTERMINATOR = '0x0a',
DATA_SOURCE = 'cse344blob', FORMAT='CSV', CODEPAGE = 65001, --UTF-8 encoding
FIRSTROW=1,TABLOCK);

bulk insert Months from 'months.csv'
with (ROWTERMINATOR = '0x0a',
DATA_SOURCE = 'cse344blob', FORMAT='CSV', CODEPAGE = 65001, --UTF-8 encoding
FIRSTROW=1,TABLOCK);

bulk insert Weekdays from 'weekdays.csv'
with (ROWTERMINATOR = '0x0a',
DATA_SOURCE = 'cse344blob', FORMAT='CSV', CODEPAGE = 65001, --UTF-8 encoding
FIRSTROW=1,TABLOCK);

-- Import for the large Flights table
-- This last import may take ~5 minutes on the provided server settings
bulk insert Flights from 'flights-small.csv'
with (ROWTERMINATOR = '0x0a',
DATA_SOURCE = 'cse344blob', FORMAT='CSV', CODEPAGE = 65001, --UTF-8 encoding
FIRSTROW=1,TABLOCK);
