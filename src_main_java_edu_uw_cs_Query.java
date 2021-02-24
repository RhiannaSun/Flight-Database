package edu.uw.cs;

import com.sun.tools.javac.comp.Check;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.xml.bind.*;

/**
 * Runs queries against a back-end database
 */

public class Query {
  // DB Connection
  private Connection conn;

  // Password hashing parameter constants
  private static final int HASH_STRENGTH = 65536;
  private static final int KEY_LENGTH = 128;



  private String onlineUser = null;

  private List<Itinerary> itineraries = new ArrayList<Itinerary>();

  // Canned queries
  private static final String CHECK_FLIGHT_CAPACITY = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement checkFlightCapacityStatement;

  private static final String CHECK_USER = "SELECT COUNT(*) AS cnt FROM Users WHERE username = ? AND password = ?";
  private PreparedStatement checkUserStatement;

  private static final String CREATE_CUSTOMER = "INSERT INTO Users values (?, ?, ?)";
  private PreparedStatement createCustomerStatement;

  private static final String SEARCH_FLIGHTS= "SELECT TOP (?) * FROM Flights "
          + "WHERE Origin_City = ? AND Dest_City = ? AND Day_of_Month =  ? "
          + "AND canceled <> 1 ORDER BY Actual_Time ASC, fid ASC;";
  private PreparedStatement searchFlightsStatement;

  private static final String SEARCH_INDIRECT_FLIGHTS=
          "SELECT TOP (?) * FROM Flights AS F1, Flights AS F2 "
                  + "WHERE F1.Origin_City = ? AND F2.Dest_City = ? AND F1.Dest_City = F2.Origin_City "
                  + "AND F1.Day_of_Month = ? AND F2.Day_of_Month = ? AND F1.canceled <> 1 AND F2.canceled <> 1 "
                  + "ORDER BY (F1.Actual_Time + F2.Actual_Time), F1.Fid ASC;";
  private PreparedStatement searchIndirectFlightsStatement;

  private static final String CHECK_RESERVATIONS_DATE =
          "SELECT * FROM Reservations WHERE username = ? AND date = ?;";
  private PreparedStatement checkReservationDateStatement;

  private static final String CHECK_CAPACITY_EXIST = "SELECT Count(*) FROM CapacityInfo WHERE fid = ?";
  private PreparedStatement checkCapacityExistStatement;

  private static final String CHECK_CAPACITY = "SELECT curCapacity FROM CapacityInfo WHERE fid = ?";
  private PreparedStatement checkCapacityStatement;

  private static final String UPDATE_CAPACITY =  "UPDATE CapacityInfo "
          + "SET curCapacity = ? WHERE fid = ?;";
  private PreparedStatement updateCapacityStatement;

  private static final String INSERT_CAPACITY = "INSERT INTO CapacityInfo values (?, ?)";
  private PreparedStatement insertCapacityStatement;

  private static final String GET_RESERVATIONS_NUMBER = "SELECT Count FROM ReservationsCount";
  private PreparedStatement getReservationNumberStatement;

  private static final String UPDATE_RESERVATIONS_NUMBER = "UPDATE ReservationsCount "
          + "SET Count = ?;";
  private PreparedStatement updateReservationNumberStatement;

  private static final String ADD_RESERVATION =
          "INSERT INTO Reservations values (?,?,?,?,?,?,?,?)";
  private PreparedStatement addReservationStatement;

  private static final String CHECK_RESERVATION = "SELECT * FROM Reservations WHERE username = ? AND rid = ?;";
  private PreparedStatement checkReservationStatement;

  private static final String CHECK_ALL_RESERVATION = "SELECT * FROM Reservations WHERE username = ?;";
  private PreparedStatement checkAllReservationsStatement;

  private static final String CHECK_BALANCE = "SELECT balance FROM Users WHERE username = ?;";
  private PreparedStatement checkBalanceStatement;

  private static final String UPDATE_BALANCE = "UPDATE Users SET balance = ? WHERE username = ?;";
  private PreparedStatement updateBalanceStatement;

  private static final String CHECK_PAID = "SELECT paid FROM Reservations WHERE rid = ?";
  private PreparedStatement checkPaidStatement;

  private static final String UPDATE_PAID = "UPDATE Reservations SET paid = ? WHERE rid = ?;";
  private PreparedStatement updatePaidStatement;

  private static final String GET_FLIGHT = "SELECT * FROM Flights WHERE Fid = ?;";
  private PreparedStatement getFlightStatement;

  private static final String CANCEL_FLIGHT = "UPDATE Reservations SET canceled = 0 WHERE rid = ?;";
  private PreparedStatement cancelFlightStatement;

  private static final String CHECK_CANCEL = "SELECT canceled FROM Reservations WHERE rid = ?";
  private PreparedStatement checkCancelStatement;
  /**
   * Establishes a new application-to-database connection. Uses the
   * dbconn.properties configuration settings
   *
   * @throws IOException
   * @throws SQLException
   */
  public void openConnection() throws IOException, SQLException {
    // Connect to the database with the provided connection configuration
    Properties configProps = new Properties();
    configProps.load(new FileInputStream("dbconn.properties"));
    String serverURL = configProps.getProperty("hw1.server_url");
    String dbName = configProps.getProperty("hw1.database_name");
    String adminName = configProps.getProperty("hw1.username");
    String password = configProps.getProperty("hw1.password");
    String connectionUrl = String.format("jdbc:sqlserver://%s:1433;databaseName=%s;user=%s;password=%s", serverURL,
            dbName, adminName, password);
    conn = DriverManager.getConnection(connectionUrl);

    // By default, automatically commit after each statement
    conn.setAutoCommit(true);

    // By default, set the transaction isolation level to serializable
    conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
  }

  /**
   * Closes the application-to-database connection
   */
  public void closeConnection() throws SQLException {
    conn.close();
  }

  /**
   * Clear the data in any custom tables created.
   *
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() {
    try {
      Statement clearStatement = conn.createStatement();
      clearStatement.executeUpdate("DELETE FROM Reservations");
      clearStatement.executeUpdate("DELETE FROM Users");
      clearStatement.executeUpdate("DELETE FROM CapacityInfo");
      updateReservationNumberStatement.setInt(1,1);
      updateReservationNumberStatement.executeUpdate();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  public void prepareStatements() throws SQLException {
    checkFlightCapacityStatement = conn.prepareStatement(CHECK_FLIGHT_CAPACITY);

    checkUserStatement = conn.prepareStatement(CHECK_USER);

    createCustomerStatement = conn.prepareStatement(CREATE_CUSTOMER);
    searchFlightsStatement = conn.prepareStatement(SEARCH_FLIGHTS);
    searchIndirectFlightsStatement = conn.prepareStatement(SEARCH_INDIRECT_FLIGHTS);
    checkReservationDateStatement = conn.prepareStatement(CHECK_RESERVATIONS_DATE);
    addReservationStatement = conn.prepareStatement(ADD_RESERVATION);
    checkCapacityExistStatement = conn.prepareStatement(CHECK_CAPACITY_EXIST);
    checkCapacityStatement = conn.prepareStatement(CHECK_CAPACITY);
    updateCapacityStatement = conn.prepareStatement(UPDATE_CAPACITY);
    insertCapacityStatement = conn.prepareStatement(INSERT_CAPACITY);
    getReservationNumberStatement = conn.prepareStatement(GET_RESERVATIONS_NUMBER);
    updateReservationNumberStatement = conn.prepareStatement(UPDATE_RESERVATIONS_NUMBER);
    checkReservationStatement = conn.prepareStatement(CHECK_RESERVATION);
    checkBalanceStatement = conn.prepareStatement(CHECK_BALANCE);
    updateBalanceStatement = conn.prepareStatement(UPDATE_BALANCE);
    checkPaidStatement = conn.prepareStatement(CHECK_PAID);
    updatePaidStatement = conn.prepareStatement(UPDATE_PAID);
    checkAllReservationsStatement = conn.prepareStatement(CHECK_ALL_RESERVATION);
    getFlightStatement = conn.prepareStatement(GET_FLIGHT);
    cancelFlightStatement = conn.prepareStatement(CANCEL_FLIGHT);
    checkCancelStatement = conn.prepareStatement(CHECK_CANCEL);
  }

  /**
   * Takes a user's username and password and attempts to log the user in.
   *
   * @param username user's username
   * @param password user's password
   *
   * @return If someone has already logged in, then return "User already logged
   *         in\n" For all other errors, return "Login failed\n". Otherwise,
   *         return "Logged in as [username]\n".
   */
  public String transaction_login(String username, String password) {
    // case insensitive
    String lcUsername = username.toLowerCase();
    String lcPassword = password.toLowerCase();
    try {
      // check if already online
      if (onlineUser != null) {
        return "User already logged in\n";
      }
      // if not online, check if existed
      checkUserStatement.clearParameters();
      checkUserStatement.setString(1, lcUsername);
      checkUserStatement.setString(2, lcPassword);
      ResultSet results = checkUserStatement.executeQuery();
      results.next();
      int cnt = results.getInt("cnt");
      results.close();
      // check valid
      if (cnt != 1) {
        return "Login failed\n";
      }
    } catch (Exception e) {
      return "Login failed\n";
    }
    onlineUser = username.toLowerCase();
    itineraries = new ArrayList<>();
    return "Logged in as " + username + "\n";
  }

  /**
   * Implement the create user function.
   *
   * @param username   new user's username. User names are unique the system.
   * @param password   new user's password.
   * @param initAmount initial amount to deposit into the user's account, should
   *                   be >= 0 (failure otherwise).
   *
   * @return either "Created user {@code username}\n" or "Failed to create user\n"
   *         if failed.
   */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    try {
      // check the initial amount
      if (initAmount < 0) {
        return "Failed to create user\n";
      }
      String lcUsername = username.toLowerCase();
      String lcPassword = password.toLowerCase();

      createCustomerStatement.clearParameters();
      createCustomerStatement.setString(1, lcUsername);
      createCustomerStatement.setString(2, lcPassword);
      createCustomerStatement.setInt(3, initAmount);
      createCustomerStatement.executeUpdate();
    } catch (Exception e) {
      return "Failed to create user\n";
    }
    return "Created user " + username + "\n";
  }

  public Itinerary flightInfo(ResultSet flightinfo, boolean direct) throws SQLException {
    Itinerary it = new Itinerary();
    if (direct) {
      int fid = flightinfo.getInt(1);
      int dayOfMonth = flightinfo.getInt(3);
      String carrierId = flightinfo.getString(5);
      String flightNum = flightinfo.getString(6);
      String originCity = flightinfo.getString(7);
      String destCity = flightinfo.getString(9);
      int time = flightinfo.getInt(15);
      int capacity = flightinfo.getInt(17);
      int price = flightinfo.getInt(18);
      Flight flight1 = new Flight(fid, dayOfMonth, carrierId, flightNum, originCity,destCity, time, capacity,price);
      it.flight1 = flight1;
      it.flight2 = null;
      it.totalTime=time;
      it.size = 1;
    } else {
      int fid1 = flightinfo.getInt(1);
      int fid2 = flightinfo.getInt(19);
      int dayOfMonth = flightinfo.getInt(3);
      String carrierId1 = flightinfo.getString(5);
      String carrierId2 = flightinfo.getString(23);
      String flightNum1 = flightinfo.getString(6);
      String flightNum2 = flightinfo.getString(24);
      String originCity1 = flightinfo.getString(7);
      String originCity2 = flightinfo.getString(25);
      String destCity1 = flightinfo.getString(9);
      String destCity2 = flightinfo.getString(27);
      int time1 = flightinfo.getInt(15);
      int time2 = flightinfo.getInt(33);
      int capacity1 = flightinfo.getInt(17);
      int capacity2 = flightinfo.getInt(35);
      int price1 = flightinfo.getInt(18);
      int price2 = flightinfo.getInt(36);
      Flight a = new Flight(fid1, dayOfMonth, carrierId1, flightNum1, originCity1, destCity1, time1, capacity1,
              price1);
      Flight b = new Flight(fid2, dayOfMonth, carrierId2, flightNum2, originCity2, destCity2, time2, capacity2,
              price2);
      it.flight1 = a;
      it.flight2 = b;
      it.totalTime = time1+time2;
      it.size = 2;
    }
    return it;
  }
  /**
   * Implement the search function.
   *
   * Searches for flights from the given origin city to the given destination
   * city, on the given day of the month. If {@code directFlight} is true, it only
   * searches for direct flights, otherwise is searches for direct flights and
   * flights with two "hops." Only searches for up to the number of itineraries
   * given by {@code numberOfItineraries}.
   *
   * The results are sorted based on total flight time.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight        if true, then only search for direct flights,
   *                            otherwise include indirect flights as well
   * @param dayOfMonth
   * @param numberOfItineraries number of itineraries to return
   *
   * @return If no itineraries were found, return "No flights match your
   *         selection\n". If an error occurs, then return "Failed to search\n".
   *
   *         Otherwise, the sorted itineraries printed in the following format:
   *
   *         Itinerary [itinerary number]: [number of flights] flight(s), [total
   *         flight time] minutes\n [first flight in itinerary]\n ... [last flight
   *         in itinerary]\n
   *
   *         Each flight should be printed using the same format as in the
   *         {@code Flight} class. Itinerary numbers in each search should always
   *         start from 0 and increase by 1.
   *
   * @see Flight#toString()
   */
  public String transaction_search(String originCity, String destinationCity, boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries) {
    // WARNING the below code is unsafe and only handles searches for direct flights
    // You can use the below code as a starting reference point or you can get rid
    // of it all and replace it with your own implementation.

    // clean the last search result
    itineraries = new ArrayList<Itinerary>();

    StringBuffer sb = new StringBuffer();

    try {
      // one direct itineraries
      searchFlightsStatement.clearParameters();
      searchFlightsStatement.setInt(1, numberOfItineraries);
      searchFlightsStatement.setString(2, originCity);
      searchFlightsStatement.setString(3, destinationCity);
      searchFlightsStatement.setInt(4, dayOfMonth);
      ResultSet oneDirectResults = searchFlightsStatement.executeQuery();
      int count = 0;
      while (oneDirectResults.next()) {
        count = count + 1;

        Itinerary result = flightInfo(oneDirectResults, true);
        // add it to the local research result
        itineraries.add(result);
      }
      oneDirectResults.close();
      if (!directFlight && count < numberOfItineraries) {
        searchIndirectFlightsStatement.clearParameters();
        searchIndirectFlightsStatement.setInt(1, numberOfItineraries-count);
        searchIndirectFlightsStatement.setString(2, originCity);
        searchIndirectFlightsStatement.setString(3, destinationCity);
        searchIndirectFlightsStatement.setInt(4, dayOfMonth);
        searchIndirectFlightsStatement.setInt(5, dayOfMonth);
        ResultSet oneHopResults = searchIndirectFlightsStatement.executeQuery();
        while (oneHopResults.next()&&count < numberOfItineraries) {
          count = count + 1;
          Itinerary result = flightInfo(oneHopResults, false);
          itineraries.add(result);
        }
        oneHopResults.close();
      }
      if (count == -1) {
        sb.append("No flights match your selection\n");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    Collections.sort(itineraries);
    for (int i = 0; i < itineraries.size(); i++) {
      Itinerary it = itineraries.get(i);
      sb.append("Itinerary "+ i + ": "+ it.size + " flight(s), "+ it.totalTime +" minutes\n");
      sb.append(it.flight1.toString());
      if (it.size == 2) {
        sb.append(it.flight2.toString());
      }
    }
    return sb.toString();
  }

  /**
   * Implements the book itinerary function.
   *
   * @param itineraryId ID of the itinerary to book. This must be one that is
   *                    returned by search in the current session.
   *
   * @return If the user is not logged in, then return "Cannot book reservations,
   *         not logged in\n". If try to book an itinerary with invalid ID, then
   *         return "No such itinerary {@code itineraryId}\n". If the user already
   *         has a reservation on the same day as the one that they are trying to
   *         book now, then return "You cannot book two flights in the same
   *         day\n". For all other errors, return "Booking failed\n".
   *
   *         And if booking succeeded, return "Booked flight(s), reservation ID:
   *         [reservationId]\n" where reservationId is a unique number in the
   *         reservation system that starts from 1 and increments by 1 each time a
   *         successful reservation is made by any user in the system.
   */
  public String transaction_book(int itineraryId) {
    // check login
    if (onlineUser == null) {
      return "Cannot book reservations, not logged in\n";
    }
    // check no results
    if (itineraries.size() < 1) {
      return "No such itinerary " + itineraryId + "\n";
    }
    // check invalid itineraryId
    if (itineraries.size()-1 < itineraryId) {
      return "No such itinerary " + itineraryId + "\n";
    }

    try {
      Itinerary it = itineraries.get(itineraryId);

      // check no same date reservations
      int date = it.flight1.dayOfMonth;
      checkReservationDateStatement.clearParameters();
      checkReservationDateStatement.setString(1, onlineUser);
      checkReservationDateStatement.setInt(2, date);
      ResultSet result = checkReservationDateStatement.executeQuery();
      int count = 0;
      while (result.next()) {
        int canceled = result.getInt("canceled");
        if (canceled == 1) {
          // if not canceled
          count++;
        }
      }
      if (count > 0) {
        return "You cannot book two flights in the same day\n";
      }

      // check flight capacity
      int flight1 = it.flight1.fid;

      checkCapacityExistStatement.clearParameters();
      checkCapacityExistStatement.setInt(1, flight1);
      ResultSet a = checkCapacityExistStatement.executeQuery();
      a.next();
      int exist = a.getInt(1);
      a.close();
      if (exist == 0) {
        int capacity = 0;
        capacity = checkFlightCapacity(flight1);
        insertCapacityStatement.clearParameters();
        insertCapacityStatement.setInt(1, flight1);
        insertCapacityStatement.setInt(2, capacity);
        insertCapacityStatement.executeUpdate();
      }

      // check booked or not with the CapacityInfo
      checkCapacityStatement.clearParameters();
      checkCapacityStatement.setInt(1, flight1);
      ResultSet r = checkCapacityStatement.executeQuery();
      r.next();
      int capacity = r.getInt("curCapacity");
      r.close();
      if (capacity <= 0) {
        return "Booking failed\n";
      }
      if (it.size == 2) {
        int flight2 = it.flight2.fid;
        checkCapacityStatement.clearParameters();
        checkCapacityStatement.setInt(1, flight2);
        ResultSet r2 = checkCapacityStatement.executeQuery();
        if (capacity <= 0) {
          return "Booking failed\n";
        }
      }

      // update capacity
      updateCapacityStatement.clearParameters();
      updateCapacityStatement.setInt(1, capacity - 1);
      updateCapacityStatement.setInt(2, it.flight1.fid);
      updateCapacityStatement.executeUpdate();
      if (it.size == 2) {
        updateCapacityStatement.clearParameters();
        updateCapacityStatement.setInt(1, capacity - 1);
        updateCapacityStatement.setInt(2, it.flight2.fid);
        updateCapacityStatement.executeUpdate();
      }

      // add reservation to table
      // get the index
      ResultSet cnts = getReservationNumberStatement.executeQuery();
      cnts.next();
      int reservationCount = cnts.getInt(1);
      cnts.close();
      updateReservationNumberStatement.clearParameters();
      updateReservationNumberStatement.setInt(1, reservationCount+1);
      updateReservationNumberStatement.executeUpdate();

      addReservationStatement.clearParameters();
      addReservationStatement.setString(1, onlineUser.toLowerCase());
      addReservationStatement.setInt(2, date);
      addReservationStatement.setInt(3, flight1);
      if (it.size == 2) {
        addReservationStatement.setInt(4, it.flight2.fid);
        addReservationStatement.setInt(8, it.flight1.price+it.flight2.price);
      } else {
        addReservationStatement.setInt(4, -1);
        addReservationStatement.setInt(8, it.flight1.price);
      }
      addReservationStatement.setInt(5, 0);
      // if cancel == 1, then this reservation is not canceled
      addReservationStatement.setInt(6, 1);
      addReservationStatement.setInt(7, reservationCount);
      addReservationStatement.executeUpdate();
      return "Booked flight(s), reservation ID: " + reservationCount + "\n";
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return "Booking failed\n";
  }

  /**
   * Implements the pay function.
   *
   * @param reservationId the reservation to pay for.
   *
   * @return If no user has logged in, then return "Cannot pay, not logged in\n"
   *         If the reservation is not found / not under the logged in user's
   *         name, then return "Cannot find unpaid reservation [reservationId]
   *         under user: [username]\n" If the user does not have enough money in
   *         their account, then return "User has only [balance] in account but
   *         itinerary costs [cost]\n" For all other errors, return "Failed to pay
   *         for reservation [reservationId]\n"
   *
   *         If successful, return "Paid reservation: [reservationId] remaining
   *         balance: [balance]\n" where [balance] is the remaining balance in the
   *         user's account.
   */
  public String transaction_pay(int reservationId) {
    try {
      if (onlineUser == null) {
        return "Cannot pay, not logged in\n";
      }

      // check if the reservation exsist
      checkReservationStatement.clearParameters();
      checkReservationStatement.setString(1, onlineUser.toLowerCase());
      checkReservationStatement.setInt(2, reservationId);
      ResultSet r1 = checkReservationStatement.executeQuery();
      if (r1.next()) {
        int paid = r1.getInt("paid");
        int cost = r1.getInt("cost");
        int canceled = r1.getInt("canceled");
        // if already paid or canceled, failed
        if (paid == 1 || canceled == 0) {
          return "Cannot find unpaid reservation " + reservationId + " under user: " + onlineUser + "\n";
        }

        r1.close();
        // check balance
        checkBalanceStatement.clearParameters();
        checkBalanceStatement.setString(1, onlineUser.toLowerCase());
        ResultSet r2 = checkBalanceStatement.executeQuery();
        r2.next();
        int balance = r2.getInt(1);
        r2.close();
        if (balance < cost) {
          // if not enough balance failed
          return "User has only " + balance + " in account but itinerary costs " + cost + "\n";
        } else {
          // if enough balance, take off the cost from balance, and set to paid.
          int remaining = balance - cost;
          updateBalanceStatement.clearParameters();
          updateBalanceStatement.setInt(1, remaining);
          updateBalanceStatement.setString(2, onlineUser.toLowerCase());
          updateBalanceStatement.executeUpdate();

          updatePaidStatement.clearParameters();
          updatePaidStatement.setInt(1, 1);
          updatePaidStatement.setInt(2, reservationId);
          updatePaidStatement.executeUpdate();

          return "Paid reservation: " + reservationId + " remaining balance: " + remaining + "\n";
        }
      } else {
        return "Cannot find unpaid reservation " + reservationId + " under user: " + onlineUser + "\n";
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return "Failed to pay for reservation " + reservationId + "\n";
  }

  /**
   * Implements the reservations function.
   *
   * @return If no user has logged in, then return "Cannot view reservations, not
   *         logged in\n" If the user has no reservations, then return "No
   *         reservations found\n" For all other errors, return "Failed to
   *         retrieve reservations\n"
   *
   *         Otherwise return the reservations in the following format:
   *
   *         Reservation [reservation ID] paid: [true or false]:\n" [flight 1
   *         under the reservation] [flight 2 under the reservation] Reservation
   *         [reservation ID] paid: [true or false]:\n" [flight 1 under the
   *         reservation] [flight 2 under the reservation] ...
   *
   *         Each flight should be printed using the same format as in the
   *         {@code Flight} class.
   *
   * @see Flight#toString()
   */
  public String transaction_reservations() {
    if (onlineUser == null) {
      return "Cannot view reservations, not logged in\n";
    }

    StringBuffer sb = new StringBuffer();

    try {
      checkAllReservationsStatement.clearParameters();
      checkAllReservationsStatement.setString(1, onlineUser);
      ResultSet r1 = checkAllReservationsStatement.executeQuery();
      int count = 0;
      while (r1.next()) {
        // if cancel, dont show
        int canceled = r1.getInt("canceled");
        if (canceled == 1) {
          count++;
          int rid = r1.getInt("rid");
          int paid = r1.getInt("paid");
          if (paid == 1) {
            sb.append("Reservation " + rid + " paid: true:\n");
          } else {
            sb.append("Reservation " + rid + " paid: false:\n");
          }
          int flight1 = r1.getInt("fid1");
          int flight2 = r1.getInt("fid2");
          getFlightStatement.clearParameters();
          getFlightStatement.setInt(1, flight1);
          ResultSet r2 = getFlightStatement.executeQuery();
          r2.next();
          Itinerary f1 = flightInfo(r2, true);
          sb.append(f1.flight1.toString());
          if (flight2 != -1) {
            getFlightStatement.clearParameters();
            getFlightStatement.setInt(1, flight2);
            ResultSet r3 = getFlightStatement.executeQuery();
            r3.next();
            Itinerary f2 = flightInfo(r3, true);
            sb.append(f2.flight1.toString());
          }
        }
      }
      r1.close();
      if (count == 0) {
        return "No reservations found\n";
      }
      return sb.toString();
    } catch (SQLException e) {

      return "Failed to retrieve reservations\n";
    }
  }

  /**
   * Implements the cancel operation.
   *
   * @param reservationId the reservation ID to cancel
   *
   * @return If no user has logged in, then return "Cannot cancel reservations,
   *         not logged in\n" For all other errors, return "Failed to cancel
   *         reservation [reservationId]\n"
   *
   *         If successful, return "Canceled reservation [reservationId]\n"
   *
   *         Even though a reservation has been canceled, its ID should not be
   *         reused by the system.
   */
  public String transaction_cancel(int reservationId) {
    // TODO: YOUR CODE HERE
    if (onlineUser == null) {
      return "Cannot cancel reservations, not logged in\n";
    }

    try {
      // if not canceled
      if (!checkReservationsCancel(reservationId)) {
        //cancel in reservation table
        cancelFlightStatement.clearParameters();
        cancelFlightStatement.setInt(1, reservationId);
        cancelFlightStatement.executeUpdate();

        // if paid, refund
        int paid = checkReservationsPaid(reservationId);
        if (paid > 0) {
          // update paid status on reservations table
          updatePaidStatement.clearParameters();
          updatePaidStatement.setInt(1, 0);
          updatePaidStatement.setInt(2, reservationId);
          updatePaidStatement.executeUpdate();

          // get previous balance
          checkBalanceStatement.clearParameters();
          checkBalanceStatement.setString(1, onlineUser.toLowerCase());
          ResultSet r2 = checkBalanceStatement.executeQuery();
          r2.next();
          int balance = r2.getInt(1);
          r2.close();

          // refund balance
          updateBalanceStatement.clearParameters();
          updateBalanceStatement.setInt(1, balance+paid);
          updateBalanceStatement.setString(2, onlineUser.toLowerCase());
          updateBalanceStatement.executeUpdate();
        }

        // restore the capacity
        checkReservationStatement.clearParameters();
        checkReservationStatement.setString(1, onlineUser.toLowerCase());
        checkReservationStatement.setInt(2, reservationId);
        ResultSet r1 = checkReservationStatement.executeQuery();
        r1.next();
        int flight1 = r1.getInt("fid1");
        int flight2 = r1.getInt("fid2");
        addFlightCapacity(flight1);
        if (flight2 != -1) {
          addFlightCapacity(flight2);
        }
        r1.close();
        return "Canceled reservation " + reservationId + "\n";
      } else {
        return "Failed to cancel reservation " + reservationId + "\n";
      }
    } catch (SQLException e) {
      return "Failed to cancel reservation " + reservationId + "\n";
    }
  }

  private void addFlightCapacity(int fid) throws SQLException {
    checkCapacityStatement.clearParameters();
    checkCapacityStatement.setInt(1, fid);
    ResultSet r = checkCapacityStatement.executeQuery();
    r.next();
    int cap = r.getInt("curCapacity");
    r.close();
    updateCapacityStatement.clearParameters();
    updateCapacityStatement.setInt(1, cap + 1);
    updateCapacityStatement.setInt(2, fid);
    updateCapacityStatement.executeUpdate();
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    checkFlightCapacityStatement.clearParameters();
    checkFlightCapacityStatement.setInt(1, fid);
    ResultSet results = checkFlightCapacityStatement.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }

  // return true if the flight is canceled.
  private boolean checkReservationsCancel(int rid) throws SQLException {
    checkReservationStatement.clearParameters();
    checkReservationStatement.setString(1, onlineUser.toLowerCase());
    checkReservationStatement.setInt(2, rid);
    ResultSet results = checkReservationStatement.executeQuery();
    results.next();
    int canceled = results.getInt("canceled");
    results.close();
    return (canceled == 0);
  }

  private int checkReservationsPaid(int rid) throws SQLException {
    checkReservationStatement.clearParameters();
    checkReservationStatement.setString(1, onlineUser.toLowerCase());
    checkReservationStatement.setInt(2, rid);
    ResultSet results = checkReservationStatement.executeQuery();
    results.next();
    int paid = results.getInt("paid");
    int paidAmount = results.getInt("cost");
    results.close();
    if (paid == 1) {
      return paidAmount;
    } else {
      return 0;
    }
  }
   /**
   * A class to store flight information.
   */

  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    public Flight (int input_fid, int input_dayOfMonth, String input_carrierId, String input_flightNum,
                   String input_originCity, String input_destCity, int input_time, int input_capacity, int input_price) {
      this.fid = input_fid;
      this.dayOfMonth = input_dayOfMonth;
      this.carrierId = input_carrierId;
      this.flightNum = input_flightNum;
      this.originCity = input_originCity;
      this.destCity = input_destCity;
      this.time = input_time;
      this.capacity = input_capacity;
      this.price = input_price;
    }

    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
              + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
              + " Capacity: " + capacity + " Price: " + price + "\n";
    }
  }

  class Itinerary implements Comparable<Itinerary> {
    public Flight flight1;
    public Flight flight2;
    public int size;
    public int totalTime;

    public int compareTo(Itinerary other) {
      if (totalTime != other.totalTime) {
        return totalTime - other.totalTime;
      } else if (flight1.fid != other.flight1.fid) {
        return flight1.fid-other.flight1.fid;
      } else {
        return flight2.fid-other.flight2.fid;
      }

    }

  }
}
